package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.saurav.pixelmusic.presentation.components.subcomps.SineWaveLine
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotificationSheet(
    isUpdateAvailable: Boolean,
    versionName: String,
    changelog: String?,
    isTestBuild: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit,
    onMigrateClick: () -> Unit = {},
    onSnoozeClick: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUpdateAvailable) "Update Available! 🚀" else "Changes in latest version ✨",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = changelog ?: "Bug fixes and performance improvements.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    MaterialYouVectorDrawable(
                        modifier = Modifier.fillMaxSize(),
                        drawableResId = R.drawable.welcome_art
                    )
                    
                    SineWaveLine(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(32.dp)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp),
                        animate = true,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        alpha = 0.95f,
                        strokeWidth = 16.dp,
                        amplitude = 4.dp,
                        waves = 7.6f,
                        phase = 0f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(22.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp)
                    )
                    SineWaveLine(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(32.dp)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp),
                        animate = true,
                        color = MaterialTheme.colorScheme.primary,
                        alpha = 0.95f,
                        strokeWidth = 4.dp,
                        amplitude = 4.dp,
                        waves = 7.6f,
                        phase = 0f
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTestBuild) {
                    Text(
                        text = "⚠️ This is a testing version of the app. It may contain bugs and unstable features. Please use the release version for the best experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            onMigrateClick()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        shape = AbsoluteSmoothCornerShape(20.dp, 60)
                    ) {
                        Text(
                            text = "Migrate to Stable Release",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Button(
                    onClick = {
                        onConfirmClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AbsoluteSmoothCornerShape(20.dp, 60)
                ) {
                    Text(
                        text = if (isUpdateAvailable) "Update Now" else "Awesome",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                if (isUpdateAvailable && onSnoozeClick != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            onSnoozeClick()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = "Don't remind me today",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
