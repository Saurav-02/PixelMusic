package com.saurav.pixelmusic.presentation.components.player.styles

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 100% continuous organic S-curve wave bottom cutout that morphs seamlessly from the MiniPlayer
 * 44dp circle (at t = 0) into a full-bleed wavy artwork silhouette (at t = 1) with ZERO threshold
 * snap or sharp bottom corners during drag.
 */
private class MorphingOrganicWaveShape(
    private val morphProgressProvider: () -> Float,
    private val isLandscape: Boolean = false
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = morphProgressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        val halfMin = minOf(w, h) * 0.5f

        if (isLandscape) {
            val leftCornerR = (halfMin * (1f - t).pow(1.15f)).coerceIn(0f, halfMin)
            val rightCornerR = (halfMin * (1f - t).pow(1.35f)).coerceIn(0f, halfMin)
            val waveT = t.pow(1.35f)
            val waveAmp = w * 0.15f * waveT

            val topX = w - waveAmp * 0.52f
            val bottomX = w - waveAmp * 0.44f
            val crestX = w - 2f

            val clampedRightR = rightCornerR.coerceAtMost(
                minOf(h * 0.49f, topX - leftCornerR, bottomX - leftCornerR).coerceAtLeast(0f)
            )

            val path = Path().apply {
                if (leftCornerR > 0.5f) {
                    moveTo(0f, leftCornerR)
                    arcTo(Rect(0f, 0f, leftCornerR * 2f, leftCornerR * 2f), 180f, 90f, false)
                } else {
                    moveTo(0f, 0f)
                }

                if (clampedRightR > 0.5f) {
                    lineTo(topX - clampedRightR, 0f)
                    arcTo(
                        Rect(topX - clampedRightR * 2f, 0f, topX, clampedRightR * 2f),
                        270f,
                        90f,
                        false
                    )
                } else {
                    lineTo(topX, 0f)
                }

                val topSpanY = clampedRightR
                val bottomSpanY = h - clampedRightR
                val spanH = (bottomSpanY - topSpanY).coerceAtLeast(1f)

                if (waveT > 0.005f) {
                    cubicTo(
                        lerp(topX, w - waveAmp * 0.96f, waveT), topSpanY + spanH * 0.16f,
                        lerp(topX, w - waveAmp * 1.02f, waveT), topSpanY + spanH * 0.34f,
                        lerp((topX + bottomX) * 0.5f, w - waveAmp * 0.52f, waveT), topSpanY + spanH * 0.50f
                    )
                    cubicTo(
                        lerp(bottomX, crestX, waveT), topSpanY + spanH * 0.66f,
                        lerp(bottomX, crestX, waveT), topSpanY + spanH * 0.86f,
                        bottomX, bottomSpanY
                    )
                } else {
                    lineTo(bottomX, bottomSpanY)
                }

                if (clampedRightR > 0.5f) {
                    arcTo(
                        Rect(bottomX - clampedRightR * 2f, h - clampedRightR * 2f, bottomX, h),
                        0f,
                        90f,
                        false
                    )
                }

                if (leftCornerR > 0.5f) {
                    lineTo(leftCornerR, h)
                    arcTo(Rect(0f, h - leftCornerR * 2f, leftCornerR * 2f, h), 90f, 90f, false)
                } else {
                    lineTo(0f, h)
                }
                close()
            }
            return Outline.Generic(path)
        }

        val topCornerR = (halfMin * (1f - t).pow(1.15f)).coerceIn(0f, halfMin)
        val bottomCornerR = (halfMin * (1f - t).pow(1.35f)).coerceIn(0f, halfMin)

        val waveT = t.pow(1.35f)
        val waveAmp = h * 0.16f * waveT

        val leftY = h - waveAmp * 0.52f
        val rightY = h - waveAmp * 0.44f
        val troughY = h - 2f

        val clampedBottomR = bottomCornerR.coerceAtMost(
            minOf(w * 0.49f, leftY - topCornerR, rightY - topCornerR).coerceAtLeast(0f)
        )

        val path = Path().apply {
            if (topCornerR > 0.5f) {
                moveTo(0f, topCornerR)
                arcTo(Rect(0f, 0f, topCornerR * 2f, topCornerR * 2f), 180f, 90f, false)
            } else {
                moveTo(0f, 0f)
            }

            if (topCornerR > 0.5f) {
                lineTo(w - topCornerR, 0f)
                arcTo(Rect(w - topCornerR * 2f, 0f, w, topCornerR * 2f), 270f, 90f, false)
            } else {
                lineTo(w, 0f)
            }

            if (clampedBottomR > 0.5f) {
                lineTo(w, rightY - clampedBottomR)
                arcTo(
                    Rect(w - clampedBottomR * 2f, rightY - clampedBottomR * 2f, w, rightY),
                    0f,
                    90f,
                    false
                )
            } else {
                lineTo(w, rightY)
            }

            val rightSpanX = w - clampedBottomR
            val leftSpanX = clampedBottomR
            val spanW = (rightSpanX - leftSpanX).coerceAtLeast(1f)

            if (waveT > 0.005f) {
                cubicTo(
                    leftSpanX + spanW * 0.86f, lerp(rightY, troughY, waveT),
                    leftSpanX + spanW * 0.64f, lerp(rightY, troughY, waveT),
                    leftSpanX + spanW * 0.48f, lerp((rightY + leftY) * 0.5f, h - waveAmp * 0.52f, waveT)
                )
                cubicTo(
                    leftSpanX + spanW * 0.32f, lerp(leftY, h - waveAmp * 1.04f, waveT),
                    leftSpanX + spanW * 0.14f, lerp(leftY, h - waveAmp * 0.96f, waveT),
                    leftSpanX, leftY
                )
            } else {
                lineTo(leftSpanX, leftY)
            }

            if (clampedBottomR > 0.5f) {
                arcTo(
                    Rect(0f, leftY - clampedBottomR * 2f, clampedBottomR * 2f, leftY),
                    90f,
                    90f,
                    false
                )
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Isolated Now Playing Style: Wave
 * Full-bleed top album artwork with a continuous morphing S-wave bottom divider,
 * 5 layered faded wave shapes cascading right under the album art wave edge with slow
 * up/down spring motion, exact old-style Play/Pause morphing & neighbor-pushing controls,
 * and exact BottomToggleRow pill padding + separated circular menu button.
 */
@Composable
fun WavePlayerLayout(
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
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val artHeightPx = with(density) { (screenHeightDp * 0.48f).toPx() }
    val miniArtSizePx = remember(density) { with(density) { 44.dp.toPx() } }
    val miniArtOffsetPx = remember(density) { with(density) { 10.dp.toPx() } }
    val slideOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }

    val rootCoordsHolder = remember { LayoutCoordsHolder() }
    val artBounds = params.aodSourceBounds?.artBounds ?: remember { MorphElementBounds() }
    val morphTracker = remember { ArtworkMorphTracker() }

    params.aodSourceBounds?.let { bounds ->
        bounds.artStyle = AodSourceArtStyle.IMMERSIVE_RECT
        bounds.washColor = solidBackgroundColor
        bounds.topShadowHeightDp = 104f
        bounds.topShadowStartAlpha = 0.36f
        bounds.topShadowMidAlpha = 0.10f
    }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val waveShape = remember(morphTracker, params.expansionFractionProvider, isLandscape) {
        MorphingOrganicWaveShape(
            morphProgressProvider = {
                morphTracker.evaluate(params.expansionFractionProvider()).sizeT
            },
            isLandscape = isLandscape
        )
    }

    val isPlaying = params.isPlayingProvider()
    val isFavorite = params.isFavoriteProvider()
    val isShuffle = params.isShuffleEnabledProvider()
    val repeatMode = params.repeatModeProvider()

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "waveArtScale"
    )

    // 5 staggered drag-only springs for the art-edge wave ribbons & bottom area waves.
    // Zero infinite animations: waves spring smoothly only while dragging open/closed, then rest still.
    val currentExpansion = params.expansionFractionProvider().coerceIn(0f, 1f)
    val edgeSpring1 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 72f),
        label = "waveEdgeSpring1"
    )
    val edgeSpring2 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.59f, stiffness = 94f),
        label = "waveEdgeSpring2"
    )
    val edgeSpring3 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 118f),
        label = "waveEdgeSpring3"
    )
    val edgeSpring4 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 144f),
        label = "waveEdgeSpring4"
    )
    val edgeSpring5 by animateFloatAsState(
        targetValue = currentExpansion,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 172f),
        label = "waveEdgeSpring5"
    )

    val waveOrder = remember {
        listOf(MiniTransportSlot.PREVIOUS, MiniTransportSlot.PLAY_PAUSE, MiniTransportSlot.NEXT)
    }
    val waveWeights = remember {
        mapOf(
            MiniTransportSlot.PREVIOUS to 0.92f,
            MiniTransportSlot.PLAY_PAUSE to 1.24f,
            MiniTransportSlot.NEXT to 0.92f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(solidBackgroundColor)
            .captureRootCoords(rootCoordsHolder)
    ) {
        // 5 layered faded wave ribbons under the album art edge + 3 subtle layered waves in the bottom area.
        // Both animate strictly via drag springs during open/close and stay completely still once settled.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                    alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                }
        ) {
            val w = size.width
            val h = size.height

            if (isLandscape) {
                val artW = w * 0.50f
                val waveAmp = artW * 0.15f

                fun drawLandscapeEdgeWaveRibbon(
                    pushPx: Float,
                    springVal: Float,
                    dragBouncePx: Float,
                    crestScale: Float,
                    color: Color
                ) {
                    val dragShiftX = (1f - springVal) * dragBouncePx
                    val xOff = pushPx + dragShiftX
                    val topX = artW - waveAmp * 0.52f * crestScale + xOff
                    val bottomX = artW - waveAmp * 0.44f * crestScale + xOff
                    val crestX = artW - 2f + xOff

                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(topX, 0f)
                        cubicTo(
                            artW - waveAmp * 0.96f * crestScale + xOff, h * 0.16f,
                            artW - waveAmp * 1.02f * crestScale + xOff, h * 0.34f,
                            artW - waveAmp * 0.52f * crestScale + xOff, h * 0.50f
                        )
                        cubicTo(
                            crestX, h * 0.66f,
                            crestX, h * 0.86f,
                            bottomX, h
                        )
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = path, color = color)
                }

                // 5 layered vertical art-edge wave ribbons cascading rightward (back-to-front)
                drawLandscapeEdgeWaveRibbon(
                    pushPx = 56.dp.toPx(),
                    springVal = edgeSpring1,
                    dragBouncePx = 64.dp.toPx(),
                    crestScale = 0.78f,
                    color = baseTheme.tertiary.copy(alpha = 0.06f)
                )
                drawLandscapeEdgeWaveRibbon(
                    pushPx = 43.dp.toPx(),
                    springVal = edgeSpring2,
                    dragBouncePx = 52.dp.toPx(),
                    crestScale = 0.86f,
                    color = accentColor.copy(alpha = 0.08f)
                )
                drawLandscapeEdgeWaveRibbon(
                    pushPx = 30.dp.toPx(),
                    springVal = edgeSpring3,
                    dragBouncePx = 40.dp.toPx(),
                    crestScale = 0.92f,
                    color = baseTheme.secondary.copy(alpha = 0.10f)
                )
                drawLandscapeEdgeWaveRibbon(
                    pushPx = 19.dp.toPx(),
                    springVal = edgeSpring4,
                    dragBouncePx = 30.dp.toPx(),
                    crestScale = 0.97f,
                    color = accentColor.copy(alpha = 0.12f)
                )
                drawLandscapeEdgeWaveRibbon(
                    pushPx = 9.dp.toPx(),
                    springVal = edgeSpring5,
                    dragBouncePx = 20.dp.toPx(),
                    crestScale = 1.0f,
                    color = baseTheme.tertiary.copy(alpha = 0.15f)
                )
            } else {
                val artH = h * 0.48f
                val waveAmp = artH * 0.16f

                fun drawEdgeWaveRibbon(
                    dropPx: Float,
                    springVal: Float,
                    dragBouncePx: Float,
                    crestScale: Float,
                    color: Color
                ) {
                    val dragShiftY = (1f - springVal) * dragBouncePx
                    val yOff = dropPx + dragShiftY

                    val leftY = artH - waveAmp * 0.52f * crestScale + yOff
                    val rightY = artH - waveAmp * 0.44f * crestScale + yOff
                    val troughY = artH - 2f + yOff

                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, rightY)
                        cubicTo(
                            w * 0.86f, troughY,
                            w * 0.64f, troughY,
                            w * 0.48f, artH - waveAmp * 0.52f * crestScale + yOff
                        )
                        cubicTo(
                            w * 0.32f, artH - waveAmp * 1.04f * crestScale + yOff,
                            w * 0.14f, artH - waveAmp * 0.96f * crestScale + yOff,
                            0f, leftY
                        )
                        close()
                    }
                    drawPath(path = path, color = color)
                }

                // Top art-edge wave ribbons (back-to-front)
                drawEdgeWaveRibbon(
                    dropPx = 64.dp.toPx(),
                    springVal = edgeSpring1,
                    dragBouncePx = 74.dp.toPx(),
                    crestScale = 0.78f,
                    color = baseTheme.tertiary.copy(alpha = 0.07f)
                )
                drawEdgeWaveRibbon(
                    dropPx = 49.dp.toPx(),
                    springVal = edgeSpring2,
                    dragBouncePx = 60.dp.toPx(),
                    crestScale = 0.86f,
                    color = accentColor.copy(alpha = 0.09f)
                )
                drawEdgeWaveRibbon(
                    dropPx = 35.dp.toPx(),
                    springVal = edgeSpring3,
                    dragBouncePx = 48.dp.toPx(),
                    crestScale = 0.92f,
                    color = baseTheme.secondary.copy(alpha = 0.11f)
                )
                drawEdgeWaveRibbon(
                    dropPx = 22.dp.toPx(),
                    springVal = edgeSpring4,
                    dragBouncePx = 36.dp.toPx(),
                    crestScale = 0.97f,
                    color = accentColor.copy(alpha = 0.13f)
                )
                drawEdgeWaveRibbon(
                    dropPx = 10.dp.toPx(),
                    springVal = edgeSpring5,
                    dragBouncePx = 24.dp.toPx(),
                    crestScale = 1.0f,
                    color = baseTheme.tertiary.copy(alpha = 0.16f)
                )
            }

            // Restored subtle layered waves in the bottom controls area (drag-spring only)
            val bottomRegionH = if (isLandscape) {
                (h * 0.34f).coerceAtLeast(100.dp.toPx())
            } else {
                250.dp.toPx() + navBarBottomPadding.toPx()
            }
            val bottomRegionTop = h - bottomRegionH

            fun drawBottomWaveLayer(
                baseTopFraction: Float,
                springValue: Float,
                bouncePx: Float,
                leftFrac: Float,
                cp1X: Float,
                cp1Y: Float,
                midX: Float,
                midY: Float,
                cp2X: Float,
                cp2Y: Float,
                rightFrac: Float,
                color: Color
            ) {
                val dragOffsetY = (1f - springValue) * bouncePx
                val baseY = bottomRegionTop + bottomRegionH * baseTopFraction + dragOffsetY
                val path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, baseY + bottomRegionH * leftFrac)
                    cubicTo(
                        w * cp1X, baseY + bottomRegionH * cp1Y,
                        w * (midX - 0.14f), baseY + bottomRegionH * (midY + 0.08f),
                        w * midX, baseY + bottomRegionH * midY
                    )
                    cubicTo(
                        w * (midX + 0.16f), baseY + bottomRegionH * (midY - 0.10f),
                        w * cp2X, baseY + bottomRegionH * cp2Y,
                        w, baseY + bottomRegionH * rightFrac
                    )
                    lineTo(w, h)
                    close()
                }
                drawPath(path = path, color = color)
            }

            drawBottomWaveLayer(
                baseTopFraction = 0.18f,
                springValue = edgeSpring1,
                bouncePx = 64.dp.toPx(),
                leftFrac = 0.08f,
                cp1X = 0.22f, cp1Y = -0.10f,
                midX = 0.52f, midY = 0.06f,
                cp2X = 0.82f, cp2Y = -0.08f,
                rightFrac = 0.04f,
                color = baseTheme.tertiary.copy(alpha = 0.06f)
            )
            drawBottomWaveLayer(
                baseTopFraction = 0.40f,
                springValue = edgeSpring3,
                bouncePx = 46.dp.toPx(),
                leftFrac = 0.12f,
                cp1X = 0.28f, cp1Y = 0.16f,
                midX = 0.56f, midY = -0.04f,
                cp2X = 0.84f, cp2Y = 0.10f,
                rightFrac = 0.02f,
                color = accentColor.copy(alpha = 0.08f)
            )
            drawBottomWaveLayer(
                baseTopFraction = 0.62f,
                springValue = edgeSpring5,
                bouncePx = 30.dp.toPx(),
                leftFrac = 0.10f,
                cp1X = 0.26f, cp1Y = -0.04f,
                midX = 0.58f, midY = 0.12f,
                cp2X = 0.86f, cp2Y = -0.08f,
                rightFrac = -0.02f,
                color = baseTheme.secondary.copy(alpha = 0.10f)
            )
        }

        // TOP (portrait) or LEFT 50% (landscape) LAYER: Continuous Morphing Organic Wave Artwork
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
                        constraints.maxHeight.toFloat().takeIf { it > 0f } ?: artHeightPx
                    } else {
                        (constraints.maxHeight.toFloat() * 0.48f).takeIf { it > 0f } ?: artHeightPx
                    }
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
                    alpha = if (params.aodTransitionProgressProvider() > 0.001f) 0f else 1f
                }
                .clip(waveShape)
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

            if (!isLandscape) {
                // Top scrim for TopAppBar legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val aodFade = (1f - params.aodTransitionProgressProvider() * 2.2f).coerceIn(0f, 1f)
                            alpha = morphTracker.evaluate(params.expansionFractionProvider()).nonArtAlpha * aodFade
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.36f),
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
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
            // Song Title + Artist + Favorite Heart
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
                    artistAlpha = 0.78f,
                    titleFontSize = 27.sp,
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

            // Progress Slider + Timestamps + Optional Audio Meta
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
                waveAmplitudeWhenPlaying = 2.6.dp,
                trackHeight = 4.5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))

            // Taller (92dp) Neighbor-pushing Expressive Transport Row matching Expressive mode
            StyleMorphingTransportRow(
                order = waveOrder,
                baseWeights = waveWeights,
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
