package com.unshoo.pixelmusic.presentation.components.player

import android.content.Context
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unshoo.pixelmusic.presentation.components.SmartImage
import kotlinx.coroutines.delay

/**
 * Manual-entry AMOLED "AOD" screen. Not a real system AOD (not possible for
 * 3rd-party apps) — this is an in-app full-screen ambient view, entered via
 * long-press on the Now Playing album art and dismissed by a single tap
 * anywhere on screen.
 */
@Composable
fun AodScreen(
    songTitle: String,
    artistName: String,
    albumArtUri: String?,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    totalDurationMs: Long,
    glowColor: Color,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var positionMs by remember { mutableLongStateOf(currentPositionMs()) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = currentPositionMs()
            delay(500)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aodGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // true black — AMOLED pixels off
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Layered "wavy" breathing glow behind the art
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
                            colors = listOf(
                                glowColor.copy(alpha = glowAlpha),
                                glowColor.copy(alpha = 0f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Plus
                    )
                }

                SmartImage(
                    model = albumArtUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    targetSize = coil.size.Size.ORIGINAL,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = songTitle,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artistName,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Minimal progress: a single thin line, no numbers, no clutter
            val progress = if (totalDurationMs > 0) {
                (positionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(glowColor.copy(alpha = 0.9f))
                )
            }
        }
    }
}
