package com.saurav.pixelmusic.presentation.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBackdrop
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentValueRange by rememberUpdatedState(valueRange)
    val currentSteps by rememberUpdatedState(steps)

    val isPressed = remember { mutableStateOf(false) }
    val pressAnim = remember { Animatable(0f) }
    val jelloStretch = remember { Animatable(0f) }
    val edgeRubberBand = remember { Animatable(0f) }
    val lastHapticStep = remember { mutableIntStateOf(-1) }

    // Normalized fraction from 0f to 1f
    val currentFraction = remember(value, valueRange) {
        val span = valueRange.endInclusive - valueRange.start
        if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    }

    var dragFraction by remember { mutableFloatStateOf(currentFraction) }
    LaunchedEffect(currentFraction, isPressed.value) {
        if (!isPressed.value) {
            dragFraction = currentFraction
        }
    }

    // iOS Horizontal Pill Thumb geometry (42dp width × 22dp height)
    val thumbBaseWidth = 42.dp
    val thumbBaseHeight = 22.dp
    val trackHeight = 6.dp
    val sliderAreaHeight = 44.dp
    val trackShape = RoundedCornerShape(3.dp)
    val thumbShape = RoundedCornerShape(11.dp)

    val trackBorder = remember(isDark) {
        BorderStroke(
            0.75.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.15f else 0.28f),
                    Color.White.copy(alpha = if (isDark) 0.04f else 0.08f)
                )
            )
        )
    }

    var widthPx by remember { mutableFloatStateOf(0f) }
    val thumbWidthPx = with(density) { thumbBaseWidth.toPx() }
    val thumbHalfWidthPx = thumbWidthPx * 0.5f
    val usableWidthPx = (widthPx - thumbWidthPx).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(sliderAreaHeight)
            .onSizeChanged { widthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {

        val usableWidthRef = rememberUpdatedState(usableWidthPx)
        val thumbHalfWidthRef = rememberUpdatedState(thumbHalfWidthPx)

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(enabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isPressed.value = true
                    down.consume()
                    coroutineScope.launch { pressAnim.animateTo(1f, spring(0.55f, 400f)) }

                    val uWidth = usableWidthRef.value
                    val thumbHalf = thumbHalfWidthRef.value
                    var lastX = down.position.x

                    fun computeFractionFromX(x: Float): Float {
                        val raw = (x - thumbHalf) / uWidth
                        return when {
                            raw >= 0.94f -> 1.0f
                            raw <= 0.06f -> 0.0f
                            else -> ((raw - 0.06f) / (0.94f - 0.06f)).coerceIn(0f, 1f)
                        }
                    }

                    var currentFrac = dragFraction
                    val currentThumbCenter = thumbHalf + (currentFrac * uWidth)
                    if (abs(down.position.x - currentThumbCenter) > thumbHalf * 1.5f) {
                        currentFrac = computeFractionFromX(down.position.x)
                    }

                    fun emitValue(frac: Float) {
                        dragFraction = frac
                        val vRange = currentValueRange
                        val stps = currentSteps

                        val newValue = if (frac >= 0.98f) {
                            vRange.endInclusive
                        } else if (frac <= 0.02f) {
                            vRange.start
                        } else if (stps > 0) {
                            val stepCount = stps + 1
                            val steppedFraction = (frac * stepCount).roundToInt() / stepCount.toFloat()
                            val computed = vRange.start + steppedFraction * (vRange.endInclusive - vRange.start)
                            computed.coerceIn(vRange.start, vRange.endInclusive)
                        } else {
                            val computed = vRange.start + frac * (vRange.endInclusive - vRange.start)
                            val rounded = (computed * 1000f).roundToInt() / 1000f
                            rounded.coerceIn(vRange.start, vRange.endInclusive)
                        }

                        val stepIndex = if (stps > 0) {
                            ((newValue - vRange.start) / ((vRange.endInclusive - vRange.start) / (stps + 1))).roundToInt()
                        } else {
                            (frac * 40f).roundToInt()
                        }
                        if (stepIndex != lastHapticStep.intValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticStep.intValue = stepIndex
                        }

                        if (newValue != value) {
                            currentOnValueChange(newValue)
                        }
                    }

                    emitValue(currentFrac)

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            val currentX = change.position.x
                            val deltaX = currentX - lastX
                            lastX = currentX

                            // Smooth 1:1 delta drag tracking so thumb easily reaches 100% full
                            val deltaFraction = deltaX / uWidth
                            currentFrac = (currentFrac + deltaFraction).coerceIn(-0.08f, 1.08f)

                            val overdrag = if (currentFrac < 0f) {
                                currentFrac * uWidth * 0.35f
                            } else if (currentFrac > 1f) {
                                (currentFrac - 1f) * uWidth * 0.35f
                            } else {
                                0f
                            }
                            coroutineScope.launch { edgeRubberBand.snapTo(overdrag) }

                            val effectiveFraction = currentFrac.coerceIn(0f, 1f)
                            val targetStretch = (deltaX / 14f).coerceIn(-1f, 1f)
                            coroutineScope.launch { jelloStretch.snapTo(targetStretch) }

                            emitValue(effectiveFraction)
                        }
                    } finally {
                        isPressed.value = false
                        coroutineScope.launch { pressAnim.animateTo(0f, spring(0.5f, 380f)) }
                        coroutineScope.launch { jelloStretch.animateTo(0f, spring(0.42f, 300f)) }
                        coroutineScope.launch { edgeRubberBand.animateTo(0f, spring(0.45f, 320f)) }
                        currentOnValueChangeFinished?.invoke()
                    }
                }
            }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderAreaHeight)
                .then(gestureModifier),
            contentAlignment = Alignment.CenterStart
        ) {
            // Inactive Track
            val trackModifier = if (backdrop != null) {
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(trackShape)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { trackShape },
                        effects = {
                            vibrancy()
                            val blurRadius = (8.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                            blur(blurRadius)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                                lens(
                                    refractionHeight = 8.dp.toPx() * glassRefractionIntensity,
                                    refractionAmount = 20.dp.toPx() * glassRefractionIntensity
                                )
                            }
                        },
                        onDrawSurface = {
                            drawRect(if (isDark) Color(0x18FFFFFF) else Color(0x35FFFFFF))
                        }
                    )
                    .border(trackBorder, trackShape)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(trackShape)
                    .background(
                        if (isDark) Color(0x35FFFFFF) else Color(0x22000000),
                        trackShape
                    )
                    .border(trackBorder, trackShape)
            }
            Box(modifier = trackModifier)

            // Active Track (Fluid glowing fill drawn in draw-phase without per-frame layout measurement)
            val activeFraction = if (isPressed.value) dragFraction.coerceIn(0f, 1f) else currentFraction
            val activeTrackBrush = remember(activeColor) {
                Brush.horizontalGradient(
                    listOf(
                        activeColor.copy(alpha = 0.85f),
                        activeColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(trackShape)
                    .drawWithContent {
                        val activeW = (thumbHalfWidthPx + (activeFraction * usableWidthPx) + edgeRubberBand.value)
                            .coerceIn(0f, size.width)
                        if (activeW > 0.5f) {
                            val r = size.height * 0.5f
                            drawRoundRect(
                                brush = activeTrackBrush,
                                size = Size(activeW, size.height),
                                cornerRadius = CornerRadius(r, r)
                            )
                        }
                    }
            )

            // Liquid Glass Horizontal Pill Thumb (Bigger 42dp x 22dp, shadow-free, affected by blur & saturation)
            val thumbModifier = if (backdrop != null) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { thumbShape },
                    effects = {
                        vibrancy()
                        val blurRadius = (14.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                        blur(blurRadius)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                            lens(
                                refractionHeight = (10.dp.toPx() * glassRefractionIntensity).coerceAtMost(11.dp.toPx()),
                                refractionAmount = (22.dp.toPx() * glassRefractionIntensity).coerceAtMost(22.dp.toPx())
                            )
                        }
                    },
                    onDrawSurface = {
                        val surfaceAlpha = (0.45f * glassBlurIntensity).coerceIn(0.20f, 0.75f)
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = (surfaceAlpha + 0.15f).coerceAtMost(0.95f)),
                                    Color.White.copy(alpha = surfaceAlpha)
                                )
                            )
                        )
                        if (glassRefractionIntensity > 0.05f) {
                            drawRect(
                                color = activeColor.copy(alpha = (0.22f * glassRefractionIntensity).coerceIn(0.05f, 0.45f)),
                                blendMode = BlendMode.SrcOver
                            )
                        }
                    }
                )
            } else {
                val baseAlpha = (0.50f * glassBlurIntensity).coerceIn(0.25f, 0.85f)
                Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = (baseAlpha + 0.15f).coerceAtMost(0.95f)),
                            Color.White.copy(alpha = baseAlpha)
                        )
                    ),
                    thumbShape
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        val thumbOffsetPx = (activeFraction * usableWidthPx) + edgeRubberBand.value
                        IntOffset(thumbOffsetPx.roundToInt(), 0)
                    }
                    .size(width = thumbBaseWidth, height = thumbBaseHeight)
                    .graphicsLayer {
                        shadowElevation = 0f
                        val sX = 1f + (pressAnim.value * 0.02f)
                        val sY = 1f - (pressAnim.value * 0.02f)
                        scaleX = sX
                        scaleY = sY
                    }
                    .clip(thumbShape)
                    .then(thumbModifier)
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.40f)
                                )
                            )
                        ),
                        thumbShape
                    )
            )
        }
    }
}
