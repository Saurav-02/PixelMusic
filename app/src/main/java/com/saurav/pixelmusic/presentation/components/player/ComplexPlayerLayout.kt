package com.saurav.pixelmusic.presentation.components.player

import com.saurav.pixelmusic.presentation.components.expressiveTapColorWave

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.clusterSyncedWords
import com.saurav.pixelmusic.presentation.components.sanitizeLyricLineText
import com.saurav.pixelmusic.presentation.components.sanitizeSyncedWords
import com.saurav.pixelmusic.presentation.components.scoped.rememberSmoothProgress
import com.saurav.pixelmusic.presentation.viewmodel.PlayerSheetState
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalIsBlackAndWhiteTheme
import com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme
import com.saurav.pixelmusic.utils.ThumbnailUrlUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

// Visual lead-in (ms) so the spring pop-in animation starts slightly before the vocal transient
// and lands right as the singer pronounces the word.
private const val VOCAL_ONSET_LEAD_IN_MS = 85L

// Pushes the lowest point (M) of the circular arch to the left side: (C--M--------C)
private const val ARCH_CENTER_X_FRACTION = 0.25f

private val FastFunctionWords = setOf(
    "a", "an", "the", "to", "in", "on", "of", "at", "by", "or", "is", "it", "as", "be",
    "ka", "ke", "ki", "ko", "se", "ye", "ya", "na", "ho", "to", "bhi", "aur", "ek", "ne", "pe"
)

private val VowelGroupRegex = Regex("[aeiouyAEIOUY]+")
private val SustainedVowelRegex = Regex("(?i)(aa|ee|oo|ai|au|ei|ou|aie|gae|gaye|hai|haan|jaan|sha|dil|chain)")

private data class AsymmetricArchCircle(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val rightEdgeY: Float,
    val leftEdgeY: Float,
    val alphaRightDeg: Float,
    val alphaLeftDeg: Float
)

/**
 * Computes the circle geometry where the lowest point (M) of the arch is shifted to the left
 * at `cx = widthPx * centerXFraction` (`C--M--------C`), and the right edge rises by `rightRisePx`.
 */
private fun computeAsymmetricArchCircle(
    widthPx: Float,
    bottomVertexYPx: Float,
    rightRisePx: Float,
    centerXFraction: Float = ARCH_CENTER_X_FRACTION
): AsymmetricArchCircle {
    val w = widthPx.coerceAtLeast(1f)
    val cx = w * centerXFraction
    val dxRight = (w - cx).coerceAtLeast(1f)
    val sRight = rightRisePx.coerceAtLeast(16f)

    // Circle passing through lowest vertex (cx, bottomVertexYPx) and right edge (w, bottomVertexYPx - sRight)
    val radius = (sRight / 2f) + ((dxRight * dxRight) / (2f * sRight))
    val cy = bottomVertexYPx - radius

    val sinRight = (dxRight / radius).coerceIn(-1f, 1f)
    val sinLeft = (cx / radius).coerceIn(-1f, 1f)
    val alphaRightDeg = Math.toDegrees(asin(sinRight.toDouble())).toFloat()
    val alphaLeftDeg = Math.toDegrees(asin(sinLeft.toDouble())).toFloat()

    val rightEdgeY = bottomVertexYPx - sRight
    val leftEdgeY = cy + sqrt((radius * radius - cx * cx).coerceAtLeast(0f))

    return AsymmetricArchCircle(
        cx = cx,
        cy = cy,
        radius = radius,
        rightEdgeY = rightEdgeY,
        leftEdgeY = leftEdgeY,
        alphaRightDeg = alphaRightDeg,
        alphaLeftDeg = alphaLeftDeg
    )
}

/**
 * Non-state holder for the FullPlayer root LayoutCoordinates and dimensions.
 * Storing coordinates in plain fields instead of MutableState avoids Snapshot writes during layout,
 * and reading them via a dedicated outer .layout modifier (outside .graphicsLayer) ensures captured
 * destination coordinates are 100% untransformed (never polluted by graphicsLayer translation/scale).
 */
class LayoutCoordsHolder {
    var coords: LayoutCoordinates? = null
    var widthPx: Float = 0f
    var heightPx: Float = 0f
}

class MorphElementBounds(
    var offset: Offset = Offset.Zero,
    var size: IntSize = IntSize.Zero,
    var hasBounds: Boolean = false
)

internal fun Modifier.captureRootCoords(holder: LayoutCoordsHolder): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            holder.coords = coordinates
            holder.widthPx = placeable.width.toFloat()
            holder.heightPx = placeable.height.toFloat()
            placeable.place(0, 0)
        }
    }

internal fun Modifier.captureUntransformedBounds(
    rootHolder: LayoutCoordsHolder?,
    bounds: MorphElementBounds
): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val childCoords = coordinates
            val rootCoords = rootHolder?.coords
            if (childCoords != null && rootCoords != null && childCoords.isAttached && rootCoords.isAttached) {
                bounds.offset = rootCoords.localPositionOf(childCoords, Offset.Zero)
                bounds.size = IntSize(placeable.width, placeable.height)
                bounds.hasBounds = true
            }
            placeable.place(0, 0)
        }
    }

data class ArtworkMorphSample(
    val rawFraction: Float,
    val sizeT: Float,
    val posT: Float,
    val overshootScale: Float,
    val nonArtAlpha: Float
)

/**
 * Tracks drag direction to provide:
 * 1. When opening (dragging up): brisk expansion with a subtle overshoot scale ("getting bigger then sitting in place").
 * 2. When closing (dragging down): smooth deceleration curve ("getting slower and going to sit in the mini player").
 * Blended continuously by travel delta so reversing direction mid-gesture never jumps.
 */
class ArtworkMorphTracker {
    fun evaluate(rawFraction: Float): ArtworkMorphSample {
        val f = rawFraction.coerceIn(0f, 1f)
        // Pure, stateless, monotonic smooth curve with zero bounce and zero directional hitch
        val smooth = f * f * (3f - 2f * f)
        val sizeT = androidx.compose.ui.util.lerp(f, smooth, 0.30f).coerceIn(0f, 1f)
        val posT = androidx.compose.ui.util.lerp(f, smooth, 0.25f).coerceIn(0f, 1f)
        val nonArtAlpha = ((f - 0.22f) / 0.78f).coerceIn(0f, 1f)

        return ArtworkMorphSample(
            rawFraction = f,
            sizeT = sizeT,
            posT = posT,
            overshootScale = 1f,
            nonArtAlpha = nonArtAlpha
        )
    }
}

/**
 * Custom shape that smoothly morphs from the 44.dp MiniPlayer Circle (at morphProgress = 0f)
 * into the left-shifted circular arch (C--M--------C) at morphProgress = 1f.
 */
private class MorphingScoopedArcBottomShape(
    private val rightRisePx: Float,
    private val morphProgressProvider: () -> Float,
    private val targetCenterXFraction: Float = ARCH_CENTER_X_FRACTION
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val t = morphProgressProvider().coerceIn(0f, 1f)

        val currentCenterXFraction = androidx.compose.ui.util.lerp(0.5f, targetCenterXFraction, t)
        val currentRightRisePx = androidx.compose.ui.util.lerp(h * 0.5f, rightRisePx, t)
            .coerceIn(2f, (h - 1f).coerceAtLeast(2f))

        val circle = computeAsymmetricArchCircle(
            widthPx = w,
            bottomVertexYPx = h,
            rightRisePx = currentRightRisePx,
            centerXFraction = currentCenterXFraction
        )

        val maxTopR = minOf(w * 0.5f, circle.leftEdgeY, circle.rightEdgeY).coerceAtLeast(0f)
        val topCornerR = (maxTopR * (1f - t).pow(1.15f)).coerceIn(0f, maxTopR)

        val path = Path().apply {
            if (topCornerR > 0.5f) {
                moveTo(0f, topCornerR)
                arcTo(
                    rect = Rect(0f, 0f, topCornerR * 2f, topCornerR * 2f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w - topCornerR, 0f)
                arcTo(
                    rect = Rect(w - topCornerR * 2f, 0f, w, topCornerR * 2f),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            } else {
                moveTo(0f, 0f)
                lineTo(w, 0f)
            }

            lineTo(w, circle.rightEdgeY)
            arcTo(
                rect = Rect(
                    left = circle.cx - circle.radius,
                    top = circle.cy - circle.radius,
                    right = circle.cx + circle.radius,
                    bottom = circle.cy + circle.radius
                ),
                startAngleDegrees = 90f - circle.alphaRightDeg,
                sweepAngleDegrees = circle.alphaRightDeg + circle.alphaLeftDeg,
                forceMoveTo = false
            )
            if (topCornerR > 0.5f) {
                lineTo(0f, topCornerR)
            } else {
                lineTo(0f, 0f)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

private enum class VerticalControlButton {
    MORE, LIKE, PREVIOUS, PLAY_PAUSE, NEXT
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComplexPlayerLayout(
    song: Song,
    currentSongArtists: List<Artist>,
    albumArtQuality: AlbumArtQuality,
    aodScreenEnabled: Boolean,
    onEnterAodScreen: () -> Unit,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    allowRealtimeUpdates: Boolean,
    currentPositionProvider: () -> Long,
    totalDurationValue: Long,
    isPlayingProvider: () -> Boolean,
    isFavoriteProvider: () -> Boolean,
    lyricsProvider: () -> Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    lyricsSyncOffset: Int,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onCollapse: () -> Unit,
    onLyricsClick: () -> Unit,
    onTitleClick: () -> Unit = {},
    onArtistClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    audioMetaLabel: String? = null,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseTheme = LocalMaterialTheme.current
    val isDarkTheme = LocalPixelMusicDarkTheme.current
    val isBwTheme = LocalIsBlackAndWhiteTheme.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val optimizedAlbumArtUri = remember(song.id, albumArtQuality) {
        ThumbnailUrlUtils.optimizeArtworkUrl(song.albumArtUriString, albumArtQuality)
            ?: song.albumArtUriString.orEmpty()
    }

    // Solid background matching the mini player main color (primaryContainer)
    val solidBackgroundColor = baseTheme.primaryContainer
    val contentColor = baseTheme.onPrimaryContainer
    val accentColor = baseTheme.primary
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Reliable non-empty song title & artist resolution
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

    val configuration = LocalConfiguration.current
    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val songInfoBounds = aodSourceBounds?.songInfoBounds ?: remember { MorphElementBounds() }
    val artBounds = aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp
        // Lowest point M of the arch sits at ~52% height and 25% from the left (C--M--------C)
        val artHeightDp = screenHeightDp * 0.52f
        val rightRiseDp = (screenWidthDp * 0.23f).coerceIn(74.dp, 108.dp)
        val rightRisePx = with(density) { rightRiseDp.toPx() }
        val screenWidthPx = with(density) { screenWidthDp.toPx() }
        val artHeightPx = with(density) { artHeightDp.toPx() }
        val miniArtSizePx = with(density) { 44.dp.toPx() }
        val miniArtOffsetPx = with(density) { 10.dp.toPx() }
        val slideOffsetPx = with(density) { 24.dp.toPx() }

        val miniSongInfoLeftPx = with(density) { 66.dp.toPx() }
        val miniSongInfoTopPx = with(density) { 13.5.dp.toPx() }
        val miniReservedWidthPx = with(density) { 210.dp.toPx() }
        val artistSpacerPx = with(density) { 4.dp.toPx() }

        aodSourceBounds?.let { bounds ->
            bounds.artStyle = AodSourceArtStyle.COMPLEX_ARCH
            bounds.complexRightRisePx = rightRisePx
            // Same 104dp top scrim the real Complex art draws, so the AOD stand-in art can carry it too.
            bounds.topShadowHeightDp = 104f
            bounds.topShadowStartAlpha = 0.38f
            bounds.topShadowMidAlpha = 0.10f
            bounds.progressBounds.hasBounds = false
        }

        val morphTracker = remember { ArtworkMorphTracker() }
        val scoopedShape = remember(rightRisePx, morphTracker, expansionFractionProvider) {
            MorphingScoopedArcBottomShape(
                rightRisePx = rightRisePx,
                morphProgressProvider = {
                    morphTracker.evaluate(expansionFractionProvider()).sizeT
                },
                targetCenterXFraction = ARCH_CENTER_X_FRACTION
            )
        }

        // =====================================================================
        // LAYER 2: Morphing Edge-to-Edge Top Artwork Arch & Tied Progress Bar
        // (MiniPlayer 44.dp Circle <-> Left-Shifted Arch, sharing the exact same transition)
        // =====================================================================
        val shouldApplyPausedScale = !isPlayingProvider()
        val artScale by animateFloatAsState(
            targetValue = if (shouldApplyPausedScale) 1.0f else 1.03f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            label = "complexArtScale"
        )

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(expansionFractionProvider())
                    val fullW = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    val fullH = (constraints.maxHeight.toFloat() * 0.52f).takeIf { it > 0f } ?: artHeightPx
                    val baseW = androidx.compose.ui.util.lerp(miniArtSizePx, fullW, sample.sizeT)
                    val baseH = androidx.compose.ui.util.lerp(miniArtSizePx, fullH, sample.sizeT)
                    val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val baseX = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                    val baseY = androidx.compose.ui.util.lerp(miniArtOffsetPx, 0f, sample.posT)
                    val placeX = (baseX - (targetW - baseW) * 0.5f).roundToInt()
                    val placeY = (baseY - (targetH - baseH) * 0.5f).roundToInt()

                    val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                    layout(targetW, targetH) {
                        artBounds.offset = Offset(placeX.toFloat(), placeY.toFloat())
                        artBounds.size = IntSize(targetW, targetH)
                        artBounds.hasBounds = true
                        placeable.place(placeX, placeY)
                    }
                }
        ) {
            // Morphing Edge-to-Edge Artwork clipped to scoopedShape
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                    }
                    .clip(scoopedShape)
            ) {
                SmartImage(
                    model = optimizedAlbumArtUri,
                    contentDescription = resolvedTitle,
                    contentScale = ContentScale.Crop,
                    targetSize = coil.size.Size.ORIGINAL,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = artScale
                            scaleY = artScale
                        }
                )

                // Top scrim for status bar and drag handle legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                            alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.38f),
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Bold Material You Expressive Curved Progress Bar on the Arch
            // Tied directly to the album art arch, sharing its exact position, bounds, and curve morphing
            CurvedArcSeekBar(
                songId = song.id,
                rightRiseDp = rightRiseDp,
                morphTracker = morphTracker,
                currentPositionProvider = currentPositionProvider,
                totalDurationValue = totalDurationValue,
                songDurationHintMs = song.duration,
                isPlayingProvider = isPlayingProvider,
                expansionFractionProvider = expansionFractionProvider,
                currentSheetState = currentSheetState,
                allowRealtimeUpdates = allowRealtimeUpdates,
                activeTrackColor = baseTheme.primary,
                inactiveTrackColor = baseTheme.onPrimary,
                thumbColor = baseTheme.primary,
                onSeek = onSeek,
                aodScreenEnabled = aodScreenEnabled,
                onEnterAodScreen = onEnterAodScreen,
                onPrevious = onPrevious,
                onNext = onNext,
                aodSourceBounds = aodSourceBounds,
                aodTransitionProgressProvider = aodTransitionProgressProvider,
                modifier = Modifier.fillMaxSize()
            )
        }

        // =====================================================================
        // LAYER 4: Lower Half Content
        // Left: Reliable Song Title/Artist + Glowing Karaoke Lyrics
        // Right: Solid-Filled Vertical Stack (3-Dot Menu, Like, Previous, Play/Pause, Next)
        // =====================================================================
        val rightColumnTopDp = (artHeightDp - rightRiseDp + 24.dp)
            .coerceAtLeast(statusBarTopPadding + 64.dp)
            .coerceAtLeast(0.dp)
        val leftColumnTopDp = (artHeightDp + 18.dp).coerceAtLeast(0.dp)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = (navBarBottomPadding + 16.dp).coerceAtLeast(0.dp),
                    start = 22.dp,
                    end = 20.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // LEFT AREA: Song Title / Artist + Glowing Word-by-Word Karaoke Lyrics
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = leftColumnTopDp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Song Title & Artist — morphs in-place via GPU graphicsLayer from MiniPlayer slot (66.dp, 13.5.dp)
                val fallbackSongInfoLeftPx = with(density) { 26.dp.toPx() }
                val fallbackSongInfoTopPx = with(density) { (leftColumnTopDp).toPx() }
                val fallbackTitleMarquee = remember { MarqueeSyncState() }
                val fallbackArtistMarquee = remember { MarqueeSyncState() }
                val shouldRunComplexMarquee =
                    expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f
                val shouldRunComplexMarqueeProvider = remember(expansionFractionProvider, aodTransitionProgressProvider) {
                    { expansionFractionProvider() > 0.004f && aodTransitionProgressProvider() <= 0.001f }
                }
                val complexMorphFractionProvider = remember(morphTracker, expansionFractionProvider) {
                    { morphTracker.evaluate(expansionFractionProvider()).sizeT }
                }
                val complexMiniAvailableWidthPxProvider = remember(aodSourceBounds, rootCoordsHolder, screenWidthPx, miniReservedWidthPx) {
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
                val complexFullAvailableWidthPxProvider = remember(songInfoBounds, rootCoordsHolder, screenWidthPx) {
                    {
                        val boundW = songInfoBounds.size.width.toFloat()
                        val curRootW = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: screenWidthPx
                        val deltaW = (screenWidthPx - curRootW).coerceAtLeast(0f)
                        if (boundW > 0f) boundW + deltaW else 0f
                    }
                }
                val complexTitleScaleProvider = remember(morphTracker, expansionFractionProvider) {
                    { androidx.compose.ui.util.lerp(0.625f, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
                }
                val complexArtistScaleProvider = remember(morphTracker, expansionFractionProvider) {
                    { androidx.compose.ui.util.lerp(13f / 15f, 1f, morphTracker.evaluate(expansionFractionProvider()).sizeT) }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .captureUntransformedBounds(rootCoordsHolder, songInfoBounds)
                        .graphicsLayer {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val destX = if (songInfoBounds.hasBounds) songInfoBounds.offset.x else fallbackSongInfoLeftPx
                            val destY = if (songInfoBounds.hasBounds) songInfoBounds.offset.y else fallbackSongInfoTopPx
                            val scale = androidx.compose.ui.util.lerp(0.625f, 1f, s.sizeT)
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = scale
                            scaleY = scale
                            translationX = (miniSongInfoLeftPx - destX) * (1f - s.posT)
                            translationY = (miniSongInfoTopPx - destY) * (1f - s.posT)
                            alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                        }
                ) {
                    SharedPlayerMarqueeText(
                        text = resolvedTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            color = contentColor
                        ),
                        marqueeState = aodSourceBounds?.titleMarquee ?: fallbackTitleMarquee,
                        shouldRunMarquee = shouldRunComplexMarquee,
                        shouldRunMarqueeProvider = shouldRunComplexMarqueeProvider,
                        morphFractionProvider = complexMorphFractionProvider,
                        miniAvailableWidthPxProvider = complexMiniAvailableWidthPxProvider,
                        fullAvailableWidthPxProvider = complexFullAvailableWidthPxProvider,
                        localScaleProvider = complexTitleScaleProvider,
                        baseMiniScale = 0.625f,
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
                        text = resolvedArtist,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = contentColor.copy(alpha = 0.78f)
                        ),
                        marqueeState = aodSourceBounds?.artistMarquee ?: fallbackArtistMarquee,
                        shouldRunMarquee = shouldRunComplexMarquee,
                        shouldRunMarqueeProvider = shouldRunComplexMarqueeProvider,
                        morphFractionProvider = complexMorphFractionProvider,
                        miniAvailableWidthPxProvider = complexMiniAvailableWidthPxProvider,
                        fullAvailableWidthPxProvider = complexFullAvailableWidthPxProvider,
                        localScaleProvider = complexArtistScaleProvider,
                        baseMiniScale = 13f / 15f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                // Counter-scale artist text so at t=0 (parent scale 0.625) it renders at 13.sp matching MiniPlayer
                                val s = morphTracker.evaluate(expansionFractionProvider())
                                val artistScale = androidx.compose.ui.util.lerp(1.3866667f, 1f, s.sizeT)
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

                if (!audioMetaLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                                val a = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                                alpha = a
                                translationY = (1f - a) * (slideOffsetPx * 0.6f)
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = contentColor.copy(alpha = 0.14f),
                            contentColor = contentColor.copy(alpha = 0.96f)
                        ) {
                            Text(
                                text = audioMetaLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Glowing Word-by-Word Karaoke Lyrics Stage (UNCLIPPED so glow radiates freely)
                ComplexKaraokeLyricsStage(
                    lyrics = lyricsProvider(),
                    playbackPositionFlow = playbackPositionFlow,
                    isPlayingProvider = isPlayingProvider,
                    syncOffsetMs = lyricsSyncOffset,
                    textColor = contentColor,
                    accentColor = accentColor,
                    onClick = onLyricsClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                            val a = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                            alpha = a
                            translationY = (1f - a) * slideOffsetPx
                        }
                )
            }

            // RIGHT AREA: Solid-Filled Material You Expressive Vertical Control Stack
            // Order: 3-Dot Menu -> Like -> Previous -> Play/Pause -> Next
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = rightColumnTopDp),
                contentAlignment = Alignment.BottomCenter
            ) {
                VerticalExpressiveControlStack(
                    containerWidthPx = screenWidthPx,
                    morphTracker = morphTracker,
                    isPlayingProvider = isPlayingProvider,
                    isFavoriteProvider = isFavoriteProvider,
                    expansionFractionProvider = expansionFractionProvider,
                    rootCoordsHolder = rootCoordsHolder,
                    onMoreOptionsClick = onMoreOptionsClick,
                    onFavoriteToggle = onFavoriteToggle,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    playPauseContainerColor = baseTheme.primary,
                    onPlayPauseContainerColor = baseTheme.onPrimary,
                    skipContainerColor = baseTheme.onPrimary,
                    onSkipContainerColor = baseTheme.primary,
                    likedContainerColor = baseTheme.tertiary,
                    onLikedContainerColor = baseTheme.onTertiary,
                    neutralContainerColor = baseTheme.onPrimary,
                    onNeutralContainerColor = baseTheme.primary,
                    aodSourceBounds = aodSourceBounds,
                    aodTransitionProgressProvider = aodTransitionProgressProvider,
                    modifier = Modifier
                        .width(68.dp)
                        .height(332.dp)
                )
            }
        }

        // Top bar (left collapse button + right speaker/listen-together/queue pills) is rendered by FullPlayerContent's TopAppBar.
    }
}

/**
 * Bold Material You Expressive curved progress bar that follows the asymmetric (C--M--------C) arch.
 * Features a thick split track with a clean gap around a bold perpendicular pill thumb, and no time labels.
 */
@Composable
private fun CurvedArcSeekBar(
    songId: String,
    rightRiseDp: Dp,
    morphTracker: ArtworkMorphTracker,
    currentPositionProvider: () -> Long,
    totalDurationValue: Long,
    songDurationHintMs: Long,
    isPlayingProvider: () -> Boolean,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    allowRealtimeUpdates: Boolean,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    thumbColor: Color,
    onSeek: (Long) -> Unit,
    aodScreenEnabled: Boolean = true,
    onEnterAodScreen: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val isVisible by remember(expansionFractionProvider) {
        derivedStateOf { expansionFractionProvider() > 0.01f }
    }
    val isExpanded by remember(currentSheetState, expansionFractionProvider) {
        derivedStateOf {
            currentSheetState == PlayerSheetState.EXPANDED && expansionFractionProvider() >= 0.995f
        }
    }
    val shouldRunRealtimeUpdates = allowRealtimeUpdates && isVisible

    val reportedDuration = totalDurationValue.coerceAtLeast(0L)
    val hintDuration = songDurationHintMs.coerceAtLeast(0L)
    val displayDurationValue = when {
        reportedDuration <= 0L && hintDuration <= 0L -> 0L
        reportedDuration <= 0L -> hintDuration
        hintDuration <= 0L -> reportedDuration
        abs(reportedDuration - hintDuration) <= 1500L -> reportedDuration
        else -> minOf(reportedDuration, hintDuration)
    }
    val durationForCalc = displayDurationValue.coerceAtLeast(1L)

    val (smoothProgressState, _) = rememberSmoothProgress(
        isPlayingProvider = isPlayingProvider,
        currentPositionProvider = currentPositionProvider,
        totalDuration = displayDurationValue,
        songId = songId,
        sampleWhilePlayingMs = if (isExpanded) 180L else 320L,
        sampleWhilePausedMs = 800L,
        isVisible = shouldRunRealtimeUpdates
    )

    var sliderDragValue by remember(songId) { mutableStateOf<Float?>(null) }
    var targetSeekFraction by remember(songId) { mutableFloatStateOf(-1f) }
    var lastSeekFinishedTime by remember(songId) { mutableLongStateOf(0L) }

    LaunchedEffect(songId) {
        sliderDragValue = null
        targetSeekFraction = -1f
        lastSeekFinishedTime = 0L
    }

    LaunchedEffect(songId) {
        snapshotFlow { smoothProgressState.value }.collect { progress ->
            if (sliderDragValue != null) return@collect
            val target = targetSeekFraction
            if (target < 0f) return@collect
            val timeSinceSeek = System.currentTimeMillis() - lastSeekFinishedTime
            val diff = abs(progress - target)
            if (timeSinceSeek > 1500L || diff < 0.04f) {
                targetSeekFraction = -1f
            }
        }
    }

    LaunchedEffect(lastSeekFinishedTime, targetSeekFraction) {
        if (targetSeekFraction >= 0f && lastSeekFinishedTime > 0L) {
            kotlinx.coroutines.delay(1500L)
            if (sliderDragValue == null) {
                targetSeekFraction = -1f
            }
        }
    }

    val currentProgress = when {
        sliderDragValue != null -> sliderDragValue!!
        targetSeekFraction >= 0f -> targetSeekFraction
        else -> smoothProgressState.value
    }.coerceIn(0f, 1f)
    aodSourceBounds?.currentSeekProgress = currentProgress

    val isDragging = sliderDragValue != null
    val thumbLengthScale by animateFloatAsState(
        targetValue = if (isDragging) 1.32f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expressiveThumbScale"
    )

    val rightRisePx = with(density) { rightRiseDp.toPx() }
    val edgeInsetPx = with(density) { 18.dp.toPx() }
    val hitTolerancePx = with(density) { 38.dp.toPx() }

    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestDurationForCalc by rememberUpdatedState(durationForCalc)
    val latestAodScreenEnabled by rememberUpdatedState(aodScreenEnabled)
    val latestOnEnterAodScreen by rememberUpdatedState(onEnterAodScreen)
    val latestOnPrevious by rememberUpdatedState(onPrevious)
    val latestOnNext by rememberUpdatedState(onNext)

    Box(
        modifier = modifier
            .graphicsLayer {
                // Keep alpha at 1f (never semi-transparent) so the dark album art underneath the upper
                // half of the stroke never bleeds through to create a two-tone split along the arch seam.
                alpha = if (aodTransitionProgressProvider() > 0.001f || expansionFractionProvider() <= 0.22f) 0f else 1f
            }
            .pointerInput(songId, rightRisePx, isExpanded) {
                if (!isExpanded) return@pointerInput
                var lastQuantizedStep = -1
                val swipeSkipThresholdPx = 72.dp.toPx()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@awaitEachGesture

                    val circle = computeAsymmetricArchCircle(
                        widthPx = w,
                        bottomVertexYPx = h,
                        rightRisePx = rightRisePx,
                        centerXFraction = ARCH_CENTER_X_FRACTION
                    )

                    val dxCurve = (down.position.x - circle.cx).coerceIn(-circle.radius * 0.99f, circle.radius * 0.99f)
                    val curveYAtTouchX = circle.cy + sqrt((circle.radius * circle.radius) - (dxCurve * dxCurve))

                    // 1. Touch near the curved arch seekbar -> scrub track
                    if (abs(down.position.y - curveYAtTouchX) <= hitTolerancePx) {
                        down.consume()
                        val usableWidth = (w - 2f * edgeInsetPx).coerceAtLeast(1f)
                        val initialFraction = ((down.position.x - edgeInsetPx) / usableWidth).coerceIn(0f, 1f)
                        sliderDragValue = initialFraction
                        lastQuantizedStep = (initialFraction * 24f).toInt()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        var dragCompleted = false
                        try {
                            drag(down.id) { change ->
                                change.consume()
                                val newFraction = ((change.position.x - edgeInsetPx) / usableWidth).coerceIn(0f, 1f)
                                sliderDragValue = newFraction
                                val step = (newFraction * 24f).toInt()
                                if (step != lastQuantizedStep) {
                                    lastQuantizedStep = step
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            dragCompleted = true
                        } finally {
                            val committed = sliderDragValue ?: initialFraction
                            sliderDragValue = null
                            if (dragCompleted) {
                                targetSeekFraction = committed
                                lastSeekFinishedTime = System.currentTimeMillis()
                                latestOnSeek((committed * latestDurationForCalc).roundToLong())
                            }
                        }
                        return@awaitEachGesture
                    }

                    // 2. Touch on the album art above the arch -> long-press for AOD or horizontal swipe for prev/next
                    if (down.position.y < curveYAtTouchX - hitTolerancePx) {
                        val touchSlop = viewConfiguration.touchSlop
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val startPos = down.position
                        var isHorizontalSwipe = false
                        var gestureEndedBeforeTimeout = false
                        var horizontalDrag = 0f

                        val timedOutForLongPress = withTimeoutOrNull(longPressTimeout) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    gestureEndedBeforeTimeout = true
                                    break
                                }
                                val totalDelta = change.position - startPos
                                if (totalDelta.getDistance() > touchSlop) {
                                    if (abs(totalDelta.x) > abs(totalDelta.y) * 1.15f) {
                                        isHorizontalSwipe = true
                                        horizontalDrag = totalDelta.x
                                        change.consume()
                                    } else {
                                        gestureEndedBeforeTimeout = true
                                    }
                                    break
                                }
                            }
                            false
                        } == null

                        if (timedOutForLongPress && !gestureEndedBeforeTimeout && !isHorizontalSwipe) {
                            if (latestAodScreenEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                latestOnEnterAodScreen()
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) break
                            }
                        } else if (isHorizontalSwipe) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) break
                                val stepDx = change.positionChange().x
                                val stepDy = change.positionChange().y
                                if (abs(stepDx) > abs(stepDy) * 1.1f) {
                                    horizontalDrag += stepDx
                                    change.consume()
                                }
                            }
                            if (horizontalDrag > swipeSkipThresholdPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnPrevious()
                            } else if (horizontalDrag < -swipeSkipThresholdPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnNext()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val sample = morphTracker.evaluate(expansionFractionProvider())
            val visScale = sample.nonArtAlpha
            if (visScale <= 0.02f) return@Canvas
            val t = sample.sizeT
            val currentCenterXFraction = androidx.compose.ui.util.lerp(0.5f, ARCH_CENTER_X_FRACTION, t)
            val currentRightRisePx = androidx.compose.ui.util.lerp(h * 0.5f, rightRisePx, t)
                .coerceIn(2f, (h - 1f).coerceAtLeast(2f))

            val circle = computeAsymmetricArchCircle(
                widthPx = w,
                bottomVertexYPx = h,
                rightRisePx = currentRightRisePx,
                centerXFraction = currentCenterXFraction
            )

            val effectiveEdgeInsetPx = edgeInsetPx * t
            val leftChord = (circle.cx - effectiveEdgeInsetPx).coerceAtLeast(0f)
            val rightChord = ((w - effectiveEdgeInsetPx) - circle.cx).coerceAtLeast(0f)

            val alphaStartDeg = Math.toDegrees(asin((leftChord / circle.radius).coerceIn(-1f, 1f).toDouble())).toFloat()
            val alphaEndDeg = Math.toDegrees(asin((rightChord / circle.radius).coerceIn(-1f, 1f).toDouble())).toFloat()

            // Start at left edge (90 + alphaStartDeg) and sweep counter-clockwise to right edge (90 - alphaEndDeg)
            val startAngleDeg = 90f + alphaStartDeg
            val totalSweepDeg = -(alphaStartDeg + alphaEndDeg)
            val thumbAngleDeg = startAngleDeg + (totalSweepDeg * currentProgress)

            val scaleFactor = androidx.compose.ui.util.lerp(0.6f, 1.0f, t) * visScale
            val trackStrokePx = 10.dp.toPx() * scaleFactor
            // Angular gap around the expressive pill thumb (Material 3 Expressive split track)
            val gapArcLengthPx = 13.dp.toPx() * androidx.compose.ui.util.lerp(0.6f, 1.0f, t)
            val gapDeg = Math.toDegrees((gapArcLengthPx / circle.radius).toDouble()).toFloat()

            val arcRectTopLeft = Offset(circle.cx - circle.radius, circle.cy - circle.radius)
            val arcRectSize = Size(circle.radius * 2f, circle.radius * 2f)

            // 1. Bold Active Track (left start -> thumb - gap)
            val activeSweepDeg = (thumbAngleDeg - startAngleDeg) + gapDeg
            if (activeSweepDeg < -0.15f) {
                drawArc(
                    color = activeTrackColor,
                    startAngle = startAngleDeg,
                    sweepAngle = activeSweepDeg.coerceAtLeast(totalSweepDeg),
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcRectSize,
                    style = Stroke(width = trackStrokePx, cap = StrokeCap.Round)
                )
            }

            // 2. Bold Inactive Track (thumb + gap -> right end)
            val inactiveStartDeg = thumbAngleDeg - gapDeg
            val endAngleDeg = startAngleDeg + totalSweepDeg
            val inactiveSweepDeg = endAngleDeg - inactiveStartDeg
            if (inactiveSweepDeg < -0.15f) {
                drawArc(
                    color = inactiveTrackColor,
                    startAngle = inactiveStartDeg,
                    sweepAngle = inactiveSweepDeg,
                    useCenter = false,
                    topLeft = arcRectTopLeft,
                    size = arcRectSize,
                    style = Stroke(width = trackStrokePx, cap = StrokeCap.Round)
                )
            }

            // 3. Bold Material You Expressive Pill Thumb oriented perpendicular (normal) to the curve
            val thumbAngleRad = Math.toRadians(thumbAngleDeg.toDouble())
            val cosA = cos(thumbAngleRad).toFloat()
            val sinA = sin(thumbAngleRad).toFloat()
            val halfThumbLenPx = 12.dp.toPx() * thumbLengthScale * scaleFactor
            val innerR = circle.radius - halfThumbLenPx
            val outerR = circle.radius + halfThumbLenPx

            drawLine(
                color = thumbColor,
                start = Offset(circle.cx + innerR * cosA, circle.cy + innerR * sinA),
                end = Offset(circle.cx + outerR * cosA, circle.cy + outerR * sinA),
                strokeWidth = 7.dp.toPx() * scaleFactor,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * High-precision 60fps interpolated playback clock.
 * Because PlaybackStateHolder emits position ticks every 250ms, waiting for raw StateFlow
 * updates can delay word appearance by up to a quarter second. This interpolates smoothly
 * between ticks using SystemClock.elapsedRealtime() while playing.
 */
@Composable
private fun rememberInterpolatedPlaybackPositionMs(
    playbackPositionFlow: StateFlow<Long>,
    isPlayingProvider: () -> Boolean
): State<Long> {
    val rawPositionMs by playbackPositionFlow.collectAsStateWithLifecycle()
    val isPlaying = isPlayingProvider()
    val interpolatedState = remember { mutableLongStateOf(rawPositionMs) }
    val anchorPosRef = remember { longArrayOf(rawPositionMs) }
    val anchorElapsedRef = remember { longArrayOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(rawPositionMs) {
        anchorPosRef[0] = rawPositionMs
        anchorElapsedRef[0] = SystemClock.elapsedRealtime()
        interpolatedState.longValue = rawPositionMs
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            interpolatedState.longValue = anchorPosRef[0]
            return@LaunchedEffect
        }
        anchorElapsedRef[0] = SystemClock.elapsedRealtime()
        while (isActive) {
            withFrameNanos {
                val elapsedSinceTick = (SystemClock.elapsedRealtime() - anchorElapsedRef[0]).coerceIn(0L, 320L)
                interpolatedState.longValue = anchorPosRef[0] + elapsedSinceTick
            }
        }
    }

    return interpolatedState
}

/**
 * Smart vocal-cadence word timing resolver:
 * 1. If explicit word-level timestamps exist, clusters multi-syllable fragments into whole words
 *    so words don't split awkwardly, preserving exact vocal start/end timestamps.
 * 2. If only line-level timestamps exist, estimates each word's vocal duration using phonetic
 *    syllable nuclei, sustained vowel digraphs (aa, ee, oo, ai, haan, hai, etc.), fast function-word
 *    compression, mid-line comma/punctuation breath pauses, and instrumental tail capping so words
 *    appear right as the singer pronounces them.
 */
private fun resolveSmartVocalWords(
    syncedLine: SyncedLine?,
    nextLineTimeMs: Long
): List<DisplayWord> {
    if (syncedLine == null) return emptyList()

    val sanitizedExplicit = syncedLine.words?.let(::sanitizeSyncedWords)
    if (!sanitizedExplicit.isNullOrEmpty()) {
        val clusters = clusterSyncedWords(sanitizedExplicit)
        if (clusters.isNotEmpty()) {
            return clusters.mapIndexed { idx, cluster ->
                val startMs = cluster.words.first().time.toLong()
                val nextClusterStartMs = if (idx < clusters.lastIndex) {
                    clusters[idx + 1].words.first().time.toLong()
                } else {
                    maxOf(cluster.words.last().time.toLong() + 420L, nextLineTimeMs)
                }
                val mergedWord = cluster.words.joinToString("") { it.word }
                DisplayWord(
                    word = mergedWord,
                    startTimeMs = startMs,
                    endTimeMs = nextClusterStartMs.coerceAtLeast(startMs + 80L)
                )
            }
        }
    }

    val sanitizedLine = sanitizeLyricLineText(syncedLine.line)
    val rawTokens = sanitizedLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (rawTokens.isEmpty()) return emptyList()

    val lineStart = syncedLine.time.toLong()
    val rawLineGapMs = (nextLineTimeMs - lineStart).coerceAtLeast(900L)

    data class TokenTimingProfile(
        val word: String,
        val vocalWeight: Float,
        val breathPauseAfterMs: Long
    )

    val profiles = rawTokens.mapIndexed { idx, token ->
        val cleanLetters = token.trim { !it.isLetterOrDigit() }.lowercase()
        val vowelMatches = VowelGroupRegex.findAll(cleanLetters).count().coerceAtLeast(1)
        val hasSustainedVowel = SustainedVowelRegex.containsMatchIn(cleanLetters)
        val isFastParticle = cleanLetters in FastFunctionWords && idx != rawTokens.lastIndex

        var weight = vowelMatches.toFloat() * 1.05f + (cleanLetters.length.coerceAtMost(8) * 0.12f)
        if (hasSustainedVowel) weight *= 1.26f
        if (isFastParticle) weight *= 0.62f
        if (idx == rawTokens.lastIndex) weight *= 1.32f

        val trimmedEnd = token.trimEnd()
        val lastChar = trimmedEnd.lastOrNull()
        val hasMidLineBreath = idx < rawTokens.lastIndex &&
            (lastChar == ',' || lastChar == ';' || lastChar == ':' || lastChar == '-' ||
                lastChar == '—' || lastChar == ')' || lastChar == '!' || lastChar == '?')

        if (hasMidLineBreath) {
            weight *= 1.18f
        }

        val pauseAfterMs = when {
            idx >= rawTokens.lastIndex -> 0L
            trimmedEnd.endsWith("...") || lastChar == '?' || lastChar == '!' || lastChar == ')' -> 280L
            hasMidLineBreath -> 230L
            else -> 0L
        }

        TokenTimingProfile(
            word = token,
            vocalWeight = weight.coerceAtLeast(0.45f),
            breathPauseAfterMs = pauseAfterMs
        )
    }

    val totalBreathPausesMs = profiles.sumOf { it.breathPauseAfterMs }
    val totalWeight = profiles.sumOf { it.vocalWeight.toDouble() }.toFloat().coerceAtLeast(1f)

    val naturalMaxVocalMs = (totalWeight * 295f).toLong() + totalBreathPausesMs + 320L
    val effectiveLineSpanMs = minOf((rawLineGapMs * 0.86f).toLong(), naturalMaxVocalMs)
        .coerceAtLeast(minOf(rawLineGapMs, 1050L))
    val availableSingingMs = (effectiveLineSpanMs - totalBreathPausesMs).coerceAtLeast(600L)

    var cursorMs = lineStart
    return profiles.mapIndexed { idx, profile ->
        val rawWordDuration = ((profile.vocalWeight / totalWeight) * availableSingingMs)
            .toLong()
            .coerceAtLeast(110L)
        val wordStart = cursorMs
        val wordEnd = if (idx == profiles.lastIndex) {
            maxOf(wordStart + rawWordDuration, minOf(nextLineTimeMs - 120L, wordStart + rawWordDuration + 650L))
        } else {
            wordStart + rawWordDuration
        }
        cursorMs = wordStart + rawWordDuration + profile.breathPauseAfterMs
        DisplayWord(
            word = profile.word,
            startTimeMs = wordStart,
            endTimeMs = wordEnd
        )
    }
}

private data class KaraokeLineState(
    val lineIndex: Int,
    val lineTimeMs: Int,
    val syncedLine: SyncedLine?,
    val nextLineTimeMs: Long,
    val nextLineText: String,
    val romanizedSubLine: String? = null,
    val translatedSubLine: String? = null
)

/**
 * Karaoke word-by-word lyrics display where:
 * - Each line transition uses the iOS Dynamic Island spring expansion + motion blur animation
 *   (matching MusicRecognitionOverlay's card appear animation).
 * - Words appear at the exact time the song speaks them and glow without any rectangular box clipping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComplexKaraokeLyricsStage(
    lyrics: Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    isPlayingProvider: () -> Boolean,
    syncOffsetMs: Int,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interpolatedPositionMs by rememberInterpolatedPlaybackPositionMs(
        playbackPositionFlow = playbackPositionFlow,
        isPlayingProvider = isPlayingProvider
    )
    val effectivePosition = interpolatedPositionMs + syncOffsetMs + VOCAL_ONSET_LEAD_IN_MS

    val context = androidx.compose.ui.platform.LocalContext.current
    val autoTranslate by remember(context) {
        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.autoTranslateFlow(context)
    }.collectAsStateWithLifecycle(initialValue = false)
    val translateLang by remember(context) {
        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.translateLanguageFlow(context)
    }.collectAsStateWithLifecycle(initialValue = "en")
    val useAiTranslation by remember(context) {
        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.useAiTranslationFlow(context)
    }.collectAsStateWithLifecycle(initialValue = false)
    val autoRomanize by remember(context) {
        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.autoRomanizeFlow(context)
    }.collectAsStateWithLifecycle(initialValue = false)
    val romanizationAsMain by remember(context) {
        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.romanizationAsMainFlow(context)
    }.collectAsStateWithLifecycle(initialValue = false)

    val syncedLines = lyrics?.synced ?: emptyList()
    val plainLines = lyrics?.plain?.filter { it.isNotBlank() } ?: emptyList()

    val sourceLineTexts = remember(syncedLines, plainLines) {
        if (syncedLines.isNotEmpty()) syncedLines.map { it.line } else plainLines
    }

    var liveTranslations by remember(sourceLineTexts, translateLang, autoTranslate, useAiTranslation) {
        mutableStateOf<List<String>?>(null)
    }
    LaunchedEffect(sourceLineTexts, translateLang, autoTranslate, useAiTranslation) {
        liveTranslations = if (autoTranslate && sourceLineTexts.isNotEmpty()) {
            val aiResult = if (useAiTranslation) {
                val langName = com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.languageDisplayName(translateLang)
                com.saurav.pixelmusic.lyrics.AiLyricsTranslator.translate(
                    context = context,
                    lines = sourceLineTexts,
                    targetLanguageName = langName,
                )
            } else null
            aiResult ?: com.saurav.pixelmusic.lyrics.GoogleTranslateEngine.translate(sourceLineTexts, targetLanguage = translateLang)
        } else null
    }

    var liveRomanizations by remember(sourceLineTexts, autoRomanize) {
        mutableStateOf<List<String?>?>(null)
    }
    LaunchedEffect(sourceLineTexts, autoRomanize) {
        liveRomanizations = if (autoRomanize && sourceLineTexts.isNotEmpty()) {
            val missingIndices = sourceLineTexts.indices.filter { i ->
                val existingRom = syncedLines.getOrNull(i)?.romanization
                existingRom.isNullOrBlank() && com.saurav.pixelmusic.lyrics.PixelMusicRomanizer.hasNonLatinScript(sourceLineTexts[i])
            }
            if (missingIndices.isEmpty()) {
                null
            } else {
                val fetched = com.saurav.pixelmusic.lyrics.GoogleTranslateEngine.romanize(missingIndices.map { sourceLineTexts[it] })
                if (fetched != null && fetched.size == missingIndices.size) {
                    MutableList<String?>(sourceLineTexts.size) { null }.also { out ->
                        missingIndices.forEachIndexed { idx, lineIndex ->
                            out[lineIndex] = fetched[idx]
                        }
                    }
                } else null
            }
        } else null
    }

    val romanizedList = remember(syncedLines, sourceLineTexts, autoRomanize, liveRomanizations) {
        if (!autoRomanize) emptyList()
        else sourceLineTexts.mapIndexed { i, lineText ->
            syncedLines.getOrNull(i)?.romanization?.takeIf { it.isNotBlank() }
                ?: liveRomanizations?.getOrNull(i)?.takeIf { it.isNotBlank() }
                ?: com.saurav.pixelmusic.lyrics.PixelMusicRomanizer.romanize(lineText)
        }
    }

    // IMPORTANT: No .clip() here so word glow and scale animations radiate freely without box edges!
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when {
            syncedLines.isNotEmpty() -> {
                val complexLyricsListState = androidx.compose.foundation.lazy.rememberLazyListState()
                key(syncedLines) {
                    com.saurav.pixelmusic.presentation.components.lyrics.PixelMusicSyncedLyricsView(
                        lines = syncedLines,
                        listState = complexLyricsListState,
                        playbackPositionFlow = playbackPositionFlow,
                        lyricsSyncOffset = syncOffsetMs,
                        positionOverrideMs = null,
                        accentColor = accentColor,
                        contentColor = textColor,
                        providerName = lyrics?.providerName,
                        lyricsAlignment = "LEFT",
                        showTranslationPref = true,
                        showRomanizationPref = true,
                        performanceModeEnabled = false,
                        immersiveMode = false,
                        isPlaying = isPlayingProvider(),
                        blurStrength = 2.5f,
                        fontSizeScale = 0.78f,
                        showFooter = false,
                        onLineClick = { onClick() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            false && syncedLines.isNotEmpty() -> {
                val currentLineIndex = remember(effectivePosition, syncedLines) {
                    syncedLines.indexOfLast { it.time <= effectivePosition }
                }

                val activeLineState = remember(
                    currentLineIndex,
                    syncedLines,
                    romanizedList,
                    liveTranslations,
                    romanizationAsMain,
                    autoRomanize,
                    autoTranslate
                ) {
                    val resolvedIdx = if (currentLineIndex >= 0) currentLineIndex else 0
                    val line = syncedLines.getOrNull(resolvedIdx)
                    val nextLine = syncedLines.getOrNull(resolvedIdx + 1)
                    val nextTimeMs = when {
                        nextLine != null -> nextLine.time.toLong()
                        line != null -> line.time.toLong() + 6500L
                        else -> 6500L
                    }
                    val rom = if (autoRomanize) {
                        romanizedList.getOrNull(resolvedIdx)?.takeIf { it.isNotBlank() && it != line?.line }
                    } else null
                    val swap = romanizationAsMain && rom != null && line != null
                    val effectiveSyncedLine = if (swap) {
                        line.copy(line = rom, words = null)
                    } else {
                        line
                    }
                    val subRom = if (swap) sanitizeLyricLineText(line.line) else rom?.let(::sanitizeLyricLineText)
                    val trans = if (autoTranslate) {
                        liveTranslations?.getOrNull(resolvedIdx)
                            ?.takeIf { it.isNotBlank() && !it.equals(line?.line, ignoreCase = true) }
                            ?.let(::sanitizeLyricLineText)
                    } else null
                    val nextRom = if (autoRomanize && romanizationAsMain) {
                        romanizedList.getOrNull(resolvedIdx + 1)?.takeIf { it.isNotBlank() }
                    } else null
                    KaraokeLineState(
                        lineIndex = resolvedIdx,
                        lineTimeMs = line?.time ?: -1,
                        syncedLine = effectiveSyncedLine,
                        nextLineTimeMs = nextTimeMs,
                        nextLineText = (nextRom ?: nextLine?.line)?.let(::sanitizeLyricLineText).orEmpty(),
                        romanizedSubLine = subRom,
                        translatedSubLine = trans
                    )
                }

                // iOS Dynamic Island / Music Recognition Card appear animation on every line change!
                // SizeTransform(clip = false) ensures glow and scaling words are NEVER clipped in a box.
                AnimatedContent(
                    targetState = activeLineState,
                    contentKey = { it.lineTimeMs to it.lineIndex },
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 360)) +
                            scaleIn(
                                initialScale = 0.44f,
                                transformOrigin = TransformOrigin(0.12f, 0f),
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f)
                            ) +
                            slideInVertically(
                                initialOffsetY = { -it / 2 - 32 },
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f)
                            ))
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 220)) +
                                    scaleOut(
                                        targetScale = 0.76f,
                                        transformOrigin = TransformOrigin(0.12f, 1f),
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 120f)
                                    ) +
                                    slideOutVertically(
                                        targetOffsetY = { it / 2 + 16 },
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 120f)
                                    )
                            )
                            .using(SizeTransform(clip = false))
                    },
                    label = "complexDynamicIslandLineTransition",
                    modifier = Modifier.fillMaxWidth()
                ) { lineState ->
                    val lineWords = remember(lineState.syncedLine, lineState.nextLineTimeMs) {
                        resolveSmartVocalWords(lineState.syncedLine, lineState.nextLineTimeMs)
                    }
                    val isRtlLine = remember(lineState.syncedLine) {
                        isRtlLyricText(lineState.syncedLine?.line.orEmpty())
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                    ) {
                        if (lineWords.isEmpty()) {
                            Text(
                                text = "•••",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = accentColor.copy(alpha = 0.80f)
                            )
                        } else {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isRtlLine) LayoutDirection.Rtl else LayoutDirection.Ltr
                            ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(11.dp, Alignment.Start),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                lineWords.forEachIndexed { index, item ->
                                    val hasAppeared = effectivePosition >= item.startTimeMs
                                    val isCurrent = effectivePosition in item.startTimeMs until item.endTimeMs

                                    key("${lineState.lineTimeMs}_${item.startTimeMs}_$index") {
                                        val appearScale by animateFloatAsState(
                                            targetValue = when {
                                                !hasAppeared -> 0.42f
                                                isCurrent -> 1.14f
                                                else -> 1.0f
                                            },
                                            animationSpec = spring(
                                                dampingRatio = if (isCurrent) 0.62f else 0.78f,
                                                stiffness = if (hasAppeared) 150f else 220f
                                            ),
                                            label = "wordAppearScale"
                                        )
                                        val appearAlpha by animateFloatAsState(
                                            targetValue = when {
                                                !hasAppeared -> 0f
                                                isCurrent -> 1f
                                                else -> 0.82f
                                            },
                                            animationSpec = tween(durationMillis = if (hasAppeared) 170 else 90),
                                            label = "wordAppearAlpha"
                                        )
                                        val wordOffsetY by animateFloatAsState(
                                            targetValue = when {
                                                !hasAppeared -> 18f
                                                isCurrent -> -3f
                                                else -> 0f
                                            },
                                            animationSpec = spring(
                                                dampingRatio = 0.74f,
                                                stiffness = 140f
                                            ),
                                            label = "wordOffsetY"
                                        )
                                        val glowStrength by animateFloatAsState(
                                            targetValue = if (isCurrent) 1f else 0f,
                                            animationSpec = tween(durationMillis = 160),
                                            label = "wordGlowStrength"
                                        )
                                        val baseWordColor by animateColorAsState(
                                            targetValue = if (isCurrent) accentColor else textColor,
                                            animationSpec = tween(durationMillis = 150),
                                            label = "wordColor"
                                        )

                                        val effectiveWordColor = baseWordColor.copy(
                                            alpha = (baseWordColor.alpha * appearAlpha).coerceIn(0f, 1f)
                                        )
                                        val effectiveShadowColor = accentColor.copy(
                                            alpha = (0.55f * glowStrength * appearAlpha).coerceIn(0f, 1f)
                                        )

                                        // Direct Text rendering with NO Modifier.blur and NO graphicsLayer alpha < 1f
                                        // so Android's hardware RenderNode never allocates a rectangular clipping box!
                                        Text(
                                            text = item.word,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 22.sp,
                                                shadow = if (glowStrength > 0.02f) {
                                                    Shadow(
                                                        color = effectiveShadowColor,
                                                        offset = Offset.Zero,
                                                        blurRadius = 14f
                                                    )
                                                } else {
                                                    null
                                                }
                                            ),
                                            color = effectiveWordColor,
                                            modifier = Modifier.graphicsLayer {
                                                clip = false
                                                scaleX = appearScale
                                                scaleY = appearScale
                                                translationY = wordOffsetY
                                            }
                                        )
                                    }
                                }
                            }
                            }
                        }

                        // Romanisation & Translation sub-lines for active karaoke line
                        if (!lineState.romanizedSubLine.isNullOrBlank()) {
                            val isRomRtl = remember(lineState.romanizedSubLine) { isRtlLyricText(lineState.romanizedSubLine) }
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isRomRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                            ) {
                                Text(
                                    text = lineState.romanizedSubLine,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = textColor.copy(alpha = 0.72f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (!lineState.translatedSubLine.isNullOrBlank()) {
                            val isTransRtl = remember(lineState.translatedSubLine) { isRtlLyricText(lineState.translatedSubLine) }
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isTransRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                            ) {
                                Text(
                                    text = lineState.translatedSubLine,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = accentColor.copy(alpha = 0.84f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Upcoming line preview for frosted depth
                        if (lineState.nextLineText.isNotBlank()) {
                            val isNextRtl = remember(lineState.nextLineText) { isRtlLyricText(lineState.nextLineText) }
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isNextRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                            ) {
                                Text(
                                    text = lineState.nextLineText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = textColor.copy(alpha = 0.36f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            plainLines.isNotEmpty() -> {
                val displayPlainLines = remember(plainLines, romanizedList, liveTranslations, autoRomanize, autoTranslate, romanizationAsMain) {
                    plainLines.take(4).mapIndexed { i, original ->
                        val rom = if (autoRomanize) romanizedList.getOrNull(i)?.takeIf { it.isNotBlank() && it != original } else null
                        val trans = if (autoTranslate) liveTranslations?.getOrNull(i)?.takeIf { it.isNotBlank() && !it.equals(original, ignoreCase = true) } else null
                        buildString {
                            if (romanizationAsMain && rom != null) {
                                append(rom)
                            } else {
                                append(original)
                                if (rom != null) append("\n").append(rom)
                            }
                            if (trans != null) append("\n").append(trans)
                        }
                    }
                }
                val isPlainRtl = remember(displayPlainLines) { isRtlLyricText(displayPlainLines.firstOrNull().orEmpty()) }
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isPlainRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Text(
                        text = displayPlainLines.joinToString("\n"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 25.sp
                        ),
                        color = textColor.copy(alpha = 0.82f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            else -> {
                ComplexExpressiveVisualizer(
                    isPlayingProvider = isPlayingProvider,
                    primaryColor = accentColor,
                    secondaryColor = LocalMaterialTheme.current.secondaryContainer,
                    tertiaryColor = LocalMaterialTheme.current.tertiary,
                    onTertiaryColor = LocalMaterialTheme.current.onTertiary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Solid-filled Material 3 Expressive vertical control stack on the right:
 * 3-Dot Menu -> Like -> Previous -> Play/Pause -> Next.
 * The bottom 3 buttons (Previous, Play/Pause, Next) morph in-place via GPU graphicsLayer
 * from their 36.dp horizontal MiniPlayer positions at the top-right of the sheet.
 */
@Composable
private fun VerticalExpressiveControlStack(
    containerWidthPx: Float,
    morphTracker: ArtworkMorphTracker,
    isPlayingProvider: () -> Boolean,
    isFavoriteProvider: () -> Boolean,
    expansionFractionProvider: () -> Float,
    rootCoordsHolder: LayoutCoordsHolder,
    onMoreOptionsClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    playPauseContainerColor: Color,
    onPlayPauseContainerColor: Color,
    skipContainerColor: Color,
    onSkipContainerColor: Color,
    likedContainerColor: Color,
    onLikedContainerColor: Color,
    neutralContainerColor: Color,
    onNeutralContainerColor: Color,
    aodSourceBounds: AodSourceBounds? = null,
    aodTransitionProgressProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isPlaying = isPlayingProvider()
    val isFavorite = isFavoriteProvider()
    val haptics = LocalHapticFeedback.current

    val miniBtnSizePx = remember(density) { with(density) { 36.dp.toPx() } }
    val miniBtnTopPx = remember(density) { with(density) { 14.dp.toPx() } }
    val miniEndPadPx = remember(density) { with(density) { 12.dp.toPx() } }
    val miniGapPx = remember(density) { with(density) { 8.dp.toPx() } }
    val miniIconSizePx = remember(density) { with(density) { 22.dp.toPx() } }

    val prevBounds = aodSourceBounds?.prevBounds ?: remember { MorphElementBounds() }
    val playBounds = aodSourceBounds?.playBounds ?: remember { MorphElementBounds() }
    val nextBounds = aodSourceBounds?.nextBounds ?: remember { MorphElementBounds() }

    var activeButton by remember { mutableStateOf<VerticalControlButton?>(null) }
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    val latestIsPlayingProvider by rememberUpdatedState(isPlayingProvider)

    LaunchedEffect(activeButton) {
        if (activeButton != null) {
            delay(230L)
            activeButton = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playPauseVisualState = true
        } else {
            if (activeButton == VerticalControlButton.NEXT || activeButton == VerticalControlButton.PREVIOUS) {
                delay(220L)
            }
            if (!latestIsPlayingProvider()) {
                playPauseVisualState = false
            }
        }
    }

    fun weightFor(button: VerticalControlButton, baseWeight: Float): Float {
        return when (activeButton) {
            button -> baseWeight * 1.22f
            null -> baseWeight
            else -> baseWeight * 0.88f
        }
    }

    val weightSpec = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMediumLow
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 3-DOT MORE OPTIONS BUTTON (Over the Like button)
        val moreWeight by animateFloatAsState(
            targetValue = weightFor(VerticalControlButton.MORE, 0.74f),
            animationSpec = weightSpec,
            label = "moreVerticalWeight"
        )
        ExpressiveMorphingStackButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(moreWeight)
                .graphicsLayer {
                    val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                },
            defaultCornerDp = 34.dp,
            pressedCornerDp = 14.dp,
            isActive = activeButton == VerticalControlButton.MORE,
            containerColor = neutralContainerColor,
            onClick = {
                activeButton = VerticalControlButton.MORE
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMoreOptionsClick()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_more_horiz_24),
                contentDescription = stringResource(R.string.cd_player_more_options),
                tint = onNeutralContainerColor,
                modifier = Modifier.size(26.dp)
            )
        }

        // 2. LIKE BUTTON
        val likeWeight by animateFloatAsState(
            targetValue = weightFor(VerticalControlButton.LIKE, 0.88f),
            animationSpec = weightSpec,
            label = "likeVerticalWeight"
        )
        val likeContainerColor by animateColorAsState(
            targetValue = if (isFavorite) likedContainerColor else neutralContainerColor,
            animationSpec = tween(220),
            label = "likeContainerColor"
        )
        val likeIconColor by animateColorAsState(
            targetValue = if (isFavorite) onLikedContainerColor else onNeutralContainerColor,
            animationSpec = tween(220),
            label = "likeIconColor"
        )

        ExpressiveMorphingStackButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(likeWeight)
                .graphicsLayer {
                    val aodFade = (1f - aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(expansionFractionProvider()).nonArtAlpha * aodFade
                },
            defaultCornerDp = if (isFavorite) 20.dp else 34.dp,
            pressedCornerDp = 14.dp,
            isActive = activeButton == VerticalControlButton.LIKE,
            containerColor = likeContainerColor,
            onClick = {
                activeButton = VerticalControlButton.LIKE
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFavoriteToggle()
            }
        ) {
            val heartScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.14f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heartPopScale"
            )
            Icon(
                painter = painterResource(R.drawable.round_favorite_24),
                contentDescription = "Favorite",
                tint = likeIconColor,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
            )
        }

        // 3. PREVIOUS BUTTON (Morphs in-place from MiniPlayer Previous button)
        val prevWeight by animateFloatAsState(
            targetValue = weightFor(VerticalControlButton.PREVIOUS, 0.96f),
            animationSpec = weightSpec,
            label = "prevVerticalWeight"
        )
        ExpressiveMorphingStackButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(prevWeight)
                .captureUntransformedBounds(rootCoordsHolder, prevBounds)
                .graphicsLayer {
                    val s = morphTracker.evaluate(expansionFractionProvider())
                    val cw = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: containerWidthPx
                    val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                    val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                    val miniPrevLeftPx = (miniPlayLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                    val destX = prevBounds.offset.x
                    val destY = prevBounds.offset.y
                    val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, size.width, s.sizeT)
                    val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = if (size.width > 0f) curW / size.width else 1f
                    scaleY = if (size.height > 0f) curH / size.height else 1f
                    translationX = (miniPrevLeftPx - destX) * (1f - s.posT)
                    translationY = (miniBtnTopPx - destY) * (1f - s.posT)
                    alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                },
            defaultCornerDp = 34.dp,
            pressedCornerDp = 16.dp,
            miniCornerDp = 18.dp,
            miniBtnSizePx = miniBtnSizePx,
            morphTracker = morphTracker,
            expansionFractionProvider = expansionFractionProvider,
            isActive = activeButton == VerticalControlButton.PREVIOUS,
            containerColor = skipContainerColor,
            onClick = {
                activeButton = VerticalControlButton.PREVIOUS
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_skip_previous_filled_24),
                contentDescription = "Previous",
                tint = onSkipContainerColor,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        val bw = prevBounds.size.width.toFloat()
                        val bh = prevBounds.size.height.toFloat()
                        if (bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, bw, s.sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = 32.dp.toPx().coerceAtLeast(1f)
                            val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    }
            )
        }

        // 4. PLAY / PAUSE BUTTON (Morphs in-place from MiniPlayer Play/Pause button)
        val playWeight by animateFloatAsState(
            targetValue = weightFor(VerticalControlButton.PLAY_PAUSE, 1.32f),
            animationSpec = weightSpec,
            label = "playVerticalWeight"
        )
        ExpressiveMorphingStackButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(playWeight)
                .captureUntransformedBounds(rootCoordsHolder, playBounds)
                .graphicsLayer {
                    val s = morphTracker.evaluate(expansionFractionProvider())
                    val cw = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: containerWidthPx
                    val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                    val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
                    val destX = playBounds.offset.x
                    val destY = playBounds.offset.y
                    val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, size.width, s.sizeT)
                    val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = if (size.width > 0f) curW / size.width else 1f
                    scaleY = if (size.height > 0f) curH / size.height else 1f
                    translationX = (miniPlayLeftPx - destX) * (1f - s.posT)
                    translationY = (miniBtnTopPx - destY) * (1f - s.posT)
                    alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
                .expressiveTapColorWave(
                    waveColor = onPlayPauseContainerColor.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(if (playPauseVisualState) 20.dp else 34.dp)
                ),
            defaultCornerDp = if (playPauseVisualState) 20.dp else 34.dp,
            pressedCornerDp = 14.dp,
            miniCornerDp = if (playPauseVisualState) 11.dp else 18.dp,
            miniBtnSizePx = miniBtnSizePx,
            morphTracker = morphTracker,
            expansionFractionProvider = expansionFractionProvider,
            isActive = activeButton == VerticalControlButton.PLAY_PAUSE,
            containerColor = playPauseContainerColor,
            onClick = {
                activeButton = VerticalControlButton.PLAY_PAUSE
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPlayPause()
            }
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val bw = playBounds.size.width.toFloat()
                    val bh = playBounds.size.height.toFloat()
                    if (bw > 0f && bh > 0f) {
                        val s = morphTracker.evaluate(expansionFractionProvider())
                        val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, bw, s.sizeT) / bw
                        val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                        val fullIconPx = 36.dp.toPx().coerceAtLeast(1f)
                        val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                        scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                        scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                    }
                }
            ) {
                Crossfade(
                    targetState = playPauseVisualState,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "complexPlayPauseIcon"
                ) { playing ->
                    Icon(
                        painter = painterResource(
                            if (playing) R.drawable.rounded_pause_filled_24 else R.drawable.rounded_play_arrow_filled_24
                        ),
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = onPlayPauseContainerColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 5. NEXT BUTTON (Morphs in-place from MiniPlayer Next button)
        val nextWeight by animateFloatAsState(
            targetValue = weightFor(VerticalControlButton.NEXT, 0.96f),
            animationSpec = weightSpec,
            label = "nextVerticalWeight"
        )
        ExpressiveMorphingStackButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(nextWeight)
                .captureUntransformedBounds(rootCoordsHolder, nextBounds)
                .graphicsLayer {
                    val s = morphTracker.evaluate(expansionFractionProvider())
                    val cw = rootCoordsHolder.widthPx.takeIf { it > 0f } ?: containerWidthPx
                    val miniNextLeftPx = (cw - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
                    val destX = nextBounds.offset.x
                    val destY = nextBounds.offset.y
                    val curW = androidx.compose.ui.util.lerp(miniBtnSizePx, size.width, s.sizeT)
                    val curH = androidx.compose.ui.util.lerp(miniBtnSizePx, size.height, s.sizeT)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = if (size.width > 0f) curW / size.width else 1f
                    scaleY = if (size.height > 0f) curH / size.height else 1f
                    translationX = (miniNextLeftPx - destX) * (1f - s.posT)
                    translationY = (miniBtnTopPx - destY) * (1f - s.posT)
                    alpha = if (aodTransitionProgressProvider() > 0.001f) 0f else 1f
                },
            defaultCornerDp = 34.dp,
            pressedCornerDp = 16.dp,
            miniCornerDp = 18.dp,
            miniBtnSizePx = miniBtnSizePx,
            morphTracker = morphTracker,
            expansionFractionProvider = expansionFractionProvider,
            isActive = activeButton == VerticalControlButton.NEXT,
            containerColor = skipContainerColor,
            onClick = {
                activeButton = VerticalControlButton.NEXT
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_skip_next_filled_24),
                contentDescription = "Next",
                tint = onSkipContainerColor,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        val bw = nextBounds.size.width.toFloat()
                        val bh = nextBounds.size.height.toFloat()
                        if (bw > 0f && bh > 0f) {
                            val s = morphTracker.evaluate(expansionFractionProvider())
                            val parentScaleX = androidx.compose.ui.util.lerp(miniBtnSizePx, bw, s.sizeT) / bw
                            val parentScaleY = androidx.compose.ui.util.lerp(miniBtnSizePx, bh, s.sizeT) / bh
                            val fullIconPx = 32.dp.toPx().coerceAtLeast(1f)
                            val desiredScale = androidx.compose.ui.util.lerp(miniIconSizePx / fullIconPx, 1f, s.sizeT)
                            scaleX = if (parentScaleX > 0.001f) desiredScale / parentScaleX else 1f
                            scaleY = if (parentScaleY > 0.001f) desiredScale / parentScaleY else 1f
                        }
                    }
            )
        }
    }
}

/**
 * Bold Material You Expressive Audio Visualizer shown in the Complex lyrics stage when no lyrics are available.
 * Combines 7 chunky rounded capsule equalizer bars with a rotating Material 3 Expressive scalloped badge.
 */
@Composable
private fun ComplexExpressiveVisualizer(
    isPlayingProvider: () -> Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onTertiaryColor: Color,
    modifier: Modifier = Modifier
) {
    val isPlaying = isPlayingProvider()
    var phaseRad by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dtSec = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                    val speed = if (isPlaying) 1.35f else 0.22f
                    phaseRad = (phaseRad + dtSec * speed) % (Math.PI.toFloat() * 80f)
                }
                lastNanos = nanos
            }
        }
    }

    val activeEnergy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.24f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessLow
        ),
        label = "visualizerEnergy"
    )

    val barPalette = remember(primaryColor, secondaryColor, tertiaryColor) {
        listOf(
            secondaryColor,
            tertiaryColor,
            primaryColor,
            tertiaryColor,
            primaryColor,
            tertiaryColor,
            secondaryColor
        )
    }

    Column(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Rotating Material 3 Expressive 12-lobed Scalloped Badge + Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = phaseRad * 6.5f
                        }
                ) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val baseR = size.minDimension * 0.455f
                    val lobeDepth = size.minDimension * 0.028f
                    val path = Path()
                    val steps = 96
                    for (i in 0..steps) {
                        val theta = (i.toFloat() / steps) * (Math.PI.toFloat() * 2f)
                        val r = baseR + lobeDepth * cos(theta * 8f)
                        val x = cx + r * cos(theta)
                        val y = cy + r * sin(theta)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path = path, color = tertiaryColor)
                }
                Icon(
                    painter = painterResource(R.drawable.rounded_graphic_eq_24),
                    contentDescription = null,
                    tint = onTertiaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = if (isPlaying) "Now Vibing" else "Paused",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = primaryColor
                )
                Text(
                    text = "Tap to search lyrics",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = primaryColor.copy(alpha = 0.68f)
                )
            }
        }

        // 7-Bar Bold Material You Expressive Pill Equalizer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val freqMultipliers = remember { floatArrayOf(0.72f, 0.94f, 1.15f, 1.0f, 1.22f, 1.02f, 0.78f) }
            val phaseOffsets = remember { floatArrayOf(0.0f, 0.9f, 1.8f, 2.7f, 3.5f, 4.4f, 5.2f) }
            val maxWeights = remember { floatArrayOf(0.62f, 0.85f, 1.0f, 0.78f, 0.96f, 0.80f, 0.58f) }
            val barShape = remember { AbsoluteSmoothCornerShape(10.dp, 60) }

            for (i in 0 until 7) {
                val w1 = sin(phaseRad * freqMultipliers[i] + phaseOffsets[i])
                val w2 = cos(phaseRad * (freqMultipliers[i] * 0.65f) - phaseOffsets[i] * 1.15f)
                val normalizedWave = ((w1 * 0.65f + w2 * 0.35f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val heightFraction = (0.22f + 0.78f * normalizedWave * maxWeights[i] * activeEnergy)
                    .coerceIn(0.22f, 1f)
                val barHeightDp = (102f * heightFraction).dp

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(barHeightDp)
                        .clip(barShape)
                        .background(barPalette[i], barShape)
                )
            }
        }
    }
}

/**
 * Shared overlay that morphs the 3 playback transport buttons (Previous, Play/Pause, Next)
 * continuously between their 36.dp horizontal MiniPlayer positions at the top-right of the sheet
 * and their full Now Playing screen target bounds across all 4 design styles.
 */
@Composable
internal fun MorphingTransportButtonsOverlay(
    containerWidthPx: Float,
    expansionFractionProvider: () -> Float,
    morphTracker: ArtworkMorphTracker,
    isPlayingProvider: () -> Boolean,
    prevTargetRect: Rect,
    playTargetRect: Rect,
    nextTargetRect: Rect,
    prevTargetCornerDp: Dp,
    playTargetCornerDp: Dp,
    nextTargetCornerDp: Dp,
    miniSkipContainerColor: Color,
    miniSkipIconColor: Color,
    miniPlayContainerColor: Color,
    miniPlayIconColor: Color,
    targetSkipContainerColor: Color,
    targetSkipIconColor: Color,
    targetPlayContainerColor: Color,
    targetPlayIconColor: Color,
    targetSkipIconSize: Dp = 32.dp,
    targetPlayIconSize: Dp = 36.dp
) {
    val density = LocalDensity.current
    val isPlaying = isPlayingProvider()

    val miniBtnSizePx = with(density) { 36.dp.toPx() }
    val miniBtnTopPx = with(density) { 14.dp.toPx() }
    val miniEndPadPx = with(density) { 12.dp.toPx() }
    val miniGapPx = with(density) { 8.dp.toPx() }

    val miniNextLeftPx = (containerWidthPx - miniEndPadPx - miniBtnSizePx).coerceAtLeast(0f)
    val miniPlayLeftPx = (miniNextLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)
    val miniPrevLeftPx = (miniPlayLeftPx - miniGapPx - miniBtnSizePx).coerceAtLeast(0f)

    val miniPrevRect = Rect(miniPrevLeftPx, miniBtnTopPx, miniPrevLeftPx + miniBtnSizePx, miniBtnTopPx + miniBtnSizePx)
    val miniPlayRect = Rect(miniPlayLeftPx, miniBtnTopPx, miniPlayLeftPx + miniBtnSizePx, miniBtnTopPx + miniBtnSizePx)
    val miniNextRect = Rect(miniNextLeftPx, miniBtnTopPx, miniNextLeftPx + miniBtnSizePx, miniBtnTopPx + miniBtnSizePx)

    MorphingSingleTransportButton(
        expansionFractionProvider = expansionFractionProvider,
        morphTracker = morphTracker,
        startRect = miniPrevRect,
        endRect = prevTargetRect,
        startCornerDp = 18.dp,
        endCornerDp = prevTargetCornerDp,
        startContainerColor = miniSkipContainerColor,
        endContainerColor = targetSkipContainerColor,
        startIconColor = miniSkipIconColor,
        endIconColor = targetSkipIconColor,
        startIconSize = 22.dp,
        endIconSize = targetSkipIconSize,
        icon = ImageVector.vectorResource(R.drawable.rounded_skip_previous_filled_24)
    )

    MorphingSingleTransportButton(
        expansionFractionProvider = expansionFractionProvider,
        morphTracker = morphTracker,
        startRect = miniPlayRect,
        endRect = playTargetRect,
        startCornerDp = if (isPlaying) 11.dp else 18.dp,
        endCornerDp = playTargetCornerDp,
        startContainerColor = miniPlayContainerColor,
        endContainerColor = targetPlayContainerColor,
        startIconColor = miniPlayIconColor,
        endIconColor = targetPlayIconColor,
        startIconSize = 22.dp,
        endIconSize = targetPlayIconSize,
        icon = ImageVector.vectorResource(if (isPlaying) R.drawable.rounded_pause_filled_24 else R.drawable.rounded_play_arrow_filled_24)
    )

    MorphingSingleTransportButton(
        expansionFractionProvider = expansionFractionProvider,
        morphTracker = morphTracker,
        startRect = miniNextRect,
        endRect = nextTargetRect,
        startCornerDp = 18.dp,
        endCornerDp = nextTargetCornerDp,
        startContainerColor = miniSkipContainerColor,
        endContainerColor = targetSkipContainerColor,
        startIconColor = miniSkipIconColor,
        endIconColor = targetSkipIconColor,
        startIconSize = 22.dp,
        endIconSize = targetSkipIconSize,
        icon = ImageVector.vectorResource(R.drawable.rounded_skip_next_filled_24)
    )
}

@Composable
private fun MorphingSingleTransportButton(
    expansionFractionProvider: () -> Float,
    morphTracker: ArtworkMorphTracker,
    startRect: Rect,
    endRect: Rect,
    startCornerDp: Dp,
    endCornerDp: Dp,
    startContainerColor: Color,
    endContainerColor: Color,
    startIconColor: Color,
    endIconColor: Color,
    startIconSize: Dp,
    endIconSize: Dp,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val density = LocalDensity.current
    val startCornerPx = with(density) { startCornerDp.toPx() }
    val endCornerPx = with(density) { endCornerDp.toPx() }

    val dynamicShape = remember(morphTracker, expansionFractionProvider, startCornerPx, endCornerPx) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val t = morphTracker.evaluate(expansionFractionProvider()).sizeT
                val maxR = minOf(size.width, size.height) * 0.5f
                val r = androidx.compose.ui.util.lerp(startCornerPx, endCornerPx, t).coerceIn(0f, maxR)
                return Outline.Rounded(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                    )
                )
            }
        }
    }

    val rawF = expansionFractionProvider().coerceIn(0f, 1f)
    val sample = morphTracker.evaluate(rawF)
    val containerColor = lerp(startContainerColor, endContainerColor, sample.sizeT)
    val iconColor = lerp(startIconColor, endIconColor, sample.sizeT)
    val iconSizeDp = androidx.compose.ui.unit.lerp(startIconSize, endIconSize, sample.sizeT)

    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val f = expansionFractionProvider()
                if (f <= 0.004f || f >= 0.985f) {
                    val p = measurable.measure(Constraints.fixed(0, 0))
                    return@layout layout(0, 0) { p.place(0, 0) }
                }
                val s = morphTracker.evaluate(f)
                val baseW = androidx.compose.ui.util.lerp(startRect.width, endRect.width, s.sizeT)
                val baseH = androidx.compose.ui.util.lerp(startRect.height, endRect.height, s.sizeT)
                val targetW = (baseW * s.overshootScale).roundToInt().coerceAtLeast(1)
                val targetH = (baseH * s.overshootScale).roundToInt().coerceAtLeast(1)
                val baseX = androidx.compose.ui.util.lerp(startRect.left, endRect.left, s.posT)
                val baseY = androidx.compose.ui.util.lerp(startRect.top, endRect.top, s.posT)
                val placeX = (baseX - (targetW - baseW) * 0.5f).roundToInt()
                val placeY = (baseY - (targetH - baseH) * 0.5f).roundToInt()

                val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                layout(targetW, targetH) {
                    placeable.place(placeX, placeY)
                }
            }
            .clip(dynamicShape)
            .background(containerColor, dynamicShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSizeDp)
        )
    }
}

/**
 * Solid-filled Material You Expressive button with smooth corner morphing and spring press scale.
 * No outlines/borders or transparent fills.
 */
@Composable
private fun ExpressiveMorphingStackButton(
    defaultCornerDp: Dp,
    pressedCornerDp: Dp,
    isActive: Boolean,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniCornerDp: Dp? = null,
    miniBtnSizePx: Float = 0f,
    morphTracker: ArtworkMorphTracker? = null,
    expansionFractionProvider: (() -> Float)? = null,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val latestOnClick by rememberUpdatedState(onClick)

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed || isActive) pressedCornerDp else defaultCornerDp,
        animationSpec = spring(
            dampingRatio = 0.64f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "stackButtonCorner"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isActive) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "stackButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                val fullCornerPx = cornerRadius.toPx()
                val rx: Float
                val ry: Float
                if (miniCornerDp != null && morphTracker != null && expansionFractionProvider != null && miniBtnSizePx > 0f) {
                    val t = morphTracker.evaluate(expansionFractionProvider()).sizeT
                    val miniCornerPx = miniCornerDp.toPx()
                    val targetVisualR = androidx.compose.ui.util.lerp(miniCornerPx, fullCornerPx, t)
                    val sx = (androidx.compose.ui.util.lerp(miniBtnSizePx, w, t) / w).coerceAtLeast(0.01f)
                    val sy = (androidx.compose.ui.util.lerp(miniBtnSizePx, h, t) / h).coerceAtLeast(0.01f)
                    rx = (targetVisualR / sx).coerceIn(0f, w * 0.5f)
                    ry = (targetVisualR / sy).coerceIn(0f, h * 0.5f)
                } else {
                    val r = fullCornerPx.coerceIn(0f, minOf(w, h) * 0.5f)
                    rx = r
                    ry = r
                }
                drawRoundRect(
                    color = containerColor,
                    cornerRadius = CornerRadius(rx, ry)
                )
            }
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
        content()
    }
}
