package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.presentation.components.liquidGlassInteractiveSurface
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled

/**
 * Optical Liquid Glass navigation tab with true backdrop blur, lens refraction,
 * and unified glass styling.
 */
@Composable
fun TabAnimation(
    modifier: Modifier = Modifier,
    index: Int,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    onSelectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.surface,
    onUnselectedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
    title: String,
    selectedIndex: Int,
    onClick: () -> Unit,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isSelected = index == selectedIndex
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val tabShape = CircleShape

    val tabGlassTint = if (isLiquidGlass) {
        if (isSelected) {
            primaryColor.copy(alpha = if (isDark) 0.35f else 0.28f)
        } else {
            null // uses default crystal clear glass tint
        }
    } else null

    val contentColor by animateColorAsState(
        targetValue = if (isLiquidGlass) {
            if (isSelected) {
                if (isDark) Color.White else primaryColor
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            }
        } else {
            if (isSelected) onSelectedColor else onUnselectedColor
        },
        animationSpec = tween(durationMillis = 200),
        label = "Tab Content Color"
    )

    Tab(
        modifier = modifier
            .padding(all = 4.dp)
            .liquidGlassInteractiveSurface(
                shape = tabShape,
                isCircle = false,
                surfaceTint = tabGlassTint,
                enabled = isLiquidGlass
            )
            .then(
                if (!isLiquidGlass) {
                    Modifier
                        .clip(tabShape)
                        .background(if (isSelected) selectedColor else unselectedColor, tabShape)
                } else Modifier
            ),
        selected = isSelected,
        text = content,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        selectedContentColor = contentColor,
        unselectedContentColor = contentColor
    )
}
