package com.saurav.pixelmusic.presentation.components.player.styles

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
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
import com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Isolated Now Playing Style: Expressive
 * Full-bleed gradient-faded top artwork, natural-case editorial title & artist with tonal heart,
 * wavy progress slider with centered audio metadata pill, taller (92dp) asymmetric neighbor-pushing
 * transport row (wide Play/Pause on left + twin circular Previous/Next on right), and exact
 * BottomToggleRow bottom padding + separated 66dp circular '+' menu button.
 */
@Composable
fun ExpressivePlayerLayout(
    params: PlayerStyleParams,
    modifier: Modifier = Modifier
) {
    val baseTheme = LocalMaterialTheme.current
    val isDark = LocalPixelMusicDarkTheme.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptics = LocalHapticFeedback.current

    val (optimizedArtUri, resolvedTitle, resolvedArtist) = rememberResolvedArtAndMeta(
        song = params.song,
        currentSongArtists = params.currentSongArtists,
        albumArtQuality = params.albumArtQuality
    )

    val solidBackgroundColor = baseTheme.primaryContainer
    val contentColor = baseTheme.onPrimaryContainer
    val accentColor = baseTheme.primary
    val onAccentColor = baseTheme.onPrimary
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val artHeightPx = with(density) { (screenHeightDp * 0.52f).toPx() }
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }
    val slideOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val artBounds = params.aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }
    val morphTracker = remember { ArtworkMorphTracker() }

    params.aodSourceBounds?.let { bounds ->
        bounds.artStyle = AodSourceArtStyle.IMMERSIVE_RECT
        bounds.washColor = solidBackgroundColor
        bounds.washStart = 0.52f
        bounds.washMid = 0.80f
        bounds.washMidAlpha = 0.88f
        bounds.topShadowHeightDp = 120f
        bounds.topShadowStartAlpha = 0.42f
        bounds.topShadowMidAlpha = 0.16f
    }

    val isPlaying = params.isPlayingProvider()
    val isFavorite = params.isFavoriteProvider()
    val isShuffle = params.isShuffleEnabledProvider()
    val repeatMode = params.repeatModeProvider()

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "expressiveArtScale"
    )

    val moreButtonBg = if (isDark) {
        baseTheme.surfaceContainer.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    val expressiveOrder = remember {
        listOf(MiniTransportSlot.PLAY_PAUSE, MiniTransportSlot.PREVIOUS, MiniTransportSlot.NEXT)
    }
    val expressiveWeights = remember {
        mapOf(
            MiniTransportSlot.PLAY_PAUSE to 1.65f,
            MiniTransportSlot.PREVIOUS to 0.94f,
            MiniTransportSlot.NEXT to 0.94f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // TOP LAYER: Full-bleed Gradient-Faded Artwork with smooth top corner rounding during drag
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val sample = morphTracker.evaluate(params.expansionFractionProvider())
                    val fullW = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: screenWidthPx
                    val fullH = (constraints.maxHeight.toFloat() * 0.52f).takeIf { it > 0f } ?: artHeightPx
                    val baseW = lerp(miniArtSizePx, fullW, sample.sizeT)
                    val baseH = lerp(miniArtSizePx, fullH, sample.sizeT)
                    val targetW = (baseW * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val targetH = (baseH * sample.overshootScale).roundToInt().coerceAtLeast(1)
                    val baseX = lerp(miniArtOffsetPx, 0f, sample.posT)
                    val baseY = lerp(miniArtOffsetPx, 0f, sample.posT)
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
                    val s = morphTracker.evaluate(params.expansionFractionProvider())
                    val t = s.sizeT.coerceIn(0f, 1f)
                    val maxR = minOf(size.width, size.height) * 0.5f
                    val topCornerPx = (maxR * (1f - t).pow(1.15f)).coerceIn(0f, maxR)
                    val bottomCornerPx = (maxR * (1f - t).pow(1.4f)).coerceIn(0f, maxR)
                    shape = RoundedCornerShape(
                        topStart = topCornerPx,
                        topEnd = topCornerPx,
                        bottomStart = bottomCornerPx,
                        bottomEnd = bottomCornerPx
                    )
                    clip = true
                    alpha = if (params.aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
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

            // Top status bar scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Smooth bottom gradient fade reaching full opacity before the clipped bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.48f)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.42f to solidBackgroundColor.copy(alpha = 0.58f),
                                0.76f to solidBackgroundColor.copy(alpha = 0.92f),
                                0.90f to solidBackgroundColor,
                                1.0f to solidBackgroundColor
                            )
                        )
                    )
            )
        }

        // Sub-pixel seam seal over the artwork Box bottom boundary so Skia clip anti-aliasing
        // never shows a 1px horizontal line (e.g., when a bottom sheet dims/scales the screen).
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val fullW = constraints.maxWidth.coerceAtLeast(1)
                    val fullH = (constraints.maxHeight.toFloat() * 0.52f).takeIf { it > 0f } ?: artHeightPx
                    val sealH = 4.dp.roundToPx().coerceAtLeast(2)
                    val placeable = measurable.measure(Constraints.fixed(fullW, sealH))
                    layout(fullW, sealH) {
                        placeable.place(0, (fullH - sealH * 0.5f).roundToInt())
                    }
                }
                .graphicsLayer {
                    val s = morphTracker.evaluate(params.expansionFractionProvider())
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = ((s.sizeT - 0.85f) / 0.15f).coerceIn(0f, 1f) * aodFade
                }
                .background(solidBackgroundColor)
        )

        // LOWER SECTION: Exact BottomToggleRow bottom padding + taller (92dp) asymmetric controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottomPadding + 22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StyleMorphingSongInfo(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    contentColor = contentColor,
                    artistAlpha = 0.76f,
                    titleFontSize = 28.sp,
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
                    size = 48.dp,
                    iconSize = 23.dp,
                    onClick = params.onFavoriteToggle,
                    modifier = Modifier.graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            StyleProgressSection(
                params = params,
                rootCoordsHolder = rootCoordsHolder,
                morphTracker = morphTracker,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.22f),
                thumbColor = accentColor,
                timeTextColor = contentColor.copy(alpha = 0.78f),
                metaPillBgColor = contentColor.copy(alpha = 0.12f),
                metaPillTextColor = contentColor.copy(alpha = 0.90f),
                waveAmplitudeWhenPlaying = 3.5.dp,
                trackHeight = 5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Taller (92dp) Asymmetric Neighbor-Pushing Transport Row with exact old-style Play/Pause morph
            StyleMorphingTransportRow(
                order = expressiveOrder,
                baseWeights = expressiveWeights,
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
                height = 92.dp,
                spacing = 8.dp,
                onPrevious = params.onPrevious,
                onPlayPause = params.onPlayPause,
                onNext = params.onNext,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Exact BottomToggleRow padding + 3-segment pill + separated 66dp circular '+' menu button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                        val a = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        alpha = a
                        translationY = (1f - a) * slideOffsetPx
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyleExpressiveBottomBarWithMenu(
                    isShuffleEnabled = isShuffle,
                    repeatMode = repeatMode,
                    onShuffleToggle = params.onShuffleToggle,
                    onRepeatToggle = params.onRepeatToggle,
                    onLyricsClick = params.onLyricsClick,
                    onMoreOptionsClick = params.onMoreOptionsClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
