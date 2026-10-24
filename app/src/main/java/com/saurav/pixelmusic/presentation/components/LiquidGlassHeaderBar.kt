package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

@Composable
fun LiquidGlassHeaderBar(
    pillText: String,
    onPillClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillIcon: (@Composable () -> Unit)? = null,
    pillTextSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    isLiquidGlass: Boolean = true,
    backdrop: Backdrop? = null,
    alpha: Float = 1f
) {
    val haptic = LocalHapticFeedback.current
    val activeBackdrop = backdrop ?: com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBackdrop.current

    if (alpha <= 0.005f) return

    Row(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 1. HEADER PILL ──────────────────────────────────────────
        val pillShape = CircleShape
        val headerBarHeight = if (pillTextSize >= 24.sp) 52.dp else 48.dp

        val pillModifier = Modifier
            .height(headerBarHeight)
            .liquidGlassInteractiveSurface(
                shape = pillShape,
                isCircle = false,
                enabled = isLiquidGlass,
                backdrop = activeBackdrop
            )
            .then(
                if (!isLiquidGlass) {
                    Modifier
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f), pillShape)
                } else Modifier
            )

        Box(
            modifier = pillModifier
                .pointerInput(alpha) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && alpha > 0.05f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPillClick()
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (pillIcon != null) 10.dp else 0.dp)
            ) {
                if (pillIcon != null) {
                    pillIcon()
                }
                Text(
                    text = pillText,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = pillTextSize,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ── 2. SETTINGS ICON BUTTON ──────────────────────────────────
        val settingsShape = CircleShape
        val settingsModifier = Modifier
            .size(headerBarHeight)
            .liquidGlassInteractiveSurface(
                shape = settingsShape,
                isCircle = true,
                enabled = isLiquidGlass,
                backdrop = activeBackdrop
            )
            .then(
                if (!isLiquidGlass) {
                    Modifier
                        .clip(settingsShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f), settingsShape)
                } else Modifier
            )

        Box(
            modifier = settingsModifier
                .pointerInput(alpha) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && alpha > 0.05f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSettingsClick()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_settings_24),
                contentDescription = stringResource(id = R.string.settings_top_bar_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
