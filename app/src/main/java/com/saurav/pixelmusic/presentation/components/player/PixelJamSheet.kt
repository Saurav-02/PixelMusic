package com.saurav.pixelmusic.presentation.components.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.session.PixelJamChatMessage
import com.saurav.pixelmusic.data.session.PixelJamConnectionState
import com.saurav.pixelmusic.data.session.PixelJamManager
import com.saurav.pixelmusic.data.session.PixelJamParticipant
import com.saurav.pixelmusic.data.session.PixelJamPlaybackState
import com.saurav.pixelmusic.data.session.PixelJamPublicRoom
import com.saurav.pixelmusic.data.session.PixelJamRoomMode
import com.saurav.pixelmusic.data.session.PixelJamSuggestion
import com.saurav.pixelmusic.data.session.PixelJamTrack
import com.saurav.pixelmusic.data.session.PixelJamUiState
import com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.max

private enum class LobbyTab { HOST, JOIN }
private enum class RoomTab { PEOPLE, CHAT, QUEUE, SUGGEST }

private fun expressiveCardShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelJamSheet(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val uiState by viewModel.pixelJamUiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.pixelJamConnectionState.collectAsStateWithLifecycle()
    val playbackState by viewModel.pixelJamPlaybackState.collectAsStateWithLifecycle()
    val sharedQueue by viewModel.pixelJamSharedQueue.collectAsStateWithLifecycle()
    val chatMessages by viewModel.pixelJamChatMessages.collectAsStateWithLifecycle()
    val suggestions by viewModel.pixelJamSuggestions.collectAsStateWithLifecycle()
    val publicRooms by viewModel.pixelJamPublicRooms.collectAsStateWithLifecycle()
    val cooldownUntilMs by viewModel.pixelJamCooldownUntilMs.collectAsStateWithLifecycle()
    val rttMs by viewModel.pixelJamRttMs.collectAsStateWithLifecycle()
    val syncDriftMs by viewModel.pixelJamSyncDriftMs.collectAsStateWithLifecycle()
    val pendingCode by viewModel.pendingPixelJamCode.collectAsStateWithLifecycle()
    val ytUsername by viewModel.currentYtUsername.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val userAvatarUrl by viewModel.currentUserLocalAvatar.collectAsStateWithLifecycle()

    val savedName by viewModel.pixelJamSavedDisplayName.collectAsStateWithLifecycle()

    var displayName by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf("") }
    var selectedLobbyTab by remember { mutableStateOf(LobbyTab.HOST) }
    var selectedRoomMode by remember { mutableStateOf(PixelJamRoomMode.HOST_ONLY) }

    LaunchedEffect(Unit) {
        viewModel.refreshPixelJamPublicRooms()
    }

    LaunchedEffect(userProfile.displayName, savedName, ytUsername) {
        if (displayName.isBlank()) {
            displayName = userProfile.displayName.ifBlank { savedName }.ifBlank { ytUsername }
        }
    }

    LaunchedEffect(pendingCode) {
        val code = pendingCode
        if (!code.isNullOrBlank()) {
            roomCodeInput = code.trim().uppercase()
            selectedLobbyTab = LobbyTab.JOIN
            viewModel.setPendingPixelJamCode(null)
        }
    }

    fun persistDisplayName(name: String) {
        viewModel.savePixelJamDisplayName(name)
    }

    SmoothModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        val smoothDismiss = LocalSmoothDismiss.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            val inRoom = uiState as? PixelJamUiState.InRoom
            AnimatedContent(
                targetState = inRoom,
                contentKey = { it != null },
                transitionSpec = {
                    if (targetState != null) {
                        (fadeIn(tween(260)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it / 8 } + scaleIn(initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
                    } else {
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically { it / 8 })
                    }
                },
                label = "pixelJamStageTransition"
            ) { currentRoom ->
                if (currentRoom != null) {
                    PixelJamRoomStage(
                        room = currentRoom,
                        myLocalAvatar = userAvatarUrl,
                        myAvatarCacheKey = userProfile.updatedAt,
                        connectionState = connectionState,
                        playbackState = playbackState,
                        sharedQueue = sharedQueue,
                        chatMessages = chatMessages,
                        suggestions = suggestions,
                        cooldownUntilMs = cooldownUntilMs,
                        rttMs = rttMs,
                        syncDriftMs = syncDriftMs,
                        inviteUrl = viewModel.getPixelJamInviteUrl(),
                        onCopyCode = { code ->
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("PixelJam Code", code))
                            viewModel.sendToast("Room code $code copied")
                        },
                        onShareInvite = { url, code ->
                            val shareText = "Join my PixelJam room ($code) on PixelMusic: $url"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PixelJam"))
                        },
                        onToggleRoomMode = { newMode -> viewModel.changePixelJamRoomMode(newMode) },
                        onGrantCoHost = { uid -> viewModel.grantPixelJamCoHost(uid) },
                        onRevokeCoHost = { uid -> viewModel.revokePixelJamCoHost(uid) },
                        onSendChat = { text, replyId -> viewModel.sendPixelJamChat(text, replyId) },
                        onSuggestTrack = { query -> viewModel.suggestPixelJamTrack(query) },
                        onApproveSuggestion = { id -> viewModel.approvePixelJamSuggestion(id) },
                        onRejectSuggestion = { id -> viewModel.rejectPixelJamSuggestion(id) },
                        onResync = { viewModel.syncPixelJamNow() },
                        onLeave = {
                            smoothDismiss {
                                viewModel.leavePixelJamRoom()
                            }
                        }
                    )
                } else {
                    PixelJamLobbyStage(
                        uiState = uiState,
                        publicRooms = publicRooms,
                        userAvatarUrl = userAvatarUrl,
                        userAvatarCacheKey = userProfile.updatedAt,
                        selectedTab = selectedLobbyTab,
                        onSelectTab = { selectedLobbyTab = it },
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it },
                        selectedMode = selectedRoomMode,
                        onSelectMode = { selectedRoomMode = it },
                        roomCodeInput = roomCodeInput,
                        onRoomCodeChange = { roomCodeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
                        onStartRoom = {
                            val effectiveName = displayName.trim().ifBlank { "DJ" }
                            persistDisplayName(effectiveName)
                            viewModel.createPixelJamRoom(effectiveName, selectedRoomMode)
                        },
                        onJoinRoom = { code ->
                            val effectiveName = displayName.trim().ifBlank { "Listener" }
                            persistDisplayName(effectiveName)
                            viewModel.joinPixelJamRoom(code, effectiveName)
                        },
                        onDismissError = { viewModel.dismissPixelJamError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelJamLobbyStage(
    uiState: PixelJamUiState,
    publicRooms: List<PixelJamPublicRoom>,
    userAvatarUrl: String,
    userAvatarCacheKey: Long,
    selectedTab: LobbyTab,
    onSelectTab: (LobbyTab) -> Unit,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    selectedMode: PixelJamRoomMode,
    onSelectMode: (PixelJamRoomMode) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    onStartRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isBusy = uiState is PixelJamUiState.Creating || uiState is PixelJamUiState.Joining
    val pulseTransition = rememberInfiniteTransition(label = "pixelJamHeroPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expressive Hero Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.primaryContainer.copy(alpha = 0.55f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primaryContainer.copy(alpha = 0.85f),
                                colors.tertiaryContainer.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                            contentDescription = "PixelMusic",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PIXELJAM",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = colors.onPrimaryContainer
                            )
                            Surface(
                                shape = CircleShape,
                                color = colors.primary.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(colors.primary)
                                    )
                                    Text(
                                        text = "SYNC",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Real-time synced rooms, shared queues & live chat in PixelMusic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Error Banner
        AnimatedVisibility(
            visible = uiState is PixelJamUiState.Error,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val err = (uiState as? PixelJamUiState.Error)?.message.orEmpty()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = colors.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colors.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = colors.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Always-On Rooms (Expressive Grouped Cards)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ALWAYS-ON ROOMS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                publicRooms.forEachIndexed { index, room ->
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (pressed) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
                        label = "publicRoomScale"
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clip(expressiveCardShape(index, publicRooms.size))
                            .clickable(enabled = !isBusy, interactionSource = interaction, indication = null) {
                                onJoinRoom(room.code)
                            },
                        shape = expressiveCardShape(index, publicRooms.size),
                        color = colors.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (room.code.uppercase()) {
                                        "INDIA" -> "\uD83C\uDDEE\uD83C\uDDF3"
                                        "WORLD" -> "\uD83C\uDF0D"
                                        else -> "\uD83C\uDFB5"
                                    },
                                    fontSize = 22.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onSurface
                                )
                                Text(
                                    text = if (room.participants > 0) {
                                        "24/7 Live Radio · ${room.participants} listening"
                                    } else {
                                        "24/7 Live Radio · Always playing"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = "Join",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expressive Segmented Mode Switch (Host vs Join)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = colors.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LobbyTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val bg by animateColorAsState(
                        targetValue = if (selected) colors.primary else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBg"
                    )
                    val fg by animateColorAsState(
                        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabFg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .clickable(enabled = !isBusy) { onSelectTab(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab == LobbyTab.HOST) "Host Room" else "Join with Code",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = fg
                        )
                    }
                }
            }
        }

        // Display Name Input
        OutlinedTextField(
            value = displayName,
            onValueChange = { onDisplayNameChange(it.take(32)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
            singleLine = true,
            label = { Text("Display name") },
            placeholder = { Text("How friends see you") },
            leadingIcon = {
                com.saurav.pixelmusic.presentation.components.ProfileAvatar(
                    avatarSpec = userAvatarUrl,
                    displayName = displayName.ifBlank { "P" },
                    size = 32.dp,
                    cacheKey = userAvatarCacheKey,
                    fallbackInitial = "P"
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainerHigh,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Animated Tab Body (Host vs Join)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val dir = if (targetState == LobbyTab.JOIN) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { dir * it / 5 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally { -dir * it / 5 })
            },
            label = "lobbyTabBody"
        ) { tab ->
            if (tab == LobbyTab.HOST) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Expressive Room Mode Selector Cards
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        val modes = listOf(
                            Triple(
                                PixelJamRoomMode.HOST_ONLY,
                                "Host Only",
                                "You & your co-host control playback; guests can suggest tracks"
                            ),
                            Triple(
                                PixelJamRoomMode.COLLABORATIVE,
                                "Collaborative",
                                "Everyone in the room can play, pause, seek & change tracks"
                            )
                        )
                        modes.forEachIndexed { idx, (mode, title, subtitle) ->
                            val chosen = mode == selectedMode
                            val cardBg by animateColorAsState(
                                targetValue = if (chosen) colors.primaryContainer else colors.surfaceContainerHigh,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "modeCardBg"
                            )
                            val titleColor by animateColorAsState(
                                targetValue = if (chosen) colors.onPrimaryContainer else colors.onSurface,
                                label = "modeTitleColor"
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(expressiveCardShape(idx, modes.size))
                                    .clickable(enabled = !isBusy) { onSelectMode(mode) },
                                shape = expressiveCardShape(idx, modes.size),
                                color = cardBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = titleColor
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = titleColor.copy(alpha = 0.8f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (chosen) colors.primary else colors.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (chosen) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = colors.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onStartRoom,
                        enabled = !isBusy && displayName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape
                    ) {
                        if (uiState is PixelJamUiState.Creating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = colors.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Starting PixelJam…", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Start PixelJam", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = onRoomCodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy,
                        singleLine = true,
                        label = { Text("Room code") },
                        placeholder = { Text("e.g. PIXEL8") },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (roomCodeInput.length >= 3 && displayName.isNotBlank()) {
                                    onJoinRoom(roomCodeInput)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainerHigh,
                            unfocusedContainerColor = colors.surfaceContainerHigh,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Button(
                        onClick = { onJoinRoom(roomCodeInput) },
                        enabled = !isBusy && roomCodeInput.length >= 3 && displayName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape
                    ) {
                        if (uiState is PixelJamUiState.Joining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = colors.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Joining ${uiState.code}…", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Join PixelJam", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelJamRoomStage(
    room: PixelJamUiState.InRoom,
    myLocalAvatar: String,
    myAvatarCacheKey: Long,
    connectionState: PixelJamConnectionState,
    playbackState: PixelJamPlaybackState?,
    sharedQueue: List<PixelJamTrack>,
    chatMessages: List<PixelJamChatMessage>,
    suggestions: List<PixelJamSuggestion>,
    cooldownUntilMs: Long,
    rttMs: Long,
    syncDriftMs: Long,
    inviteUrl: String?,
    onCopyCode: (String) -> Unit,
    onShareInvite: (String, String) -> Unit,
    onToggleRoomMode: (PixelJamRoomMode) -> Unit,
    onGrantCoHost: (String) -> Unit,
    onRevokeCoHost: (String) -> Unit,
    onSendChat: (String, String?) -> Unit,
    onSuggestTrack: (String) -> Unit,
    onApproveSuggestion: (String) -> Unit,
    onRejectSuggestion: (String) -> Unit,
    onResync: () -> Unit,
    onLeave: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(RoomTab.PEOPLE) }
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(1800)
            copiedFeedback = false
        }
    }

    var cooldownRemainingSec by remember { mutableStateOf(0) }
    LaunchedEffect(cooldownUntilMs) {
        while (cooldownUntilMs > System.currentTimeMillis()) {
            cooldownRemainingSec = max(0, ((cooldownUntilMs - System.currentTimeMillis()) / 1000L).toInt())
            delay(500)
        }
        cooldownRemainingSec = 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Expressive Live Room Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.primaryContainer.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dotColor = when (connectionState) {
                            PixelJamConnectionState.CONNECTED -> colors.primary
                            PixelJamConnectionState.RECONNECTING, PixelJamConnectionState.CONNECTING -> colors.tertiary
                            PixelJamConnectionState.DISCONNECTED -> colors.error
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = when (connectionState) {
                                PixelJamConnectionState.CONNECTED ->
                                    if (rttMs > 0) "PIXELJAM LIVE · ${rttMs}ms" else "PIXELJAM LIVE"
                                PixelJamConnectionState.RECONNECTING -> "RECONNECTING…"
                                PixelJamConnectionState.CONNECTING -> "CONNECTING…"
                                PixelJamConnectionState.DISCONNECTED -> "OFFLINE"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = colors.onPrimaryContainer
                        )
                    }

                    // Mode pill (24/7 Live Radio for stations, tappable by host in private rooms)
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = room.isHost && !room.permanent && !room.isRadio) {
                                val next = if (room.roomMode == PixelJamRoomMode.HOST_ONLY) {
                                    PixelJamRoomMode.COLLABORATIVE
                                } else {
                                    PixelJamRoomMode.HOST_ONLY
                                }
                                onToggleRoomMode(next)
                            },
                        shape = CircleShape,
                        color = colors.surface.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (room.isRadio) "24/7 Radio" else room.roomMode.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                copiedFeedback = true
                                onCopyCode(room.roomCode)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        if (room.roomName.isNotBlank()) {
                            Text(
                                text = room.roomName,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp
                                ),
                                color = colors.onPrimaryContainer
                            )
                            AnimatedVisibility(visible = copiedFeedback) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Copied",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = if (copiedFeedback) "Copied to clipboard" else "Tap code to copy · ${room.participants.size} in room",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onResync,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surface.copy(alpha = 0.65f),
                                contentColor = colors.onSurface
                            )
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Resync playback")
                        }
                        if (inviteUrl != null) {
                            FilledTonalIconButton(
                                onClick = { onShareInvite(inviteUrl, room.roomCode) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                )
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share invite link")
                            }
                        }
                    }
                }
            }
        }

        // Synced Track Strip
        val track = playbackState?.currentTrack
        AnimatedVisibility(visible = track != null || cooldownRemainingSec > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (track != null && track.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_music_note_24),
                                contentDescription = null,
                                tint = colors.onSecondaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Waiting for a track…",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                cooldownRemainingSec > 0 -> "Cooldown: ${cooldownRemainingSec}s"
                                track != null -> track.artist.ifBlank { "Synced playback" }
                                else -> "Play any song to sync the room"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (syncDriftMs < 1_000L) colors.primaryContainer else colors.tertiaryContainer
                    ) {
                        Text(
                            text = if (syncDriftMs < 1_000L) "In sync" else "${syncDriftMs}ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (syncDriftMs < 1_000L) colors.onPrimaryContainer else colors.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Expressive Room Section Tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = colors.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val tabs = listOf(
                    RoomTab.PEOPLE to "People (${room.participants.size})",
                    RoomTab.CHAT to if (chatMessages.isNotEmpty()) "Chat (${chatMessages.size})" else "Chat",
                    RoomTab.QUEUE to "Queue",
                    RoomTab.SUGGEST to if (suggestions.isNotEmpty()) "Pick (${suggestions.size})" else "Suggest"
                )
                tabs.forEach { (tab, label) ->
                    val selected = tab == selectedTab
                    val bg by animateColorAsState(
                        targetValue = if (selected) colors.primary else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "roomTabBg"
                    )
                    val fg by animateColorAsState(
                        targetValue = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        label = "roomTabFg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tab Body with Smooth Expressive Transitions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp, max = 320.dp)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(tween(200)) + slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { dir * it / 6 }) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally { -dir * it / 6 })
                },
                label = "roomTabContent"
            ) { tab ->
                when (tab) {
                    RoomTab.PEOPLE -> PeopleTabContent(
                        room = room,
                        myLocalAvatar = myLocalAvatar,
                        myAvatarCacheKey = myAvatarCacheKey,
                        onGrantCoHost = onGrantCoHost,
                        onRevokeCoHost = onRevokeCoHost
                    )
                    RoomTab.CHAT -> ChatTabContent(
                        myUserId = room.userId,
                        myDisplayName = room.displayName,
                        myLocalAvatar = myLocalAvatar,
                        myAvatarCacheKey = myAvatarCacheKey,
                        messages = chatMessages,
                        onSendChat = onSendChat
                    )
                    RoomTab.QUEUE -> QueueTabContent(queue = sharedQueue)
                    RoomTab.SUGGEST -> if (room.isRadio) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "24/7 Live Radio — playing nonstop with no host.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    } else {
                        SuggestionsTabContent(
                            isHost = room.isHost,
                            suggestions = suggestions,
                            onSuggestTrack = onSuggestTrack,
                            onApprove = onApproveSuggestion,
                            onReject = onRejectSuggestion
                        )
                    }
                }
            }
        }

        // Leave Button
        FilledTonalButton(
            onClick = onLeave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.errorContainer,
                contentColor = colors.onErrorContainer
            )
        ) {
            Text(
                text = "Leave PixelJam",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun PeopleTabContent(
    room: PixelJamUiState.InRoom,
    myLocalAvatar: String,
    myAvatarCacheKey: Long,
    onGrantCoHost: (String) -> Unit,
    onRevokeCoHost: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        itemsIndexed(room.participants, key = { _, p -> p.userId }) { index, participant ->
            val isMe = participant.userId == room.userId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                shape = expressiveCardShape(index, room.participants.size),
                color = if (isMe) colors.secondaryContainer.copy(alpha = 0.6f) else colors.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val resolvedAvatar = if (isMe && myLocalAvatar.isNotBlank()) {
                        myLocalAvatar
                    } else {
                        participant.avatarUrl
                    }
                    com.saurav.pixelmusic.presentation.components.ProfileAvatar(
                        avatarSpec = resolvedAvatar,
                        displayName = participant.name,
                        size = 40.dp,
                        cacheKey = if (isMe) myAvatarCacheKey else 0L,
                        fallbackContainerColor = if (participant.isHost) colors.primary else colors.surfaceContainerHighest,
                        fallbackContentColor = if (participant.isHost) colors.onPrimary else colors.onSurface,
                        fallbackInitial = "P"
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMe) "${participant.name} (You)" else participant.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roleLabel = when {
                            participant.userId == room.hostId -> "Host"
                            participant.isCoHost -> "Co-Host"
                            else -> "Listener"
                        }
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (participant.isHost) colors.primary else colors.onSurfaceVariant
                        )
                    }
                    if (room.isMainHost && !isMe) {
                        if (participant.isCoHost) {
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onRevokeCoHost(participant.userId) },
                                shape = CircleShape,
                                color = colors.errorContainer
                            ) {
                                Text(
                                    text = "Revoke",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        } else if (room.coHostId.isBlank()) {
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onGrantCoHost(participant.userId) },
                                shape = CircleShape,
                                color = colors.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = colors.onPrimaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Co-Host",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTabContent(
    myUserId: String,
    myDisplayName: String,
    myLocalAvatar: String,
    myAvatarCacheKey: Long,
    messages: List<PixelJamChatMessage>,
    onSendChat: (String, String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<PixelJamChatMessage?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No messages yet. Say hello to the room!",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.userId == myUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!isMine) {
                            com.saurav.pixelmusic.presentation.components.ProfileAvatar(
                                avatarSpec = msg.avatarUrl,
                                displayName = msg.userName,
                                size = 28.dp,
                                fallbackContainerColor = if (msg.isHost) colors.primary else colors.surfaceContainerHighest,
                                fallbackContentColor = if (msg.isHost) colors.onPrimary else colors.onSurface,
                                fallbackInitial = "P"
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (isMine) 20.dp else 6.dp,
                                        bottomEnd = if (isMine) 6.dp else 20.dp
                                    )
                                )
                                .clickable { replyingTo = msg },
                            color = if (isMine) colors.primaryContainer else colors.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (!isMine) {
                                    Text(
                                        text = if (msg.isHost) "${msg.userName} · Host" else msg.userName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary
                                    )
                                }
                                msg.replyTo?.let { rep ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surface.copy(alpha = 0.45f)
                                    ) {
                                        Text(
                                            text = "${rep.userName}: ${rep.text}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMine) colors.onPrimaryContainer else colors.onSurface
                                )
                            }
                        }
                        if (isMine) {
                            Spacer(Modifier.width(8.dp))
                            com.saurav.pixelmusic.presentation.components.ProfileAvatar(
                                avatarSpec = myLocalAvatar.ifBlank { msg.avatarUrl },
                                displayName = msg.userName.ifBlank { myDisplayName },
                                size = 28.dp,
                                cacheKey = myAvatarCacheKey,
                                fallbackContainerColor = colors.primaryContainer,
                                fallbackContentColor = colors.onPrimaryContainer,
                                fallbackInitial = "P"
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = replyingTo != null) {
            replyingTo?.let { rep ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replying to ${rep.userName}: ${rep.text}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(500) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Message room…") },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            FilledTonalIconButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotBlank()) {
                        onSendChat(text, replyingTo?.id)
                        draft = ""
                        replyingTo = null
                    }
                },
                enabled = draft.isNotBlank(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
private fun QueueTabContent(queue: List<PixelJamTrack>) {
    val colors = MaterialTheme.colorScheme
    if (queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Shared up-next queue is empty.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        itemsIndexed(queue, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = expressiveCardShape(index, queue.size),
                color = colors.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (track.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_queue_music_24),
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist.ifBlank { "Up next" },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsTabContent(
    isHost: Boolean,
    suggestions: List<PixelJamSuggestion>,
    onSuggestTrack: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isHost) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Suggest a song to host…") },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerHigh,
                        unfocusedContainerColor = colors.surfaceContainerHigh,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                FilledTonalIconButton(
                    onClick = {
                        val q = query.trim()
                        if (q.isNotBlank()) {
                            onSuggestTrack(q)
                            query = ""
                        }
                    },
                    enabled = query.isNotBlank(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Suggest song")
                }
            }
        }

        if (suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHost) "No pending track suggestions." else "Type a song title & artist to suggest it to the host.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(suggestions, key = { _, s -> s.suggestionId }) { index, sug ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        shape = expressiveCardShape(index, suggestions.size),
                        color = colors.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sug.track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sug.track.artist} · from ${sug.fromUserName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isHost) {
                                IconButton(
                                    onClick = { onApprove(sug.suggestionId) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Approve", tint = colors.primary)
                                }
                                IconButton(
                                    onClick = { onReject(sug.suggestionId) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Reject", tint = colors.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
