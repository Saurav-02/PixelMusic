package com.saurav.pixelmusic.presentation.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBackdrop
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassChromaticAberration
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassDepthEffect
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassHighlightOpacity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionAmount
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionHeight
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassStyle
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceOpacity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceTint
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassVibrancyIntensity
import kotlinx.coroutines.launch

/**
 * Unified Liquid Glass interactive surface and physical shader pipeline modifier.
 *
 * Implements the core physical glass rules:
 * 1. REAL BACKDROP SAMPLING: Samples live content behind the element using Kyant0's backdrop shader.
 * 2. LIVE OPTICAL SHADER: colorControls() saturation scaling -> Gaussian blur() -> AGSL lens() with depthEffect and chromaticAberration.
 * 3. ZERO WHITE FOG / FADE: Keeps glass crystal-clear without milky white gradient overlays.
 * 4. ZERO SHADOW: Sets shadowElevation = 0f in graphicsLayer when Liquid Glass is active.
 * 5. SPECULAR RIM BORDER: High-contrast adaptive vertical gradient border with spectral dispersion when enabled.
 * 6. PHYSICAL MICRO-DEFLECTION: Subtle 2% spring touch deflection, avoiding cartoonish warping.
 * 7. SEAMLESS FALLBACK: When liquid glass is disabled or backdrop is unavailable, provides clean matching optical fallback.
 */
@Composable
fun Modifier.liquidGlassInteractiveSurface(
    shape: Shape = CircleShape,
    isCircle: Boolean = false,
    borderWidth: Dp = 0.75.dp,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    surfaceTint: Color? = null
): Modifier {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    if (!isLiquidGlass || !enabled) return this

    val activeBackdrop = backdrop ?: LocalLiquidGlassBackdrop.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary

    val blurIntensity = LocalLiquidGlassBlurIntensity.current
    val refractionIntensity = LocalLiquidGlassRefractionIntensity.current
    val vibrancyIntensity = LocalLiquidGlassVibrancyIntensity.current
    val refractionHeight = LocalLiquidGlassRefractionHeight.current
    val refractionAmount = LocalLiquidGlassRefractionAmount.current
    val chromaticAberration = LocalLiquidGlassChromaticAberration.current
    val depthEffect = LocalLiquidGlassDepthEffect.current
    val glassStyle = LocalLiquidGlassStyle.current
    val tintSetting = LocalLiquidGlassSurfaceTint.current
    val surfaceOpacity = LocalLiquidGlassSurfaceOpacity.current

    val glassBorderBrush = remember(isDark, chromaticAberration) {
        if (chromaticAberration) {
            Brush.verticalGradient(
                colors = if (isDark) listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color(0xFF80D8FF).copy(alpha = 0.18f),
                    Color(0xFFFF80AB).copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.06f)
                ) else listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color(0xFF80D8FF).copy(alpha = 0.22f),
                    Color(0xFFFF80AB).copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = if (isDark) listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.05f)
                ) else listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        }
    }

    val press = remember { Animatable(0f) }
    val jiggle = remember { Animatable(0f) }
    val pressSpec = remember { spring<Float>(dampingRatio = 0.6f, stiffness = 500f) }
    val jiggleSpec = remember { spring<Float>(dampingRatio = 0.4f, stiffness = 350f) }
    val scope = rememberCoroutineScope()

    val scaleDeflection = if (isCircle) 0.025f else 0.02f

    val baseModifier = this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch {
                    jiggle.snapTo(1f)
                    jiggle.animateTo(0f, jiggleSpec)
                }
                scope.launch { press.animateTo(1f, pressSpec) }
                waitForUpOrCancellation()
                scope.launch { press.animateTo(0f, pressSpec) }
            }
        }

    return if (activeBackdrop != null) {
        baseModifier
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    if (vibrancyIntensity > 0.01f) {
                        colorControls(saturation = 1f + (vibrancyIntensity * 0.5f))
                    }
                    val baseBlurRadius = when (glassStyle) {
                        "TRANSPARENT" -> 3.dp.toPx()
                        else -> 12.dp.toPx()
                    }
                    val blurRadiusPx = (baseBlurRadius * blurIntensity).coerceAtLeast(1f)
                    blur(blurRadiusPx)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && refractionIntensity > 0.01f && glassStyle != "BLUR") {
                        lens(
                            refractionHeight = (10.dp.toPx() * refractionIntensity * refractionHeight).coerceAtMost(16.dp.toPx()),
                            refractionAmount = (20.dp.toPx() * refractionIntensity * refractionAmount).coerceAtMost(24.dp.toPx()),
                            depthEffect = false,
                            chromaticAberration = chromaticAberration
                        )
                    }
                },
                layerBlock = {
                    shadowElevation = 0f
                    scaleX = 1f + (press.value * scaleDeflection) + (jiggle.value * 0.015f)
                    scaleY = 1f - (press.value * scaleDeflection) - (jiggle.value * 0.015f)
                },
                onDrawSurface = {
                    val tint = surfaceTint ?: when (tintSetting) {
                        "WHITE" -> Color.White.copy(alpha = surfaceOpacity.coerceIn(0.04f, 0.25f))
                        "DARK" -> Color(0xFF141416).copy(alpha = surfaceOpacity.coerceIn(0.06f, 0.30f))
                        "ACCENT" -> primaryColor.copy(alpha = (surfaceOpacity * 0.4f).coerceIn(0.05f, 0.25f))
                        else -> if (isDark) Color(0x10FFFFFF) else Color(0x1FFFFFFF)
                    }
                    drawRect(tint)
                }
            )
            .border(BorderStroke(borderWidth, glassBorderBrush), shape)
            .clip(shape)
    } else {
        baseModifier
            .graphicsLayer {
                shadowElevation = 0f
                scaleX = 1f + (press.value * scaleDeflection) + (jiggle.value * 0.015f)
                scaleY = 1f - (press.value * scaleDeflection) - (jiggle.value * 0.015f)
            }
            .clip(shape)
            .background(
                color = surfaceTint ?: when (tintSetting) {
                    "WHITE" -> Color.White.copy(alpha = surfaceOpacity.coerceIn(0.06f, 0.30f))
                    "DARK" -> Color(0xFF141416).copy(alpha = surfaceOpacity.coerceIn(0.08f, 0.35f))
                    "ACCENT" -> primaryColor.copy(alpha = (surfaceOpacity * 0.5f).coerceIn(0.06f, 0.30f))
                    else -> if (isDark) Color(0x10FFFFFF) else Color(0x20FFFFFF)
                },
                shape = shape
            )
            .border(BorderStroke(borderWidth, glassBorderBrush), shape)
    }
}
