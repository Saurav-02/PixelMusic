@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.size.Size
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

internal val LocalMaterialTheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

val MiniPlayerHeight = 64.dp
const val ANIMATION_DURATION_MS = 255
val MiniPlayerBottomSpacer = 8.dp

@Composable
fun getNavigationBarHeight(): Dp {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    return sanitizeNavigationBarBottomInset(insets.calculateBottomPadding())
}

@Composable
internal fun MiniPlayerContentInternal(
    song: Song,
    isPlaying: Boolean,
    isCastConnecting: Boolean,
    isPreparingPlayback: Boolean,
    isBuffering: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    cornerRadiusAlb: Dp,
    onNext: () -> Unit,
    isLiquidGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val controlsEnabled = !isCastConnecting && !isPreparingPlayback && !isBuffering

    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val miniPlayerIndication = remember { ripple(bounded = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArtModel = song.albumArtUriString?.takeIf { it.isNotBlank() }
        Box(contentAlignment = Alignment.Center) {
            key(song.id) {
                SmartImage(crossfadeDurationMillis = 0, 
                    model = albumArtModel,
                    contentDescription = "Carátula de ${song.title}",
                    shape = CircleShape,
                    targetSize = Size(150, 150),
                    modifier = Modifier.size(44.dp),
                    placeholderModel = if (albumArtModel?.startsWith("telegram_art") == true) {
                        "$albumArtModel?quality=thumb"
                    } else null
                )
            }
            if (isCastConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = LocalMaterialTheme.current.onPrimaryContainer
                )
            } else if (isPreparingPlayback) {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        val coroutineScope = rememberCoroutineScope()
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            val titleColor = if (isLiquidGlass) (if (isDark) Color.White else Color(0xFF1C1C1E)) else LocalMaterialTheme.current.onPrimaryContainer
            val artistColor = if (isLiquidGlass) (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.75f) else LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.7f)
            val gradientEdge = LocalMaterialTheme.current.primaryContainer

            val titleStyle = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFamily = GoogleSansRounded,
                color = titleColor
            )
            val artistStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                fontFamily = GoogleSansRounded,
                color = artistColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    AutoScrollingText(
                        text = when {
                            isCastConnecting -> "Connecting to device…"
                            isPreparingPlayback -> "Preparing playback…"
                            else -> song.title
                        },
                        style = titleStyle,
                        gradientEdgeColor = gradientEdge
                    )
                }
            }
            AutoScrollingText(
                text = if (isPreparingPlayback) "Loading audio…" else song.displayArtist,
                style = artistStyle,
                gradientEdgeColor = gradientEdge
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        val prevPress = remember { Animatable(0f) }
        val prevJiggle = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = 1f + (prevPress.value * 0.10f) + (prevJiggle.value * 0.16f)
                    scaleY = 1f - (prevPress.value * 0.08f) - (prevJiggle.value * 0.14f)
                }
                .clip(CircleShape)
                .background(
                    if (isLiquidGlass) {
                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.10f)
                    } else {
                        LocalMaterialTheme.current.onPrimary
                    }
                )
                .then(
                    if (isLiquidGlass) {
                        Modifier.border(
                            0.75.dp,
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.12f),
                            CircleShape
                        )
                    } else Modifier
                )
                .pointerInput(controlsEnabled) {
                    if (!controlsEnabled) return@pointerInput
                    val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 450f)
                    val jiggleSpec = spring<Float>(dampingRatio = 0.32f, stiffness = 280f)
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        coroutineScope.launch {
                            prevJiggle.snapTo(1f)
                            prevJiggle.animateTo(0f, jiggleSpec)
                        }
                        coroutineScope.launch { prevPress.animateTo(1f, pressSpec) }
                        waitForUpOrCancellation()
                        coroutineScope.launch { prevPress.animateTo(0f, pressSpec) }
                    }
                }
                .clickable(
                    interactionSource = previousInteraction,
                    indication = null,
                    enabled = controlsEnabled
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrevious()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Anterior",
                tint = if (isLiquidGlass) (if (isDark) Color.White else Color(0xFF1C1C1E)) else LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val playPausePress = remember { Animatable(0f) }
        val playPauseJiggle = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = 1f + (playPausePress.value * 0.10f) + (playPauseJiggle.value * 0.16f)
                    scaleY = 1f - (playPausePress.value * 0.08f) - (playPauseJiggle.value * 0.14f)
                }
                .clip(CircleShape)
                .background(
                    if (isLiquidGlass) {
                        LocalMaterialTheme.current.primary.copy(alpha = 0.88f)
                    } else {
                        LocalMaterialTheme.current.primary
                    }
                )
                .then(
                    if (isLiquidGlass) {
                        Modifier.border(
                            0.75.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            ),
                            CircleShape
                        )
                    } else Modifier
                )
                .pointerInput(controlsEnabled) {
                    if (!controlsEnabled) return@pointerInput
                    val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 450f)
                    val jiggleSpec = spring<Float>(dampingRatio = 0.32f, stiffness = 280f)
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        coroutineScope.launch {
                            playPauseJiggle.snapTo(1f)
                            playPauseJiggle.animateTo(0f, jiggleSpec)
                        }
                        coroutineScope.launch { playPausePress.animateTo(1f, pressSpec) }
                        waitForUpOrCancellation()
                        coroutineScope.launch { playPausePress.animateTo(0f, pressSpec) }
                    }
                }
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = null,
                    enabled = controlsEnabled
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isPreparingPlayback || isBuffering) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalMaterialTheme.current.onPrimary
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = LocalMaterialTheme.current.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val nextPress = remember { Animatable(0f) }
        val nextJiggle = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = 1f + (nextPress.value * 0.10f) + (nextJiggle.value * 0.16f)
                    scaleY = 1f - (nextPress.value * 0.08f) - (nextJiggle.value * 0.14f)
                }
                .clip(CircleShape)
                .background(
                    if (isLiquidGlass) {
                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.10f)
                    } else {
                        LocalMaterialTheme.current.onPrimary
                    }
                )
                .then(
                    if (isLiquidGlass) {
                        Modifier.border(
                            0.75.dp,
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.12f),
                            CircleShape
                        )
                    } else Modifier
                )
                .pointerInput(controlsEnabled) {
                    if (!controlsEnabled) return@pointerInput
                    val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 450f)
                    val jiggleSpec = spring<Float>(dampingRatio = 0.32f, stiffness = 280f)
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        coroutineScope.launch {
                            nextJiggle.snapTo(1f)
                            nextJiggle.animateTo(0f, jiggleSpec)
                        }
                        coroutineScope.launch { nextPress.animateTo(1f, pressSpec) }
                        waitForUpOrCancellation()
                        coroutineScope.launch { nextPress.animateTo(0f, pressSpec) }
                    }
                }
                .clickable(
                    interactionSource = nextInteraction,
                    indication = null,
                    enabled = controlsEnabled
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNext()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Siguiente",
                tint = if (isLiquidGlass) (if (isDark) Color.White else Color(0xFF1C1C1E)) else LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
