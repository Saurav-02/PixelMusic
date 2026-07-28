package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sin

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
        
        // Hide status bar and nav bar for true full screen
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
                    WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        val highResAlbumArtUri = remember(albumArtUriString) {
            val rawUri = albumArtUriString ?: ""
            when {
                rawUri.contains("ggpht.com") || rawUri.contains("googleusercontent.com") -> {
                    rawUri.replace(Regex("=w\\d+-h\\d+"), "=w1200-h1200").replace(Regex("=s\\d+"), "=s1200")
                }
                else -> rawUri
            }
        }

        var rawGlowColor by remember { mutableStateOf(Color(0xFF888888)) }
        LaunchedEffect(highResAlbumArtUri) {
            rawGlowColor = extractDominantColor(context, highResAlbumArtUri, Color(0xFF888888), isDarkTheme = true)
        }

        // BOOST the extracted color so it's guaranteed to be rich and expressive
        val vibrantGlowColor = remember(rawGlowColor) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(rawGlowColor.toArgb(), hsv)
            hsv[1] = (hsv[1] * 1.6f).coerceAtMost(1f) // Boost Saturation
            hsv[2] = (hsv[2] * 1.4f).coerceAtMost(1f) // Boost Brightness
            Color(android.graphics.Color.HSVToColor(hsv))
        }

        var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
        LaunchedEffect(Unit) {
            while (true) {
                positionMs = currentPositionProvider()
                delay(50) // Faster polling for smoother UI updates
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.9f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    // Double tap to dismiss to prevent accidental exits
                    detectTapGestures(onDoubleTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Premium Background Glow
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
                                0.0f to vibrantGlowColor.copy(alpha = glowAlpha),
                                0.4f to vibrantGlowColor.copy(alpha = glowAlpha * 0.4f),
                                1.0f to Color.Transparent,
                                center = center, 
                                radius = radius
                            ),
                            radius = radius, 
                            center = center, 
                            blendMode = BlendMode.Screen
                        )
                    }

                    SmartImage(
                        model = highResAlbumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = coil.size.Size.ORIGINAL,
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(Modifier.height(56.dp))
                
                Text(
                    text = songTitle, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 22.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = artistName, 
                    color = Color.White.copy(alpha = 0.6f), 
                    fontSize = 16.sp,
                    maxLines = 1
                )
                
                Spacer(Modifier.height(48.dp))

                val totalMs = totalDurationProvider()
                val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
                
                // Custom animated squiggly progress bar
                SquigglyProgressBar(
                    progress = progress,
                    color = vibrantGlowColor,
                    isPlaying = isPlayingProvider(),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(32.dp) // Taller hit area for the wave
                )
            }
        }
    }
}

@Composable
fun SquigglyProgressBar(
    progress: Float,
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * Math.PI).toFloat(), // Negative for forward motion
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1500 else 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseOffset"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        
        val thumbX = canvasWidth * progress
        val amplitude = 5.dp.toPx() // Height of the wave
        val frequency = 0.08f // Tightness of the wave
        val strokeWidthPx = 4.dp.toPx()
        val thumbRadiusPx = 7.dp.toPx()
        
        // 1. Draw track (straight line for unplayed portion)
        drawLine(
            color = color.copy(alpha = 0.25f),
            start = Offset(thumbX, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        
        // 2. Draw played part (animated sine wave)
        val path = Path()
        if (thumbX > 0f) {
            path.moveTo(0f, centerY)
            var x = 0f
            while (x < thumbX) {
                // If it's paused, we zero out the amplitude so it flattens out, 
                // or just keep it wavy but frozen. Keeping it wavy but frozen here:
                val y = centerY + sin((x * frequency + phaseOffset).toDouble()).toFloat() * amplitude
                path.lineTo(x, y)
                x += 2f // Step size for drawing smoothness
            }
            path.lineTo(thumbX, centerY)
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
        
        // 3. Draw the thumb (the prominent circle at the end)
        drawCircle(
            color = color,
            radius = thumbRadiusPx,
            center = Offset(thumbX, centerY)
        )
    }
}
