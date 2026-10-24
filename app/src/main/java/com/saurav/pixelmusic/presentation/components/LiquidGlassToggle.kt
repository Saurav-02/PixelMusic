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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.sin

@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val activeColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    val checkedState by rememberUpdatedState(checked)
    val onCheckedChangeState by rememberUpdatedState(onCheckedChange)

    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbBaseSize = 24.dp
    val thumbStretchExtra = 8.dp
    val trackPadding = 4.dp
    val maxOffset = trackWidth - thumbBaseSize - (trackPadding * 2) // 20.dp

    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val thumbBaseSizePx = with(density) { thumbBaseSize.toPx() }
    val thumbStretchExtraPx = with(density) { thumbStretchExtra.toPx() }

    val isInteracting = remember { mutableStateOf(false) }
    val pressAnim = remember { Animatable(0f) }
    val thumbProgress = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        if (!isInteracting.value) {
            thumbProgress.animateTo(
                targetValue = if (checked) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f)
            )
        }
    }

    val trackShape = CircleShape
    val thumbShape = RoundedCornerShape(12.dp)

    val p = thumbProgress.value.coerceIn(0f, 1f)

    val trackBorder = remember(p, isDark, activeColor) {
        val borderColor = lerp(
            Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
            activeColor.copy(alpha = 0.6f),
            p
        )
        BorderStroke(0.75.dp, borderColor)
    }

    val gestureModifier = if (enabled && onCheckedChange != null) {
        Modifier.pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                isInteracting.value = true
                down.consume()

                coroutineScope.launch { pressAnim.animateTo(1f, spring(0.6f, 400f)) }

                val startProgress = thumbProgress.value
                val downX = down.position.x
                var hasDragged = false

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        val dx = change.position.x - downX
                        if (abs(dx) > 6f) {
                            hasDragged = true
                            val progressDelta = dx / maxOffsetPx
                            val target = (startProgress + progressDelta).coerceIn(0f, 1f)
                            coroutineScope.launch { thumbProgress.snapTo(target) }
                        }
                    }
                } finally {
                    isInteracting.value = false
                    coroutineScope.launch { pressAnim.animateTo(0f, spring(0.5f, 380f)) }

                    val targetChecked = if (hasDragged) {
                        thumbProgress.value > 0.5f
                    } else {
                        !checkedState
                    }

                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (targetChecked != checkedState) {
                        onCheckedChangeState?.invoke(targetChecked)
                    } else {
                        coroutineScope.launch {
                            thumbProgress.animateTo(
                                targetValue = if (targetChecked) 1f else 0f,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f)
                            )
                        }
                    }
                }
            }
        }
    } else Modifier

    val trackModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { trackShape },
            effects = {
                vibrancy()
                val blurRadius = (10.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                blur(blurRadius)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                    lens(
                        refractionHeight = 12.dp.toPx() * glassRefractionIntensity,
                        refractionAmount = 28.dp.toPx() * glassRefractionIntensity
                    )
                }
            },
            onDrawSurface = {
                val tintAlpha = p * 0.45f
                if (tintAlpha > 0.01f) {
                    drawRect(color = activeColor.copy(alpha = tintAlpha), blendMode = BlendMode.SrcOver)
                }
                val neutralAlpha = (1f - p) * (if (isDark) 0.20f else 0.28f)
                if (neutralAlpha > 0.01f) {
                    drawRect(color = if (isDark) Color(0x18FFFFFF) else Color(0x35FFFFFF))
                }
            }
        )
    } else {
        val trackBgColor = lerp(
            if (isDark) Color(0x35FFFFFF) else Color(0x22000000),
            if (enabled) activeColor else activeColor.copy(alpha = 0.5f),
            p
        )
        Modifier.background(trackBgColor, trackShape)
    }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .graphicsLayer {
                val s = 1f - (pressAnim.value * 0.04f)
                scaleX = s
                scaleY = s
            }
            .clip(trackShape)
            .then(trackModifier)
            .border(trackBorder, trackShape)
            .then(gestureModifier)
            .padding(trackPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Liquid Glass Thumb (Translucent refractive glass with specular rim)
        val thumbModifier = if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { thumbShape },
                effects = {
                    vibrancy()
                    val blurRadius = (8.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                    blur(blurRadius)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                        lens(
                            refractionHeight = (8.dp.toPx() * glassRefractionIntensity).coerceAtMost(12.dp.toPx()),
                            refractionAmount = (18.dp.toPx() * glassRefractionIntensity).coerceAtMost(24.dp.toPx())
                        )
                    }
                },
                onDrawSurface = {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.55f else 0.75f),
                                Color.White.copy(alpha = if (isDark) 0.30f else 0.50f)
                            )
                        )
                    )
                }
            )
        } else {
            Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.65f else 0.85f),
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.55f)
                    )
                ),
                thumbShape
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    val curP = thumbProgress.value.coerceIn(0f, 1f)
                    val curLeftPx = (curP * maxOffsetPx).coerceIn(0f, maxOffsetPx)
                    IntOffset(curLeftPx.roundToInt(), 0)
                }
                .size(thumbBaseSize)
                .graphicsLayer {
                    shadowElevation = 0f
                    val s = 1f + (pressAnim.value * 0.02f)
                    scaleX = s
                    scaleY = 1f - (pressAnim.value * 0.02f)
                }
                .clip(thumbShape)
                .then(thumbModifier)
                .border(
                    BorderStroke(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.45f)
                            )
                        )
                    ),
                    thumbShape
                )
        )
    }
}
