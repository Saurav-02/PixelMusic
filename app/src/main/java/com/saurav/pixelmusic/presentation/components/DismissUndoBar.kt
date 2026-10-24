package com.saurav.pixelmusic.presentation.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun DismissUndoBar(
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    durationMillis: Long,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null
) {
    val progress = remember { Animatable(1f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()
    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    LaunchedEffect(key1 = onUndo) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = durationMillis.toInt(),
                easing = LinearEasing
            )
        )
    }

    if (isLiquidGlass && backdrop != null) {
        val undoBarShape = CircleShape
        val progressFillColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { undoBarShape },
                    effects = {
                        vibrancy()
                        val blurRadius = (10.dp.toPx() * glassBlurIntensity).coerceAtLeast(1f)
                        blur(blurRadius)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && glassRefractionIntensity > 0.01f) {
                            lens(
                                refractionHeight = (12.dp.toPx() * glassRefractionIntensity).coerceAtMost(16.dp.toPx()),
                                refractionAmount = (28.dp.toPx() * glassRefractionIntensity).coerceAtMost(32.dp.toPx())
                            )
                        }
                    },
                    layerBlock = {
                        shadowElevation = 0f
                    },
                    onDrawSurface = {
                        drawRect(if (isDark) Color(0x18FFFFFF) else Color(0x35FFFFFF))
                    }
                )
                .border(
                    border = BorderStroke(
                        0.75.dp,
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.07f else 0.14f),
                                Color.White.copy(alpha = if (isDark) 0.02f else 0.04f)
                            )
                        )
                    ),
                    shape = undoBarShape
                )
        ) {
            // Top gloss reflection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.04f else 0.08f),
                                Color.Transparent
                            )
                        ),
                        shape = undoBarShape
                    )
            )

            // Fluid countdown progress bar drawn in draw-phase (zero recompositions during countdown)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val frac = progress.value.coerceIn(0f, 1f)
                        val w = size.width * frac
                        if (w > 0.5f) {
                            val r = size.height / 2f
                            drawRoundRect(
                                color = progressFillColor,
                                size = Size(w, size.height),
                                cornerRadius = CornerRadius(r, r)
                            )
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = stringResource(R.string.playlist_dismissed_message),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val undoPress = remember { Animatable(0f) }
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val undoScale = 1f - (undoPress.value * 0.08f)
                                scaleX = undoScale
                                scaleY = undoScale
                            }
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.30f else 0.20f)
                            )
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = if (isDark) 0.35f else 0.50f)
                                ),
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                val spec = spring<Float>(dampingRatio = 0.5f, stiffness = 450f)
                                awaitEachGesture {
                                    awaitFirstDown()
                                    coroutineScope.launch { undoPress.animateTo(1f, spec) }
                                    waitForUpOrCancellation()
                                    coroutineScope.launch { undoPress.animateTo(0f, spec) }
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onUndo
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_undo),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansRounded,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val closePress = remember { Animatable(0f) }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                val closeScale = 1f - (closePress.value * 0.08f)
                                scaleX = closeScale
                                scaleY = closeScale
                            }
                            .clip(CircleShape)
                            .background(
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)
                            )
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.40f)
                                ),
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                val spec = spring<Float>(dampingRatio = 0.5f, stiffness = 450f)
                                awaitEachGesture {
                                    awaitFirstDown()
                                    coroutineScope.launch { closePress.animateTo(1f, spec) }
                                    waitForUpOrCancellation()
                                    coroutineScope.launch { closePress.animateTo(0f, spec) }
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = stringResource(R.string.playlist_dismissed_message),
                        style = MaterialTheme.typography.titleSmall
                            .copy(
                                fontFamily = GoogleSansRounded
                            ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = onUndo
                        ) {
                            Text(stringResource(R.string.action_undo), color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledIconButton(
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = onClose
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = AbsoluteSmoothCornerShape(
                                cornerRadiusTR = 12.dp,
                                smoothnessAsPercentTL = 60,
                                cornerRadiusTL = 12.dp,
                                smoothnessAsPercentTR = 60,
                                cornerRadiusBR = 12.dp,
                                smoothnessAsPercentBL = 60,
                                cornerRadiusBL = 12.dp,
                                smoothnessAsPercentBR = 60
                            )
                        )
                )
            }
        }
    }
}
