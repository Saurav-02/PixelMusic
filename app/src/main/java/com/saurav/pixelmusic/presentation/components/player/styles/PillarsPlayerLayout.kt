package com.saurav.pixelmusic.presentation.components.player.styles

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.player.AodSourceArtStyle
import com.saurav.pixelmusic.presentation.components.player.ArtworkMorphTracker
import com.saurav.pixelmusic.presentation.components.player.LayoutCoordsHolder
import com.saurav.pixelmusic.presentation.components.player.MorphElementBounds
import com.saurav.pixelmusic.presentation.components.player.captureRootCoords
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Slices a single continuous album artwork through 3 staggered vertical rounded capsules,
 * morphing smoothly from the MiniPlayer 44dp circle into the 3-pillar composition and
 * reacting dynamically with staggered spring motion on Play/Pause.
 */
private class MorphingTriplePillarShape(
    private val morphProgressProvider: () -> Float,
    private val pillar1PlayProvider: () -> Float,
    private val pillar2PlayProvider: () -> Float,
    private val pillar3PlayProvider: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = morphProgressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        if (t <= 0.06f) {
            val r = minOf(w, h) * 0.5f
            return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(r, r)))
        }

        val splitT = ((t - 0.06f) / 0.94f).coerceIn(0f, 1f)
        val p1 = pillar1PlayProvider()
        val p2 = pillar2PlayProvider()
        val p3 = pillar3PlayProvider()
        val path = Path()

        fun addInterpolatedPill(
            targetLeft: Float,
            pausedTop: Float,
            playingTop: Float,
            targetRight: Float,
            pausedBottom: Float,
            playingBottom: Float,
            playT: Float
        ) {
            val dynamicTop = lerp(pausedTop, playingTop, playT).coerceIn(0.01f, 0.95f)
            val dynamicBottom = lerp(pausedBottom, playingBottom, playT).coerceIn(dynamicTop + 0.12f, 0.995f)
            val l = lerp(0f, w * targetLeft, splitT)
            val top = lerp(0f, h * dynamicTop, splitT)
            val r = lerp(w, w * targetRight, splitT)
            val b = lerp(h, h * dynamicBottom, splitT)
            val radius = ((r - l) * 0.5f).coerceAtLeast(1f)
            path.addRoundRect(RoundRect(l, top, r, b, CornerRadius(radius, radius)))
        }

        // Left pillar: settles calm & centered when paused, drops into energetic low stance when playing
        addInterpolatedPill(
            targetLeft = 0.11f,
            pausedTop = 0.34f,
            playingTop = 0.42f,
            targetRight = 0.36f,
            pausedBottom = 0.84f,
            playingBottom = 0.95f,
            playT = p1
        )
        // Center pillar: compacts gently when paused, stretches tall & proud when playing
        addInterpolatedPill(
            targetLeft = 0.39f,
            pausedTop = 0.24f,
            playingTop = 0.13f,
            targetRight = 0.64f,
            pausedBottom = 0.90f,
            playingBottom = 0.99f,
            playT = p2
        )
        // Right pillar: lowers slightly when paused, lifts high into the upper corner when playing
        addInterpolatedPill(
            targetLeft = 0.67f,
            pausedTop = 0.16f,
            playingTop = 0.03f,
            targetRight = 0.92f,
            pausedBottom = 0.80f,
            playingBottom = 0.87f,
            playT = p3
        )

        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Pillars
 * Displays continuous album artwork through three staggered vertical capsule windows
 * accented by unclipped Material You decorative circles and a subtle live audio waveform,
 * paired with exact old-style Play/Pause morphing, neighbor-pushing controls, and the
 * exact BottomToggleRow bottom padding + separated circular menu button.
 */
@Composable
fun PillarsPlayerLayout(
    params: PlayerStyleParams,
    modifier: Modifier = Modifier
) {
    val baseTheme = LocalMaterialTheme.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val (optimizedArtUri, resolvedTitle, resolvedArtist) = rememberResolvedArtAndMeta(
        song = params.song,
        currentSongArtists = params.currentSongArtists,
        albumArtQuality = params.albumArtQuality
    )

    val solidBackgroundColor = baseTheme.primaryContainer
    val contentColor = baseTheme.onPrimaryContainer
    val accentColor = baseTheme.primary
    val onAccentColor = baseTheme.onPrimary
    val tertiaryAccent = baseTheme.tertiary
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val artTopOffsetDp = statusBarTopPadding + 58.dp
    val artTopOffsetPx = with(density) { artTopOffsetDp.toPx() }
    val artHeightDp = (screenHeightDp * 0.40f).coerceIn(240.dp, 345.dp)
    val artHeightPx = with(density) { artHeightDp.toPx() }
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }
    val slideOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val artBounds = params.aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }
    val morphTracker = remember { ArtworkMorphTracker() }

    params.aodSourceBounds?.let { bounds ->
        bounds.artStyle = AodSourceArtStyle.STANDARD_ROUNDED
        bounds.washColor = solidBackgroundColor
        bounds.topShadowHeightDp = 0f
        bounds.topShadowStartAlpha = 0f
        bounds.topShadowMidAlpha = 0f
    }

    val isPlaying = params.isPlayingProvider()
    val isFavorite = params.isFavoriteProvider()
    val isShuffle = params.isShuffleEnabledProvider()
    val repeatMode = params.repeatModeProvider()

    // Staggered spring reaction on Play/Pause for the 3 artwork pillars (zero infinite cost when settled)
    val pillar1Play by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 210f),
        label = "pillar1Play"
    )
    val pillar2Play by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 170f),
        label = "pillar2Play"
    )
    val pillar3Play by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 140f),
        label = "pillar3Play"
    )

    val pillarShape = remember(morphTracker, params.expansionFractionProvider) {
        MorphingTriplePillarShape(
            morphProgressProvider = {
                morphTracker.evaluate(params.expansionFractionProvider()).sizeT
            },
            pillar1PlayProvider = { pillar1Play },
            pillar2PlayProvider = { pillar2Play },
            pillar3PlayProvider = { pillar3Play }
        )
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.035f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "pillarsArtScale"
    )

    val waveTransition = rememberInfiniteTransition(label = "pillarsWave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pillarsWavePhase"
    )
    val liveWaveFactor by animateFloatAsState(
        targetValue = if (isPlaying && params.allowRealtimeUpdates) 1f else 0f,
        animationSpec = tween(320),
        label = "pillarsWaveLive"
    )

    val pillarsOrder = remember {
        listOf(MiniTransportSlot.PREVIOUS, MiniTransportSlot.PLAY_PAUSE, MiniTransportSlot.NEXT)
    }
    val pillarsWeights = remember {
        mapOf(
            MiniTransportSlot.PREVIOUS to 0.90f,
            MiniTransportSlot.PLAY_PAUSE to 1.32f,
            MiniTransportSlot.NEXT to 0.90f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Full-screen unclipped Canvas for decorative circles + live waveform
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                }
        ) {
            val w = if (isLandscape) size.width * 0.50f else size.width
            val topY = if (isLandscape) size.height * 0.08f else artTopOffsetPx
            val h = if (isLandscape) size.height * 0.84f else artHeightPx

            val circleScale = lerp(0.94f, 1.0f, pillar2Play)
            drawCircle(
                color = accentColor.copy(alpha = 0.09f),
                radius = w * 0.44f * circleScale,
                center = Offset(w * 0.84f, topY + h * lerp(0.26f, 0.22f, pillar3Play))
            )
            drawCircle(
                color = tertiaryAccent.copy(alpha = 0.38f),
                radius = w * 0.105f * lerp(0.92f, 1.0f, pillar1Play),
                center = Offset(w * 0.23f, topY + h * lerp(0.14f, 0.11f, pillar1Play))
            )
            drawCircle(
                color = tertiaryAccent.copy(alpha = 0.30f),
                radius = w * 0.185f * circleScale,
                center = Offset(w * 0.04f, topY + h * lerp(0.64f, 0.68f, pillar1Play))
            )

            val baseHeights = floatArrayOf(0.24f, 0.48f, 0.34f, 0.82f, 0.96f, 0.56f, 0.30f, 0.44f, 0.28f)
            val barCount = baseHeights.size
            val regionStart = w * 0.06f
            val regionWidth = w * 0.29f
            val step = regionWidth / (barCount - 1).coerceAtLeast(1)
            val centerY = topY + h * 0.31f
            val maxHalfH = h * 0.082f
            val strokeW = 3.2.dp.toPx()

            for (i in 0 until barCount) {
                val animMod = 0.65f + 0.35f * sin(wavePhase + i * 0.78f)
                val heightFraction = lerp(baseHeights[i] * 0.55f, baseHeights[i] * animMod, liveWaveFactor)
                val halfH = (maxHalfH * heightFraction).coerceAtLeast(strokeW)
                val x = regionStart + i * step
                drawLine(
                    color = contentColor.copy(alpha = 0.24f),
                    start = Offset(x, centerY - halfH),
                    end = Offset(x, centerY + halfH),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }

        // TOP (portrait) or LEFT 50% (landscape) LAYER: Morphing 3-Pillar Capsule Artwork
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(params.expansionFractionProvider())
                    val fullW = if (isLandscape) {
                        (constraints.maxWidth.toFloat() * 0.50f).takeIf { it > 0f } ?: (screenWidthPx * 0.50f)
                    } else {
                        constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    }
                    val fullH = if (isLandscape) {
                        (constraints.maxHeight.toFloat() * 0.84f).takeIf { it > 0f } ?: artHeightPx
                    } else {
                        artHeightPx
                    }
                    val targetTopY = if (isLandscape) {
                        (constraints.maxHeight.toFloat() - fullH) * 0.5f
                    } else {
                        artTopOffsetPx
                    }
                    val baseW = lerp(miniArtSizePx, fullW, sample.sizeT)
                    val baseH = lerp(miniArtSizePx, fullH, sample.sizeT)
                    val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val baseX = lerp(miniArtOffsetPx, 0f, sample.posT)
                    val baseY = lerp(miniArtOffsetPx, targetTopY, sample.posT)
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
                .graphicsLayer {
                    alpha = if (params.aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
                .clip(pillarShape)
                .styleArtworkGestures(
                    aodScreenEnabled = params.aodScreenEnabled,
                    onEnterAodScreen = params.onEnterAodScreen
                )
        ) {
            SmartImage(
                model = optimizedArtUri,
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
        }

        // LOWER (portrait) or RIGHT 50% (landscape) SECTION
        Column(
            modifier = if (isLandscape) {
                Modifier
                    .fillMaxWidth(0.50f)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 12.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarBottomPadding + 22.dp)
            },
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.76f,
                    titleFontSize = 26.sp,
                    artistFontSize = 15.sp,
                    screenWidthPx = screenWidthPx,
                    rootCoordsHolder = rootCoordsHolder,
                    morphTracker = morphTracker,
                    expansionFractionProvider = params.expansionFractionProvider,
                    aodSourceBounds = params.aodSourceBounds,
                    aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                    onTitleClick = params.onTitleClick,
                    onArtistClick = params.onArtistClick,
                    modifier = Modifier.weight(1f)
                )

                StyleFavoriteButton(
                    isFavorite = isFavorite,
                    containerColor = onAccentColor.copy(alpha = 0.80f),
                    activeContainerColor = accentColor,
                    iconTint = accentColor,
                    activeIconTint = onAccentColor,
                    size = 46.dp,
                    iconSize = 23.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.18f),
                thumbColor = accentColor,
                timeTextColor = contentColor.copy(alpha = 0.76f),
                metaPillBgColor = contentColor.copy(alpha = 0.10f),
                metaPillTextColor = contentColor.copy(alpha = 0.90f),
                waveAmplitudeWhenPlaying = 3.dp,
                trackHeight = 5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))

            // Taller (92dp) Neighbor-pushing Capsule Transport Row matching Wave & Expressive
            StyleMorphingTransportRow(
                order = pillarsOrder,
                baseWeights = pillarsWeights,
                screenWidthPx = screenWidthPx,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                expansionFractionProvider = params.expansionFractionProvider,
                aodSourceBounds = params.aodSourceBounds,
                aodTransitionProgressProvider = params.aodTransitionProgressProvider,
                isPlaying = isPlaying,
                playContainerColor = accentColor,
                playContentColor = onAccentColor,
                skipContainerColor = onAccentColor,
                skipContentColor = accentColor,
                playCornerPlaying = 28.dp,
                playCornerPaused = 46.dp,
                skipCorner = 46.dp,
                height = if (isLandscape) 76.dp else 92.dp,
                spacing = 8.dp,
                onPrevious = params.onPrevious,
                onPlayPause = params.onPlayPause,
                onNext = params.onNext,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))

            // Exact BottomToggleRow height + separated circular menu button
            StyleExpressiveBottomBarWithMenu(
                isShuffleEnabled = isShuffle,
                repeatMode = repeatMode,
                onShuffleToggle = params.onShuffleToggle,
                onRepeatToggle = params.onRepeatToggle,
                onLyricsClick = params.onLyricsClick,
                onMoreOptionsClick = params.onMoreOptionsClick,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    }
            )
        }
    }
}
