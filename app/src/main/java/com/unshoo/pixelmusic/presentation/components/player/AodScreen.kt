package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.unshoo.pixelmusic.presentation.components.SmartImage

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
        DisposableEffect(Unit) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
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
            glowColor = extractDominantColor(context, highResAlbumArtUri, Color(0xFF888888), isDarkTheme = true)
        }

        var positionMs by remember { mutableLongStateOf(currentPositionProvider()) }
        LaunchedEffect(Unit) {
            while (true) {
                positionMs = currentPositionProvider()
                delay(500)
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.94f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f, targetValue = 0.75f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .size(340.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                    ) {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = glowAlpha), glowColor.copy(alpha = 0f)),
                                center = center, radius = radius
                            ),
                            radius = radius, center = center, blendMode = BlendMode.Plus
                        )
                    }

                    SmartImage(
                        model = highResAlbumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = coil.size.Size.ORIGINAL,
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(Modifier.height(32.dp))
                Text(songTitle, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(artistName, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))

                val totalMs = totalDurationProvider()
                val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier.width(120.dp).height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                            .background(glowColor.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}
