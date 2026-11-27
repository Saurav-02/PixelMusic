package com.saurav.pixelmusic.ui.blur

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import android.graphics.Matrix
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy as LayerCompositing
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class BlurQuality { LOW, HIGH }

enum class BlurDirection { TopToBottom, BottomToTop }

private val StrongGlassChips = listOf(
    "sm8350", "sm8450", "sm8475", "sm8550", "sm8635", "sm8650", "sm8750",
    "mt6983", "mt6985", "mt6989", "mt6991",
)

val DefaultBlurQuality: BlurQuality by lazy {
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.lowercase(Locale.ROOT) else ""
    val strong = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S || StrongGlassChips.any { soc.startsWith(it) })
    if (strong) BlurQuality.HIGH else BlurQuality.LOW
}

val LocalBlurQuality = staticCompositionLocalOf { DefaultBlurQuality }
val LocalScreenGlassState = compositionLocalOf<GlassState?> { null }

object BlurEffectCache {
    private val composeCache = ConcurrentHashMap<Int, RenderEffect>()
    private val nativeCache = ConcurrentHashMap<Int, android.graphics.RenderEffect>()

    fun get(
        radius: Float,
        tileMode: Shader.TileMode = Shader.TileMode.CLAMP,
        allowLargeRadius: Boolean = false,
    ): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radius <= 0.5f) return null
        val effectiveRadius = effectiveRadius(radius, allowLargeRadius)
        val quant = quantStep(effectiveRadius)
        val step = (effectiveRadius / quant).roundToInt().coerceAtLeast(1)
        val key = (step * 100) + (quant * 10).toInt() + tileMode.ordinal
        return composeCache.getOrPut(key) {
            val r = step * quant
            android.graphics.RenderEffect
                .createBlurEffect(r, r, tileMode)
                .asComposeRenderEffect()
        }
    }

    fun effectiveRadius(radius: Float, allowLargeRadius: Boolean = false): Float =
        if (Build.VERSION.SDK_INT <= 34 && !allowLargeRadius) radius.coerceAtMost(54f) else radius

    fun quantize(radius: Float): Float {
        val quant = quantStep(radius)
        return (radius / quant).roundToInt().coerceAtLeast(1) * quant
    }

    private fun quantStep(radius: Float): Float = when {
        radius <= 16f -> 0.5f
        radius <= 32f -> 1.0f
        radius <= 64f -> 2.0f
        else -> 4.0f
    }

    /** Drops cached native render effects when the UI hides so Skia filters aren't pinned in RAM. */
    fun clear() {
        composeCache.clear()
        nativeCache.clear()
    }
}

/** Tracks live [GlassState] instances weakly so backgrounding/trim can drop all recorded GPU backdrops at once. */
object GlassBackdropRegistry {
    private val states = Collections.newSetFromMap(WeakHashMap<GlassState, Boolean>())

    internal fun register(state: GlassState) {
        synchronized(states) { states.add(state) }
    }

    fun discardAll() {
        val snapshot = synchronized(states) { states.toList() }
        snapshot.forEach { it.discard() }
    }
}

@Stable
class GlassState {
    var renderNode by mutableStateOf<Any?>(null)
    var rootPosition by mutableStateOf(Offset.Zero)

    init {
        GlassBackdropRegistry.register(this)
    }

    /** Drops the recorded screen display list when the window hides so GPU textures can be reclaimed. */
    fun discard() {
        val node = renderNode ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) glassNodeDiscard(node)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun glassNodeDiscard(node: Any) {
    (node as RenderNode).discardDisplayList()
}

@RequiresApi(Build.VERSION_CODES.Q)
internal fun glassNodeFor(existing: Any?, width: Int, height: Int): Any =
    (existing as? RenderNode ?: RenderNode("PixelGlassRoot")).apply { setPosition(0, 0, width, height) }

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeHasContent(node: Any): Boolean = (node as RenderNode).hasDisplayList()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun android.graphics.Canvas.drawGlassNode(node: Any) {
    drawRenderNode(node as RenderNode)
}

fun Modifier.glassRoot(state: GlassState, isActive: () -> Boolean = { true }): Modifier = this
    .onGloballyPositioned { state.rootPosition = it.positionInWindow() }
    .drawWithCache {
        val width = size.width.toInt()
        val height = size.height.toInt()
        val node: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && width > 0 && height > 0) {
            glassNodeFor(state.renderNode, width, height)
        } else null
        state.renderNode = node

        onDrawWithContent {
            val drawContextCanvas = drawContext.canvas
            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isActive()) {
                val nativeCanvas = glassNodeBeginRecording(node)
                drawContext.canvas = ComposeCanvas(nativeCanvas)
                try {
                    drawContent()
                } finally {
                    drawContext.canvas = drawContextCanvas
                    glassNodeEndRecording(node)
                }
                if (glassNodeHasContent(node)) {
                    drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
                }
            } else {
                drawContent()
            }
        }
    }

internal fun glassDownscale(effectiveRadius: Float, low: Boolean = false): Int = when {
    effectiveRadius >= 20f -> 4
    low && effectiveRadius >= 12f -> 4
    effectiveRadius >= 6f -> 2
    low && effectiveRadius >= 2f -> 2
    else -> 1
}

private const val VariableBlurSource = """
uniform shader content;
uniform float2 dir;
uniform float2 size;
uniform float2 origin;
uniform float2 full;
uniform float maxRadius;
uniform float growDown;
uniform float across;
uniform float ramp;
uniform float preSigma;

half4 main(float2 p) {
    float2 g = (p + origin) / full;
    float along = clamp(mix(g.y, g.x, across), 0.0, 1.0);
    if (growDown < 0.5) { along = 1.0 - along; }
    float t = clamp(along / ramp, 0.0, 1.0);
    float r = maxRadius * t * t;
    float sigma = 0.57735 * r + 0.5;
    float rest = sigma * sigma - preSigma * preSigma;
    if (r < 0.5 || rest < 0.25) { return content.eval(p); }
    float stepPx = sqrt(rest) * 0.5;
    float4 sum = float4(0.0);
    float total = 0.0;
    for (int i = -6; i <= 6; i++) {
        float2 q = p + dir * (float(i) * stepPx);
        float inside = step(0.0, q.x) * step(q.x, size.x) * step(0.0, q.y) * step(q.y, size.y);
        float w = exp(-0.125 * float(i * i)) * inside;
        sum += float4(content.eval(q)) * w;
        total += w;
    }
    return half4(sum / max(total, 0.0001));
}
"""

private object VariableBlurShader {
    var shader: Any? = null
}

private const val PreBlurOfSigma = 0.15f
private const val MinPreSigma = 0.6f
private const val BandGrid = 4f
private const val MaxBands = 3

private class BlurBand {
    var k = 1
    var start = 0f
    var end = 0f
    var captureStart = 0f
    var captureEnd = 0f
    var preSigma = 0f
}

private class BandPlan {
    val bands = Array(MaxBands) { BlurBand() }
    var count = 0
    private var length = -1f
    private var radius = -1f
    private var ramp = -1f
    private var low = false

    fun update(length: Float, radius: Float, ramp: Float, low: Boolean) {
        if (length == this.length && radius == this.radius && ramp == this.ramp && low == this.low) return
        this.length = length
        this.radius = radius
        this.ramp = ramp
        this.low = low
        val rampLength = max(ramp * length, 1f)
        fun radiusAt(a: Float): Float {
            val t = (a / rampLength).coerceIn(0f, 1f)
            return radius * t * t
        }
        fun reach(r: Float, k: Int, preSigma: Float): Float {
            val sigma = 0.57735f * r / k + 0.5f
            return (3f * sigma + 3f * preSigma + 1f) * k
        }

        val starts = FloatArray(MaxBands)
        val ks = IntArray(MaxBands)
        count = 1
        starts[0] = 0f
        // Low quality draws the whole ramp at the resolution its strongest end needs: one capture
        ks[0] = glassDownscale(if (low) radius else 0f, low)
        if (!low) for (step in StepRadii) {
            if (count == MaxBands || step >= radius) break
            val k = glassDownscale(step, low)
            if (k == ks[count - 1]) continue
            val at = (rampLength * sqrt(step / radius) / BandGrid).roundToInt() * BandGrid
            if (at <= starts[count - 1] || at >= length) continue
            starts[count] = at
            ks[count] = k
            count++
        }

        for (i in 0 until count) {
            val band = bands[i]
            band.k = ks[i]
            band.start = starts[i]
            band.end = if (i + 1 < count) starts[i + 1] else length
            val strongest = 0.57735f * radiusAt(band.end) / band.k + 0.5f
            band.preSigma = (strongest * PreBlurOfSigma).takeIf { it >= MinPreSigma } ?: 0f
            var before = 0f
            var after = 0f
            for (j in 0..16) {
                val a = band.start + (band.end - band.start) * j / 16f
                val r = reach(radiusAt(a), band.k, band.preSigma)
                before = max(before, r - (a - band.start))
                after = max(after, a + r - band.end)
            }
            band.captureStart = (floor((band.start - before) / BandGrid) * BandGrid).coerceAtLeast(0f)
            band.captureEnd = (ceil((band.end + after) / BandGrid) * BandGrid).coerceAtMost(length)
        }
    }

    private companion object {
        val StepRadii = floatArrayOf(8f, 16f, 32f, 64f)
    }
}

private class BandEffect {
    var k = 0
    var preSigma = 0f
    var maxRadius = 0f
    var full = Size.Zero
    var layerSize = IntSize.Zero
    var origin = Offset.Zero
    var growDown = false
    var across = false
    var ramp = 0f
    var mask: Brush? = null
    var effect: RenderEffect? = null
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createAgslShader(source: String): Any = RuntimeShader(source)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Any.setAgslUniform(name: String, value: Float) {
    (this as RuntimeShader).setFloatUniform(name, value)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Any.setAgslUniform(name: String, value1: Float, value2: Float) {
    (this as RuntimeShader).setFloatUniform(name, value1, value2)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun agslRenderEffect(shader: Any, inputName: String): android.graphics.RenderEffect =
    android.graphics.RenderEffect.createRuntimeShaderEffect(shader as RuntimeShader, inputName)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun ProgressiveBlurBands(
    radius: () -> Float,
    mask: Brush?,
    modifier: Modifier,
    offscreen: (() -> Boolean)?,
    growDown: Boolean,
    across: Boolean,
    ramp: Float,
    draw: DrawScope.(fullSize: Size) -> Unit,
) {
    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)
    val currentDraw by rememberUpdatedState(draw)
    val low = LocalBlurQuality.current == BlurQuality.LOW
    // Low quality only ever uses one band, so skip allocating four idle graphics layers
    val layer0 = rememberGraphicsLayer()
    val frame0 = rememberGraphicsLayer()
    val layers = if (low) arrayOf(layer0) else arrayOf(layer0, rememberGraphicsLayer(), rememberGraphicsLayer())
    val frames = if (low) arrayOf(frame0) else arrayOf(frame0, rememberGraphicsLayer(), rememberGraphicsLayer())
    val plan = remember { BandPlan() }
    val effects = remember { Array(MaxBands) { BandEffect() } }

    Box(modifier = modifier.clipToBounds()) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val full = size
                    val length = if (across) full.width else full.height
                    if (length <= 0f || full.minDimension <= 0f) return@drawBehind
                    val r = currentRadius()
                    val coarsest = glassDownscale(r, low)
                    val maxRadius = if (r > 0.5f) BlurEffectCache.quantize(r / coarsest) * coarsest else 0f
                    plan.update(length, maxRadius, ramp, low)
                    val isOffscreen = currentOffscreen?.invoke() == true

                    for (i in 0 until plan.count) {
                        val band = plan.bands[i]
                        val k = band.k.toFloat()
                        val from = (if (growDown) band.captureStart else length - band.captureEnd).roundToInt().toFloat()
                        val to = if (growDown) band.captureEnd else length - band.captureStart
                        val shownFrom = if (growDown) band.start else length - band.end
                        val shownTo = if (growDown) band.end else length - band.start
                        val left = if (across) from else 0f
                        val top = if (across) 0f else from
                        val width = if (across) to - from else full.width
                        val height = if (across) full.height else to - from
                        val layerSize = IntSize(
                            ceil(width / k).toInt().coerceAtLeast(1),
                            ceil(height / k).toInt().coerceAtLeast(1),
                        )

                        val layer = layers[i]
                        layer.record(layerSize) {
                            scale(1f / k, 1f / k, pivot = Offset.Zero) {
                                translate(-left, -top) { currentDraw(full) }
                            }
                        }
                        layer.renderEffect = effects[i].effectFor(
                            band, maxRadius, full, layerSize, Offset(left / k, top / k), growDown, across, ramp, mask,
                        )
                        layer.clip = true
                        layer.compositingStrategy = if (isOffscreen) LayerCompositing.Offscreen else LayerCompositing.Auto

                        val frame = frames[i]
                        frame.record(layerSize) { drawLayer(layer) }
                        frame.compositingStrategy = LayerCompositing.Offscreen
                        frame.pivotOffset = Offset.Zero
                        frame.scaleX = k
                        frame.scaleY = k
                        frame.topLeft = IntOffset(left.roundToInt(), top.roundToInt())
                        frame.clip = true

                        if (across) {
                            clipRect(left = shownFrom, top = 0f, right = shownTo, bottom = full.height) { drawLayer(frame) }
                        } else {
                            clipRect(left = 0f, top = shownFrom, right = full.width, bottom = shownTo) { drawLayer(frame) }
                        }
                    }
                },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun BandEffect.effectFor(
    band: BlurBand,
    maxRadius: Float,
    full: Size,
    layerSize: IntSize,
    origin: Offset,
    growDown: Boolean,
    across: Boolean,
    ramp: Float,
    mask: Brush?,
): RenderEffect {
    val cached = effect
    if (cached != null && k == band.k && preSigma == band.preSigma && this.maxRadius == maxRadius && this.full == full &&
        this.layerSize == layerSize && this.origin == origin && this.growDown == growDown && this.across == across &&
        this.ramp == ramp && this.mask == mask
    ) return cached
    k = band.k
    preSigma = band.preSigma
    this.maxRadius = maxRadius
    this.full = full
    this.layerSize = layerSize
    this.origin = origin
    this.growDown = growDown
    this.across = across
    this.ramp = ramp
    this.mask = mask
    return variableBlurEffect(band, maxRadius, full, layerSize, origin, growDown, across, ramp, mask).also { effect = it }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun variableBlurEffect(
    band: BlurBand,
    maxRadius: Float,
    full: Size,
    layerSize: IntSize,
    origin: Offset,
    growDown: Boolean,
    across: Boolean,
    ramp: Float,
    mask: Brush?,
): RenderEffect {
    val k = band.k.toFloat()
    val shader = VariableBlurShader.shader ?: createAgslShader(VariableBlurSource).also { VariableBlurShader.shader = it }
    shader.setAgslUniform("size", layerSize.width.toFloat(), layerSize.height.toFloat())
    shader.setAgslUniform("origin", origin.x, origin.y)
    shader.setAgslUniform("full", full.width / k, full.height / k)
    shader.setAgslUniform("maxRadius", maxRadius / k)
    shader.setAgslUniform("growDown", if (growDown) 1f else 0f)
    shader.setAgslUniform("across", if (across) 1f else 0f)
    shader.setAgslUniform("ramp", ramp.coerceIn(0.01f, 1f))
    shader.setAgslUniform("preSigma", band.preSigma)
    shader.setAgslUniform("dir", 1f, 0f)
    val acrossPass = agslRenderEffect(shader, "content")
    shader.setAgslUniform("dir", 0f, 1f)
    val downPass = agslRenderEffect(shader, "content")
    val firstPass = if (band.preSigma > 0f) {
        val preRadius = (band.preSigma - 0.5f) / 0.57735f
        val pre = android.graphics.RenderEffect.createBlurEffect(preRadius, preRadius, Shader.TileMode.CLAMP)
        android.graphics.RenderEffect.createChainEffect(acrossPass, pre)
    } else {
        acrossPass
    }
    val blurred = android.graphics.RenderEffect.createChainEffect(downPass, firstPass)
    val maskShader = (mask as? ShaderBrush)?.createShader(Size(full.width / k, full.height / k))
    val effect = if (maskShader != null) {
        maskShader.setLocalMatrix(Matrix().apply { setTranslate(-origin.x, -origin.y) })
        android.graphics.RenderEffect.createBlendModeEffect(
            blurred,
            android.graphics.RenderEffect.createShaderEffect(maskShader),
            android.graphics.BlendMode.DST_IN,
        )
    } else {
        blurred
    }
    return effect.asComposeRenderEffect()
}

private class FullSize {
    var width = 0
    var height = 0
}

private fun Modifier.downscaledLayout(downscale: () -> Int, full: FullSize): Modifier = layout { measurable, constraints ->
    val k = downscale().coerceAtLeast(1)
    val fullWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    val fullHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
    full.width = fullWidth
    full.height = fullHeight
    val placeable = measurable.measure(
        Constraints.fixed((fullWidth + k - 1) / k, (fullHeight + k - 1) / k)
    )
    layout(fullWidth, fullHeight) { placeable.place(0, 0) }
}

private class MaskedEffectCache {
    var radius = -1f
    var width = -1f
    var height = -1f
    var tileOrdinal = -1
    var mask: Brush? = null
    var effect: RenderEffect? = null
}

@RequiresApi(Build.VERSION_CODES.S)
private fun maskedBlurEffect(radius: Float, tileMode: Shader.TileMode, mask: Shader): RenderEffect {
    val content = if (radius > 0.5f) {
        android.graphics.RenderEffect.createBlurEffect(radius, radius, tileMode)
    } else {
        android.graphics.RenderEffect.createOffsetEffect(0f, 0f)
    }
    return android.graphics.RenderEffect.createBlendModeEffect(
        content,
        android.graphics.RenderEffect.createShaderEffect(mask),
        android.graphics.BlendMode.DST_IN,
    ).asComposeRenderEffect()
}

@Composable
internal fun DownscaledBlur(
    radius: () -> Float,
    tileMode: Shader.TileMode?,
    mask: Brush?,
    modifier: Modifier = Modifier,
    offscreen: (() -> Boolean)? = null,
    growDown: Boolean? = null,
    growAcross: Boolean = false,
    growRamp: Float = 1f,
    draw: DrawScope.(fullSize: Size) -> Unit,
) {
    if (growDown != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ProgressiveBlurBands(radius, mask, modifier, offscreen, growDown, growAcross, growRamp, draw)
        return
    }
    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)
    val low = LocalBlurQuality.current == BlurQuality.LOW
    val downscale by remember(low) { derivedStateOf { glassDownscale(currentRadius(), low) } }
    val maskCache = remember { MaskedEffectCache() }
    val full = remember { FullSize() }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(
                if (mask != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            )
    ) {
        Canvas(
            modifier = Modifier
                .downscaledLayout({ downscale }, full)
                .graphicsLayer {
                    val k = downscale.toFloat()
                    scaleX = k
                    scaleY = k
                    transformOrigin = TransformOrigin(0f, 0f)
                    val r = currentRadius() / k
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val mode = tileMode ?: Shader.TileMode.DECAL
                        if (mask is ShaderBrush) {
                            val quantized = if (r > 0.5f) BlurEffectCache.quantize(r) else 0f
                            val cache = maskCache
                            if (cache.effect == null || cache.radius != quantized || cache.width != size.width ||
                                cache.height != size.height || cache.tileOrdinal != mode.ordinal || cache.mask != mask
                            ) {
                                cache.effect = maskedBlurEffect(quantized, mode, mask.createShader(size))
                                cache.radius = quantized
                                cache.width = size.width
                                cache.height = size.height
                                cache.tileOrdinal = mode.ordinal
                                cache.mask = mask
                            }
                            cache.effect
                        } else {
                            BlurEffectCache.get(r, mode)
                        }
                    } else null
                    compositingStrategy = if (currentOffscreen?.invoke() == true) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    clip = true
                }
        ) {
            val k = downscale.toFloat()
            val fullSize = Size(full.width.toFloat(), full.height.toFloat())
            scale(1f / k, 1f / k, pivot = Offset.Zero) { draw(fullSize) }
        }
    }
}

@Composable
fun GlassPillBackground(
    state: GlassState,
    blurRadius: () -> Float = { 24f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    shape: Shape? = null,
    tileMode: Shader.TileMode? = null,
    mask: Brush? = null,
    progressive: BlurDirection? = null,
    progressiveRamp: Float = 1f,
    modifier: Modifier = Modifier,
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    val currentBlurRadius by rememberUpdatedState(blurRadius)
    val radius = { BlurEffectCache.effectiveRadius(currentBlurRadius()) }

    DownscaledBlur(
        radius = radius,
        tileMode = tileMode,
        mask = mask,
        growDown = progressive?.let { it == BlurDirection.TopToBottom },
        growRamp = progressiveRamp,
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier)
            .onGloballyPositioned { position = it.positionInWindow() },
    ) { fullSize ->
        if (foundationColor != null) {
            drawRect(color = foundationColor, size = fullSize)
        }
        val node = state.renderNode
        if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && glassNodeHasContent(node)) {
            val relX = position.x - state.rootPosition.x
            val relY = position.y - state.rootPosition.y
            translate(left = -relX, top = -relY) {
                drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
            }
        }
        if (tint != Color.Transparent) {
            drawRect(color = tint, size = fullSize)
        }
    }
}

@Composable
fun ProgressiveGlassBackground(
    state: GlassState,
    maxBlurRadius: () -> Float = { 36f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    direction: BlurDirection = BlurDirection.BottomToTop,
    modifier: Modifier = Modifier,
    steps: Int = 4,
) {
    val smooth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        LocalBlurQuality.current == BlurQuality.HIGH
    if (smooth) {
        val fade = 1f / steps.coerceAtLeast(1)
        val mask = remember(direction, fade) {
            if (direction == BlurDirection.TopToBottom) {
                Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f to Color.Black)
            } else {
                Brush.verticalGradient(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent)
            }
        }
        Box(modifier = modifier) {
            GlassPillBackground(
                state = state,
                blurRadius = maxBlurRadius,
                tint = tint,
                foundationColor = foundationColor,
                mask = mask,
                progressive = direction,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    Box(modifier = modifier) {
        for (i in 1..steps) {
            val fraction = i.toFloat() / steps
            val radiusProvider = { maxBlurRadius() * (fraction * fraction) }
            val fadeStart = (i - 1).toFloat() / steps
            val heightFraction = 1f - fadeStart
            if (heightFraction > 0f) {
                val internalFadeDistance = (1f / steps) / heightFraction
                val mask = remember(direction, internalFadeDistance) {
                    if (direction == BlurDirection.TopToBottom) {
                        Brush.verticalGradient(0f to Color.Transparent, internalFadeDistance to Color.Black, 1f to Color.Black)
                    } else {
                        Brush.verticalGradient(0f to Color.Black, (1f - internalFadeDistance) to Color.Black, 1f to Color.Transparent)
                    }
                }
                GlassPillBackground(
                    state = state,
                    blurRadius = radiusProvider,
                    tint = tint,
                    foundationColor = foundationColor,
                    mask = mask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction)
                        .align(if (direction == BlurDirection.TopToBottom) Alignment.BottomCenter else Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * Drop-in progressive top header blur overlay (PixelMusic style) that works seamlessly in both
 * light and dark themes. When a [GlassState] is attached to the scrolling body underneath,
 * it renders a real progressive AGSL/RenderNode backdrop blur; otherwise it gracefully
 * falls back to a smooth multi-stop progressive scrim.
 */
@Composable
fun rememberGlassState(): GlassState {
    val state = remember { GlassState() }
    DisposableEffect(state) {
        onDispose { state.discard() }
    }
    return state
}

@Composable
fun ProgressiveHeaderBackdrop(
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.background,
    glassState: GlassState? = LocalScreenGlassState.current,
    maxBlurRadius: Float = 24f,
) {
    val clamped = alpha.coerceIn(0f, 1f)
    if (clamped <= 0.01f) return
    val scrimFactor = scrimColor.alpha
    Box(
        modifier = modifier.graphicsLayer { this.alpha = clamped }
    ) {
        if (glassState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ProgressiveGlassBackground(
                state = glassState,
                maxBlurRadius = { maxBlurRadius * clamped },
                foundationColor = if (scrimFactor > 0.01f) scrimColor.copy(alpha = 0.55f * scrimFactor) else null,
                direction = BlurDirection.BottomToTop,
                steps = 3,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (scrimFactor > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            0f to scrimColor.copy(alpha = 0.92f * scrimFactor),
                            0.32f to scrimColor.copy(alpha = 0.62f * scrimFactor),
                            0.68f to scrimColor.copy(alpha = 0.24f * scrimFactor),
                            1f to Color.Transparent,
                        )
                        onDrawBehind { drawRect(brush = gradient) }
                    }
            )
        }
    }
}

@Composable
fun ProgressiveStatusBarHeaderBackdrop(
    modifier: Modifier = Modifier,
    extraHeight: Dp = 64.dp,
    alpha: Float = 1f,
    scrimColor: Color = MaterialTheme.colorScheme.surface,
    glassState: GlassState? = LocalScreenGlassState.current,
    maxBlurRadius: Float = 24f,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ProgressiveHeaderBackdrop(
        alpha = alpha,
        scrimColor = scrimColor,
        glassState = glassState,
        maxBlurRadius = maxBlurRadius,
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + extraHeight),
    )
}
