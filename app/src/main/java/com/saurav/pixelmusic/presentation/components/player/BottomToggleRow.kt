package com.saurav.pixelmusic.presentation.components.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.ToggleSegmentButton
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBackdrop
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme

@Composable
fun BottomToggleRow(
    modifier: Modifier,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    activeColorMain: Color = MaterialTheme.colorScheme.primary,
    activeColorSecondary: Color = MaterialTheme.colorScheme.secondary,
    activeColorTertiary: Color = MaterialTheme.colorScheme.tertiary,
    onActiveColorMain: Color = MaterialTheme.colorScheme.onPrimary,
    onActiveColorSecondary: Color = MaterialTheme.colorScheme.onSecondary,
    onActiveColorTertiary: Color = MaterialTheme.colorScheme.onTertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color? = null,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current
) {
    val isFavorite = isFavoriteProvider()
    val rowCorners = 60.dp
    val rowShape = RoundedCornerShape(rowCorners)
    val isDark = LocalPixelMusicDarkTheme.current
    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    val isLiquidGlassGlobal = LocalLiquidGlassEnabled.current
    val isGlass = isLiquidGlass || isLiquidGlassGlobal

    val resolvedInactiveColor = if (isGlass) {
        if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
    } else {
        inactiveColor
    }
    val resolvedInactiveContentColor = if (isGlass) {
        if (isDark) Color.White else Color(0xFF1C1C1E)
    } else {
        inactiveContentColor
    }

    val pillTint = MaterialTheme.colorScheme.surfaceContainer
    val resolvedContainerColor = containerColor ?: if (isDark) {
        pillTint.copy(alpha = 0.5f)
    } else {
        Color.White.copy(alpha = 0.9f)
    }

    val containerModifier = if (isGlass && backdrop != null) {
        modifier
            .clip(rowShape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { rowShape },
                effects = {
                    vibrancy()
                    val blurRadius = (10.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                    blur(blurRadius)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                        lens(
                            refractionHeight = 14.dp.toPx() * glassRefractionIntensity,
                            refractionAmount = 32.dp.toPx() * glassRefractionIntensity
                        )
                    }
                },
                layerBlock = {
                    shadowElevation = 0f
                },
                onDrawSurface = {
                    drawRect(if (isDark) Color(0x18FFFFFF) else Color(0x35FFFFFF))
                    drawRect(pillTint, blendMode = BlendMode.Hue)
                    drawRect(pillTint.copy(alpha = if (isDark) 0.30f else 0.20f))
                }
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.75f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.20f)
                    )
                ),
                shape = rowShape
            )
    } else {
        modifier
            .clip(rowShape)
            .background(color = resolvedContainerColor, shape = rowShape)
    }

    Box(
        modifier = containerModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(rowShape)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commonModifier = Modifier.weight(1f)

            ToggleSegmentButton(
                modifier = commonModifier,
                active = isShuffleEnabled,
                activeColor = activeColorMain,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorMain,
                inactiveColor = resolvedInactiveColor,
                inactiveContentColor = resolvedInactiveContentColor,
                onClick = onShuffleToggle,
                iconId = R.drawable.rounded_shuffle_24,
                contentDesc = "Shuffle"
            )
            val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.rounded_repeat_one_24
                Player.REPEAT_MODE_ALL -> R.drawable.rounded_repeat_24
                else -> R.drawable.rounded_repeat_24
            }
            ToggleSegmentButton(
                modifier = commonModifier,
                active = repeatActive,
                activeColor = activeColorSecondary,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorSecondary,
                inactiveColor = resolvedInactiveColor,
                inactiveContentColor = resolvedInactiveContentColor,
                onClick = onRepeatToggle,
                iconId = repeatIcon,
                contentDesc = "Repeat"
            )
            ToggleSegmentButton(
                modifier = commonModifier,
                active = isFavorite,
                activeColor = activeColorTertiary,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorTertiary,
                inactiveColor = resolvedInactiveColor,
                inactiveContentColor = resolvedInactiveContentColor,
                onClick = onFavoriteToggle,
                iconId = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                contentDesc = "Favorite"
            )
        }
    }
}
