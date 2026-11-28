package com.saurav.pixelmusic.presentation.components.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.isActive
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.data.preferences.AodScreenStyle
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.sanitizeLyricLineText
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AodSourceArtStyle {
    STANDARD_ROUNDED,
    IMMERSIVE_RECT,
    COMPLEX_ARCH
}

class AodSourceBounds {
    val artBounds = MorphElementBounds()
    val songInfoBounds = MorphElementBounds()
    val progressBounds = MorphElementBounds()
    val prevBounds = MorphElementBounds()
    val playBounds = MorphElementBounds()
    val nextBounds = MorphElementBounds()
    var artStyle: AodSourceArtStyle = AodSourceArtStyle.STANDARD_ROUNDED
    var complexRightRisePx: Float = 0f
    var washColor: Color = Color.Unspecified
    var washStart: Float = 0.55f
    var washMid: Float = 0.80f
    var washMidAlpha: Float = 0.85f
    var topScrimAlpha: Float = 0f
    // Top status-bar scrim the real Now Playing art draws; the AOD stand-in art draws the same one so
    // the hand-off to the real art doesn't pop darker at the top.
    var topShadowHeightDp: Float = 0f
    var topShadowStartAlpha: Float = 0f
    var topShadowMidAlpha: Float = 0f
    var carouselStyle: String = ""
    var currentSeekProgress: Float = -1f
    var miniSongInfoWidthPx: Float = 0f
    val titleMarquee = MarqueeSyncState()
    val artistMarquee = MarqueeSyncState()
}

class MarqueeSyncState {
    var text: String = ""
    var scrollFraction: Float by mutableFloatStateOf(0f)
    var scrollPx: Float by mutableFloatStateOf(0f)
    var frozenEnterScrollPx: Float = 0f
    var pauseRemainingMs: Long = 1600L

    fun syncText(newText: String) {
        if (text != newText) {
            text = newText
            scrollFraction = 0f
            scrollPx = 0f
            frozenEnterScrollPx = 0f
            pauseRemainingMs = 1600L
        }
    }

    fun reset() {
        scrollFraction = 0f
        scrollPx = 0f
        frozenEnterScrollPx = 0f
        pauseRemainingMs = 1600L
    }
}

private class MarqueeLayoutMetrics {
    var textWidthPx: Float = 0f
    var containerWidthPx: Float = 0f
    var miniLocalWidthPx: Float = 0f
    var fullLocalWidthPx: Float = 0f
}

@Composable
fun SharedPlayerMarqueeText(
    text: String,
    style: TextStyle,
    marqueeState: MarqueeSyncState,
    shouldRunMarquee: Boolean,
    modifier: Modifier = Modifier,
    isAodOverlay: Boolean = false,
    enterProgress: Float = 0f,
    srcWidthPx: Float = 0f,
    destWidthPx: Float = 0f,
    shouldRunMarqueeProvider: (() -> Boolean)? = null,
    morphFractionProvider: (() -> Float)? = null,
    miniAvailableWidthPxProvider: (() -> Float)? = null,
    fullAvailableWidthPxProvider: (() -> Float)? = null,
    localScaleProvider: (() -> Float)? = null,
    baseMiniScale: Float = 1f,
    gapDp: Dp = 36.dp,
    velocityDpPerSec: Dp = 28.dp
) {
    val density = LocalDensity.current
    val gapPx = with(density) { gapDp.toPx() }
    val velocityPxPerSec = with(density) { velocityDpPerSec.toPx() }

    marqueeState.syncText(text)

    val metrics = remember(text) { MarqueeLayoutMetrics() }

    LaunchedEffect(text, isAodOverlay) {
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { nanos ->
                val shouldRun = shouldRunMarqueeProvider?.invoke() ?: shouldRunMarquee
                val tW = metrics.textWidthPx
                val cW = metrics.containerWidthPx
                val t = morphFractionProvider?.invoke()?.coerceIn(0f, 1f) ?: 1f
                val miniLocalW = metrics.miniLocalWidthPx.takeIf { it > 0f } ?: cW
                val fullLocalW = metrics.fullLocalWidthPx.takeIf { it > 0f } ?: cW
                val overflowsMini = tW > miniLocalW + 1f && miniLocalW > 0f
                val overflowsFull = tW > fullLocalW + 1f && fullLocalW > 0f
                val overflowsTarget = when {
                    isAodOverlay -> {
                        val dW = destWidthPx.takeIf { it > 0f } ?: cW
                        tW > dW + 1f && dW > 0f
                    }
                    morphFractionProvider != null && metrics.miniLocalWidthPx > 0f -> {
                        when {
                            t <= 0.01f -> overflowsMini
                            t >= 0.99f -> overflowsFull
                            else -> overflowsMini || overflowsFull
                        }
                    }
                    else -> tW > cW + 1f && cW > 0f
                }

                if (!shouldRun || !overflowsTarget) {
                    lastFrameNanos = 0L
                    if (shouldRun && !overflowsTarget && tW > 0f && cW > 0f) {
                        val settledNonOverflow = isAodOverlay ||
                            morphFractionProvider == null ||
                            (t <= 0.01f && !overflowsMini) ||
                            (t >= 0.99f && !overflowsFull)
                        if (settledNonOverflow && marqueeState.scrollFraction != 0f) {
                            marqueeState.reset()
                        }
                    }
                } else {
                    if (lastFrameNanos != 0L) {
                        val dtMs = ((nanos - lastFrameNanos) / 1_000_000L).coerceIn(0L, 100L)
                        if (marqueeState.pauseRemainingMs > 0L) {
                            marqueeState.pauseRemainingMs = (marqueeState.pauseRemainingMs - dtMs).coerceAtLeast(0L)
                        } else {
                            val cyclePx = (tW + gapPx).coerceAtLeast(1f)
                            val curScrollPx = marqueeState.scrollFraction.coerceIn(0f, 1f) * cyclePx
                            val nextPx = curScrollPx + velocityPxPerSec * (dtMs / 1000f)
                            if (nextPx >= cyclePx) {
                                marqueeState.scrollFraction = 0f
                                marqueeState.scrollPx = 0f
                                marqueeState.frozenEnterScrollPx = 0f
                                marqueeState.pauseRemainingMs = 1400L
                            } else {
                                val nextFrac = (nextPx / cyclePx).coerceIn(0f, 1f)
                                marqueeState.scrollFraction = nextFrac
                                marqueeState.scrollPx = nextPx
                                marqueeState.frozenEnterScrollPx = nextPx
                            }
                        }
                    }
                    lastFrameNanos = nanos
                }
            }
        }
    }

    Layout(
        content = {
            Text(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Text(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        },
        modifier = modifier.drawWithContent {
            val clipRight = if (!isAodOverlay && morphFractionProvider != null && miniAvailableWidthPxProvider != null) {
                val t = morphFractionProvider().coerceIn(0f, 1f)
                val miniW = miniAvailableWidthPxProvider()
                val fullW = fullAvailableWidthPxProvider?.invoke()?.takeIf { it > 0f } ?: size.width
                val scale = (localScaleProvider?.invoke() ?: 1f).coerceAtLeast(0.01f)
                val targetScreenW = if (miniW > 0f && t < 0.999f) lerp(miniW, fullW, t) else size.width
                (targetScreenW / scale).coerceAtLeast(1f)
            } else {
                size.width
            }
            clipRect(left = 0f, top = 0f, right = clipRight, bottom = size.height) {
                this@drawWithContent.drawContent()
            }
        }
    ) { measurables, constraints ->
        val cW = constraints.maxWidth.coerceAtLeast(1)
        val cWF = cW.toFloat()
        val unbounded = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val primaryPlaceable = measurables[0].measure(unbounded)
        val tW = primaryPlaceable.width.toFloat()
        val tH = primaryPlaceable.height

        val miniW = miniAvailableWidthPxProvider?.invoke() ?: 0f
        val safeBaseScale = baseMiniScale.coerceAtLeast(0.01f)
        val miniLocalW = if (miniW > 0f) miniW / safeBaseScale else cWF
        val fullLocalW = fullAvailableWidthPxProvider?.invoke()?.takeIf { it > 0f } ?: cWF

        metrics.textWidthPx = tW
        metrics.containerWidthPx = cWF
        metrics.miniLocalWidthPx = miniLocalW
        metrics.fullLocalWidthPx = fullLocalW

        val cyclePx = (tW + gapPx).coerceAtLeast(1f)
        val canEverOverflow = isAodOverlay || tW > minOf(cWF, miniLocalW, fullLocalW) + 1f
        val secondPlaceable = if (canEverOverflow) measurables[1].measure(unbounded) else null

        layout(cW, tH) {
            val primaryX: Float
            val secondX: Float
            val placeSecond: Boolean

            if (!isAodOverlay) {
                val t = morphFractionProvider?.invoke()?.coerceIn(0f, 1f) ?: 1f
                val curMiniW = miniAvailableWidthPxProvider?.invoke() ?: 0f
                val curMiniLocalW = if (curMiniW > 0f) curMiniW / safeBaseScale else cWF
                val curFullLocalW = fullAvailableWidthPxProvider?.invoke()?.takeIf { it > 0f } ?: cWF
                val localScale = (localScaleProvider?.invoke() ?: 1f).coerceAtLeast(0.01f)
                val localVisibleW = if (curMiniW > 0f && morphFractionProvider != null && t < 0.999f) {
                    (lerp(curMiniW, curFullLocalW, t) / localScale).coerceAtLeast(1f)
                } else {
                    cWF
                }

                val overflowsMini = tW > curMiniLocalW + 1f
                val overflowsFull = tW > curFullLocalW + 1f
                val rawScroll = (marqueeState.scrollFraction.coerceIn(0f, 1f) * cyclePx).coerceIn(0f, cyclePx)

                val scrollMultiplier = if (morphFractionProvider != null && curMiniW > 0f) {
                    lerp(if (overflowsMini) 1f else 0f, if (overflowsFull) 1f else 0f, t)
                } else {
                    if (tW > cWF + 1f) 1f else 0f
                }

                val wrapForward = overflowsMini != overflowsFull && rawScroll > cyclePx * 0.5f
                val signedScroll = if (wrapForward) rawScroll - cyclePx else rawScroll

                primaryX = -signedScroll * scrollMultiplier
                secondX = if (wrapForward) primaryX - cyclePx else primaryX + cyclePx
                placeSecond = secondPlaceable != null && scrollMultiplier > 0.001f &&
                    if (wrapForward) (secondX + tW > 0f) else (primaryX < -0.5f && secondX < localVisibleW)
            } else {
                val sW = srcWidthPx.takeIf { it > 0f } ?: cWF
                val dW = destWidthPx.takeIf { it > 0f } ?: cWF
                val overflowsSrc = tW > sW + 1f
                val overflowsDest = tW > dW + 1f
                val rawFrozen = (marqueeState.scrollFraction.coerceIn(0f, 1f) * cyclePx).coerceIn(0f, cyclePx)
                val wrapLeft = overflowsSrc && !overflowsDest && rawFrozen > cyclePx * 0.5f

                val startX = when {
                    !overflowsSrc -> 0f
                    wrapLeft -> -rawFrozen + cyclePx
                    else -> -rawFrozen
                }
                val endX = if (overflowsDest) {
                    -(marqueeState.scrollFraction.coerceIn(0f, 1f) * cyclePx).coerceIn(0f, cyclePx)
                } else {
                    (dW - tW) * 0.5f
                }

                primaryX = lerp(startX, endX, enterProgress.coerceIn(0f, 1f))
                secondX = if (wrapLeft) primaryX - cyclePx else primaryX + cyclePx
                placeSecond = secondPlaceable != null && (overflowsSrc || overflowsDest) &&
                    if (wrapLeft) (secondX + tW > 0f) else (primaryX < -0.5f && secondX < cWF)
            }

            primaryPlaceable.placeRelative(primaryX.roundToInt(), 0)
            if (placeSecond) {
                secondPlaceable?.placeRelative(secondX.roundToInt(), 0)
            }
        }
    }
}

private val SmoothMorphEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The Now Playing album-cover container is wider than the focal card when a peek carousel style is
 * selected (and has 8dp of vertical padding). Narrow the captured container bounds down to the focal
 * card so the AOD art morphs from exactly where the visible album art is.
 */
private fun resolveFocalArtBounds(
    raw: MorphElementBounds,
    style: String,
    density: Density
): MorphElementBounds {
    val pad = with(density) { 8.dp.toPx() }
    val w = raw.size.width.toFloat()
    val h = raw.size.height.toFloat()
    val itemW: Float
    val itemX: Float
    when (style) {
        com.saurav.pixelmusic.data.preferences.CarouselStyle.ONE_PEEK -> {
            itemW = w * 0.8f
            itemX = 0f
        }
        com.saurav.pixelmusic.data.preferences.CarouselStyle.TWO_PEEK -> {
            itemW = w * 0.6f
            itemX = w * 0.2f
        }
        else -> {
            itemW = w
            itemX = 0f
        }
    }
    val itemH = min(itemW, (h - pad * 2f).coerceAtLeast(1f))
    return MorphElementBounds(
        offset = androidx.compose.ui.geometry.Offset(raw.offset.x + itemX, raw.offset.y + pad),
        size = androidx.compose.ui.unit.IntSize(
            itemW.roundToInt().coerceAtLeast(1),
            itemH.roundToInt().coerceAtLeast(1)
        ),
        hasBounds = true
    )
}

/**
 * Curated Material You Expressive shapes:
 * - Soft cookies with gentle, shallow curves (never deeply pinched or lumpy)
 * - Clean geometric expressive shapes: Pill, Oval, Rounded Rectangle, Rounded Diamond,
 *   Rounded Triangle, Squircle, and Soft Pentagon.
 * Ordered so consecutive taps alternate between cookies and distinct geometric silhouettes.
 */
private enum class AodExpressiveShapeKind {
    // 0: 8-Sided Soft Cookie (subtle 3.6% wave)
    COOKIE_8 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(8.0 * th)
    },
    // 1: Material You Stadium Pill (tilted 28 deg for expressive balance)
    PILL {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - (28.0 * PI / 180.0)
            val c = abs(cos(a)) / 0.78
            val s = abs(sin(a)) / 1.00
            return 1.0 / (c.pow(4.6) + s.pow(4.6)).pow(1.0 / 4.6)
        }
    },
    // 2: 6-Sided Soft Cookie (gentle 4.2% wave)
    COOKIE_6 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.042 * cos(6.0 * th)
    },
    // 3: Material You Rounded Diamond (45-deg rotated superellipse, convex sides + soft corners)
    ROUNDED_DIAMOND {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 4.0
            val c = abs(cos(a))
            val s = abs(sin(a))
            return 1.0 / (c.pow(3.5) + s.pow(3.5)).pow(1.0 / 3.5)
        }
    },
    // 4: Material You Smooth Oval (tilted -22 deg)
    OVAL {
        override fun rawRadiusAt(th: Double): Double {
            val a = th + (22.0 * PI / 180.0)
            val c = cos(a) / 0.82
            val s = sin(a) / 1.00
            return 1.0 / sqrt(c * c + s * s)
        }
    },
    // 5: 4-Sided Soft Clover Cookie (gentle 4.6% cushion curve)
    COOKIE_4 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.046 * cos(4.0 * th + PI / 4.0)
    },
    // 6: Material You Soft Rounded Triangle (strictly convex with rounded vertices)
    ROUNDED_TRIANGLE {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.064 * cos(3.0 * a) - 0.011 * cos(6.0 * a)
        }
    },
    // 7: Material You Rounded Rectangle / Soft Squircle
    ROUNDED_RECTANGLE {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th)) / 0.88
            val s = abs(sin(th)) / 1.00
            return 1.0 / (c.pow(4.2) + s.pow(4.2)).pow(1.0 / 4.2)
        }
    },
    // 8: 12-Sided Sunny Cookie (refined 2.5% scallop)
    COOKIE_12 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.025 * cos(12.0 * th)
    },
    // 9: Material You Soft Pentagon
    ROUNDED_PENTAGON {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.034 * cos(5.0 * a) - 0.005 * cos(10.0 * a)
        }
    },
    // 10: 7-Sided Soft Cookie (gentle 3.6% wave)
    COOKIE_7 {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(7.0 * th - PI / 2.0)
    },
    // 11: Material You 1:1 Squircle Cushion
    ROUNDED_SQUIRCLE {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th))
            val s = abs(sin(th))
            return 1.0 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)
        }
    };

    abstract fun rawRadiusAt(th: Double): Double

    private val normScale: Double by lazy {
        val samples = 180
        var maxR = 0.001
        var sumR = 0.0
        for (i in 0 until samples) {
            val r = rawRadiusAt((2.0 * PI * i) / samples)
            if (r > maxR) maxR = r
            sumR += r
        }
        val meanR = sumR / samples
        // Blend max and mean so Pill/Oval/Triangle feel visually just as full as Cookies
        (0.62 * maxR + 0.38 * meanR).coerceAtLeast(maxR * 0.94)
    }

    fun normalizedRadiusAt(th: Double): Float {
        return (rawRadiusAt(th) / normScale).toFloat().coerceAtMost(1.04f)
    }
}

private val AOD_EXPRESSIVE_SHAPES = AodExpressiveShapeKind.entries

private fun pickNextExpressiveShapeIndex(currentIndex: Int): Int {
    if (AOD_EXPRESSIVE_SHAPES.size <= 1) return 0
    return (currentIndex + 1) % AOD_EXPRESSIVE_SHAPES.size
}

/**
 * Unified shape that:
 * 1. Morphs between the Now Playing source artwork outline (Complex asymmetric arch,
 *    Standard 18.dp rounded square, or Immersive rectangle) at `enterMorphT = 0f`
 *    and the AOD Material You Expressive shape at `enterMorphT = 1f`.
 * 2. Smoothly morphs and slowly rotates between `fromShape` and `toShape` when the user
 *    taps the album art on the AOD screen.
 */
private class AodMorphingAlbumShape(
    private val fromShape: AodExpressiveShapeKind,
    private val toShape: AodExpressiveShapeKind,
    private val shapeMorphProgress: Float,
    private val shapeRotationRad: Double,
    private val enterMorphT: Float,
    private val sourceArtStyle: AodSourceArtStyle,
    private val complexRightRisePx: Float,
    private val standardCornerPx: Float,
    private val srcWidthPx: Float = 0f,
    private val srcHeightPx: Float = 0f,
    private val destSizePx: Float = 0f,
    private val steps: Int = 240
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val srcW0 = srcWidthPx.takeIf { it > 0f } ?: w
        val srcH0 = srcHeightPx.takeIf { it > 0f } ?: h
        val destD = destSizePx.takeIf { it > 0f } ?: min(w, h)
        val baseRadius = destD * 0.476f
        val clampedShapeMorph = shapeMorphProgress.coerceIn(0f, 1f)
        val clampedEnter = enterMorphT.coerceIn(0f, 1f)
        val count = steps.coerceAtLeast(120)
        val twoPi = 2.0 * PI

        // Evaluate the source Now Playing shape in its true t=0 dimensions (srcW0, srcH0)
        // and the AOD shape in its true t=1 dimensions (destD, destD) so that interpolating
        // finalR = lerp(rSrc0, rAod, clampedEnter) moves every boundary point along a pure
        // monotonic straight line in screen space without any mid-transition upward bow or end jump.
        val useComplexArch = sourceArtStyle == AodSourceArtStyle.COMPLEX_ARCH && clampedEnter < 0.999f
        var archCx0 = 0f
        var archCy0 = 0f
        var archRadius0 = 1f
        if (useComplexArch) {
            val archCenterXFraction = 0.25f
            archCx0 = srcW0 * archCenterXFraction
            val dxRight = (srcW0 - archCx0).coerceAtLeast(1f)
            val sRight = complexRightRisePx.coerceIn(16f, (srcH0 - 4f).coerceAtLeast(16f))
            archRadius0 = (sRight / 2f) + ((dxRight * dxRight) / (2f * sRight))
            archCy0 = srcH0 - archRadius0
        }

        val minDim0 = min(srcW0, srcH0)
        // Smoothly round the corners as soon as the album art pulls away from the screen edges
        // so the transition never exhibits sharp 90-degree polygon vertices.
        val srcCornerR = when (sourceArtStyle) {
            AodSourceArtStyle.STANDARD_ROUNDED -> {
                val startR = standardCornerPx.coerceIn(0f, minDim0 * 0.5f)
                lerp(startR, minDim0 * 0.45f, (clampedEnter * 2.2f).coerceIn(0f, 1f))
            }
            AodSourceArtStyle.IMMERSIVE_RECT, AodSourceArtStyle.COMPLEX_ARCH -> {
                minDim0 * (clampedEnter * 2.8f).coerceAtMost(0.46f)
            }
        }
        val halfW0 = srcW0 * 0.5f
        val halfH0 = srcH0 * 0.5f
        val innerCornerX = (halfW0 - srcCornerR).coerceAtLeast(0f)
        val innerCornerY = (halfH0 - srcCornerR).coerceAtLeast(0f)

        val path = Path().apply {
            for (i in 0 until count) {
                val theta = (twoPi * i) / count
                val cosT = cos(theta).toFloat()
                val sinT = sin(theta).toFloat()

                // 1. Target AOD Expressive Shape radius at rotated angle (in destD coordinates)
                val rotatedTheta = theta - shapeRotationRad
                val rFrom = fromShape.normalizedRadiusAt(rotatedTheta)
                val rTo = toShape.normalizedRadiusAt(rotatedTheta)
                val rAod = baseRadius * lerp(rFrom, rTo, clampedShapeMorph)

                // 2. Source Now Playing shape radius along ray (cosT, sinT) in (srcW0, srcH0)
                val finalR = if (clampedEnter >= 0.999f) {
                    rAod
                } else {
                    val absCos = abs(cosT).coerceAtLeast(1e-5f)
                    val absSin = abs(sinT).coerceAtLeast(1e-5f)
                    var rSrc0 = min(halfW0 / absCos, halfH0 / absSin)

                    if (srcCornerR > 0.5f) {
                        val px = rSrc0 * absCos
                        val py = rSrc0 * absSin
                        if (px > innerCornerX && py > innerCornerY) {
                            val dot = innerCornerX * absCos + innerCornerY * absSin
                            val cVal = innerCornerX * innerCornerX + innerCornerY * innerCornerY - srcCornerR * srcCornerR
                            val disc = (dot * dot - cVal).coerceAtLeast(0f)
                            rSrc0 = min(rSrc0, dot + sqrt(disc))
                        }
                    }

                    if (useComplexArch) {
                        val ox = halfW0 - archCx0
                        val oy = halfH0 - archCy0
                        val bQuad = ox * cosT + oy * sinT
                        val cQuad = ox * ox + oy * oy - archRadius0 * archRadius0
                        val disc = (bQuad * bQuad - cQuad).coerceAtLeast(0f)
                        val tArch = -bQuad + sqrt(disc)
                        if (tArch > 0f) {
                            val kSmooth = srcCornerR * 0.65f
                            rSrc0 = if (kSmooth > 0.5f) {
                                val hSm = (0.5f + 0.5f * (tArch - rSrc0) / kSmooth).coerceIn(0f, 1f)
                                lerp(tArch, rSrc0, hSm) - kSmooth * hSm * (1f - hSm)
                            } else {
                                min(rSrc0, tArch)
                            }
                        }
                    }

                    lerp(rSrc0, rAod, clampedEnter)
                }

                val x = (cx + finalR * cosT).coerceIn(0f, w)
                val y = (cy + finalR * sinT).coerceIn(0f, h)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        return Outline.Generic(path)
    }
}

private data class AodPaletteColors(
    val accent: Color,
    val onAccent: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val glow: Color,
    val haloColor: Color,
    val canvasColor: Color,
    val sourceBgColor: Color
)

/**
 * Derives every AOD color directly from the active app colour palette (`LocalMaterialTheme.current`),
 * ensuring 100% palette harmony in both Light and Dark themes without random HSL overrides or raw white.
 */
private fun deriveAodPaletteFromAppTheme(
    baseTheme: ColorScheme,
    sourceArtStyle: AodSourceArtStyle
): AodPaletteColors {
    val lightPastelTone = if (baseTheme.primaryContainer.luminance() >= baseTheme.onPrimaryContainer.luminance()) {
        baseTheme.primaryContainer
    } else {
        baseTheme.onPrimaryContainer
    }
    val deepRichTone = if (baseTheme.primaryContainer.luminance() < baseTheme.onPrimaryContainer.luminance()) {
        baseTheme.primaryContainer
    } else {
        baseTheme.onPrimaryContainer
    }

    val vibrantPrimary = when {
        baseTheme.primary.luminance() >= 0.30f -> baseTheme.primary
        baseTheme.inversePrimary != Color.Unspecified && baseTheme.inversePrimary.luminance() >= 0.30f ->
            baseTheme.inversePrimary
        else -> lerp(baseTheme.primary, lightPastelTone, 0.48f)
    }

    val darkOnAccent = if (vibrantPrimary.luminance() > 0.42f) {
        lerp(Color.Black, deepRichTone, 0.55f)
    } else {
        lightPastelTone
    }

    val secondaryTonalBase = if (baseTheme.secondaryContainer.luminance() < 0.35f) {
        baseTheme.secondaryContainer
    } else {
        deepRichTone
    }
    val secondaryContainer = lerp(Color(0xFF09090B), secondaryTonalBase, 0.42f)
    val onSecondaryContainer = lerp(lightPastelTone, vibrantPrimary, 0.18f)

    val titleColor = lerp(lightPastelTone, Color.White, 0.18f)
    val subtitleColor = onSecondaryContainer.copy(alpha = 0.76f)
    val glowColor = lerp(vibrantPrimary, baseTheme.tertiary, 0.16f)
    val haloColor = lerp(secondaryContainer, vibrantPrimary, 0.22f)
    val canvasColor = lerp(Color.Black, deepRichTone, 0.10f)

    val sourceBgColor = when (sourceArtStyle) {
        AodSourceArtStyle.COMPLEX_ARCH -> baseTheme.primaryContainer
        AodSourceArtStyle.IMMERSIVE_RECT -> baseTheme.surface
        AodSourceArtStyle.STANDARD_ROUNDED -> baseTheme.primaryContainer
    }

    return AodPaletteColors(
        accent = vibrantPrimary,
        onAccent = darkOnAccent,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        glow = glowColor,
        haloColor = haloColor,
        canvasColor = canvasColor,
        sourceBgColor = sourceBgColor
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun AodScreen(
    songTitle: String,
    artistName: String,
    albumArtUriString: String?,
    isPlayingProvider: () -> Boolean,
    currentPositionProvider: () -> Long,
    totalDurationProvider: () -> Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    enterProgressProvider: () -> Float = { 1f },
    sourceBounds: AodSourceBounds? = null,
    aodStyle: AodScreenStyle = AodScreenStyle.DEFAULT,
    lyrics: Lyrics? = null,
    lyricsSyncOffsetMs: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val baseTheme = LocalMaterialTheme.current
    val coroutineScope = rememberCoroutineScope()

    val enterProgress = enterProgressProvider().coerceIn(0f, 1f)
    // Only flips once the enter transition has fully finished (and flips back as soon as exit starts),
    // so system bars / brightness changes never happen mid-transition.
    val isSettledInAod by remember {
        androidx.compose.runtime.derivedStateOf { enterProgressProvider() >= 0.999f }
    }

    BackHandler(enabled = enterProgress > 0.05f) {
        onDismiss()
    }

    val activityWindow = remember(context) { context.findActivity()?.window }

    DisposableEffect(activityWindow) {
        view.keepScreenOn = true
        val originalBrightness = activityWindow?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        onDispose {
            view.keepScreenOn = false
            activityWindow?.let { window ->
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = originalBrightness
                window.attributes = lp
            }
        }
    }

    LaunchedEffect(activityWindow, isSettledInAod) {
        activityWindow?.let { window ->
            val insetsController = WindowInsetsControllerCompat(window, view)
            if (isSettledInAod) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = 0.015f
                window.attributes = lp
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    val effectiveQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
    val optimizedAlbumArtUri = remember(albumArtUriString, effectiveQuality) {
        com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(albumArtUriString, effectiveQuality)
            ?: (albumArtUriString ?: "")
    }

    val sourceArtStyle = sourceBounds?.artStyle ?: AodSourceArtStyle.STANDARD_ROUNDED
    val targetPalette = remember(baseTheme, sourceArtStyle) {
        deriveAodPaletteFromAppTheme(baseTheme, sourceArtStyle)
    }

    val colorAnimSpec = tween<Color>(durationMillis = 520, easing = FastOutSlowInEasing)
    val accentColor by animateColorAsState(targetPalette.accent, colorAnimSpec, label = "aodAccent")
    val onAccentColor by animateColorAsState(targetPalette.onAccent, colorAnimSpec, label = "aodOnAccent")
    val secondaryContainerColor by animateColorAsState(
        targetPalette.secondaryContainer,
        colorAnimSpec,
        label = "aodSecondaryContainer"
    )
    val onSecondaryContainerColor by animateColorAsState(
        targetPalette.onSecondaryContainer,
        colorAnimSpec,
        label = "aodOnSecondaryContainer"
    )
    val titlePaletteColor by animateColorAsState(targetPalette.titleColor, colorAnimSpec, label = "aodTitleColor")
    val subtitlePaletteColor by animateColorAsState(targetPalette.subtitleColor, colorAnimSpec, label = "aodSubtitleColor")
    val glowColor by animateColorAsState(targetPalette.glow, colorAnimSpec, label = "aodGlowColor")
    val haloColor by animateColorAsState(targetPalette.haloColor, colorAnimSpec, label = "aodHaloColor")

    // Curated Material You Expressive shapes with smooth slow spin + morph (zero bounce!)
    var previousShapeIndex by remember { mutableIntStateOf(0) }
    var targetShapeIndex by remember { mutableIntStateOf(0) }
    val shapeMorphAnim = remember { Animatable(1f) }
    val tapSpinRotationDeg = remember { Animatable(0f) }

    var firstShapeRun by remember { mutableStateOf(true) }
    LaunchedEffect(songTitle, optimizedAlbumArtUri) {
        // Skip on first composition: the enter transition already morphs the art, and this 780ms
        // morph/spin would otherwise keep running after the 560ms enter tween has finished.
        if (firstShapeRun) {
            firstShapeRun = false
            return@LaunchedEffect
        }
        val nextIdx = pickNextExpressiveShapeIndex(targetShapeIndex)
        previousShapeIndex = targetShapeIndex
        targetShapeIndex = nextIdx
        shapeMorphAnim.snapTo(0f)
        launch {
            shapeMorphAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 780, easing = SmoothMorphEasing)
            )
        }
        launch {
            tapSpinRotationDeg.animateTo(
                targetValue = tapSpinRotationDeg.value + 95f,
                animationSpec = tween(durationMillis = 780, easing = SmoothMorphEasing)
            )
        }
    }

    val isPlaying = isPlayingProvider()

    val syncedLines = remember(lyrics) {
        lyrics?.synced
            ?.map { it.copy(line = sanitizeLyricLineText(it.line)) }
            ?.filter { it.line.isNotBlank() }
            .orEmpty()
    }
    val showLyricsMode = aodStyle == AodScreenStyle.LYRICS && syncedLines.isNotEmpty()
    val lyricsModeProgress by animateFloatAsState(
        targetValue = if (showLyricsMode) 1f else 0f,
        animationSpec = tween(durationMillis = 720, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
        label = "aodLyricsModeProgress"
    )
    val defaultModeAlpha = (1f - lyricsModeProgress).coerceIn(0f, 1f)

    var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    var currentClockTime by remember { mutableStateOf(timeFormatter.format(Date())) }
    var currentClockDate by remember { mutableStateOf(dateFormatter.format(Date())) }

    LaunchedEffect(showLyricsMode, isPlaying) {
        while (true) {
            positionMs = currentPositionProvider()
            val now = Date()
            currentClockTime = timeFormatter.format(now)
            currentClockDate = dateFormatter.format(now)
            delay(if (showLyricsMode && isPlaying) 50L else 150L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aodAmbient")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(4600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            tween(3400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val continuousRotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(36000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "aodSlowSpin"
    )

    // Smooth non-bouncy playing scale
    val artPlayingScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = tween(durationMillis = 480, easing = SmoothMorphEasing),
        label = "artPlayingScale"
    )

    val triggerShapeMorph: () -> Unit = {
        val nextIdx = pickNextExpressiveShapeIndex(targetShapeIndex)
        previousShapeIndex = targetShapeIndex
        targetShapeIndex = nextIdx
        coroutineScope.launch {
            shapeMorphAnim.snapTo(0f)
            launch {
                shapeMorphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 820, easing = SmoothMorphEasing)
                )
            }
            launch {
                tapSpinRotationDeg.animateTo(
                    targetValue = tapSpinRotationDeg.value + 115f,
                    animationSpec = tween(durationMillis = 820, easing = SmoothMorphEasing)
                )
            }
        }
    }

    val smoothEnter = enterProgress * enterProgress * (3f - 2f * enterProgress)
    val sizeT = enterProgress
    val posT = enterProgress
    val aodChromeAlpha = ((enterProgress - 0.34f) / 0.66f).coerceIn(0f, 1f)
    val bgFadeAlpha = ((enterProgress - 0.01f) / 0.88f).coerceIn(0f, 1f)

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val aodArtSizeDp = 240.dp
    val aodArtSizePx = with(density) { aodArtSizeDp.toPx() }

    val rawSrcArtForShape = sourceBounds?.artBounds
    val resolvedSrcArtForShape = if (rawSrcArtForShape != null && rawSrcArtForShape.hasBounds &&
        sourceArtStyle == AodSourceArtStyle.STANDARD_ROUNDED
    ) {
        resolveFocalArtBounds(rawSrcArtForShape, sourceBounds.carouselStyle, density)
    } else {
        rawSrcArtForShape
    }
    val fallbackShapeSrcH = when (sourceArtStyle) {
        AodSourceArtStyle.COMPLEX_ARCH -> screenHeightPx * 0.52f
        AodSourceArtStyle.IMMERSIVE_RECT -> screenHeightPx * 0.48f
        AodSourceArtStyle.STANDARD_ROUNDED -> screenWidthPx - with(density) { 48.dp.toPx() }
    }
    val fallbackShapeSrcW = when (sourceArtStyle) {
        AodSourceArtStyle.STANDARD_ROUNDED -> screenWidthPx - with(density) { 48.dp.toPx() }
        else -> screenWidthPx
    }
    val shapeSrcW = if (resolvedSrcArtForShape != null && resolvedSrcArtForShape.hasBounds && resolvedSrcArtForShape.size.width > 0) {
        resolvedSrcArtForShape.size.width.toFloat()
    } else fallbackShapeSrcW
    val shapeSrcH = if (resolvedSrcArtForShape != null && resolvedSrcArtForShape.hasBounds && resolvedSrcArtForShape.size.height > 0) {
        resolvedSrcArtForShape.size.height.toFloat()
    } else fallbackShapeSrcH

    val effectiveRotationDeg = (tapSpinRotationDeg.value + if (isPlaying) continuousRotationDeg else 0f) * sizeT
    val quantizedRotationDeg = (effectiveRotationDeg * 4f).roundToInt() / 4f
    val quantizedMorph = (shapeMorphAnim.value * 90f).roundToInt() / 90f
    val quantizedEnter = (sizeT * 120f).roundToInt() / 120f
    val standardCornerPx = with(density) { 18.dp.toPx() }
    val complexRightRisePx = sourceBounds?.complexRightRisePx?.takeIf { it > 0f }
        ?: with(density) { (configuration.screenWidthDp.dp * 0.23f).coerceIn(74.dp, 108.dp).toPx() }

    val albumShape = remember(
        previousShapeIndex,
        targetShapeIndex,
        quantizedMorph,
        quantizedRotationDeg,
        quantizedEnter,
        sourceArtStyle,
        complexRightRisePx,
        standardCornerPx,
        shapeSrcW,
        shapeSrcH,
        aodArtSizePx
    ) {
        AodMorphingAlbumShape(
            fromShape = AOD_EXPRESSIVE_SHAPES[previousShapeIndex],
            toShape = AOD_EXPRESSIVE_SHAPES[targetShapeIndex],
            shapeMorphProgress = quantizedMorph,
            shapeRotationRad = Math.toRadians(quantizedRotationDeg.toDouble()),
            enterMorphT = quantizedEnter,
            sourceArtStyle = sourceArtStyle,
            complexRightRisePx = complexRightRisePx,
            standardCornerPx = standardCornerPx,
            srcWidthPx = shapeSrcW,
            srcHeightPx = shapeSrcH,
            destSizePx = aodArtSizePx
        )
    }

    val haloShape = remember(
        previousShapeIndex,
        targetShapeIndex,
        quantizedMorph,
        quantizedRotationDeg
    ) {
        AodMorphingAlbumShape(
            fromShape = AOD_EXPRESSIVE_SHAPES[previousShapeIndex],
            toShape = AOD_EXPRESSIVE_SHAPES[targetShapeIndex],
            shapeMorphProgress = quantizedMorph,
            shapeRotationRad = Math.toRadians((quantizedRotationDeg + 18f).toDouble()),
            enterMorphT = 1f,
            sourceArtStyle = AodSourceArtStyle.STANDARD_ROUNDED,
            complexRightRisePx = complexRightRisePx,
            standardCornerPx = standardCornerPx
        )
    }

    val aodRootCoordsHolder = remember { LayoutCoordsHolder() }
    val aodArtSlotBounds = remember { MorphElementBounds() }
    var slotReady by remember { mutableStateOf(false) }
    // The morphing art is measured before the AOD layout is placed, so on its own it only ever sees the
    // slot position from an earlier pass. Bump this tick whenever the slot really moves (lockscreen
    // launch, system bars hiding, clock text appearing...) so the art re-measures and follows it
    // instead of staying at a stale position next to the halo.
    var slotLayoutTick by remember { mutableIntStateOf(0) }
    val lastSeenSlot = remember { floatArrayOf(Float.NaN, Float.NaN, Float.NaN, Float.NaN) }
    LaunchedEffect(aodArtSlotBounds) {
        while (true) {
            if (aodArtSlotBounds.hasBounds) {
                val sx = aodArtSlotBounds.offset.x
                val sy = aodArtSlotBounds.offset.y
                val sw = aodArtSlotBounds.size.width.toFloat()
                val sh = aodArtSlotBounds.size.height.toFloat()
                if (sx != lastSeenSlot[0] || sy != lastSeenSlot[1] || sw != lastSeenSlot[2] || sh != lastSeenSlot[3]) {
                    lastSeenSlot[0] = sx
                    lastSeenSlot[1] = sy
                    lastSeenSlot[2] = sw
                    lastSeenSlot[3] = sh
                    slotLayoutTick++
                }
            }
            delay(40)
        }
    }
    val aodSongInfoDestBounds = remember { MorphElementBounds() }
    val fallbackTitleMarquee = remember { MarqueeSyncState() }
    val fallbackArtistMarquee = remember { MarqueeSyncState() }
    val aodProgressDestBounds = remember { MorphElementBounds() }

    val liveStatusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val liveNavBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Frozen on first composition (bars still visible) so hiding the system bars cannot shift the AOD layout.
    val statusBarTopDp = remember(configuration.orientation) { liveStatusBarTopDp }
    val navBarBottomDp = remember(configuration.orientation) { liveNavBarBottomDp }

    // Interpolate text colors between Now Playing and AOD palette, and draw background in Draw phase
    val morphedTitleColor = lerp(baseTheme.onPrimaryContainer, titlePaletteColor, sizeT)
    val morphedArtistColor = lerp(baseTheme.onPrimaryContainer.copy(alpha = 0.76f), subtitlePaletteColor, sizeT)

    Box(
        modifier = modifier
            .fillMaxSize()
            .captureRootCoords(aodRootCoordsHolder)
            .graphicsLayer {
                val ep = enterProgressProvider().coerceIn(0f, 1f)
                val sm = ep * ep * (3f - 2f * ep)
                if (showLyricsMode) {
                    val pageScale = lerp(0.88f, 1f, sm)
                    scaleX = pageScale
                    scaleY = pageScale
                    alpha = (ep / 0.35f).coerceIn(0f, 1f)
                    val cornerDp = lerp(44f, 0f, sm).dp
                    if (cornerDp > 0.5.dp) {
                        shape = RoundedCornerShape(cornerDp)
                        clip = true
                    }
                } else {
                    val cornerDp = lerp(36f, 0f, sm).dp
                    if (cornerDp > 0.5.dp) {
                        shape = RoundedCornerShape(cornerDp)
                        clip = true
                    }
                }
            }
            .drawBehind {
                val ep = enterProgressProvider().coerceIn(0f, 1f)
                val sm = ep * ep * (3f - 2f * ep)
                if (showLyricsMode) {
                    drawRect(targetPalette.canvasColor)
                } else {
                    val bgAlpha = ((ep - 0.01f) / 0.88f).coerceIn(0f, 1f)
                    val bgColor = lerp(targetPalette.sourceBgColor, targetPalette.canvasColor, sm)
                    drawRect(bgColor.copy(alpha = bgAlpha))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDismiss() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                }
            }
    ) {
        // =====================================================================
        // LAYER 1: Main Centered AOD Layout (Clock, Album Art Slot, Title, Seekbar, Controls)
        // =====================================================================
        if (defaultModeAlpha > 0.005f) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = defaultModeAlpha }
                .padding(
                    top = (statusBarTopDp + 18.dp).coerceAtLeast(28.dp),
                    bottom = (navBarBottomDp + 20.dp).coerceAtLeast(24.dp),
                    start = 24.dp,
                    end = 24.dp
                )
        ) {
            // Top AOD Expressive Clock & Date Pill (follows app palette)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = aodChromeAlpha
                    translationY = (1f - aodChromeAlpha) * (-18.dp.toPx())
                }
            ) {
                Text(
                    text = currentClockTime,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    letterSpacing = (-1.0).sp,
                    color = titlePaletteColor.copy(alpha = 0.92f)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(secondaryContainerColor.copy(alpha = 0.88f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = currentClockDate,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = onSecondaryContainerColor
                    )
                }
            }

            // Center Stage: Album Art Slot + Metadata + Wavy Progress + Transport Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Album Art destination slot & ambient glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(aodArtSizeDp)
                ) {
                    // Ambient radial palette glow
                    Box(
                        modifier = Modifier
                            .requiredSize(336.dp)
                            .graphicsLayer {
                                val effectiveGlowScale = glowScale * sizeT
                                scaleX = effectiveGlowScale
                                scaleY = effectiveGlowScale
                                alpha = (if (isPlaying) glowAlpha else glowAlpha * 0.6f) * aodChromeAlpha
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.44f),
                                        glowColor.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Subtle tonal expressive backing halo (solid fill, no wireframe outline)
                    Box(
                        modifier = Modifier
                            .requiredSize(256.dp)
                            .graphicsLayer {
                                val s = artPlayingScale * lerp(0.85f, 1f, aodChromeAlpha)
                                scaleX = s
                                scaleY = s
                                alpha = aodChromeAlpha * 0.42f
                            }
                            .clip(haloShape)
                            .background(haloColor)
                    )

                    // Invisible slot anchor capturing exact destination coordinates for the morphing Album Art
                    Box(
                        modifier = Modifier
                            .size(aodArtSizeDp)
                            .captureUntransformedBounds(aodRootCoordsHolder, aodArtSlotBounds)
                            .onGloballyPositioned { slotReady = true }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Song Title & Artist — morphs container bounds and shared marquee offset from Now Playing
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .captureUntransformedBounds(aodRootCoordsHolder, aodSongInfoDestBounds)
                ) {
                    val srcSongInfo = sourceBounds?.songInfoBounds
                    val destW = aodSongInfoDestBounds.size.width.toFloat()
                    val srcW = if (srcSongInfo != null && srcSongInfo.hasBounds && srcSongInfo.size.width > 0) {
                        srcSongInfo.size.width.toFloat()
                    } else destW

                    Column(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val dW = constraints.maxWidth.toFloat()
                                val hasSrc = srcSongInfo != null && srcSongInfo.hasBounds && srcSongInfo.size.width > 0
                                val sW = if (hasSrc) srcSongInfo.size.width.toFloat() else dW
                                val curW = lerp(sW, dW, posT).roundToInt().coerceAtLeast(1)
                                val placeable = measurable.measure(
                                    constraints.copy(minWidth = curW, maxWidth = curW)
                                )
                                layout(constraints.maxWidth, placeable.height) {
                                    val dx = if (hasSrc && aodSongInfoDestBounds.hasBounds) {
                                        (srcSongInfo.offset.x - aodSongInfoDestBounds.offset.x) * (1f - posT)
                                    } else 0f
                                    val dy = if (hasSrc && aodSongInfoDestBounds.hasBounds) {
                                        (srcSongInfo.offset.y - aodSongInfoDestBounds.offset.y) * (1f - posT)
                                    } else 0f
                                    placeable.place(dx.roundToInt(), dy.roundToInt())
                                }
                            }
                            .graphicsLayer {
                                val hasSrc = srcSongInfo != null && srcSongInfo.hasBounds
                                alpha = if (hasSrc) {
                                    1f
                                } else {
                                    ((enterProgress - 0.45f) / 0.40f).coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        SharedPlayerMarqueeText(
                            text = songTitle,
                            style = TextStyle(
                                fontFamily = GoogleSansRounded,
                                color = morphedTitleColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                letterSpacing = 0.sp
                            ),
                            marqueeState = sourceBounds?.titleMarquee ?: fallbackTitleMarquee,
                            shouldRunMarquee = isSettledInAod,
                            isAodOverlay = true,
                            enterProgress = posT,
                            srcWidthPx = srcW,
                            destWidthPx = destW,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        SharedPlayerMarqueeText(
                            text = artistName,
                            style = TextStyle(
                                fontFamily = GoogleSansRounded,
                                color = morphedArtistColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.sp
                            ),
                            marqueeState = sourceBounds?.artistMarquee ?: fallbackArtistMarquee,
                            shouldRunMarquee = isSettledInAod,
                            isAodOverlay = true,
                            enterProgress = posT,
                            srcWidthPx = srcW,
                            destWidthPx = destW,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                val totalMs = totalDurationProvider().coerceAtLeast(0L)
                val liveProgress = if (totalMs > 0L) {
                    (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                AodExpressiveProgressBar(
                    progress = liveProgress,
                    positionMs = positionMs,
                    totalMs = totalMs,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    trackContainerColor = secondaryContainerColor,
                    subtitleColor = subtitlePaletteColor,
                    onSeekTo = onSeekTo,
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .captureUntransformedBounds(aodRootCoordsHolder, aodProgressDestBounds)
                        .graphicsLayer {
                            val src = sourceBounds?.progressBounds
                            if (sourceArtStyle != AodSourceArtStyle.COMPLEX_ARCH &&
                                src != null && src.hasBounds && aodProgressDestBounds.hasBounds
                            ) {
                                val destX = aodProgressDestBounds.offset.x
                                val destY = aodProgressDestBounds.offset.y
                                translationX = (src.offset.x - destX) * (1f - posT)
                                translationY = (src.offset.y - destY) * (1f - posT)
                            }
                            alpha = ((enterProgress - 0.45f) / 0.40f).coerceIn(0f, 1f)
                        }
                )

                Spacer(Modifier.height(24.dp))

                val controlAnimationSpec = remember {
                    tween<Float>(durationMillis = 240, easing = FastOutSlowInEasing)
                }

                // Playback Controls — Previous, Play/Pause, Next morph in-place from Now Playing slots!
                val isComplexSource = sourceArtStyle == AodSourceArtStyle.COMPLEX_ARCH
                AnimatedPlaybackControls(
                    modifier = Modifier.fillMaxWidth(),
                    isPlayingProvider = isPlayingProvider,
                    onPrevious = onSkipPreviousClick,
                    onPlayPause = onPlayPauseClick,
                    onNext = onSkipNextClick,
                    height = 80.dp,
                    pressAnimationSpec = controlAnimationSpec,
                    releaseDelay = 220L,
                    colorOtherButtons = secondaryContainerColor,
                    colorPlayPause = accentColor,
                    tintPlayPauseIcon = onAccentColor,
                    tintOtherIcons = onSecondaryContainerColor,
                    colorPreviousButton = secondaryContainerColor,
                    colorNextButton = secondaryContainerColor,
                    tintPreviousIcon = onSecondaryContainerColor,
                    tintNextIcon = onSecondaryContainerColor,
                    playPauseCornerPlaying = 26.dp,
                    playPauseCornerPaused = 26.dp,
                    rootCoordsHolder = aodRootCoordsHolder,
                    sourcePrevBounds = sourceBounds?.prevBounds,
                    sourcePlayBounds = sourceBounds?.playBounds,
                    sourceNextBounds = sourceBounds?.nextBounds,
                    sourceMorphProgressProvider = enterProgressProvider,
                    sourceBtnCornerDp = if (isComplexSource) 34.dp else 40.dp,
                    sourcePlayCornerDp = if (isComplexSource) {
                        if (isPlaying) 20.dp else 34.dp
                    } else {
                        26.dp
                    },
                    sourceColorOtherButtons = baseTheme.onPrimary,
                    sourceColorPlayPause = baseTheme.primary,
                    sourceTintPlayPauseIcon = baseTheme.onPrimary,
                    sourceTintOtherIcons = baseTheme.primary
                )
            }

            // Subtle bottom AOD hint pill following app palette
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = aodChromeAlpha * 0.72f
                        translationY = (1f - aodChromeAlpha) * 12.dp.toPx()
                    }
                    .clip(RoundedCornerShape(50))
                    .background(secondaryContainerColor.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Tap art to morph shape  •  Double-tap to exit",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = onSecondaryContainerColor.copy(alpha = 0.82f)
                )
            }
        }
        }

        // =====================================================================
        // LAYER 2: Morphing Album Art (Morphs from Now Playing bounds & shape -> AOD Expressive Shape)
        // =====================================================================
        val lyricsArtDissolveAlpha = ((1f - enterProgress / 0.62f).coerceIn(0f, 1f))
        val effectiveArtLayerAlpha = lerp(lyricsArtDissolveAlpha, 1f, defaultModeAlpha)
        if (effectiveArtLayerAlpha > 0.005f) {
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    // Re-run layout once the destination slot has reported its bounds; otherwise the art
                    // stays at the fallback position whenever enterProgress is constant (lockscreen AOD).
                    @Suppress("UNUSED_VARIABLE")
                    val slotReadyTick = slotReady
                    @Suppress("UNUSED_VARIABLE")
                    val slotMovedTick = slotLayoutTick
                    val rawSrcArt = sourceBounds?.artBounds
                    val srcArt = if (rawSrcArt != null && rawSrcArt.hasBounds &&
                        sourceArtStyle == AodSourceArtStyle.STANDARD_ROUNDED
                    ) {
                        resolveFocalArtBounds(rawSrcArt, sourceBounds.carouselStyle, density)
                    } else {
                        rawSrcArt
                    }
                    val fallbackSrcH = when (sourceArtStyle) {
                        AodSourceArtStyle.COMPLEX_ARCH -> screenHeightPx * 0.52f
                        AodSourceArtStyle.IMMERSIVE_RECT -> screenHeightPx * 0.48f
                        AodSourceArtStyle.STANDARD_ROUNDED -> screenWidthPx - with(density) { 48.dp.toPx() }
                    }
                    val fallbackSrcW = when (sourceArtStyle) {
                        AodSourceArtStyle.STANDARD_ROUNDED -> screenWidthPx - with(density) { 48.dp.toPx() }
                        else -> screenWidthPx
                    }
                    val fallbackSrcX = if (sourceArtStyle == AodSourceArtStyle.STANDARD_ROUNDED) {
                        with(density) { 24.dp.toPx() }
                    } else 0f
                    val fallbackSrcY = if (sourceArtStyle == AodSourceArtStyle.STANDARD_ROUNDED) {
                        with(density) { 96.dp.toPx() }
                    } else 0f

                    val srcW = if (srcArt != null && srcArt.hasBounds && srcArt.size.width > 0) {
                        srcArt.size.width.toFloat()
                    } else fallbackSrcW
                    val srcH = if (srcArt != null && srcArt.hasBounds && srcArt.size.height > 0) {
                        srcArt.size.height.toFloat()
                    } else fallbackSrcH
                    val srcX = if (srcArt != null && srcArt.hasBounds) srcArt.offset.x else fallbackSrcX
                    val srcY = if (srcArt != null && srcArt.hasBounds) srcArt.offset.y else fallbackSrcY

                    val destW = if (aodArtSlotBounds.hasBounds && aodArtSlotBounds.size.width > 0) {
                        aodArtSlotBounds.size.width.toFloat()
                    } else aodArtSizePx
                    val destH = if (aodArtSlotBounds.hasBounds && aodArtSlotBounds.size.height > 0) {
                        aodArtSlotBounds.size.height.toFloat()
                    } else aodArtSizePx

                    val effectiveSizeT = sizeT * defaultModeAlpha
                    val effectivePosT = posT * defaultModeAlpha
                    val curW = lerp(srcW, destW, effectiveSizeT).roundToInt().coerceAtLeast(1)
                    val curH = lerp(srcH, destH, effectiveSizeT).roundToInt().coerceAtLeast(1)

                    val placeable = measurable.measure(Constraints.fixed(curW, curH))
                    layout(curW, curH) {
                        // Read aodArtSlotBounds.offset inside placement (after Layer 1 has been placed)
                        // so the destination coordinates are always up-to-date on the very first frame.
                        val destX = if (aodArtSlotBounds.hasBounds) {
                            aodArtSlotBounds.offset.x
                        } else (screenWidthPx - destW) * 0.5f
                        val destY = if (aodArtSlotBounds.hasBounds) {
                            aodArtSlotBounds.offset.y
                        } else (screenHeightPx * 0.24f)
                        val placeX = lerp(srcX, destX, effectivePosT).roundToInt()
                        val placeY = lerp(srcY, destY, effectivePosT).roundToInt()
                        placeable.place(placeX, placeY)
                    }
                }
                .graphicsLayer {
                    val baseReadyAlpha = if (slotReady || !showLyricsMode.not()) 1f else 0f
                    alpha = baseReadyAlpha * effectiveArtLayerAlpha
                    val startScale = if (sourceArtStyle == AodSourceArtStyle.STANDARD_ROUNDED) artPlayingScale else 1f
                    val defaultScale = lerp(startScale, artPlayingScale, sizeT)
                    val lyricsDissolveScale = lerp(1f, 0.90f, smoothEnter)
                    val effectiveScale = lerp(lyricsDissolveScale, defaultScale, defaultModeAlpha)
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                }
                .pointerInput(showLyricsMode) {
                    if (showLyricsMode) return@pointerInput
                    var lastTapTimeMs = 0L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapTimeMs < 280L) {
                                lastTapTimeMs = 0L
                                onDismiss()
                            } else {
                                lastTapTimeMs = now
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                triggerShapeMorph()
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(albumShape)
            ) {
                SmartImage(
                    model = optimizedAlbumArtUri,
                    contentDescription = songTitle,
                    contentScale = ContentScale.Crop,
                    targetSize = coil.size.Size.ORIGINAL,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val srcImgScale = if (sourceArtStyle == AodSourceArtStyle.COMPLEX_ARCH && isPlaying) 1.03f else 1f
                            val imgScale = lerp(srcImgScale, 1f, sizeT)
                            scaleX = imgScale
                            scaleY = imgScale
                        }
                )
                // Top scrim the real Now Playing art draws for status-bar legibility. Without it the real art
                // pops darker at the top the instant it replaces this stand-in.
                val topShadowHeightDp = sourceBounds?.topShadowHeightDp ?: 0f
                if (topShadowHeightDp > 0f) {
                    val topShadowStart = sourceBounds?.topShadowStartAlpha ?: 0f
                    val topShadowMid = sourceBounds?.topShadowMidAlpha ?: 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topShadowHeightDp.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer { alpha = (1f - sizeT).coerceIn(0f, 1f) }
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = topShadowStart),
                                        Color.Black.copy(alpha = topShadowMid),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                // Same bottom wash the immersive Now Playing art draws, so the hand-off to the real art
                // at the end of the exit transition doesn't pop (and the image's dark matte stays hidden).
                val washColorForArt = sourceBounds?.washColor ?: Color.Unspecified
                if (sourceArtStyle == AodSourceArtStyle.IMMERSIVE_RECT && washColorForArt != Color.Unspecified) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = (1f - sizeT).coerceIn(0f, 1f) }
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to if (sourceBounds?.topScrimAlpha ?: 0f > 0f) Color.Black.copy(alpha = sourceBounds!!.topScrimAlpha) else Color.Transparent,
                                        (sourceBounds?.washStart ?: 0.55f) to Color.Transparent,
                                        (sourceBounds?.washMid ?: 0.80f) to washColorForArt.copy(alpha = sourceBounds?.washMidAlpha ?: 0.85f),
                                        1.0f to washColorForArt
                                    )
                                )
                            )
                    )
                }
            }

            // Stand-in Complex curved arch seekbar drawn at 100% opacity ON TOP of the clipped stand-in art
            // and scaled down by thickness rather than alpha so the dark album art underneath never bleeds
            // through the top half of the track/thumb to create a two-tone split along the arch seam.
            val arcVisScale = (1f - sizeT * 3.0f).coerceIn(0f, 1f)
            if (sourceArtStyle == AodSourceArtStyle.COMPLEX_ARCH && arcVisScale > 0.02f) {
                val totalMsForArc = totalDurationProvider().coerceAtLeast(0L)
                val liveProgressForArc = if (totalMsForArc > 0L) {
                    (positionMs.toFloat() / totalMsForArc.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val seekProgressForArc = sourceBounds?.currentSeekProgress
                    ?.takeIf { it >= 0f }
                    ?.let { lerp(it, liveProgressForArc, sizeT) }
                    ?: liveProgressForArc
                val activeArcColor = baseTheme.primary
                val inactiveArcColor = baseTheme.onPrimary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@Canvas

                    val srcW0 = shapeSrcW.coerceAtLeast(1f)
                    val srcH0 = shapeSrcH.coerceAtLeast(1f)
                    val destD = aodArtSizePx.coerceAtLeast(1f)
                    val halfW0 = srcW0 * 0.5f
                    val halfH0 = srcH0 * 0.5f
                    val minDim0 = min(srcW0, srcH0)
                    val srcCornerR = minDim0 * (sizeT * 2.8f).coerceAtMost(0.46f)
                    val innerCornerX = (halfW0 - srcCornerR).coerceAtLeast(0f)
                    val innerCornerY = (halfH0 - srcCornerR).coerceAtLeast(0f)

                    val archCx0 = srcW0 * 0.25f
                    val dxRight0 = (srcW0 - archCx0).coerceAtLeast(1f)
                    val sRight0 = complexRightRisePx.coerceIn(16f, (srcH0 - 4f).coerceAtLeast(16f))
                    val archRadius0 = (sRight0 / 2f) + ((dxRight0 * dxRight0) / (2f * sRight0))
                    val archCy0 = srcH0 - archRadius0

                    val edgeInsetPx = lerp(18.dp.toPx(), 42.dp.toPx(), sizeT)
                    val leftChord = (archCx0 - edgeInsetPx).coerceAtLeast(0f)
                    val rightChord = ((srcW0 - edgeInsetPx) - archCx0).coerceAtLeast(0f)
                    val alphaStartDeg = Math.toDegrees(asin((leftChord / archRadius0).coerceIn(-1f, 1f).toDouble())).toFloat()
                    val alphaEndDeg = Math.toDegrees(asin((rightChord / archRadius0).coerceIn(-1f, 1f).toDouble())).toFloat()

                    val startAngleDeg = 90f + alphaStartDeg
                    val totalSweepDeg = -(alphaStartDeg + alphaEndDeg)
                    val thumbAngleDeg = startAngleDeg + (totalSweepDeg * seekProgressForArc)

                    val geomScale = lerp(1f, destD / srcW0, sizeT)
                    val strokeScale = geomScale * arcVisScale
                    val trackStrokePx = 10.dp.toPx() * strokeScale
                    val gapArcLengthPx = 13.dp.toPx()
                    val gapDeg = Math.toDegrees((gapArcLengthPx / archRadius0).toDouble()).toFloat()

                    val shapeRotRad = Math.toRadians(quantizedRotationDeg.toDouble())
                    val fromKind = AOD_EXPRESSIVE_SHAPES[previousShapeIndex]
                    val toKind = AOD_EXPRESSIVE_SHAPES[targetShapeIndex]

                    fun mapArchPoint(circleAngleDeg: Float, radialOffsetPx: Float): Offset {
                        val rad = Math.toRadians(circleAngleDeg.toDouble())
                        val xOnArch = archCx0 + archRadius0 * cos(rad).toFloat()
                        val yOnArch = archCy0 + archRadius0 * sin(rad).toFloat()
                        val dx0 = xOnArch - halfW0
                        val dy0 = yOnArch - halfH0
                        val rayLen = sqrt(dx0 * dx0 + dy0 * dy0).coerceAtLeast(1e-4f)
                        val cosT = dx0 / rayLen
                        val sinT = dy0 / rayLen
                        val absCos = abs(cosT).coerceAtLeast(1e-5f)
                        val absSin = abs(sinT).coerceAtLeast(1e-5f)

                        var rRect0 = min(halfW0 / absCos, halfH0 / absSin)
                        if (srcCornerR > 0.5f) {
                            val px = rRect0 * absCos
                            val py = rRect0 * absSin
                            if (px > innerCornerX && py > innerCornerY) {
                                val dot = innerCornerX * absCos + innerCornerY * absSin
                                val cVal = innerCornerX * innerCornerX + innerCornerY * innerCornerY - srcCornerR * srcCornerR
                                val disc = (dot * dot - cVal).coerceAtLeast(0f)
                                rRect0 = min(rRect0, dot + sqrt(disc))
                            }
                        }
                        val kSmooth = srcCornerR * 0.65f
                        val rSrcBoundary = if (kSmooth > 0.5f) {
                            val hSm = (0.5f + 0.5f * (rayLen - rRect0) / kSmooth).coerceIn(0f, 1f)
                            lerp(rayLen, rRect0, hSm) - kSmooth * hSm * (1f - hSm)
                        } else {
                            min(rRect0, rayLen)
                        }
                        val r0 = rSrcBoundary + radialOffsetPx * arcVisScale

                        val theta = atan2(dy0.toDouble(), dx0.toDouble())
                        val rotatedTheta = theta - shapeRotRad
                        val rFrom = fromKind.normalizedRadiusAt(rotatedTheta)
                        val rTo = toKind.normalizedRadiusAt(rotatedTheta)
                        val r1 = destD * 0.476f * lerp(rFrom, rTo, quantizedMorph) + radialOffsetPx * strokeScale
                        val fR = lerp(r0, r1, sizeT)
                        return Offset(
                            x = w * 0.5f + fR * cosT,
                            y = h * 0.5f + fR * sinT
                        )
                    }

                    val activeSweepDeg = (thumbAngleDeg - startAngleDeg) + gapDeg
                    if (activeSweepDeg < -0.15f) {
                        val clampedSweep = activeSweepDeg.coerceAtLeast(totalSweepDeg)
                        val segs = 32
                        val activePath = Path().apply {
                            for (i in 0..segs) {
                                val a = startAngleDeg + clampedSweep * (i.toFloat() / segs)
                                val pt = mapArchPoint(a, 0f)
                                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = activePath,
                            color = activeArcColor,
                            style = Stroke(width = trackStrokePx, cap = StrokeCap.Round)
                        )
                    }

                    val inactiveStartDeg = thumbAngleDeg - gapDeg
                    val endAngleDeg = startAngleDeg + totalSweepDeg
                    val inactiveSweepDeg = endAngleDeg - inactiveStartDeg
                    if (inactiveSweepDeg < -0.15f) {
                        val segs = 32
                        val inactivePath = Path().apply {
                            for (i in 0..segs) {
                                val a = inactiveStartDeg + inactiveSweepDeg * (i.toFloat() / segs)
                                val pt = mapArchPoint(a, 0f)
                                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = inactivePath,
                            color = inactiveArcColor,
                            style = Stroke(width = trackStrokePx, cap = StrokeCap.Round)
                        )
                    }

                    val halfThumbLenPx = 12.dp.toPx()
                    val innerPt = mapArchPoint(thumbAngleDeg, -halfThumbLenPx)
                    val outerPt = mapArchPoint(thumbAngleDeg, +halfThumbLenPx)
                    drawLine(
                        color = activeArcColor,
                        start = innerPt,
                        end = outerPt,
                        strokeWidth = 7.dp.toPx() * strokeScale,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        }

        // =====================================================================
        // LAYER 3: Smooth Line-by-Line AOD Lyrics View (No Controls)
        // =====================================================================
        if (lyricsModeProgress > 0.005f && syncedLines.isNotEmpty()) {
            val lyricsEnterT = ((enterProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
            val smoothLyricsEnter = lyricsEnterT * lyricsEnterT * (3f - 2f * lyricsEnterT)
            val effectivePositionMs = (positionMs + lyricsSyncOffsetMs.toLong()).coerceAtLeast(0L)

            AodLiveLyricsView(
                syncedLines = syncedLines,
                effectivePositionMs = effectivePositionMs,
                titleColor = titlePaletteColor,
                accentColor = accentColor,
                topInsetDp = (statusBarTopDp + 24.dp).coerceAtLeast(36.dp),
                bottomInsetDp = (navBarBottomDp + 24.dp).coerceAtLeast(36.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = smoothLyricsEnter * lyricsModeProgress
                        translationY = (1f - smoothLyricsEnter) * 14.dp.toPx()
                    }
            )
        }
    }
}

private val IOSLyricsFluidEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

@Composable
private fun AodLiveLyricsView(
    syncedLines: List<SyncedLine>,
    effectivePositionMs: Long,
    titleColor: Color,
    accentColor: Color,
    topInsetDp: Dp,
    bottomInsetDp: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentLineIndex = remember(effectivePositionMs, syncedLines) {
        syncedLines.indexOfLast { it.time.toLong() <= effectivePositionMs }
    }
    val activeAnchorIndex = currentLineIndex.coerceAtLeast(0)

    val scrollYAnim = remember { Animatable(0f) }
    var targetScrollY by remember { mutableFloatStateOf(0f) }
    var hasAlignedInitialScroll by remember(syncedLines) { mutableStateOf(false) }

    LaunchedEffect(targetScrollY, syncedLines) {
        if (!hasAlignedInitialScroll) {
            scrollYAnim.snapTo(targetScrollY)
            hasAlignedInitialScroll = true
        } else {
            scrollYAnim.animateTo(
                targetValue = targetScrollY,
                animationSpec = tween(durationMillis = 780, easing = IOSLyricsFluidEasing)
            )
        }
    }

    val lineSpacingPx = with(density) { 28.dp.toPx().roundToInt() }

    Box(modifier = modifier.fillMaxSize()) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .clipToBounds(),
            content = {
                syncedLines.forEachIndexed { index, line ->
                    val signedDistance = if (currentLineIndex >= 0) {
                        index - currentLineIndex
                    } else {
                        index
                    }
                    key("${line.time}_$index") {
                        AodLineByLineItem(
                            line = line,
                            isCurrentLine = index == currentLineIndex,
                            signedDistance = signedDistance,
                            titleColor = titleColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val itemConstraints = constraints.copy(minWidth = width, maxWidth = width, minHeight = 0)
            val placeables = measurables.map { it.measure(itemConstraints) }

            val itemTops = IntArray(placeables.size)
            val itemCenters = FloatArray(placeables.size)
            var yCursor = 0
            for (i in placeables.indices) {
                itemTops[i] = yCursor
                val h = placeables[i].height
                itemCenters[i] = yCursor + h * 0.5f
                yCursor += h + lineSpacingPx
            }

            if (placeables.isNotEmpty()) {
                val safeIndex = activeAnchorIndex.coerceIn(0, placeables.lastIndex)
                val focalY = height * 0.42f
                val computedTarget = itemCenters[safeIndex] - focalY
                if (abs(computedTarget - targetScrollY) > 0.5f || !hasAlignedInitialScroll) {
                    targetScrollY = computedTarget
                }
            }

            layout(width, height) {
                val currentScroll = if (hasAlignedInitialScroll) {
                    scrollYAnim.value.roundToInt()
                } else {
                    targetScrollY.roundToInt()
                }
                for (i in placeables.indices) {
                    val p = placeables[i]
                    val y = itemTops[i] - currentScroll
                    if (y + p.height >= -lineSpacingPx * 2 && y <= height + lineSpacingPx * 2) {
                        p.placeRelative(0, y)
                    }
                }
            }
        }

        // Top & bottom smooth AMOLED black edge fades so lines glide cleanly in and out
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topInsetDp + 84.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black,
                            0.45f to Color.Black.copy(alpha = 0.82f),
                            1.0f to Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomInsetDp + 104.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Black.copy(alpha = 0.82f),
                            1.0f to Color.Black
                        )
                    )
                )
        )
    }
}

@Composable
private fun AodLineByLineItem(
    line: SyncedLine,
    isCurrentLine: Boolean,
    signedDistance: Int,
    titleColor: Color,
    accentColor: Color
) {
    val absDistance = abs(signedDistance)

    val targetScale = when {
        isCurrentLine -> 1.0f
        absDistance == 1 -> 0.88f
        absDistance == 2 -> 0.80f
        else -> 0.74f
    }
    val targetAlpha = when {
        isCurrentLine -> 1.0f
        signedDistance == -1 -> 0.32f
        signedDistance == 1 -> 0.48f
        absDistance == 2 -> 0.20f
        absDistance == 3 -> 0.08f
        else -> 0.0f
    }
    val targetBlurDp = when {
        isCurrentLine -> 0.dp
        absDistance == 1 -> 1.2.dp
        absDistance == 2 -> 2.8.dp
        else -> 5.0.dp
    }
    val targetWaveOffsetDp = when {
        isCurrentLine -> 0.dp
        signedDistance < 0 -> (-4).dp
        else -> 6.dp
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 680, easing = IOSLyricsFluidEasing),
        label = "aodLineScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 620, easing = IOSLyricsFluidEasing),
        label = "aodLineAlpha"
    )
    val animatedBlurDp by animateDpAsState(
        targetValue = targetBlurDp,
        animationSpec = tween(durationMillis = 640, easing = IOSLyricsFluidEasing),
        label = "aodLineBlur"
    )
    val animatedWaveOffsetDp by animateDpAsState(
        targetValue = targetWaveOffsetDp,
        animationSpec = tween(durationMillis = 720, easing = IOSLyricsFluidEasing),
        label = "aodLineWaveOffset"
    )

    val activeHighlightColor = remember(titleColor, accentColor) {
        lerp(titleColor, accentColor, 0.22f)
    }
    val targetLineColor = remember(isCurrentLine, signedDistance, activeHighlightColor, titleColor) {
        when {
            isCurrentLine -> activeHighlightColor
            signedDistance < 0 -> titleColor.copy(alpha = 0.72f)
            else -> titleColor.copy(alpha = 0.86f)
        }
    }
    val animatedLineColor by animateColorAsState(
        targetValue = targetLineColor,
        animationSpec = tween(durationMillis = 520, easing = IOSLyricsFluidEasing),
        label = "aodLineColor"
    )

    val lineTextStyle = remember {
        TextStyle(
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
                translationY = animatedWaveOffsetDp.toPx()
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(if (animatedBlurDp > 0.1.dp) Modifier.blur(animatedBlurDp) else Modifier),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = line.line,
            style = lineTextStyle,
            color = animatedLineColor
        )

        val translation = line.translation?.takeIf { it.isNotBlank() }
        if (translation != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = translation,
                style = TextStyle(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = animatedLineColor.copy(alpha = 0.62f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AodExpressiveProgressBar(
    progress: Float,
    positionMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    trackContainerColor: Color,
    subtitleColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestTotalMs by rememberUpdatedState(totalMs)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(progress) }
    var lastHapticBucket by remember { mutableIntStateOf(-1) }

    val displayedProgress = if (isScrubbing) scrubProgress else progress
    val animatedProgress by animateFloatAsState(
        targetValue = displayedProgress.coerceIn(0f, 1f),
        animationSpec = if (isScrubbing) tween(40, easing = LinearEasing) else tween(180, easing = LinearEasing),
        label = "aodProgress"
    )

    val interactionFraction by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "aodScrubFraction"
    )

    val waveAmplitudeFraction by animateFloatAsState(
        targetValue = if (isPlaying && !isScrubbing) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "aodWaveAmp"
    )

    val strokeWidthDp = 8.dp
    val edgeInsetDp = 8.dp
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
    val edgeInsetPx = with(density) { edgeInsetDp.toPx() }
    val restThumbWidthPx = with(density) { 6.dp.toPx() }
    val activeThumbWidthPx = with(density) { 8.dp.toPx() }
    val restThumbHeightPx = with(density) { 24.dp.toPx() }
    val activeThumbHeightPx = with(density) { 30.dp.toPx() }

    val trackStroke = remember(strokeWidthPx) {
        Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(Unit) {
                    fun progressForX(x: Float): Float {
                        val inset = edgeInsetPx.coerceIn(0f, size.width / 2f)
                        val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                        return ((x - inset) / trackWidth).coerceIn(0f, 1f)
                    }

                    awaitEachGesture {
                        try {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isScrubbing = true
                            down.consume()
                            var currentVal = progressForX(down.position.x)
                            scrubProgress = currentVal
                            lastHapticBucket = (currentVal * 25f).toInt()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break
                                pointerId = change.id
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                    currentVal = progressForX(change.position.x)
                                    scrubProgress = currentVal
                                    val bucket = (currentVal * 25f).toInt()
                                    if (bucket != lastHapticBucket) {
                                        lastHapticBucket = bucket
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            if (latestTotalMs > 0L) {
                                latestOnSeekTo((currentVal * latestTotalMs).toLong())
                            }
                        } finally {
                            isScrubbing = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgeInsetDp),
                color = accentColor,
                trackColor = lerp(trackContainerColor, accentColor, 0.25f),
                stroke = trackStroke,
                trackStroke = trackStroke,
                gapSize = 11.dp,
                stopSize = 4.dp,
                amplitude = { p -> if (p > 0f) waveAmplitudeFraction else 0f },
                wavelength = 38.dp,
                waveSpeed = 19.dp
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = edgeInsetPx.coerceIn(0f, size.width / 2f)
                val trackStart = inset
                val trackEnd = size.width - inset
                val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)

                val thumbWidth = lerp(restThumbWidthPx, activeThumbWidthPx, interactionFraction)
                val thumbHeight = lerp(restThumbHeightPx, activeThumbHeightPx, interactionFraction)
                val rawThumbX = trackStart + trackWidth * animatedProgress
                val minCenter = (thumbWidth / 2f).coerceAtMost(size.width / 2f)
                val maxCenter = (size.width - thumbWidth / 2f).coerceAtLeast(minCenter)
                val thumbX = rawThumbX.coerceIn(minCenter, maxCenter)
                val thumbY = size.height / 2f

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(thumbX - thumbWidth / 2f, thumbY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val shownPositionMs = if (isScrubbing && totalMs > 0L) {
            (scrubProgress * totalMs).toLong()
        } else {
            positionMs
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgeInsetDp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatAodTime(shownPositionMs),
                fontFamily = GoogleSansRounded,
                color = accentColor.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = formatAodTime(totalMs),
                fontFamily = GoogleSansRounded,
                color = subtitleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatAodTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
