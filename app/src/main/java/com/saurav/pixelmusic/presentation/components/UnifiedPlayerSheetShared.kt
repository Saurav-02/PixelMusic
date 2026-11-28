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
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
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
import androidx.compose.ui.layout.onSizeChanged
import coil.size.Size
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.presentation.components.player.AodSourceBounds
import com.saurav.pixelmusic.presentation.components.player.MarqueeSyncState
import com.saurav.pixelmusic.presentation.components.player.SharedPlayerMarqueeText
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

internal val LocalMaterialTheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

val MiniPlayerHeight = 64.dp
const val ANIMATION_DURATION_MS = 420
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
    albumArtAlphaProvider: () -> Float = { 1f },
    aodSourceBounds: AodSourceBounds? = null,
    shouldRunMarqueeProvider: () -> Boolean = { true },
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
            .expressiveTapColorWave(
                waveColor = LocalMaterialTheme.current.primary.copy(alpha = 0.20f)
            )
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArtModel = song.albumArtUriString?.takeIf { it.isNotBlank() }
        Box(contentAlignment = Alignment.Center) {
            key(song.id) {
                SmartImage(
                    crossfadeDurationMillis = 0,
                    model = albumArtModel,
                    contentDescription = "Carátula de ${song.title}",
                    shape = CircleShape,
                    targetSize = Size.ORIGINAL,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            alpha = albumArtAlphaProvider()
                        },
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

        val fallbackTitleMarquee = remember { MarqueeSyncState() }
        val fallbackArtistMarquee = remember { MarqueeSyncState() }
        Column(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { size ->
                    if (size.width > 0) {
                        aodSourceBounds?.miniSongInfoWidthPx = size.width.toFloat()
                    }
                }
                .graphicsLayer {
                    alpha = albumArtAlphaProvider()
                },
            verticalArrangement = Arrangement.Center
        ) {
            val titleColor = LocalMaterialTheme.current.onPrimaryContainer
            val artistColor = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.7f)

            val titleStyle = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 15.sp,
                lineHeight = 18.75.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                fontFamily = GoogleSansRounded,
                color = titleColor
            )
            val artistStyle = MaterialTheme.typography.titleMedium.copy(
                fontSize = 13.sp,
                lineHeight = 17.33.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                fontFamily = GoogleSansRounded,
                color = artistColor
            )

            val resolvedMiniTitle = when {
                isCastConnecting -> "Connecting to device…"
                isPreparingPlayback -> "Preparing playback…"
                else -> song.title.takeIf { it.isNotBlank() } ?: "Unknown Title"
            }
            val resolvedMiniArtist = if (isPreparingPlayback) {
                "Loading audio…"
            } else {
                song.displayArtist.takeIf { it.isNotBlank() } ?: song.artist
            }

            SharedPlayerMarqueeText(
                text = resolvedMiniTitle,
                style = titleStyle,
                marqueeState = aodSourceBounds?.titleMarquee ?: fallbackTitleMarquee,
                shouldRunMarquee = true,
                shouldRunMarqueeProvider = shouldRunMarqueeProvider,
                gapDp = 22.5.dp,
                velocityDpPerSec = 17.5.dp,
                modifier = Modifier.fillMaxWidth()
            )
            SharedPlayerMarqueeText(
                text = resolvedMiniArtist,
                style = artistStyle,
                marqueeState = aodSourceBounds?.artistMarquee ?: fallbackArtistMarquee,
                shouldRunMarquee = true,
                shouldRunMarqueeProvider = shouldRunMarqueeProvider,
                gapDp = 31.2.dp,
                velocityDpPerSec = 24.27.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        val prevPress = remember { Animatable(0f) }
        val prevJiggle = remember { Animatable(0f) }
        val prevCornerDp = (18f - prevPress.value * 7f).dp
        val prevShape = AbsoluteSmoothCornerShape(prevCornerDp, 60)

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    alpha = albumArtAlphaProvider()
                    scaleX = 1f + (prevPress.value * 0.10f) + (prevJiggle.value * 0.16f)
                    scaleY = 1f - (prevPress.value * 0.08f) - (prevJiggle.value * 0.14f)
                }
                .clip(prevShape)
                .background(LocalMaterialTheme.current.onPrimary)
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
                painter = painterResource(R.drawable.rounded_skip_previous_filled_24),
                contentDescription = "Anterior",
                tint = LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val playPausePress = remember { Animatable(0f) }
        val playPauseJiggle = remember { Animatable(0f) }
        val playPauseBaseCorner by animateDpAsState(
            targetValue = if (isPlaying) 11.dp else 18.dp,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "miniPlayPauseCorner"
        )
        val playPauseEffectiveCorner = (playPauseBaseCorner.value - playPausePress.value * 3f).coerceAtLeast(8f).dp
        val playPauseShape = AbsoluteSmoothCornerShape(playPauseEffectiveCorner, 60)

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    alpha = albumArtAlphaProvider()
                    scaleX = 1f + (playPausePress.value * 0.10f) + (playPauseJiggle.value * 0.16f)
                    scaleY = 1f - (playPausePress.value * 0.08f) - (playPauseJiggle.value * 0.14f)
                }
                .clip(playPauseShape)
                .background(LocalMaterialTheme.current.primary)
                .expressiveTapColorWave(
                    waveColor = LocalMaterialTheme.current.onPrimary.copy(alpha = 0.30f),
                    shape = playPauseShape,
                    enabled = controlsEnabled
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
                    painter = painterResource(if (isPlaying) R.drawable.rounded_pause_filled_24 else R.drawable.rounded_play_arrow_filled_24),
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = LocalMaterialTheme.current.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val nextPress = remember { Animatable(0f) }
        val nextJiggle = remember { Animatable(0f) }
        val nextCornerDp = (18f - nextPress.value * 7f).dp
        val nextShape = AbsoluteSmoothCornerShape(nextCornerDp, 60)

        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    alpha = albumArtAlphaProvider()
                    scaleX = 1f + (nextPress.value * 0.10f) + (nextJiggle.value * 0.16f)
                    scaleY = 1f - (nextPress.value * 0.08f) - (nextJiggle.value * 0.14f)
                }
                .clip(nextShape)
                .background(LocalMaterialTheme.current.onPrimary)
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
                painter = painterResource(R.drawable.rounded_skip_next_filled_24),
                contentDescription = "Siguiente",
                tint = LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
