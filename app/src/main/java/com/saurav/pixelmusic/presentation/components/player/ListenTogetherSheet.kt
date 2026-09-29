package com.saurav.pixelmusic.presentation.components.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.session.ListenTogetherUiState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel

/**
 * Listen Together bottom sheet: start a session, join one with a room code,
 * or manage the live session (room code, members, leave).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSheet(
    viewModel: PlayerViewModel,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.listenTogetherUiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    // Back press dismisses with the slow slide-down.
    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    // Custom overlay sheet: the scrim fades while the sheet slides up from
    // the bottom edge (700ms) and back down on dismiss (500ms). Material3's
    // sheet API no longer accepts a custom animation spec, so the sheet
    // drives its own animation instead of using ModalBottomSheet.
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400)),
            label = "listenTogetherScrim"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            ),
            label = "listenTogetherSheet"
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = colors.surfaceContainerLow,
                    tonalElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 12.dp, bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Visual drag handle
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .background(
                                    color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.listen_together),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.listen_together_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
            
                        when (val s = uiState) {
                            is ListenTogetherUiState.Idle, is ListenTogetherUiState.Error -> {
                                if (s is ListenTogetherUiState.Error) {
                                    Text(
                                        text = s.message,
                                        color = colors.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(stringResource(R.string.listen_together_your_name)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.startHostingSession(name) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.listen_together_start))
                                }
            
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { code = it.uppercase().filter { c -> c in 'A'..'Z' }.take(6) },
                                    label = { Text(stringResource(R.string.listen_together_room_code)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    onClick = { viewModel.joinListenTogetherSession(code, name) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.listen_together_join))
                                }
                            }
            
                            ListenTogetherUiState.Creating, ListenTogetherUiState.Joining -> {
                                Spacer(Modifier.height(8.dp))
                                CircularProgressIndicator()
                                Text(
                                    text = if (s is ListenTogetherUiState.Creating) {
                                        stringResource(R.string.listen_together_creating)
                                    } else {
                                        stringResource(R.string.listen_together_joining)
                                    },
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
            
                            is ListenTogetherUiState.Hosting -> {
                                Text(
                                    text = stringResource(R.string.listen_together_share_code),
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = s.code,
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 10.sp,
                                    color = colors.onSurface
                                )
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("room code", s.code))
                                        viewModel.sendToast(context.getString(R.string.listen_together_code_copied))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.listen_together_copy_code))
                                }
                                Text(
                                    text = context.getString(R.string.listen_together_listeners, s.members.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurfaceVariant
                                )
                                s.members.take(8).forEach { member ->
                                    Text(
                                        text = "\u2022 $member",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.leaveListenTogetherSession() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.error,
                                        contentColor = colors.onError
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.listen_together_end_session))
                                }
                            }
            
                            is ListenTogetherUiState.Guest -> {
                                Text(
                                    text = context.getString(R.string.listen_together_listening_with, s.hostName),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.listen_together_guest_note),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { viewModel.leaveListenTogetherSession() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.listen_together_leave))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
