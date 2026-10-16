package com.saurav.pixelmusic.presentation.components.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.view.WindowManager
import androidx.core.view.WindowInsetsControllerCompat
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.SmartImage
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class ExpressivePolarShapeSpec(
    val lobes1: Int,
    val amp1: Float,
    val phase1: Float = 0f,
    val lobes2: Int = 0,
    val amp2: Float = 0f,
    val phase2: Float = 0f
) {
    fun radiusAt(theta: Double): Float {
        var r = 1.0
        if (lobes1 > 0 && amp1 != 0f) {
            r += amp1 * cos(lobes1 * theta + phase1)
        }
        if (lobes2 > 0 && amp2 != 0f) {
            r += amp2 * cos(lobes2 * theta + phase2)
        }
        return r.toFloat()
    }
}

private val AOD_EXPRESSIVE_SHAPES = listOf(
    // 0: 8-lobe Sunny Scallop Cookie
    ExpressivePolarShapeSpec(lobes1 = 8, amp1 = 0.076f),
    // 1: 6-lobe Soft Flower
    ExpressivePolarShapeSpec(lobes1 = 6, amp1 = 0.096f),
    // 2: 4-leaf Clover Cookie
    ExpressivePolarShapeSpec(lobes1 = 4, amp1 = 0.115f, phase1 = (PI / 4.0).toFloat()),
    // 3: 10-lobe Decagon Badge
    ExpressivePolarShapeSpec(lobes1 = 10, amp1 = 0.058f),
    // 4: 5-petal Blossom
    ExpressivePolarShapeSpec(lobes1 = 5, amp1 = 0.095f, phase1 = (-PI / 2.0).toFloat()),
    // 5: 7-lobe Heptagon Wave
    ExpressivePolarShapeSpec(lobes1 = 7, amp1 = 0.082f),
    // 6: 12-lobe Sunburst Seal
    ExpressivePolarShapeSpec(lobes1 = 12, amp1 = 0.046f),
    // 7: Soft Squircle Cushion
    ExpressivePolarShapeSpec(
        lobes1 = 4,
        amp1 = 0.072f,
        phase1 = (PI / 4.0).toFloat(),
        lobes2 = 8,
        amp2 = -0.016f
    ),
    // 8: 9-lobe Nonagon Rosette
    ExpressivePolarShapeSpec(lobes1 = 9, amp1 = 0.066f),
    // 9: 3-lobe Rounded Trefoil Pebble
    ExpressivePolarShapeSpec(
        lobes1 = 3,
        amp1 = 0.098f,
        phase1 = (PI / 6.0).toFloat(),
        lobes2 = 6,
        amp2 = 0.024f
    ),
    // 10: Harmonic 6-Star Cookie
    ExpressivePolarShapeSpec(lobes1 = 6, amp1 = 0.078f, lobes2 = 12, amp2 = 0.025f),
    // 11: Harmonic 8-Petal Lotus
    ExpressivePolarShapeSpec(
        lobes1 = 8,
        amp1 = 0.062f,
        lobes2 = 4,
        amp2 = 0.048f,
        phase2 = (PI / 4.0).toFloat()
    )
)

private class MorphingExpressivePolarShape(
    private val fromSpec: ExpressivePolarShapeSpec,
    private val toSpec: ExpressivePolarShapeSpec,
    private val morphProgress: Float,
    private val steps: Int = 120
) : Shape {
    private var cachedWidth = -1f
    private var cachedHeight = -1f
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val existing = cachedOutline
        if (existing != null && size.width == cachedWidth && size.height == cachedHeight) {
            return existing
        }

        val count = steps.coerceAtLeast(60)
        val radii = FloatArray(count)
        var maxRadius = 0.001f
        val twoPi = 2.0 * PI
        val clampedMorph = morphProgress.coerceIn(0f, 1f)

        for (i in 0 until count) {
            val theta = (twoPi * i) / count
            val rFrom = fromSpec.radiusAt(theta)
            val rTo = toSpec.radiusAt(theta)
            val r = lerp(rFrom, rTo, clampedMorph)
            radii[i] = r
            if (r > maxRadius) maxRadius = r
        }

        val baseRadius = min(size.width, size.height) * 0.485f
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f

        val path = Path().apply {
            for (i in 0 until count) {
                val theta = (twoPi * i) / count
                val normalizedR = baseRadius * (radii[i] / maxRadius)
                val x = centerX + (normalizedR * cos(theta)).toFloat()
                val y = centerY + (normalizedR * sin(theta)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        val outline = Outline.Generic(path)
        cachedWidth = size.width
        cachedHeight = size.height
        cachedOutline = outline
        return outline
    }
}

private data class AodPaletteColors(
    val accent: Color,
    val onAccent: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val glow: Color
)

private fun deriveAodPaletteColors(seedColor: Color, fallbackPrimary: Color): AodPaletteColors {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedColor.toArgb(), hsl)
    val fallbackHsl = FloatArray(3)
    ColorUtils.colorToHSL(fallbackPrimary.toArgb(), fallbackHsl)

    val hasChroma = hsl[1] >= 0.12f
    val hue = if (hasChroma) hsl[0] else fallbackHsl[0]
    val baseSat = if (hasChroma) hsl[1].coerceIn(0.45f, 0.78f) else fallbackHsl[1].coerceIn(0.35f, 0.70f)

    val accent = Color(ColorUtils.HSLToColor(floatArrayOf(hue, baseSat, 0.66f)))
    val onAccent = Color(ColorUtils.HSLToColor(floatArrayOf(hue, (baseSat * 0.65f).coerceIn(0.20f, 0.55f), 0.10f)))
    val secondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, (baseSat * 0.45f).coerceIn(0.15f, 0.40f), 0.16f)))
    val onSecondaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, (baseSat * 0.65f).coerceIn(0.25f, 0.70f), 0.82f)))
    val glow = Color(ColorUtils.HSLToColor(floatArrayOf(hue, (baseSat * 0.75f).coerceIn(0.35f, 0.75f), 0.42f)))

    return AodPaletteColors(
        accent = accent,
        onAccent = onAccent,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        glow = glow
    )
}

private fun pickNextRandomShapeIndex(currentIndex: Int): Int {
    if (AOD_EXPRESSIVE_SHAPES.size <= 1) return 0
    var candidate = Random.nextInt(AOD_EXPRESSIVE_SHAPES.size)
    if (candidate == currentIndex) {
        candidate = (currentIndex + Random.nextInt(1, AOD_EXPRESSIVE_SHAPES.size)) % AOD_EXPRESSIVE_SHAPES.size
    }
    return candidate
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun AodScreen(
    songTitle: String,
    artistName: String,
    albumArtUriString: String?,
    isPlayingProvider: () -> Boolean,
    currentPositionProvider: () -> Long,
    totalDurationProvider: () -> Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val view = LocalView.current
        val hapticFeedback = LocalHapticFeedback.current
        val baseTheme = LocalMaterialTheme.current

        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        DisposableEffect(dialogWindow) {
            view.keepScreenOn = true
            val originalBrightness = dialogWindow?.attributes?.screenBrightness
                ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

            dialogWindow?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())

                val lp = window.attributes
                lp.screenBrightness = 0.05f
                window.attributes = lp
            }

            onDispose {
                view.keepScreenOn = false
                dialogWindow?.let { window ->
                    val insetsController = WindowInsetsControllerCompat(window, view)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())

                    val lp = window.attributes
                    lp.screenBrightness = originalBrightness
                    window.attributes = lp
                }
            }
        }

        val effectiveQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
        val optimizedAlbumArtUri = remember(albumArtUriString, effectiveQuality) {
            com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(albumArtUriString, effectiveQuality)
                ?: (albumArtUriString ?: "")
        }

        var rawSeedColor by remember { mutableStateOf(baseTheme.primary) }
        LaunchedEffect(optimizedAlbumArtUri, baseTheme.primary) {
            rawSeedColor = extractDominantColor(
                context = context,
                uriString = optimizedAlbumArtUri,
                fallback = baseTheme.primary,
                isDarkTheme = false
            )
        }

        val targetPalette = remember(rawSeedColor, baseTheme.primary) {
            deriveAodPaletteColors(rawSeedColor, baseTheme.primary)
        }

        val colorAnimSpec = tween<Color>(durationMillis = 600, easing = FastOutSlowInEasing)
        val accentColor by animateColorAsState(targetPalette.accent, colorAnimSpec, label = "aodAccent")
        val onAccentColor by animateColorAsState(targetPalette.onAccent, colorAnimSpec, label = "aodOnAccent")
        val secondaryContainerColor by animateColorAsState(
            targetPalette.secondaryContainer,
            colorAnimSpec,
            label = "aodSecondaryContainer"
        )
        val onSecondaryContainerColor by animateColorAsState(
            targetPalette.onSecondaryContainer,
            colorAnimSpec,
            label = "aodOnSecondaryContainer"
        )
        val glowColor by animateColorAsState(targetPalette.glow, colorAnimSpec, label = "aodGlowColor")

        // Random shape selection per song and on album art tap, with smooth morphing
        var previousShapeIndex by remember { mutableIntStateOf(Random.nextInt(AOD_EXPRESSIVE_SHAPES.size)) }
        var targetShapeIndex by remember { mutableIntStateOf(previousShapeIndex) }
        val shapeMorphAnim = remember { Animatable(1f) }
        val artTapPulse = remember { Animatable(1f) }

        LaunchedEffect(songTitle, optimizedAlbumArtUri) {
            val nextIdx = pickNextRandomShapeIndex(targetShapeIndex)
            previousShapeIndex = targetShapeIndex
            targetShapeIndex = nextIdx
            shapeMorphAnim.snapTo(0f)
            shapeMorphAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }

        val quantizedMorph = (shapeMorphAnim.value * 60f).roundToInt() / 60f
        val albumShape = remember(previousShapeIndex, targetShapeIndex, quantizedMorph) {
            MorphingExpressivePolarShape(
                fromSpec = AOD_EXPRESSIVE_SHAPES[previousShapeIndex],
                toSpec = AOD_EXPRESSIVE_SHAPES[targetShapeIndex],
                morphProgress = quantizedMorph
            )
        }

        val isPlaying = isPlayingProvider()

        var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
        LaunchedEffect(Unit) {
            while (true) {
                positionMs = currentPositionProvider()
                delay(100)
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                tween(4800, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(
                tween(3600, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        val artScale by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0.93f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "artPlayingScale"
        )

        var shapeChangeTrigger by remember { mutableIntStateOf(0) }
        LaunchedEffect(shapeChangeTrigger) {
            if (shapeChangeTrigger > 0) {
                val nextIdx = pickNextRandomShapeIndex(targetShapeIndex)
                previousShapeIndex = targetShapeIndex
                targetShapeIndex = nextIdx
                artTapPulse.snapTo(0.94f)
                shapeMorphAnim.snapTo(0f)
                artTapPulse.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                shapeMorphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDismiss() }
                    )
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Subtle ambient radial glow
                    Box(
                        modifier = Modifier
                            .requiredSize(320.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                alpha = if (isPlaying) glowAlpha else glowAlpha * 0.6f
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.42f),
                                        glowColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Expressive shaped album art (upright, tap to morph shape)
                    Box(
                        modifier = Modifier
                            .size(232.dp)
                            .graphicsLayer {
                                val combinedScale = artScale * artTapPulse.value
                                scaleX = combinedScale
                                scaleY = combinedScale
                            }
                            .clip(albumShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        shapeChangeTrigger++
                                    },
                                    onDoubleTap = { onDismiss() }
                                )
                            }
                    ) {
                        SmartImage(
                            model = optimizedAlbumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            targetSize = coil.size.Size.ORIGINAL,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(38.dp))

                Text(
                    text = songTitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .basicMarquee()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artistName,
                    color = onSecondaryContainerColor.copy(alpha = 0.68f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .basicMarquee()
                )

                Spacer(Modifier.height(34.dp))

                val totalMs = totalDurationProvider().coerceAtLeast(0L)
                val liveProgress = if (totalMs > 0L) {
                    (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                AodExpressiveProgressBar(
                    progress = liveProgress,
                    positionMs = positionMs,
                    totalMs = totalMs,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.fillMaxWidth(0.88f)
                )

                Spacer(Modifier.height(28.dp))

                val controlAnimationSpec = remember {
                    tween<Float>(durationMillis = 240, easing = FastOutSlowInEasing)
                }

                AnimatedPlaybackControls(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    isPlayingProvider = isPlayingProvider,
                    onPrevious = onSkipPreviousClick,
                    onPlayPause = onPlayPauseClick,
                    onNext = onSkipNextClick,
                    height = 76.dp,
                    pressAnimationSpec = controlAnimationSpec,
                    releaseDelay = 220L,
                    colorOtherButtons = secondaryContainerColor,
                    colorPlayPause = accentColor,
                    tintPlayPauseIcon = onAccentColor,
                    tintOtherIcons = onSecondaryContainerColor,
                    colorPreviousButton = secondaryContainerColor,
                    colorNextButton = secondaryContainerColor,
                    tintPreviousIcon = onSecondaryContainerColor,
                    tintNextIcon = onSecondaryContainerColor,
                    playPauseCornerPlaying = 38.dp,
                    playPauseCornerPaused = 24.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AodExpressiveProgressBar(
    progress: Float,
    positionMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestTotalMs by rememberUpdatedState(totalMs)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(progress) }
    var lastHapticBucket by remember { mutableIntStateOf(-1) }

    val displayedProgress = if (isScrubbing) scrubProgress else progress
    val animatedProgress by animateFloatAsState(
        targetValue = displayedProgress.coerceIn(0f, 1f),
        animationSpec = if (isScrubbing) tween(40, easing = LinearEasing) else tween(180, easing = LinearEasing),
        label = "aodProgress"
    )

    val interactionFraction by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "aodScrubFraction"
    )

    val waveAmplitudeFraction by animateFloatAsState(
        targetValue = if (isPlaying && !isScrubbing) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "aodWaveAmp"
    )

    val strokeWidthDp = 8.dp
    val edgeInsetDp = 8.dp
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }
    val edgeInsetPx = with(density) { edgeInsetDp.toPx() }
    val restThumbWidthPx = with(density) { 6.dp.toPx() }
    val activeThumbWidthPx = with(density) { 8.dp.toPx() }
    val restThumbHeightPx = with(density) { 24.dp.toPx() }
    val activeThumbHeightPx = with(density) { 30.dp.toPx() }

    val trackStroke = remember(strokeWidthPx) {
        Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(Unit) {
                    fun progressForX(x: Float): Float {
                        val inset = edgeInsetPx.coerceIn(0f, size.width / 2f)
                        val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                        return ((x - inset) / trackWidth).coerceIn(0f, 1f)
                    }

                    awaitEachGesture {
                        try {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isScrubbing = true
                            down.consume()
                            var currentVal = progressForX(down.position.x)
                            scrubProgress = currentVal
                            lastHapticBucket = (currentVal * 25f).toInt()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break
                                pointerId = change.id
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                    currentVal = progressForX(change.position.x)
                                    scrubProgress = currentVal
                                    val bucket = (currentVal * 25f).toInt()
                                    if (bucket != lastHapticBucket) {
                                        lastHapticBucket = bucket
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                            if (latestTotalMs > 0L) {
                                latestOnSeekTo((currentVal * latestTotalMs).toLong())
                            }
                        } finally {
                            isScrubbing = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgeInsetDp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.18f),
                stroke = trackStroke,
                trackStroke = trackStroke,
                gapSize = 11.dp,
                stopSize = 4.dp,
                amplitude = { p -> if (p > 0f) waveAmplitudeFraction else 0f },
                wavelength = 38.dp,
                waveSpeed = 19.dp
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = edgeInsetPx.coerceIn(0f, size.width / 2f)
                val trackStart = inset
                val trackEnd = size.width - inset
                val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)

                val thumbWidth = lerp(restThumbWidthPx, activeThumbWidthPx, interactionFraction)
                val thumbHeight = lerp(restThumbHeightPx, activeThumbHeightPx, interactionFraction)
                val rawThumbX = trackStart + trackWidth * animatedProgress
                val minCenter = (thumbWidth / 2f).coerceAtMost(size.width / 2f)
                val maxCenter = (size.width - thumbWidth / 2f).coerceAtLeast(minCenter)
                val thumbX = rawThumbX.coerceIn(minCenter, maxCenter)
                val thumbY = size.height / 2f

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(thumbX - thumbWidth / 2f, thumbY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val shownPositionMs = if (isScrubbing && totalMs > 0L) {
            (scrubProgress * totalMs).toLong()
        } else {
            positionMs
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgeInsetDp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatAodTime(shownPositionMs),
                color = accentColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = formatAodTime(totalMs),
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}



private fun formatAodTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
