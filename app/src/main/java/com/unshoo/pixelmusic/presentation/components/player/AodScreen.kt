package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.unshoo.pixelmusic.presentation.components.SmartImage
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AodScreen(
    songTitle: String,
    artistName: String,
    albumArtUriString: String?,
    isPlayingProvider: () -> Boolean,
    currentPositionProvider: () -> Long,
    totalDurationProvider: () -> Long,
    onDismiss: () -> Unit
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

        // Access the specific window holding the Dialog to hide the status bar
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        DisposableEffect(dialogWindow) {
            view.keepScreenOn = true

            dialogWindow?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }

            onDispose {
                view.keepScreenOn = false
                dialogWindow?.let { window ->
                    val insetsController = WindowInsetsControllerCompat(window, view)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        val highResAlbumArtUri = remember(albumArtUriString) {
            val rawUri = albumArtUriString ?: ""
            when {
                rawUri.contains("ggpht.com") || rawUri.contains("googleusercontent.com") -> {
                    rawUri.replace(Regex("=w\\d+-h\\d+"), "=w1200-h1200")
                          .replace(Regex("=s\\d+"), "=s1200")
                }
                else -> rawUri
            }
        }

        // Material You expressive seed color, extracted from the album art (used for
        // the frame border, inner ring, and progress bar).
        var glowColor by remember { mutableStateOf(Color(0xFF888888)) }
        LaunchedEffect(highResAlbumArtUri) {
            glowColor = extractDominantColor(context, highResAlbumArtUri, Color(0xFF888888), isDarkTheme = true)
        }

        // Multi-color aurora sourced directly from the app's Material You expressive
        // color scheme (dynamic color from the system wallpaper), not synthetic hues.
        val colorScheme = MaterialTheme.colorScheme
        val glowPalette = remember(colorScheme) {
            listOf(
                colorScheme.primary,
                colorScheme.secondary,
                colorScheme.tertiary
            )
        }

        var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
        LaunchedEffect(Unit) {
            while (true) {
                positionMs = currentPositionProvider()
                delay(100)
            }
        }

        // --- Slow, ambient animation values ---
        // Everything below is intentionally slow (7-22s cycles) so the AOD glow feels
        // like a calm, breathing aurora rather than a pulsing/flashing effect.
        val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")

        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.92f, targetValue = 1.10f,
            animationSpec = infiniteRepeatable(
                tween(9000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                tween(7000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        // Slow orbit angle that drifts the three color blobs around the art.
        val orbitAngle by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(
                tween(22000, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "orbitAngle"
        )

        val density = LocalDensity.current
        val orbitRadiusPx = with(density) { 46.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {

                    // 1. Wide, slow-breathing background bloom (single soft radial wash).
                    Canvas(
                        modifier = Modifier
                            .size(420.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                    ) {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to glowColor.copy(alpha = glowAlpha * 0.6f),
                                0.5f to glowColor.copy(alpha = glowAlpha * 0.3f),
                                1.0f to Color.Transparent,
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center,
                            blendMode = BlendMode.Screen
                        )
                    }

                    // 2. Slow orbiting Material You colored blobs sitting OUTSIDE the
                    // album art — produces the soft multi-color aurora bleeding out
                    // from behind the cover, drifting very slowly.
                    glowPalette.forEachIndexed { index, color ->
                        val phase = orbitAngle + index * 120f
                        val radians = Math.toRadians(phase.toDouble())
                        val offsetX = (cos(radians) * orbitRadiusPx).toFloat()
                        val offsetY = (sin(radians) * orbitRadiusPx).toFloat()

                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .graphicsLayer {
                                    translationX = offsetX
                                    translationY = offsetY
                                    alpha = glowAlpha
                                    scaleX = glowScale
                                    scaleY = glowScale
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .blur(60.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        0.0f to color,
                                        0.6f to color.copy(alpha = 0.5f),
                                        1.0f to Color.Transparent
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }

                    // 3. Blurry glowing rounded frame around the art (soft neon bleed,
                    // synced to the same slow pulse).
                    Box(
                        modifier = Modifier
                            .size(256.dp)
                            .border(
                                width = 8.dp,
                                color = glowColor.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .blur(20.dp)
                            .graphicsLayer {
                                alpha = glowAlpha * 0.9f + 0.1f
                            }
                    )

                    // 4. Crisp inner accent ring to frame the art sharply against the blur.
                    Box(
                        modifier = Modifier
                            .size(242.dp)
                            .border(
                                width = 1.dp,
                                color = glowColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(25.dp)
                            )
                    )

                    // 5. Album Art
                    SmartImage(
                        model = highResAlbumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = coil.size.Size.ORIGINAL,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(Modifier.height(48.dp))

                Text(
                    text = songTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artistName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    maxLines = 1
                )

                Spacer(Modifier.height(36.dp))

                val totalMs = totalDurationProvider()
                val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f

                // Material You Expressive Wavy Progress Bar
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = glowColor,
                    trackColor = glowColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}
