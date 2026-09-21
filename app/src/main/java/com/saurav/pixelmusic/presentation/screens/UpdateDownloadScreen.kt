package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.saurav.pixelmusic.utils.InAppUpdater
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDownloadScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val downloadState by InAppUpdater.downloadState.collectAsStateWithLifecycle()

    val uiPhase by remember(downloadState) {
        derivedStateOf {
            when (downloadState) {
                is InAppUpdater.GlobalDownloadState.Idle -> "IDLE"
                is InAppUpdater.GlobalDownloadState.Downloading -> "DOWNLOADING"
                is InAppUpdater.GlobalDownloadState.Finished -> "FINISHED"
                is InAppUpdater.GlobalDownloadState.Error -> "ERROR"
            }
        }
    }

    Dialog(
        onDismissRequest = { navController.popBackStack() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                if (downloadState is InAppUpdater.GlobalDownloadState.Downloading) {
                    val state = downloadState as InAppUpdater.GlobalDownloadState.Downloading
                    FloatingParticlesBackground(progress = state.progress, isPaused = state.isPaused)
                }

                // Foreground UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = uiPhase,
                        transitionSpec = {
                            fadeIn(tween(400)) + scaleIn(initialScale = 0.9f) togetherWith fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
                        },
                        label = "Download UI State"
                    ) { phase ->
                        when (phase) {
                            "DOWNLOADING" -> {
                                val state = downloadState as? InAppUpdater.GlobalDownloadState.Downloading
                                DownloadingView(
                                    progress = state?.progress ?: 0f,
                                    isPaused = state?.isPaused ?: false,
                                    versionName = state?.versionName ?: "",
                                    totalBytes = state?.totalBytes ?: 0L,
                                    downloadedBytes = state?.downloadedBytes ?: 0L,
                                    onPauseResume = {
                                        if (state?.isPaused == true) {
                                            InAppUpdater.resumeDownload(context) 
                                        } else {
                                            InAppUpdater.pauseDownload()
                                        }
                                    },
                                    onCancel = {
                                        InAppUpdater.cancelDownload(context)
                                        navController.popBackStack()
                                    }
                                )
                            }
                            "FINISHED" -> {
                                val state = downloadState as? InAppUpdater.GlobalDownloadState.Finished
                                FinishedView(
                                    versionName = state?.versionName ?: "",
                                    apkFile = state?.apkFile,
                                    onInstall = { state?.apkFile?.let { InAppUpdater.installApk(context, it) } },
                                    onDelete = {
                                        InAppUpdater.deleteApk(context)
                                        navController.popBackStack()
                                    }
                                )
                            }
                            "ERROR" -> {
                                val state = downloadState as? InAppUpdater.GlobalDownloadState.Error
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Download Failed", style = MaterialTheme.typography.headlineMedium)
                                    Text(state?.message ?: "Unknown error", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(24.dp))
                                    Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No active download", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { navController.popBackStack() }) { Text("Go Back") }
                                }
                            }
                        }
                    }
                }

                // Floating Back Button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        }
    }
}

@Composable
private fun FloatingParticlesBackground(progress: Float, isPaused: Boolean) {
    class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        val drift: Float,
        var driftOffset: Float,
        val baseAlpha: Float
    )

    val particles = remember {
        List(40) {
            Particle(
                x = Math.random().toFloat(),
                y = 1.0f + Math.random().toFloat(),
                radius = 2f + (Math.random().toFloat() * 6f),
                speed = 0.08f + (Math.random().toFloat() * 0.15f), // Independent vertical speeds
                drift = (Math.random().toFloat() - 0.5f) * 0.05f,   // Smooth horizontal sway
                driftOffset = Math.random().toFloat() * 100f,
                baseAlpha = 0.2f + (Math.random().toFloat() * 0.6f)
            )
        }
    }

    var frameTime by remember { mutableLongStateOf(0L) }
    val currentProgress by rememberUpdatedState(progress)

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            var lastTime = withFrameNanos { it }
            while (true) {
                val currentTime = withFrameNanos { it }
                val deltaSeconds = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                val prog = currentProgress
                
                // Stop calculating math once we fade out entirely at 95%
                if (prog > 0f && prog < 0.95f) {
                    for (p in particles) {
                        p.y -= (p.speed * deltaSeconds)
                        p.driftOffset += deltaSeconds
                        p.x += (sin(p.driftOffset) * p.drift * deltaSeconds)
                        
                        if (p.y < -0.1f) {
                            p.y = 1.1f + Math.random().toFloat() * 0.2f
                            p.x = Math.random().toFloat()
                        }
                    }
                }
                frameTime = currentTime
            }
        }
    }

    val dotColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentTick = frameTime // Force recomposition
        val prog = currentProgress

        // Fade out perfectly between 90% and 95%
        val globalAlpha = when {
            prog <= 0.01f -> 0f
            prog < 0.05f -> (prog - 0.01f) / 0.04f
            prog < 0.90f -> 1f
            prog <= 0.95f -> 1f - ((prog - 0.90f) / 0.05f).coerceIn(0f, 1f)
            else -> 0f
        }

        if (globalAlpha > 0f) {
            for (p in particles) {
                drawCircle(
                    color = dotColor.copy(alpha = p.baseAlpha * globalAlpha),
                    radius = p.radius.dp.toPx(),
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }
    }
}

@Composable
private fun DownloadingView(
    progress: Float,
    isPaused: Boolean,
    versionName: String,
    totalBytes: Long,
    downloadedBytes: Long,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    val sizeString = remember(totalBytes, downloadedBytes) {
        if (totalBytes > 0) {
            val totalMb = totalBytes / (1024f * 1024f)
            val downMb = downloadedBytes / (1024f * 1024f)
            String.format(Locale.US, "%.1f / %.1f MB", downMb, totalMb)
        } else {
            "Calculating size..."
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 84.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isPaused) "Paused" else "Downloading $versionName",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = AbsoluteSmoothCornerShape(10.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = sizeString,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(AbsoluteSmoothCornerShape(12.dp, 60))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.02f))
                    .clip(AbsoluteSmoothCornerShape(12.dp, 60))
                    .background(if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = AbsoluteSmoothCornerShape(12.dp, 60)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Please do not close the app during the update.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cancel")
            }

            Button(
                onClick = onPauseResume,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPaused) "Resume" else "Pause")
            }
        }
    }
}

@Composable
private fun FinishedView(
    versionName: String,
    apkFile: java.io.File?,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeString = remember(apkFile) {
        if (apkFile != null && apkFile.exists()) {
            val sizeMb = apkFile.length() / (1024f * 1024f)
            String.format(java.util.Locale.US, "%.1f MB", sizeMb)
        } else {
            "Unknown Size"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Download Complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Version $versionName is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = AbsoluteSmoothCornerShape(12.dp, 60),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = sizeString,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onInstall,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = AbsoluteSmoothCornerShape(20.dp, 60)
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Install Update", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete APK file")
        }
    }
}
