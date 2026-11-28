package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

/**
 * Material You Expressive smooth wave easing used across PixelMusic
 * for corner-only morphs and touch-point radial color waves.
 */
val ExpressiveWaveEasing = CubicBezierEasing(0.32f, 0.0f, 0.18f, 1.0f)

/**
 * Draws a smooth, soft-edged radial wave of [waveColor] spreading outward from
 * the user's exact touch point on tap, purely in the GPU draw phase with zero
 * layout shifts or gesture interference.
 */
fun Modifier.expressiveTapColorWave(
    waveColor: Color,
    shape: Shape? = null,
    durationMillis: Int = 560,
    enabled: Boolean = true
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val waveProgress = remember { Animatable(0f) }
    val waveAlpha = remember { Animatable(0f) }
    var waveOrigin by remember { mutableStateOf<Offset?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    val latestEnabled = androidx.compose.runtime.rememberUpdatedState(enabled)

    val clipModifier = if (shape != null) Modifier.clip(shape) else Modifier

    this
        .then(clipModifier)
        .pointerInput(durationMillis) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                if (!latestEnabled.value) return@awaitEachGesture
                waveOrigin = down.position
                activeJob?.cancel()
                activeJob = scope.launch {
                    waveProgress.snapTo(0f)
                    waveAlpha.snapTo(1f)
                    val spreadJob = launch {
                        waveProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = durationMillis,
                                easing = ExpressiveWaveEasing
                            )
                        )
                    }
                    delay((durationMillis * 0.42f).toLong())
                    waveAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = (durationMillis * 0.65f).toInt(),
                            easing = FastOutSlowInEasing
                        )
                    )
                    spreadJob.join()
                }
            }
        }
        .drawWithContent {
            drawContent()
            val alpha = waveAlpha.value
            val p = waveProgress.value
            if (alpha > 0.005f && p > 0.001f) {
                val origin = waveOrigin ?: Offset(size.width * 0.5f, size.height * 0.5f)
                val maxDx = max(origin.x, size.width - origin.x)
                val maxDy = max(origin.y, size.height - origin.y)
                val maxRadius = hypot(maxDx, maxDy).coerceAtLeast(1f)
                val feather = maxRadius * 0.40f
                val outerRadius = (p * (maxRadius + feather)).coerceAtLeast(1f)
                val solidStop = ((outerRadius - feather) / outerRadius).coerceIn(0f, 1f)
                val effectiveColor = waveColor.copy(alpha = (waveColor.alpha * alpha).coerceIn(0f, 1f))
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to effectiveColor,
                            solidStop to effectiveColor,
                            1f to Color.Transparent
                        ),
                        center = origin,
                        radius = outerRadius
                    )
                )
            }
        }
}

/**
 * Draws a persistent Material You Expressive selection background where
 * [selectedColor] spreads smoothly like a liquid wave from the touch point
 * over [unselectedColor] when [isSelected] becomes true, and recedes smoothly
 * when deselected.
 */
fun Modifier.expressiveSelectionColorWave(
    isSelected: Boolean,
    unselectedColor: Color,
    selectedColor: Color,
    shape: Shape,
    durationMillis: Int = 600
): Modifier = composed {
    var waveOrigin by remember { mutableStateOf<Offset?>(null) }

    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = ExpressiveWaveEasing),
        label = "expressive_selection_wave"
    )

    this
        .clip(shape)
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                waveOrigin = down.position
            }
        }
        .drawBehind {
            drawRect(color = unselectedColor)
            val p = selectionProgress
            if (p >= 0.999f) {
                drawRect(color = selectedColor)
            } else if (p > 0.001f) {
                val origin = waveOrigin ?: Offset(size.width * 0.5f, size.height * 0.5f)
                val maxDx = max(origin.x, size.width - origin.x)
                val maxDy = max(origin.y, size.height - origin.y)
                val maxRadius = hypot(maxDx, maxDy).coerceAtLeast(1f)
                val feather = maxRadius * 0.38f
                val outerRadius = (p * (maxRadius + feather)).coerceAtLeast(1f)
                val solidStop = ((outerRadius - feather) / outerRadius).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to selectedColor,
                            solidStop to selectedColor,
                            1f to selectedColor.copy(alpha = 0f)
                        ),
                        center = origin,
                        radius = outerRadius
                    )
                )
            }
        }
}
