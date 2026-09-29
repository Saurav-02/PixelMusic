[lines 1-524 of 574; 29487 chars in file]
package com.saurav.pixelmusic.presentation.components.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.session.ListenTogetherUiState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val reactionEvents by viewModel.listenTogetherReactions.collectAsStateWithLifecycle()
    val chatMessages by viewModel.listenTogetherMessages.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }

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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = colors.onSurfaceVariant,
                                        focusedBorderColor = colors.primary
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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = colors.onSurfaceVariant,
                                        focusedBorderColor = colors.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = guestName,
                                    onValueChange = { guestName = it },
                                    label = { Text(stringResource(R.string.listen_together_guest_name)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = colors.onSurfaceVariant,
                                        focusedBorderColor = colors.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    onClick = { viewModel.joinListenTogetherSession(code, guestName) },
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
                                val livePhase = s.members.size > 1
                                val copyCode = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("room code", s.code))
                                    viewModel.sendToast(context.getString(R.string.listen_together_code_copied))
                                }
                                AnimatedContent(
                                    targetState = livePhase,
                                    label = "listenTogetherHostPhase",
                                    modifier = Modifier.fillMaxWidth()
                                ) { live ->
                                    if (!live) {
                                        // Phase 1: waiting room — big code, centered.
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
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
                                            OutlinedButton(onClick = copyCode) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.listen_together_copy_code))
                                            }
                                            Text(
                                                text = context.getString(
                                                    R.string.listen_together_listeners,
                                                    s.members.size
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                            s.members.forEach { member ->
                                                Text(
                                                    text = "\u2022 ${member.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.onSurface
                                                )
                                            }
                                        }
                                    } else {
                                        // Phase 2: live — compact header, guest cards.
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF4CAF50))
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.listen_together_live),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.onSurface
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = context.getString(
                                                        R.string.listen_together_listeners,
                                                        s.members.size
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.onSurfaceVariant
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = colors.surfaceContainerHigh,
                                                    tonalElevation = 2.dp,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable(onClick = copyCode)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 8.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = s.code,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 2.sp,
                                                            color = colors.onSurface
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Rounded.ContentCopy,
                                                            contentDescription = stringResource(R.string.listen_together_copy_code),
                                                            tint = colors.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            s.members.forEach { member ->
                                                MemberRow(member = member, colors = colors)
                                            }
                                            SocialSection(
                                                messages = chatMessages,
                                                onReaction = { viewModel.sendListenTogetherReaction(it) },
                                                onLoved = { viewModel.sendLovedReaction() },
                                                onMessage = { viewModel.sendListenTogetherMessage(it) },
                                                colors = colors
                                            )
                                        }
                                    }
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
                                if (s.members.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = context.getString(
                                            R.string.listen_together_listeners,
                                            s.members.size
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                    s.members.forEach { member ->
                                        MemberRow(member = member, colors = colors)
                                    }
                                    SocialSection(
                                        messages = chatMessages,
                                        onReaction = { viewModel.sendListenTogetherReaction(it) },
                                        onLoved = { viewModel.sendLovedReaction() },
                                        onMessage = { viewModel.sendListenTogetherMessage(it) },
                                        colors = colors
                                    )
                                }
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
                FloatingReactionsOverlay(
                    events = reactionEvents,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private val avatarPalette = listOf(
    Color(0xFF7C4DFF),
    Color(0xFF00ACC1),
    Color(0xFFF4511E),
    Color(0xFF43A047),
    Color(0xFFD81B60),
    Color(0xFFFB8C00),
    Color(0xFF5C6BC0),
    Color(0xFF00897B)
)

private fun avatarColorFor(name: String): Color {
    val index = (name.hashCode() and Int.MAX_VALUE) % avatarPalette.size
    return avatarPalette[index]
}

/** One guest card: avatar, name, liveness caption and equalizer. */
@Composable
private fun MemberRow(
    member: com.saurav.pixelmusic.data.session.SessionMember,
    colors: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColorFor(member.name)),
            contentAlignment = Alignment.Center
        ) {
            if (!member.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = member.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = member.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface
            )
            Text(
                text = if (member.isLive) {
                    stringResource(R.string.listen_together_listening)
                } else {
                    stringResource(R.string.listen_together_reconnecting)
                },
[More: call again with start_line=525]

/** Emoji reaction bar + preset message chips + recent message bubbles. */
@Composable
private fun SocialSection(
    messages: List<com.saurav.pixelmusic.data.session.ChatMessage>,
    onReaction: (String) -> Unit,
    onLoved: () -> Unit,
    onMessage: (String) -> Unit,
    colors: ColorScheme
) {
    val presets = stringArrayResource(R.array.listen_together_preset_messages)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        messages.takeLast(3).forEach { msg ->
            key(msg.key) { MessageBubble(msg = msg, colors = colors) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("\u2764\uFE0F", "\uD83D\uDD25", "\uD83D\uDE2E", "\uD83D\uDC4F").forEach { emoji ->
                ReactionButton(emoji = emoji, onClick = { onReaction(emoji) })
            }
            LovedButton(onClick = onLoved)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                SuggestionChip(
                    onClick = { onMessage(preset) },
                    label = {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionButton(emoji: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 22.sp)
        }
    }
}

/** The special once-per-song "loved this" reaction. */
@Composable
private fun LovedButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "\uD83D\uDC9C", fontSize = 22.sp)
        }
    }
}

/** A preset message bubble; fades away after a few seconds. */
@Composable
private fun MessageBubble(
    msg: com.saurav.pixelmusic.data.session.ChatMessage,
    colors: ColorScheme
) {
    var visible by remember(msg.key) { mutableStateOf(true) }
    LaunchedEffect(msg.key) {
        delay(8_000)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut(),
        label = "chatBubble"
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 2.dp,
            color = colors.surfaceContainerHigh
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(msg.from)
                    }
                    append("  " + msg.text)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/** Floats received reactions upward, Instagram-live style. */
@Composable
private fun FloatingReactionsOverlay(
    events: List<com.saurav.pixelmusic.data.session.ReactionEvent>,
    modifier: Modifier = Modifier
) {
    val shownKeys = remember { mutableSetOf<String>() }
    val floating = remember { mutableStateListOf<com.saurav.pixelmusic.data.session.ReactionEvent>() }
    LaunchedEffect(events) {
        events.forEach { event ->
            if (shownKeys.add(event.key)) {
                floating.add(event)
            }
        }
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        floating.forEach { event ->
            key(event.key) {
                FloatingEmoji(
                    event = event,
                    onDone = { floating.remove(event) }
                )
            }
        }
    }
}

@Composable
private fun FloatingEmoji(
    event: com.saurav.pixelmusic.data.session.ReactionEvent,
    onDone: () -> Unit
) {
    val rise = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val xDrift = remember { (-70..70).random().toFloat() }
    LaunchedEffect(event.key) {
        launch {
            rise.animateTo(
                targetValue = -420f,
                animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(1200)
            alpha.animateTo(0f, tween(1200))
        }
        onDone()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = xDrift.dp, y = rise.value.dp)
            .alpha(alpha.value)
    ) {
        Text(
            text = event.emoji,
            fontSize = if (event.isLoved) 44.sp else 32.sp
        )
        Text(
            text = event.from,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
