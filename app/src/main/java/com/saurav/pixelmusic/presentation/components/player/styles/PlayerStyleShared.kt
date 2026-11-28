package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.media3.common.Player
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.WavyMusicSlider
import com.saurav.pixelmusic.presentation.components.expressiveTapColorWave
import com.saurav.pixelmusic.presentation.components.player.AodSourceBounds
import com.saurav.pixelmusic.presentation.components.player.ArtworkMorphTracker
import com.saurav.pixelmusic.presentation.components.player.LayoutCoordsHolder
import com.saurav.pixelmusic.presentation.components.player.MarqueeSyncState
import com.saurav.pixelmusic.presentation.components.player.MorphElementBounds
import com.saurav.pixelmusic.presentation.components.player.SharedPlayerMarqueeText
import com.saurav.pixelmusic.presentation.components.player.captureUntransformedBounds
import com.saurav.pixelmusic.presentation.components.scoped.rememberSmoothProgress
import com.saurav.pixelmusic.presentation.viewmodel.PlayerSheetState
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme
import com.saurav.pixelmusic.utils.ThumbnailUrlUtils
import com.saurav.pixelmusic.utils.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Self-contained parameter contract for isolated Now Playing design styles.
 * Keeps every new style completely decoupled from FullPlayerContent's existing layouts.
 */
data class PlayerStyleParams(
    val song: Song,
    val currentSongArtists: List<Artist>,
    val albumArtQuality: AlbumArtQuality,
    val aodScreenEnabled: Boolean,
    val onEnterAodScreen: () -> Unit,
    val expansionFractionProvider: () -> Float,
    val currentSheetState: PlayerSheetState,
    val allowRealtimeUpdates: Boolean,
    val currentPositionProvider: () -> Long,
    val totalDurationValue: Long,
    val isPlayingProvider: () -> Boolean,
    val isFavoriteProvider: () -> Boolean,
    val isShuffleEnabledProvider: () -> Boolean,
    val repeatModeProvider: () -> Int,
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onFavoriteToggle: () -> Unit,
    val onShuffleToggle: () -> Unit,
    val onRepeatToggle: () -> Unit,
    val onLyricsClick: () -> Unit,
    val onTitleClick: () -> Unit = {},
    val onArtistClick: () -> Unit,
    val onMoreOptionsClick: () -> Unit,
    val audioMetaLabel: String? = null,
    val aodSourceBounds: AodSourceBounds? = null,
    val aodTransitionProgressProvider: () -> Float = { 0f }
)

internal enum class MiniTransportSlot {
    PREVIOUS,
    PLAY_PAUSE,
    NEXT
}

/**
 * Exact equivalent of AnimatedPlaybackControls's DynamicTransportButtonShape so
 * Play/Pause, Previous, and Next morph with identical GPU-clipped corner radii.
 */
private data class DynamicStyleTransportShape(
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
        return Outline.Rounded(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                cornerRadius = CornerRadius(rx, ry)
            )
        )
    }
}

@Composable
internal fun rememberResolvedArtAndMeta(
    song: Song,
    currentSongArtists: List<Artist>,
    albumArtQuality: AlbumArtQuality
): Triple<String, String, String> {
    val optimizedArtUri = remember(song.id, albumArtQuality) {
        ThumbnailUrlUtils.optimizeArtworkUrl(song.albumArtUriString, albumArtQuality)
            ?: song.albumArtUriString.orEmpty()
    }
    val resolvedTitle = remember(song.id, song.title) {
        song.title.takeIf { it.isNotBlank() } ?: "Unknown Title"
    }
    val resolvedArtist = remember(song.id, song.displayArtist, song.artist, currentSongArtists) {
        song.displayArtist.takeIf { it.isNotBlank() }
            ?: currentSongArtists
                .mapNotNull { it.name.takeIf(String::isNotBlank) }
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
            ?: song.artist.takeIf { it.isNotBlank() }
            ?: "Unknown Artist"
    }
    return Triple(optimizedArtUri, resolvedTitle, resolvedArtist)
}

/**
 * Long-press on artwork to enter AOD (no horizontal swipe-to-skip).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.styleArtworkGestures(
    aodScreenEnabled: Boolean,
    onEnterAodScreen: () -> Unit
): Modifier {
    val haptics = LocalHapticFeedback.current
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = {
            if (aodScreenEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onEnterAodScreen()
            }
        }
    )
}

/**
 * Song Title & Artist block that morphs seamlessly from the MiniPlayer text slot
 * via GPU graphicsLayer and registers its bounds for AOD morphing.
 * Always preserves natural artist casing.
 */
@Composable
internal fun StyleMorphingSongInfo(
    title: String,
    artist: String,
    contentColor: Color,
    artistAlpha: Float = 0.76f,
    titleFontSize: TextUnit = 26.sp,
    artistFontSize: TextUnit = 15.sp,
    screenWidthPx: Float,
    rootCoordsHolder: LayoutCoordsHolder,
    morphTracker: ArtworkMorphTracker,
    expansionFractionProvider: () -> Float,
    aodSourceBounds: AodSourceBounds?,
    aodTransitionProgressProvider: () -> Float,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val songInfoBounds = aodSourceBounds?.songInfoBounds ?: remember { MorphElementBounds() }
    val miniSongInfoLeftPx = remember(density) { with(density) { 66.dp.toPx() } }
    val miniSongInfoTopPx = remember(density) { with(density) { 13.5.dp.toPx() } }
    val miniReservedWidthPx = remember(density) { with(density) { 210.dp.toPx() } }
    val artistSpacerPx = remember(density) { with(density) { 4.dp.toPx() } }

    val baseTitleMiniScale = remember(titleFontSize) { (15f / titleFontSize.value).coerceIn(0.5f, 0.8f) }
    val baseArtistMiniScale = remember(artistFontSize) { (13f / artistFontSize.value).coerceIn(0.7f, 1.0f) }
    val artistCounterScale = remember(baseTitleMiniScale, baseArtistMiniScale) {
        (baseArtistMiniScale / baseTitleMiniScale).coerceIn(1f, 1.8f)
    }

    val fallbackTitleMarquee = remember { MarqueeSyncState() }
    val fallbackArtistMarquee = remember { MarqueeSyncState() }
    val shouldRunMarquee = expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f
    val shouldRunMarqueeProvider = remember(expansionFractionProvider, aodTransitionProgressProvider) {
        { expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f }
    }
    val morphFractionProvider = remember(morphTracker, expansionFractionProvider) {
        { morphTracker.evaluate(expansionFractionProvider()).sizeT }
    }
    val miniAvailableWidthPxProvider = remember(aodSourceBounds, rootCoordsHolder, screenWidthPx, miniReservedWidthPx) {
        {
            val measuredMiniW = aodSourceBounds?.miniSongInfoWidthPx ?: 0f
            if (measuredMiniW > 0f) {
                measuredMiniW
            } else {
                val sheetW = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: screenWidthPx
                (sheetW - miniReservedWidthPx).coerceAtLeast(60f)
            }
        }
    }
    val fullAvailableWidthPxProvider = remember(songInfoBounds, rootCoordsHolder, screenWidthPx) {
        {
            val boundW = songInfoBounds.size.width.toFloat()
            val curRootW = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: screenWidthPx
            val deltaW = (screenWidthPx - curRootW).coerceAtLeast(0f)
            if (boundW > 0f) boundW + deltaW else 0f
        }
    }
    val titleScaleProvider = remember(morphTracker, expansionFractionProvider, baseTitleMiniScale) {
        { lerp(baseTitleMiniScale, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
    }
    val artistScaleProvider = remember(morphTracker, expansionFractionProvider, baseArtistMiniScale) {
        { lerp(baseArtistMiniScale, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
    }

    Column(
        modifier = modifier
            .captureUntransformedBounds(rootCoordsHolder, songInfoBounds)
            .graphicsLayer {
                val s = morphTracker.evaluate(expansionFractionProvider())
                val destX = if (songInfoBounds.hasBounds) songInfoBounds.offset.x else miniSongInfoLeftPx
                val destY = if (songInfoBounds.hasBounds) songInfoBounds.offset.y else miniSongInfoTopPx
                val scale = lerp(baseTitleMiniScale, 1f, s.sizeT)
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = (miniSongInfoLeftPx - destX) * (1f - s.posT)
                translationY = (miniSongInfoTopPx - destY) * (1f - s.posT)
                alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
            }
    ) {
        SharedPlayerMarqueeText(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.ExtraBold,
                fontSize = titleFontSize,
                lineHeight = (titleFontSize.value + 5f).sp,
                color = contentColor
            ),
            marqueeState = aodSourceBounds?.titleMarquee ?: fallbackTitleMarquee,
            shouldRunMarquee = shouldRunMarquee,
            shouldRunMarqueeProvider = shouldRunMarqueeProvider,
            morphFractionProvider = morphFractionProvider,
            miniAvailableWidthPxProvider = miniAvailableWidthPxProvider,
            fullAvailableWidthPxProvider = fullAvailableWidthPxProvider,
            localScaleProvider = titleScaleProvider,
            baseMiniScale = baseTitleMiniScale,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTitleClick
                )
        )

        Spacer(modifier = Modifier.height(4.dp))

        SharedPlayerMarqueeText(
            text = artist,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold,
                fontSize = artistFontSize,
                lineHeight = (artistFontSize.value + 5f).sp,
                color = contentColor.copy(alpha = artistAlpha)
            ),
            marqueeState = aodSourceBounds?.artistMarquee ?: fallbackArtistMarquee,
            shouldRunMarquee = shouldRunMarquee,
            shouldRunMarqueeProvider = shouldRunMarqueeProvider,
            morphFractionProvider = morphFractionProvider,
            miniAvailableWidthPxProvider = miniAvailableWidthPxProvider,
            fullAvailableWidthPxProvider = fullAvailableWidthPxProvider,
            localScaleProvider = artistScaleProvider,
            baseMiniScale = baseArtistMiniScale,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val s = morphTracker.evaluate(expansionFractionProvider())
                    val artistScale = lerp(artistCounterScale, 1f, s.sizeT)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = artistScale
                    scaleY = artistScale
                    translationY = -artistSpacerPx * (1f - s.sizeT)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onArtistClick
                )
        )
    }
}

/**
 * Smooth wavy progress slider with timestamps, optional centered audio metadata pill,
 * MiniPlayer non-art fade, and AOD progressBounds registration.
 */
@Composable
internal fun StyleProgressSection(
    params: PlayerStyleParams,
    rootCoordsHolder: LayoutCoordsHolder,
    morphTracker: ArtworkMorphTracker,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    thumbColor: Color,
    timeTextColor: Color,
    metaPillBgColor: Color,
    metaPillTextColor: Color,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    trackHeight: Dp = 5.dp,
    modifier: Modifier = Modifier
) {
    val isVisible by remember(params.expansionFractionProvider) {
        derivedStateOf { params.expansionFractionProvider() > 0.01f }
    }
    val isExpanded by remember(params.currentSheetState, params.expansionFractionProvider) {
        derivedStateOf {
            params.currentSheetState == PlayerSheetState.EXPANDED && params.expansionFractionProvider() >= 0.995f
        }
    }
    val shouldRunRealtimeUpdates = params.allowRealtimeUpdates && isVisible
    val isPlaying = params.isPlayingProvider()

    val reportedDuration = params.totalDurationValue.coerceAtLeast(0L)
    val hintDuration = params.song.duration.coerceAtLeast(0L)
    val displayDurationValue = when {
        reportedDuration <= 0L && hintDuration <= 0L -> 0L
        reportedDuration <= 0L -> hintDuration
        hintDuration <= 0L -> reportedDuration
        abs(reportedDuration - hintDuration) <= 1500L -> reportedDuration
        else -> minOf(reportedDuration, hintDuration)
    }
    val durationForCalc = displayDurationValue.coerceAtLeast(1L)

    val (smoothProgressState, _) = rememberSmoothProgress(
        isPlayingProvider = params.isPlayingProvider,
        currentPositionProvider = params.currentPositionProvider,
        totalDuration = displayDurationValue,
        songId = params.song.id,
        sampleWhilePlayingMs = if (isExpanded) 180L else 320L,
        sampleWhilePausedMs = 800L,
        isVisible = shouldRunRealtimeUpdates
    )

    var sliderDragValue by remember(params.song.id) { mutableStateOf<Float?>(null) }
    var targetSeekFraction by remember(params.song.id) { mutableFloatStateOf(-1f) }
    var lastSeekFinishedTime by remember(params.song.id) { mutableLongStateOf(0L) }

    LaunchedEffect(params.song.id) {
        sliderDragValue = null
        targetSeekFraction = -1f
        lastSeekFinishedTime = 0L
    }

    LaunchedEffect(params.song.id) {
        snapshotFlow { smoothProgressState.value }.collect { progress ->
            if (sliderDragValue != null) return@collect
            val target = targetSeekFraction
            if (target < 0f) return@collect
            val elapsed = System.currentTimeMillis() - lastSeekFinishedTime
            if (abs(progress - target) < 0.025f || elapsed > 1200L) {
                targetSeekFraction = -1f
            }
        }
    }

    val displayFraction = (
        sliderDragValue
            ?: if (targetSeekFraction >= 0f) targetSeekFraction else smoothProgressState.value
    ).coerceIn(0f, 1f)

    params.aodSourceBounds?.currentSeekProgress = displayFraction

    val displayedPositionMs = (displayFraction * durationForCalc).roundToLong()
    val progressBounds = params.aodSourceBounds?.progressBounds ?: remember { MorphElementBounds() }
    val density = LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 18.dp.toPx() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                alpha = a
                translationY = (1f - a) * slideOffsetPx
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .captureUntransformedBounds(rootCoordsHolder, progressBounds)
        ) {
            WavyMusicSlider(
                value = displayFraction,
                onValueChange = { frac ->
                    sliderDragValue = frac.coerceIn(0f, 1f)
                },
                onValueChangeFinished = {
                    val committed = sliderDragValue ?: return@WavyMusicSlider
                    targetSeekFraction = committed
                    lastSeekFinishedTime = System.currentTimeMillis()
                    sliderDragValue = null
                    params.onSeek((committed * durationForCalc).roundToLong())
                },
                modifier = Modifier.fillMaxWidth(),
                trackHeight = trackHeight,
                thumbRadius = 7.5.dp,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = inactiveTrackColor,
                thumbColor = thumbColor,
                waveAmplitudeWhenPlaying = waveAmplitudeWhenPlaying,
                waveLength = 54.dp,
                hideInactiveTrackPortion = false,
                isPlaying = isPlaying,
                isWaveEligible = isExpanded && sliderDragValue == null
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isVisible) formatDuration(displayedPositionMs) else "--:--",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = timeTextColor
                )
            )

            if (!params.audioMetaLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = metaPillBgColor,
                    contentColor = metaPillTextColor
                ) {
                    Text(
                        text = params.audioMetaLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = if (isVisible) formatDuration(displayDurationValue) else "--:--",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = timeTextColor
                )
            )
        }
    }
}

/**
 * Full-width Expressive Transport Row replicating AnimatedPlaybackControls from the existing styles:
 * - Exact single-layer GPU morphing (DynamicStyleTransportShape + drawRect + expressiveTapColorWave)
 * - Exact Play/Pause corner behavior (playPauseCornerPlaying = 26.dp, playPauseCornerPaused = 26.dp or 40.dp,
 *   no conflicting press-scale or 16dp jump on tap)
 * - Exact neighbor-pushing weight spring animation on tap
 */
@Composable
internal fun StyleMorphingTransportRow(
    order: List<MiniTransportSlot>,
    baseWeights: Map<MiniTransportSlot, Float>,
    screenWidthPx: Float,
    rootCoordsHolder: LayoutCoordsHolder,
    morphTracker: ArtworkMorphTracker,
    expansionFractionProvider: () -> Float,
    aodSourceBounds: AodSourceBounds?,
    aodTransitionProgressProvider: () -> Float,
    isPlaying: Boolean,
    playContainerColor: Color,
    playContentColor: Color,
    skipContainerColor: Color,
    skipContentColor: Color,
    playCornerPlaying: Dp = 26.dp,
    playCornerPaused: Dp = 40.dp,
    skipCorner: Dp = 40.dp,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    height: Dp = 80.dp,
    spacing: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val miniBtnSizePx = remember(density) { with(density) { 36.dp.toPx() } }
    val miniBtnTopPx = remember(density) { with(density) { 14.dp.toPx() } }
    val miniEndPadPx = remember(density) { with(density) { 12.dp.toPx() } }
    val miniGapPx = remember(density) { with(density) { 8.dp.toPx() } }
    val miniIconSizePx = remember(density) { with(density) { 22.dp.toPx() } }

    var lastClicked by remember { mutableStateOf<MiniTransportSlot?>(null) }
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestLastClicked by rememberUpdatedState(lastClicked)
    val isPlayPauseLocked =
        lastClicked == MiniTransportSlot.NEXT || lastClicked == MiniTransportSlot.PREVIOUS

    LaunchedEffect(lastClicked) {
        if (lastClicked != null) {
            delay(220L)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }
        val shouldDelay = latestLastClicked != MiniTransportSlot.PLAY_PAUSE
        if (shouldDelay) {
            delay(220L)
        }
        if (!latestIsPlaying) {
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

    val weightSpring = remember {
        spring<Float>(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    fun targetWeightFor(slot: MiniTransportSlot): Float {
        val base = baseWeights[slot] ?: 1f
        return when (lastClicked) {
            slot -> base * 1.18f
            null -> base
            else -> base * 0.72f
        }
    }

    val playCorner by animateDpAsState(
        targetValue = if (playPauseVisualState) playCornerPlaying else playCornerPaused,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "stylePlayCorner"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
            },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        order.forEach { slot ->
            val isPlay = slot == MiniTransportSlot.PLAY_PAUSE
            val animatedWeight by animateFloatAsState(
                targetValue = targetWeightFor(slot),
                animationSpec = weightSpring,
                label = "transportWeight_${slot.name}"
            )
            val elementBounds = when (slot) {
                MiniTransportSlot.PREVIOUS -> aodSourceBounds?.prevBounds
                MiniTransportSlot.PLAY_PAUSE -> aodSourceBounds?.playBounds
                MiniTransportSlot.NEXT -> aodSourceBounds?.nextBounds
            } ?: remember { MorphElementBounds() }

            val targetBgColor = if (isPlay) playContainerColor else skipContainerColor
            val targetIconColor = if (isPlay) playContentColor else skipContentColor
            val targetIconSize = if (isPlay) 36.dp else 32.dp
            val miniCornerDp = if (isPlay && playPauseVisualState) 11.dp else 18.dp
            val fullCornerDp = if (isPlay) playCorner else skipCorner

            Box(
                modifier = Modifier
                    .weight(animatedWeight)
                    .fillMaxHeight()
                    .captureUntransformedBounds(rootCoordsHolder, elementBounds)
                    .graphicsLayer {
                        val s = morphTracker.evaluate(expansionFractionProvider())
                        val cw = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: screenWidthPx
                        val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                        val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                        val miniPrevLeftPx = (miniPlayLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                        val miniLeftPx = when (slot) {
                            MiniTransportSlot.PREVIOUS -> miniPrevLeftPx
                            MiniTransportSlot.PLAY_PAUSE -> miniPlayLeftPx
                            MiniTransportSlot.NEXT -> miniNextLeftPx
                        }
                        val destX = elementBounds.offset.x
                        val destY = elementBounds.offset.y
                        val curW = lerp(miniBtnSizePx, size.width, s.sizeT)
                        val curH = lerp(miniBtnSizePx, size.height, s.sizeT)
                        val sx = if (size.width > 0f) (curW / size.width).coerceAtLeast(0.01f) else 1f
                        val sy = if (size.height > 0f) (curH / size.height).coerceAtLeast(0.01f) else 1f
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = sx
                        scaleY = sy
                        translationX = (miniLeftPx - destX) * (1f - s.posT)
                        translationY = (miniBtnTopPx - destY) * (1f - s.posT)

                        val fullCornerPx = fullCornerDp.toPx().coerceAtMost(minOf(size.width, size.height) * 0.5f)
                        val miniCornerPx = miniCornerDp.toPx().coerceAtMost(miniBtnSizePx * 0.5f)
                        val targetVisualR = lerp(miniCornerPx, fullCornerPx, s.sizeT)
                        val rx = (targetVisualR / sx).coerceIn(0f, size.width * 0.5f)
                        val ry = (targetVisualR / sy).coerceIn(0f, size.height * 0.5f)

                        clip = true
                        shape = DynamicStyleTransportShape(rxPx = rx, ryPx = ry)
                    }
                    .drawBehind {
                        drawRect(targetBgColor)
                    }
                    .expressiveTapColorWave(
                        waveColor = targetIconColor.copy(alpha = 0.28f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        lastClicked = slot
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (slot) {
                            MiniTransportSlot.PREVIOUS -> onPrevious()
                            MiniTransportSlot.PLAY_PAUSE -> onPlayPause()
                            MiniTransportSlot.NEXT -> onNext()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        val bw = elementBounds.size.width.toFloat()
                        val bh = elementBounds.size.height.toFloat()
                        if (bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val parentScaleX = lerp(miniBtnSizePx, bw, s.sizeT) / bw
                            val parentScaleY = lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = targetIconSize.toPx().coerceAtLeast(1f)
                            val desiredScale = lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    when (slot) {
                        MiniTransportSlot.PREVIOUS -> Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_skip_previous_filled_24),
                            contentDescription = "Previous",
                            tint = targetIconColor,
                            modifier = Modifier.size(targetIconSize)
                        )
                        MiniTransportSlot.NEXT -> Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_skip_next_filled_24),
                            contentDescription = "Next",
                            tint = targetIconColor,
                            modifier = Modifier.size(targetIconSize)
                        )
                        MiniTransportSlot.PLAY_PAUSE -> Crossfade(
                            targetState = playPauseVisualState,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "stylePlayPauseIcon"
                        ) { playing ->
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (playing) R.drawable.rounded_pause_filled_24
                                    else R.drawable.rounded_play_arrow_filled_24
                                ),
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = targetIconColor,
                                modifier = Modifier.size(targetIconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated favorite heart button with bouncy spring pop.
 */
@Composable
internal fun StyleFavoriteButton(
    isFavorite: Boolean,
    containerColor: Color,
    activeContainerColor: Color,
    iconTint: Color,
    activeIconTint: Color,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val bgColor by animateColorAsState(
        targetValue = if (isFavorite) activeContainerColor else containerColor,
        animationSpec = tween(220),
        label = "favBg"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isFavorite) activeIconTint else iconTint,
        animationSpec = tween(220),
        label = "favTint"
    )
    val popScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "favScale"
    )

    Surface(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = popScale
                scaleY = popScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = CircleShape,
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24
                ),
                contentDescription = "Favorite",
                tint = tintColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Full Material You Expressive bottom bar:
 * 3-segment pill dock (Shuffle | Repeat | Lyrics) matching the exact BottomToggleRow geometry (66dp height,
 * 60dp outer corners, 6dp inner padding/gap) paired with a separated 66dp circular menu button on the right.
 */
@Composable
internal fun StyleExpressiveBottomBarWithMenu(
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onLyricsClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    menuIconRes: Int = R.drawable.rounded_more_horiz_24,
    modifier: Modifier = Modifier
) {
    val baseTheme = LocalMaterialTheme.current
    val isDark = LocalPixelMusicDarkTheme.current
    val haptics = LocalHapticFeedback.current

    val rowCorners = 60.dp
    val rowShape = RoundedCornerShape(rowCorners)

    val outerContainerColor = if (isDark) {
        baseTheme.surfaceContainer.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val inactiveSegmentBg = baseTheme.onSurface.copy(alpha = 0.07f)
    val inactiveIconTint = baseTheme.onSurface

    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pressedIndex) {
        if (pressedIndex != null) {
            delay(200L)
            pressedIndex = null
        }
    }

    val weightSpring = remember {
        spring<Float>(
            dampingRatio = 0.64f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    fun segmentWeight(index: Int): Float = when (pressedIndex) {
        index -> 1.18f
        null -> 1f
        else -> 0.91f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 66.dp, max = 68.dp)
                .height(66.dp)
                .clip(rowShape)
                .background(outerContainerColor, rowShape)
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(rowShape),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val w0 by animateFloatAsState(segmentWeight(0), weightSpring, label = "dockW0")
                ExpressiveDockPillButton(
                    iconRes = R.drawable.rounded_shuffle_24,
                    contentDescription = "Shuffle",
                    isActive = isShuffleEnabled,
                    isFirst = true,
                    isLast = false,
                    activeBg = baseTheme.primary,
                    activeTint = baseTheme.onPrimary,
                    inactiveBg = inactiveSegmentBg,
                    inactiveTint = inactiveIconTint,
                    onClick = {
                        pressedIndex = 0
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShuffleToggle()
                    },
                    modifier = Modifier
                        .weight(w0)
                        .fillMaxHeight()
                )

                val w1 by animateFloatAsState(segmentWeight(1), weightSpring, label = "dockW1")
                ExpressiveDockPillButton(
                    iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        R.drawable.rounded_repeat_one_24
                    } else {
                        R.drawable.rounded_repeat_24
                    },
                    contentDescription = "Repeat",
                    isActive = repeatMode != Player.REPEAT_MODE_OFF,
                    isFirst = false,
                    isLast = false,
                    activeBg = baseTheme.secondary,
                    activeTint = baseTheme.onSecondary,
                    inactiveBg = inactiveSegmentBg,
                    inactiveTint = inactiveIconTint,
                    onClick = {
                        pressedIndex = 1
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRepeatToggle()
                    },
                    modifier = Modifier
                        .weight(w1)
                        .fillMaxHeight()
                )

                val w2 by animateFloatAsState(segmentWeight(2), weightSpring, label = "dockW2")
                ExpressiveDockPillButton(
                    iconRes = R.drawable.rounded_lyrics_24,
                    contentDescription = "Lyrics",
                    isActive = false,
                    isFirst = false,
                    isLast = true,
                    activeBg = baseTheme.tertiary,
                    activeTint = baseTheme.onTertiary,
                    inactiveBg = inactiveSegmentBg,
                    inactiveTint = inactiveIconTint,
                    onClick = {
                        pressedIndex = 2
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLyricsClick()
                    },
                    modifier = Modifier
                        .weight(w2)
                        .fillMaxHeight()
                )
            }
        }

        // Separated circular menu button matching Expressive style
        StyleCircleMenuButton(
            containerColor = outerContainerColor,
            iconTint = inactiveIconTint,
            iconRes = menuIconRes,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMoreOptionsClick()
            }
        )
    }
}

@Composable
internal fun StyleCircleMenuButton(
    containerColor: Color,
    iconTint: Color,
    iconRes: Int? = null,
    imageVector: ImageVector? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val latestOnClick by rememberUpdatedState(onClick)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "menuBtnScale"
    )
    val shape = CircleShape

    Box(
        modifier = Modifier
            .size(66.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(containerColor, shape)
            .expressiveTapColorWave(
                waveColor = iconTint.copy(alpha = 0.22f),
                shape = shape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        up.consume()
                        latestOnClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = "More options",
                tint = iconTint,
                modifier = Modifier.size(25.dp)
            )
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = "More options",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveDockPillButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    activeBg: Color,
    activeTint: Color,
    inactiveBg: Color,
    inactiveTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val latestOnClick by rememberUpdatedState(onClick)

    val innerCorner by animateDpAsState(
        targetValue = when {
            isPressed -> 14.dp
            isActive -> 30.dp
            else -> 12.dp
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "dockInnerCorner"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "dockPressScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeBg else inactiveBg,
        animationSpec = tween(200),
        label = "dockBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isActive) activeTint else inactiveTint,
        animationSpec = tween(200),
        label = "dockTint"
    )

    val startR = if (isFirst) 30.dp else innerCorner
    val endR = if (isLast) 30.dp else innerCorner
    val shape = RoundedCornerShape(
        topStart = startR,
        bottomStart = startR,
        topEnd = endR,
        bottomEnd = endR
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(bgColor, shape)
            .expressiveTapColorWave(
                waveColor = iconColor.copy(alpha = 0.22f),
                shape = shape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        up.consume()
                        latestOnClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(23.dp)
        )
    }
}
