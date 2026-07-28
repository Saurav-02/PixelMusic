package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

        var glowColor by remember { mutableStateOf(Color(0xFF888888)) }
        LaunchedEffect(highResAlbumArtUri) {
            // Note: Assuming extractDominantColor is handled elsewhere in your codebase
            glowColor = Color(0xFF888888) // Replace with your extractDominantColor logic
        }

        var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
        LaunchedEffect(Unit) {
            while (true) {
                positionMs = currentPositionProvider()
                delay(100)
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.90f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.55f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowAlpha"
        )

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
                modifier = Modifier.fillMaxWidth() // Removed 32.dp horizontal padding here to unconstrain the glow image
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Ambient glow
                    SmartImage(
                        model = highResAlbumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = coil.size.Size.ORIGINAL,
                        modifier = Modifier
                            .requiredSize(420.dp) // Switched to requiredSize to ignore parent width boundaries
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                alpha = glowAlpha
                            }
                            .clip(CircleShape) // Clipped to a circle before blur for a smooth, radial aura
                            .blur(radius = 80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    )

                    // The sharp, in-focus art on top
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

                // Moved the 32.dp horizontal padding directly to the text elements
                Text(
                    text = songTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artistName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(36.dp))

                val totalMs = totalDurationProvider()
                val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f

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
