package com.saurav.pixelmusic.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.FullPlayerLoadingTweaks
import com.saurav.pixelmusic.presentation.components.player.FullPlayerContent
import com.saurav.pixelmusic.presentation.components.scoped.FullPlayerVisualState
import com.saurav.pixelmusic.presentation.components.scoped.rememberFullPlayerRuntimePolicy
import com.saurav.pixelmusic.presentation.viewmodel.PlayerSheetState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.StablePlayerState
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity

@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.UnifiedPlayerMiniAndFullLayers(
    currentSong: Song?,
    miniPlayerScheme: ColorScheme?,
    overallSheetTopCornerRadiusProvider: () -> Dp,
    infrequentPlayerState: StablePlayerState,
    isCastConnecting: Boolean,
    isPreparingPlayback: Boolean,
    playerContentExpansionFraction: Animatable<Float, AnimationVector1D>,
    albumColorScheme: ColorScheme,
    bottomSheetOpenFraction: Float,
    fullPlayerVisualState: FullPlayerVisualState,
    containerHeight: Dp,
    currentQueueSourceName: String,
    currentSheetContentState: PlayerSheetState,
    carouselStyle: String,
    fullPlayerLoadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean = false,
    playerViewModel: PlayerViewModel,
    currentPositionProvider: () -> Long,
    isFavorite: Boolean,
    shouldRenderFullPlayer: Boolean = true,
    isLiquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit,
    onShowCastClicked: () -> Unit
) {
    val isLiquidGlassActive = isLiquidGlass && backdrop != null
    val glassBlurIntensity = LocalLiquidGlassBlurIntensity.current
    val glassRefractionIntensity = LocalLiquidGlassRefractionIntensity.current

    currentSong?.let { currentSongNonNull ->
        miniPlayerScheme?.let { readyScheme ->
            CompositionLocalProvider(
                LocalMaterialTheme provides readyScheme
            ) {
                val miniPlayerZIndex by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value < 0.5f) 1f else 0f
                    }
                }
                val isMiniPlayerVisible by remember {
                    derivedStateOf {
                        playerContentExpansionFraction.value < 0.42f
                    }
                }
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val miniShape = remember { RoundedCornerShape(32.dp) }
                val miniBorderBrush = remember(isDark) {
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        ) else listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                }

                if (isMiniPlayerVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(MiniPlayerHeight)
                            .graphicsLayer {
                                // Compute miniAlpha in the draw phase from the Animatable,
                                // avoiding per-frame recomposition during gestures.
                                alpha = (1f - playerContentExpansionFraction.value * 2.5f)
                                    .coerceIn(0f, 1f)
                            }
                            .then(
                                if (isLiquidGlassActive) {
                                    Modifier
                                        .drawBackdrop(
                                            backdrop = backdrop,
                                            shape = { miniShape },
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
                                            onDrawSurface = {
                                                drawRect(if (isDark) Color(0x301C1C1E) else Color(0x25F5F5F7))
                                            }
                                        )
                                        .border(BorderStroke(0.75.dp, miniBorderBrush), miniShape)
                                } else Modifier
                            )
                            .zIndex(miniPlayerZIndex)
                    ) {
                        val miniAlbumCornerRadius by remember(overallSheetTopCornerRadiusProvider) {
                            derivedStateOf {
                                (overallSheetTopCornerRadiusProvider().value * 0.5f).dp
                            }
                        }
                        MiniPlayerContentInternal(
                            song = currentSongNonNull,
                            cornerRadiusAlb = miniAlbumCornerRadius,
                            isPlaying = infrequentPlayerState.isPlaying,
                            isCastConnecting = isCastConnecting,
                            isPreparingPlayback = isPreparingPlayback,
                            isBuffering = infrequentPlayerState.isBuffering,
                            onPlayPause = { playerViewModel.playPause() },
                            onPrevious = { playerViewModel.previousSong() },
                            onNext = { playerViewModel.nextSong() },
                            isLiquidGlass = isLiquidGlassActive,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (shouldRenderFullPlayer) {
            CompositionLocalProvider(
                LocalMaterialTheme provides albumColorScheme
            ) {
                val fullPlayerZIndex by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value >= 0.5f) 1f else 0f
                    }
                }
                val fullPlayerOffset by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value <= 0.01f) IntOffset(0, 10000)
                        else IntOffset.Zero
                    }
                }
                val fullPlayerRuntimePolicy = rememberFullPlayerRuntimePolicy(
                    currentSheetState = currentSheetContentState,
                    expansionFraction = playerContentExpansionFraction,
                    bottomSheetOpenFraction = bottomSheetOpenFraction
                )

                // Scoped queue collection: only the FullPlayer subtree observes
                // the queue. Sibling MiniPlayer composable and the whole
                // UnifiedPlayerSheetV2 caller are insulated from queue churn.
                val currentPlaybackQueue by playerViewModel.queueFlow
                    .collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(containerHeight)
                        .graphicsLayer {
                            // Read from FullPlayerVisualState lazy getters in the draw phase;
                            // these read Animatable.value internally → re-draw only, no recomposition.
                            alpha = fullPlayerVisualState.contentAlpha
                            translationY = fullPlayerVisualState.translationY
                        }
                        .zIndex(fullPlayerZIndex)
                        .offset { fullPlayerOffset }
                ) {
                    val latestInfrequentPlayerState = rememberUpdatedState(infrequentPlayerState)
                    val latestIsFavorite = rememberUpdatedState(isFavorite)
                    val expansionFractionProvider = remember(playerContentExpansionFraction) {
                        { playerContentExpansionFraction.value }
                    }
                    val isPlayingProvider = remember {
                        { latestInfrequentPlayerState.value.isPlaying }
                    }
                    val playWhenReadyProvider = remember {
                        { latestInfrequentPlayerState.value.playWhenReady }
                    }
                    val repeatModeProvider = remember {
                        { latestInfrequentPlayerState.value.repeatMode }
                    }
                    val isShuffleEnabledProvider = remember {
                        { latestInfrequentPlayerState.value.isShuffleEnabled }
                    }
                    val totalDurationProvider = remember {
                        { latestInfrequentPlayerState.value.totalDuration }
                    }
                    val lyricsProvider = remember {
                        { latestInfrequentPlayerState.value.lyrics }
                    }
                    val isFavoriteProvider = remember {
                        { latestIsFavorite.value }
                    }
                    val onPlayPause = remember(playerViewModel) { playerViewModel::playPause }
                    val onSeek = remember(playerViewModel) { playerViewModel::seekTo }
                    val onNext = remember(playerViewModel) { playerViewModel::nextSong }
                    val onPrevious = remember(playerViewModel) { playerViewModel::previousSong }
                    val onCollapse = remember(playerViewModel) {
                        { playerViewModel.collapsePlayerSheet() }
                    }
                    val onShuffleToggle = remember(playerViewModel) {
                        { playerViewModel.toggleShuffle() }
                    }
                    val onRepeatToggle = remember(playerViewModel) { playerViewModel::cycleRepeatMode }
                    val onFavoriteToggle = remember(playerViewModel) { playerViewModel::toggleFavorite }

                    FullPlayerContent(
                        currentSong = currentSongNonNull,
                        currentPlaybackQueue = currentPlaybackQueue,
                        currentQueueSourceName = currentQueueSourceName,
                        currentMediaItemIndex = infrequentPlayerState.currentMediaItemIndex,
                        isShuffleEnabled = infrequentPlayerState.isShuffleEnabled,
                        shuffleTransitionInProgress = infrequentPlayerState.isShuffleTransitionInProgress,
                        repeatMode = infrequentPlayerState.repeatMode,
                        allowRealtimeUpdates = fullPlayerRuntimePolicy.allowRealtimeUpdates,
                        expansionFractionProvider = expansionFractionProvider,
                        currentSheetState = currentSheetContentState,
                        carouselStyle = carouselStyle,
                        loadingTweaks = fullPlayerLoadingTweaks,
                        isSheetDragGestureActive = isSheetDragGestureActive,
                        playerViewModel = playerViewModel,
                        currentPositionProvider = currentPositionProvider,
                        isPlayingProvider = isPlayingProvider,
                        playWhenReadyProvider = playWhenReadyProvider,
                        repeatModeProvider = repeatModeProvider,
                        isShuffleEnabledProvider = isShuffleEnabledProvider,
                        totalDurationProvider = totalDurationProvider,
                        lyricsProvider = lyricsProvider,
                        isCastConnecting = isCastConnecting,
                        isFavoriteProvider = isFavoriteProvider,
                        onPlayPause = onPlayPause,
                        onSeek = onSeek,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onCollapse = onCollapse,
                        onShowQueueClicked = onShowQueueClicked,
                        onQueueDragStart = onQueueDragStart,
                        onQueueDrag = onQueueDrag,
                        onQueueRelease = onQueueRelease,
                        onShowCastClicked = onShowCastClicked,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatToggle = onRepeatToggle,
                        onFavoriteToggle = onFavoriteToggle,
                        isLiquidGlass = isLiquidGlassActive,
                        backdrop = null
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun UnifiedPlayerPrewarmLayer(
    prewarmFullPlayer: Boolean,
    currentSong: Song?,
    containerHeight: Dp,
    albumColorScheme: ColorScheme,
    currentQueueSourceName: String,
    infrequentPlayerState: StablePlayerState,
    carouselStyle: String,
    fullPlayerLoadingTweaks: FullPlayerLoadingTweaks,
    playerViewModel: PlayerViewModel,
    currentPositionProvider: () -> Long,
    isCastConnecting: Boolean,
    isFavorite: Boolean,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit
) {
    if (prewarmFullPlayer && currentSong != null) {
        // Scoped queue collection: the prewarmed FullPlayer owns its own
        // subscription, keeping the queue out of the outer sheet's state.
        val currentPlaybackQueue by playerViewModel.queueFlow
            .collectAsStateWithLifecycle()
        CompositionLocalProvider(
            LocalMaterialTheme provides albumColorScheme
        ) {
            Box(
                modifier = Modifier
                    .height(containerHeight)
                    .fillMaxWidth()
                    .alpha(0f)
                    .clipToBounds()
            ) {
                // Memoize closures the same way the main layer does to avoid creating
                // new lambda instances on every recomposition.
                val latestInfrequentPlayerState = rememberUpdatedState(infrequentPlayerState)
                val latestIsFavorite = rememberUpdatedState(isFavorite)
                val isPlayingProvider = remember { { latestInfrequentPlayerState.value.isPlaying } }
                val playWhenReadyProvider = remember { { latestInfrequentPlayerState.value.playWhenReady } }
                val repeatModeProvider = remember { { latestInfrequentPlayerState.value.repeatMode } }
                val isShuffleEnabledProvider = remember { { latestInfrequentPlayerState.value.isShuffleEnabled } }
                val totalDurationProvider = remember { { latestInfrequentPlayerState.value.totalDuration } }
                val lyricsProvider = remember { { latestInfrequentPlayerState.value.lyrics } }
                val isFavoriteProvider = remember { { latestIsFavorite.value } }
                val onPlayPause = remember(playerViewModel) { playerViewModel::playPause }
                val onSeek = remember(playerViewModel) { playerViewModel::seekTo }
                val onNext = remember(playerViewModel) { playerViewModel::nextSong }
                val onPrevious = remember(playerViewModel) { playerViewModel::previousSong }
                val onShuffleToggle = remember(playerViewModel) { { playerViewModel.toggleShuffle() } }
                val onRepeatToggle = remember(playerViewModel) { playerViewModel::cycleRepeatMode }
                val onFavoriteToggle = remember(playerViewModel) { playerViewModel::toggleFavorite }

                FullPlayerContent(
                    currentSong = currentSong,
                    currentPlaybackQueue = currentPlaybackQueue,
                    currentQueueSourceName = currentQueueSourceName,
                    currentMediaItemIndex = infrequentPlayerState.currentMediaItemIndex,
                    isShuffleEnabled = infrequentPlayerState.isShuffleEnabled,
                    shuffleTransitionInProgress = infrequentPlayerState.isShuffleTransitionInProgress,
                    repeatMode = infrequentPlayerState.repeatMode,
                    allowRealtimeUpdates = false,
                    expansionFractionProvider = { 1f },
                    currentSheetState = PlayerSheetState.EXPANDED,
                    carouselStyle = carouselStyle,
                    loadingTweaks = fullPlayerLoadingTweaks,
                    playerViewModel = playerViewModel,
                    currentPositionProvider = currentPositionProvider,
                    isPlayingProvider = isPlayingProvider,
                    playWhenReadyProvider = playWhenReadyProvider,
                    repeatModeProvider = repeatModeProvider,
                    isShuffleEnabledProvider = isShuffleEnabledProvider,
                    totalDurationProvider = totalDurationProvider,
                    lyricsProvider = lyricsProvider,
                    isCastConnecting = isCastConnecting,
                    isFavoriteProvider = isFavoriteProvider,
                    onShowQueueClicked = onShowQueueClicked,
                    onQueueDragStart = onQueueDragStart,
                    onQueueDrag = onQueueDrag,
                    onQueueRelease = onQueueRelease,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onCollapse = {},
                    onShowCastClicked = {},
                    onShuffleToggle = onShuffleToggle,
                    onRepeatToggle = onRepeatToggle,
                    onFavoriteToggle = onFavoriteToggle,
                    isLiquidGlass = false,
                    backdrop = null
                )
            }
        }
    }
}
