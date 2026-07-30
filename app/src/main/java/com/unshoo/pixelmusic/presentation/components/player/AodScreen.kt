package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
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
            glowColor = extractDominantColor(context, highResAlbumArtUri, Color(0xFF888888), isDarkTheme = true)
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
            initialValue = 0.90f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.40f, targetValue = 0.85f,
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
                }
        ) {
             Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Ambient radial glow animation
                    Box(
                        modifier = Modifier
                            .requiredSize(340.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                                alpha = glowAlpha
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Sharp, in-focus, circular art — sized down from before
                    SmartImage(
                        model = highResAlbumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        targetSize = coil.size.Size.ORIGINAL,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.height(40.dp))

                Text(
                    text = songTitle,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
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

                Spacer(Modifier.height(40.dp))

                val totalMs = totalDurationProvider()
                val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f

                // Two-tone progress track with a thumb divider, matching the
                // reference: solid white "played" portion, dim gray
                // "remaining" portion, thin vertical white thumb at the seam.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(progress.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - progress).coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatAodTime(positionMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatAodTime(totalMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Transport controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    IconButton(onClick = onSkipPreviousClick, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPlayPauseClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingProvider()) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingProvider()) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(onClick = onSkipNextClick, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatAodTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
