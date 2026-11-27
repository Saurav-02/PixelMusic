package com.saurav.pixelmusic.presentation.components.player

import androidx.compose.foundation.interaction.MutableInteractionSource
import com.saurav.pixelmusic.presentation.components.expressiveTapColorWave

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.saurav.pixelmusic.R
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import androidx.compose.ui.draw.drawBehind
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private enum class PlaybackButtonType { NONE, PREVIOUS, PLAY_PAUSE, NEXT }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedPlaybackControls(
    isPlayingProvider: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp,
    baseWeight: Float = 1f,
    expansionWeight: Float = 1.1f,
    compressionWeight: Float = 0.65f,
    pressAnimationSpec: AnimationSpec<Float>,
    releaseDelay: Long = 220L,
    playPauseCornerPlaying: Dp = 26.dp,
    playPauseCornerPaused: Dp = 26.dp,
    colorOtherButtons: Color = LocalMaterialTheme.current.onPrimary,
    colorPlayPause: Color = LocalMaterialTheme.current.primary,
    tintPlayPauseIcon: Color = LocalMaterialTheme.current.onPrimary,
    tintOtherIcons: Color = LocalMaterialTheme.current.primary,
    colorPreviousButton: Color = colorOtherButtons,
    colorNextButton: Color = colorOtherButtons,
    tintPreviousIcon: Color = tintOtherIcons,
    tintNextIcon: Color = tintOtherIcons,
    playPauseIconSize: Dp = 36.dp,
    iconSize: Dp = 32.dp,
    morphTracker: ArtworkMorphTracker? = null,
    expansionFractionProvider: (() -> Float)? = null,
    rootCoordsHolder: LayoutCoordsHolder? = null,
    containerWidthPxProvider: () -> Float = { 0f },
    capturePrevBounds: MorphElementBounds? = null,
    capturePlayBounds: MorphElementBounds? = null,
    captureNextBounds: MorphElementBounds? = null,
    sourcePrevBounds: MorphElementBounds? = null,
    sourcePlayBounds: MorphElementBounds? = null,
    sourceNextBounds: MorphElementBounds? = null,
    sourceMorphProgressProvider: (() -> Float)? = null,
    sourceBtnCornerDp: Dp = 40.dp,
    sourcePlayCornerDp: Dp = 26.dp,
    sourceColorOtherButtons: Color = Color.Unspecified,
    sourceColorPlayPause: Color = Color.Unspecified,
    sourceTintPlayPauseIcon: Color = Color.Unspecified,
    sourceTintOtherIcons: Color = Color.Unspecified,
) {
    val density = LocalDensity.current
    val miniBtnSizePx = remember(density) { with(density) { 36.dp.toPx() } }
    val miniBtnTopPx = remember(density) { with(density) { 14.dp.toPx() } }
    val miniEndPadPx = remember(density) { with(density) { 12.dp.toPx() } }
    val miniGapPx = remember(density) { with(density) { 8.dp.toPx() } }
    val miniIconSizePx = remember(density) { with(density) { 22.dp.toPx() } }

    val rowBounds = remember { MorphElementBounds() }
    val rowGapPx = remember(density) { with(density) { 6.dp.toPx() } }
    val prevBounds = capturePrevBounds ?: remember { MorphElementBounds() }
    val playBounds = capturePlayBounds ?: remember { MorphElementBounds() }
    val nextBounds = captureNextBounds ?: remember { MorphElementBounds() }
    val isPlaying = isPlayingProvider()
    var lastClicked by remember { mutableStateOf<PlaybackButtonType?>(null) }
    val latestIsPlayingProvider by rememberUpdatedState(newValue = isPlayingProvider)
    val latestLastClicked by rememberUpdatedState(newValue = lastClicked)
    val isPlayPauseLocked =
        lastClicked == PlaybackButtonType.NEXT || lastClicked == PlaybackButtonType.PREVIOUS
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(lastClicked) {
        if (lastClicked != null) {
            delay(releaseDelay)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }

        val shouldDelay = latestLastClicked != PlaybackButtonType.PLAY_PAUSE
        if (shouldDelay) {
            delay(releaseDelay)
        }
        if (!latestIsPlayingProvider()) {
            pendingPlayPauseState = false
        }
    }

    LaunchedEffect(isPlayPauseLocked, pendingPlayPauseState) {
        if (!isPlayPauseLocked) {
            pendingPlayPauseState?.let {
                playPauseVisualState = it
                pendingPlayPauseState = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .captureUntransformedBounds(rootCoordsHolder, rowBounds),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun weightFor(button: PlaybackButtonType): Float = when (lastClicked) {
                button -> expansionWeight
                null -> baseWeight
                else -> compressionWeight
            }

            val prevWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.PREVIOUS),
                animationSpec = pressAnimationSpec,
                label = "prevWeight"
            )
            val playWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.PLAY_PAUSE),
                animationSpec = pressAnimationSpec,
                label = "playWeight"
            )
            val nextWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.NEXT),
                animationSpec = pressAnimationSpec,
                label = "nextWeight"
            )

            // Exact floating-point slot geometry cancels integer-pixel Row weight jitter during drag
            fun exactSlotWidth(weight: Float, fallbackW: Float): Float {
                if (!rowBounds.hasBounds || rowBounds.size.width <= 0) return fallbackW
                val totalW = (prevWeight + playWeight + nextWeight).coerceAtLeast(0.001f)
                val usableW = (rowBounds.size.width.toFloat() - 2f * rowGapPx).coerceAtLeast(1f)
                return usableW * (weight / totalW)
            }

            fun exactSlotLeft(slotIndex: Int, fallbackX: Float): Float {
                if (!rowBounds.hasBounds || rowBounds.size.width <= 0) return fallbackX
                val w0 = exactSlotWidth(prevWeight, 0f)
                val w1 = exactSlotWidth(playWeight, 0f)
                return when (slotIndex) {
                    0 -> rowBounds.offset.x
                    1 -> rowBounds.offset.x + w0 + rowGapPx
                    else -> rowBounds.offset.x + w0 + rowGapPx + w1 + rowGapPx
                }
            }

            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .captureUntransformedBounds(rootCoordsHolder, prevBounds)
                    .graphicsLayer {
                        val fullCornerPx = 40.dp.toPx().coerceAtMost(minOf(size.width, size.height) * 0.5f)
                        var rx = fullCornerPx
                        var ry = fullCornerPx
                        if (sourceMorphProgressProvider != null && sourcePrevBounds != null && sourcePrevBounds.hasBounds) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val posT = f
                            val srcW = sourcePrevBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourcePrevBounds.size.height.toFloat().coerceAtLeast(1f)
                            val destX = prevBounds.offset.x
                            val destY = prevBounds.offset.y
                            val curW = androidx.compose.ui.util.lerp(srcW, size.width, sizeT)
                            val curH = androidx.compose.ui.util.lerp(srcH, size.height, sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            translationX = (sourcePrevBounds.offset.x - destX) * (1f - posT)
                            translationY = (sourcePrevBounds.offset.y - destY) * (1f - posT)
                            val srcCornerPx = sourceBtnCornerDp.toPx().coerceAtMost(minOf(srcW, srcH) * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(srcCornerPx, fullCornerPx, sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        } else if (morphTracker != null && expansionFractionProvider != null) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val cw = rootCoordsHolder?.widthPx?.takeIf { it > 0f }
                                ?: containerWidthPxProvider().takeIf { it > 0f }
                                ?: (size.width * 3f)
                            val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                            val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                            val miniPrevLeftPx = (miniPlayLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                            val destX = prevBounds.offset.x
                            val destY = prevBounds.offset.y
                            val exactW = exactSlotWidth(prevWeight, size.width)
                            val exactX = exactSlotLeft(0, destX)
                            val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT)
                            val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            val targetX = androidx.compose.ui.util.lerp(miniPrevLeftPx, exactX, s.posT)
                            val targetY = androidx.compose.ui.util.lerp(miniBtnTopPx, destY, s.posT)
                            translationX = targetX - destX
                            translationY = targetY - destY
                            val miniCornerPx = 18.dp.toPx().coerceAtMost(miniBtnSizePx * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, s.sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        }
                        clip = true
                        shape = DynamicTransportButtonShape(rxPx = rx, ryPx = ry)
                    }
                    .drawBehind {
                        val baseColor = if (sourceMorphProgressProvider != null && sourceColorOtherButtons != Color.Unspecified) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            androidx.compose.ui.graphics.lerp(sourceColorOtherButtons, colorPreviousButton, f)
                        } else {
                            colorPreviousButton
                        }
                        drawRect(baseColor)
                    }
                    .clickable {
                        lastClicked = PlaybackButtonType.PREVIOUS
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                TransportSkipIcon(
                    imageVector = ImageVector.vectorResource(R.drawable.rounded_skip_previous_filled_24),
                    contentDescription = "Anterior",
                    targetTint = tintPreviousIcon,
                    sourceTint = sourceTintOtherIcons,
                    sourceMorphProgressProvider = sourceMorphProgressProvider,
                    iconSize = iconSize,
                    modifier = Modifier.graphicsLayer {
                        val bw = prevBounds.size.width.toFloat()
                        val bh = prevBounds.size.height.toFloat()
                        if (sourceMorphProgressProvider != null && sourcePrevBounds != null && sourcePrevBounds.hasBounds && bw > 0f && bh > 0f) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val srcW = sourcePrevBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourcePrevBounds.size.height.toFloat().coerceAtLeast(1f)
                            val parentScaleX = androidx.compose.ui.util.lerp(srcW, bw, sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(srcH, bh, sizeT) / bh
                            scaleX = if (parentScaleX > 0.001f) 1f / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) 1f / parentScaleY else 1f
                        } else if (morphTracker != null && expansionFractionProvider != null && bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val exactW = exactSlotWidth(prevWeight, bw)
                            val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = iconSize.toPx().coerceAtLeast(1f)
                            val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    }
                )
            }

            val playCorner by animateDpAsState(
                targetValue = if (!playPauseVisualState) playPauseCornerPlaying else playPauseCornerPaused,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "playCorner"
            )
            val miniPlayCornerDp = if (playPauseVisualState) 11.dp else 18.dp
            Box(
                modifier = Modifier
                    .weight(playWeight)
                    .fillMaxHeight()
                    .captureUntransformedBounds(rootCoordsHolder, playBounds)
                    .graphicsLayer {
                        val fullCornerPx = playCorner.toPx().coerceAtMost(minOf(size.width, size.height) * 0.5f)
                        var rx = fullCornerPx
                        var ry = fullCornerPx
                        if (sourceMorphProgressProvider != null && sourcePlayBounds != null && sourcePlayBounds.hasBounds) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val posT = f
                            val srcW = sourcePlayBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourcePlayBounds.size.height.toFloat().coerceAtLeast(1f)
                            val destX = playBounds.offset.x
                            val destY = playBounds.offset.y
                            val curW = androidx.compose.ui.util.lerp(srcW, size.width, sizeT)
                            val curH = androidx.compose.ui.util.lerp(srcH, size.height, sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            translationX = (sourcePlayBounds.offset.x - destX) * (1f - posT)
                            translationY = (sourcePlayBounds.offset.y - destY) * (1f - posT)
                            val srcCornerPx = sourcePlayCornerDp.toPx().coerceAtMost(minOf(srcW, srcH) * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(srcCornerPx, fullCornerPx, sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        } else if (morphTracker != null && expansionFractionProvider != null) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val cw = rootCoordsHolder?.widthPx?.takeIf { it > 0f }
                                ?: containerWidthPxProvider().takeIf { it > 0f }
                                ?: (size.width * 3f)
                            val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                            val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                            val destX = playBounds.offset.x
                            val destY = playBounds.offset.y
                            val exactW = exactSlotWidth(playWeight, size.width)
                            val exactX = exactSlotLeft(1, destX)
                            val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT)
                            val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            val targetX = androidx.compose.ui.util.lerp(miniPlayLeftPx, exactX, s.posT)
                            val targetY = androidx.compose.ui.util.lerp(miniBtnTopPx, destY, s.posT)
                            translationX = targetX - destX
                            translationY = targetY - destY
                            val miniCornerPx = miniPlayCornerDp.toPx().coerceAtMost(miniBtnSizePx * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, s.sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        }
                        clip = true
                        shape = DynamicTransportButtonShape(rxPx = rx, ryPx = ry)
                    }
                    .drawBehind {
                        val baseColor = if (sourceMorphProgressProvider != null && sourceColorPlayPause != Color.Unspecified) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            androidx.compose.ui.graphics.lerp(sourceColorPlayPause, colorPlayPause, f)
                        } else {
                            colorPlayPause
                        }
                        drawRect(baseColor)
                    }
                    .expressiveTapColorWave(
                        waveColor = tintPlayPauseIcon.copy(alpha = 0.28f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        lastClicked = PlaybackButtonType.PLAY_PAUSE
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        val bw = playBounds.size.width.toFloat()
                        val bh = playBounds.size.height.toFloat()
                        if (sourceMorphProgressProvider != null && sourcePlayBounds != null && sourcePlayBounds.hasBounds && bw > 0f && bh > 0f) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val srcW = sourcePlayBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourcePlayBounds.size.height.toFloat().coerceAtLeast(1f)
                            val parentScaleX = androidx.compose.ui.util.lerp(srcW, bw, sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(srcH, bh, sizeT) / bh
                            scaleX = if (parentScaleX > 0.001f) 1f / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) 1f / parentScaleY else 1f
                        } else if (morphTracker != null && expansionFractionProvider != null && bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val exactW = exactSlotWidth(playWeight, bw)
                            val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = playPauseIconSize.toPx().coerceAtLeast(1f)
                            val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    }
                ) {
                    MorphingPlayPauseIcon(
                        isPlaying = playPauseVisualState,
                        tint = tintPlayPauseIcon,
                        sourceTint = sourceTintPlayPauseIcon,
                        sourceMorphProgressProvider = sourceMorphProgressProvider,
                        size = playPauseIconSize
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .captureUntransformedBounds(rootCoordsHolder, nextBounds)
                    .graphicsLayer {
                        val fullCornerPx = 40.dp.toPx().coerceAtMost(minOf(size.width, size.height) * 0.5f)
                        var rx = fullCornerPx
                        var ry = fullCornerPx
                        if (sourceMorphProgressProvider != null && sourceNextBounds != null && sourceNextBounds.hasBounds) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val posT = f
                            val srcW = sourceNextBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourceNextBounds.size.height.toFloat().coerceAtLeast(1f)
                            val destX = nextBounds.offset.x
                            val destY = nextBounds.offset.y
                            val curW = androidx.compose.ui.util.lerp(srcW, size.width, sizeT)
                            val curH = androidx.compose.ui.util.lerp(srcH, size.height, sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            translationX = (sourceNextBounds.offset.x - destX) * (1f - posT)
                            translationY = (sourceNextBounds.offset.y - destY) * (1f - posT)
                            val srcCornerPx = sourceBtnCornerDp.toPx().coerceAtMost(minOf(srcW, srcH) * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(srcCornerPx, fullCornerPx, sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        } else if (morphTracker != null && expansionFractionProvider != null) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val cw = rootCoordsHolder?.widthPx?.takeIf { it > 0f }
                                ?: containerWidthPxProvider().takeIf { it > 0f }
                                ?: (size.width * 3f)
                            val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                            val destX = nextBounds.offset.x
                            val destY = nextBounds.offset.y
                            val exactW = exactSlotWidth(nextWeight, size.width)
                            val exactX = exactSlotLeft(2, destX)
                            val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT)
                            val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                            val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                            val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = sx
                            scaleY = sy
                            val targetX = androidx.compose.ui.util.lerp(miniNextLeftPx, exactX, s.posT)
                            val targetY = androidx.compose.ui.util.lerp(miniBtnTopPx, destY, s.posT)
                            translationX = targetX - destX
                            translationY = targetY - destY
                            val miniCornerPx = 18.dp.toPx().coerceAtMost(miniBtnSizePx * 0.5f)
                            val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, s.sizeT)
                            rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                            ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)
                        }
                        clip = true
                        shape = DynamicTransportButtonShape(rxPx = rx, ryPx = ry)
                    }
                    .drawBehind {
                        val baseColor = if (sourceMorphProgressProvider != null && sourceColorOtherButtons != Color.Unspecified) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            androidx.compose.ui.graphics.lerp(sourceColorOtherButtons, colorNextButton, f)
                        } else {
                            colorNextButton
                        }
                        drawRect(baseColor)
                    }
                    .clickable {
                        lastClicked = PlaybackButtonType.NEXT
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                TransportSkipIcon(
                    imageVector = ImageVector.vectorResource(R.drawable.rounded_skip_next_filled_24),
                    contentDescription = "Siguiente",
                    targetTint = tintNextIcon,
                    sourceTint = sourceTintOtherIcons,
                    sourceMorphProgressProvider = sourceMorphProgressProvider,
                    iconSize = iconSize,
                    modifier = Modifier.graphicsLayer {
                        val bw = nextBounds.size.width.toFloat()
                        val bh = nextBounds.size.height.toFloat()
                        if (sourceMorphProgressProvider != null && sourceNextBounds != null && sourceNextBounds.hasBounds && bw > 0f && bh > 0f) {
                            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
                            val sizeT = f
                            val srcW = sourceNextBounds.size.width.toFloat().coerceAtLeast(1f)
                            val srcH = sourceNextBounds.size.height.toFloat().coerceAtLeast(1f)
                            val parentScaleX = androidx.compose.ui.util.lerp(srcW, bw, sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(srcH, bh, sizeT) / bh
                            scaleX = if (parentScaleX > 0.001f) 1f / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) 1f / parentScaleY else 1f
                        } else if (morphTracker != null && expansionFractionProvider != null && bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val exactW = exactSlotWidth(nextWeight, bw)
                            val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, exactW, s.sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = iconSize.toPx().coerceAtLeast(1f)
                            val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    }
                )
            }
        }
    }
}

private data class DynamicTransportButtonShape(
    val rxPx: Float,
    val ryPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rx = rxPx.coerceIn(0f, size.width * 0.5f)
        val ry = ryPx.coerceIn(0f, size.height * 0.5f)
        return Outline.Rounded(RoundRect(Rect(0f, 0f, size.width, size.height), CornerRadius(rx, ry)))
    }
}

private class MorphingTransportButtonShape(
    private val miniCornerDp: Dp,
    private val fullCornerDpProvider: () -> Dp,
    private val miniBtnSizePx: Float,
    private val morphTracker: ArtworkMorphTracker?,
    private val expansionFractionProvider: (() -> Float)?,
    private val sourceBounds: MorphElementBounds? = null,
    private val sourceMorphProgressProvider: (() -> Float)? = null
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val fullCornerPx = with(density) { fullCornerDpProvider().toPx() }
        if (sourceMorphProgressProvider != null && sourceBounds != null && sourceBounds.hasBounds) {
            val f = sourceMorphProgressProvider().coerceIn(0f, 1f)
            val t = f
            val srcW = sourceBounds.size.width.toFloat().coerceAtLeast(1f)
            val srcH = sourceBounds.size.height.toFloat().coerceAtLeast(1f)
            val srcCornerPx = with(density) { miniCornerDp.toPx() }.coerceAtMost(minOf(srcW, srcH) * 0.5f)
            val targetVisualR = androidx.compose.ui.util.lerp(srcCornerPx, fullCornerPx, t)
            val scaleX = (androidx.compose.ui.util.lerp(srcW, w, t) / w).coerceAtLeast(0.01f)
            val scaleY = (androidx.compose.ui.util.lerp(srcH, h, t) / h).coerceAtLeast(0.01f)
            val rx = (targetVisualR / scaleX).coerceIn(0f, w * 0.5f)
            val ry = (targetVisualR / scaleY).coerceIn(0f, h * 0.5f)
            return Outline.Rounded(RoundRect(Rect(0f, 0f, w, h), CornerRadius(rx, ry)))
        }
        if (morphTracker == null || expansionFractionProvider == null) {
            val r = fullCornerPx.coerceAtMost(minOf(w, h) * 0.5f)
            return Outline.Rounded(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r)))
        }
        val t = morphTracker.evaluate(expansionFractionProvider()).sizeT
        val miniCornerPx = with(density) { miniCornerDp.toPx() }
        val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, t)
        val scaleX = (androidx.compose.ui.util.lerp(miniBtnSizePx, w, t) / w).coerceAtLeast(0.01f)
        val scaleY = (androidx.compose.ui.util.lerp(miniBtnSizePx, h, t) / h).coerceAtLeast(0.01f)
        val rx = (targetVisualR / scaleX).coerceIn(0f, w * 0.5f)
        val ry = (targetVisualR / scaleY).coerceIn(0f, h * 0.5f)
        return Outline.Rounded(RoundRect(Rect(0f, 0f, w, h), CornerRadius(rx, ry)))
    }
}

@Composable
private fun TransportSkipIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    targetTint: Color,
    sourceTint: Color,
    sourceMorphProgressProvider: (() -> Float)?,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val effectiveTint = if (sourceMorphProgressProvider != null && sourceTint != Color.Unspecified) {
        val f = ((sourceMorphProgressProvider().coerceIn(0f, 1f) * 24f).roundToInt() / 24f)
        androidx.compose.ui.graphics.lerp(sourceTint, targetTint, f)
    } else {
        targetTint
    }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = effectiveTint,
        modifier = modifier.size(iconSize)
    )
}

@Composable
private fun MorphingPlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    sourceTint: Color = Color.Unspecified,
    sourceMorphProgressProvider: (() -> Float)? = null,
    size: Dp,
) {
    val effectiveTint = if (sourceMorphProgressProvider != null && sourceTint != Color.Unspecified) {
        val f = ((sourceMorphProgressProvider().coerceIn(0f, 1f) * 24f).roundToInt() / 24f)
        androidx.compose.ui.graphics.lerp(sourceTint, tint, f)
    } else {
        tint
    }
    Crossfade(
        targetState = isPlaying,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "playPauseCrossfade"
    ) { playing ->
        Icon(
            imageVector = ImageVector.vectorResource(if (playing) R.drawable.rounded_pause_filled_24 else R.drawable.rounded_play_arrow_filled_24),
            contentDescription = if (playing) "Pausar" else "Reproducir",
            tint = effectiveTint,
            modifier = Modifier.size(size)
        )
    }
}
