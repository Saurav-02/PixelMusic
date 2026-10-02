package com.saurav.pixelmusic.presentation.components.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.dataStore
import com.saurav.pixelmusic.data.session.ChatMessage
import com.saurav.pixelmusic.data.session.ListenTogetherManager
import com.saurav.pixelmusic.data.session.ListenTogetherConnectionState
import com.saurav.pixelmusic.data.session.ListenTogetherUiState
import com.saurav.pixelmusic.data.session.ReactionEvent
import com.saurav.pixelmusic.data.session.SessionMember
import com.saurav.pixelmusic.data.session.SessionTrack
import com.saurav.pixelmusic.data.session.SongRequest
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private enum class PreSessionTab { HOST, JOIN }
private enum class SheetStage { SETUP, HOSTING, GUEST }

/**
 * Enhanced Listen Together bottom sheet with Material 3 ModalBottomSheet,
 * smooth stable transitions without flickering, responsive controls,
 * real-time member sync rings, and interactive social reactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSheet(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.listenTogetherUiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.listenTogetherConnectionState.collectAsStateWithLifecycle()
    val reactionEvents by viewModel.listenTogetherReactions.collectAsStateWithLifecycle()
    val chatMessages by viewModel.listenTogetherMessages.collectAsStateWithLifecycle()
    val requests by viewModel.listenTogetherRequests.collectAsStateWithLifecycle()
    val remoteState by viewModel.listenTogetherRemoteState.collectAsStateWithLifecycle()
    val syncDriftMs by viewModel.listenTogetherSyncDriftMs.collectAsStateWithLifecycle()
    val stablePlayerState by viewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSong = stablePlayerState.currentSong
    val pendingCode by viewModel.pendingListenTogetherCode.collectAsStateWithLifecycle()
    val currentUserAvatarUrl by viewModel.currentUserAvatarUrl.collectAsStateWithLifecycle()
    val currentYtUsername by viewModel.currentYtUsername.collectAsStateWithLifecycle()

    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // DataStore settings
    val dataStore = remember(context) { context.dataStore }
    val savedNameFlow = remember(dataStore) {
        dataStore.data.map { it[stringPreferencesKey("listen_together_user_name")] ?: "" }
    }
    val savedName by savedNameFlow.collectAsStateWithLifecycle(initialValue = "")

    val compactModeFlow = remember(dataStore) {
        dataStore.data.map { it[booleanPreferencesKey("listen_together_compact_members")] ?: false }
    }
    val compactMode by compactModeFlow.collectAsStateWithLifecycle(initialValue = false)

    val animatedReactionsFlow = remember(dataStore) {
        dataStore.data.map { it[booleanPreferencesKey("listen_together_animated_reactions")] ?: true }
    }
    val animatedReactions by animatedReactionsFlow.collectAsStateWithLifecycle(initialValue = true)

    val showSocialFlow = remember(dataStore) {
        dataStore.data.map { it[booleanPreferencesKey("listen_together_show_social")] ?: true }
    }
    val showSocial by showSocialFlow.collectAsStateWithLifecycle(initialValue = true)

    var selectedTab by remember { mutableStateOf(PreSessionTab.HOST) }
    var nameInput by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var requestText by remember { mutableStateOf("") }

    // When a deep link code is passed in, switch to JOIN mode and pre-fill code
    LaunchedEffect(pendingCode) {
        if (!pendingCode.isNullOrBlank()) {
            code = pendingCode.orEmpty().uppercase().take(7)
            selectedTab = PreSessionTab.JOIN
            viewModel.setPendingListenTogetherCode(null)
        }
    }

    val activeMessagesByUser = remember { mutableStateMapOf<String, ChatMessage>() }

    LaunchedEffect(chatMessages) {
        val now = System.currentTimeMillis()
        chatMessages.forEach { msg ->
            val fromKey = msg.from.lowercase().trim()
            val isFresh = msg.ts == 0L || (now - msg.ts) < 8_500L
            if (isFresh) {
                val existing = activeMessagesByUser[fromKey]
                if (existing == null || existing.key != msg.key || existing.ts < msg.ts) {
                    activeMessagesByUser[fromKey] = msg
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(800L)
            val now = System.currentTimeMillis()
            val expiredKeys = activeMessagesByUser.entries.filter { (_, msg) ->
                msg.ts > 0L && (now - msg.ts) >= 8_500L
            }.map { it.key }
            expiredKeys.forEach { activeMessagesByUser.remove(it) }
        }
    }

    fun persistUserName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            scope.launch {
                dataStore.edit { it[stringPreferencesKey("listen_together_user_name")] = trimmed }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isCreating = uiState is ListenTogetherUiState.Creating
    val isJoining = uiState is ListenTogetherUiState.Joining
    val isBusy = isCreating || isJoining

    val stage = when (uiState) {
        is ListenTogetherUiState.Hosting -> SheetStage.HOSTING
        is ListenTogetherUiState.Guest -> SheetStage.GUEST
        else -> SheetStage.SETUP
    }

    val effectiveName = if (isEditingName || (savedName.isBlank() && currentYtUsername.isBlank())) {
        nameInput
    } else {
        savedName.ifBlank { currentYtUsername }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surfaceContainerLow,
        tonalElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Crossfade(
                    targetState = stage,
                    animationSpec = tween(220),
                    label = "listenTogetherStageTransition"
                ) { currentStage ->
                    when (currentStage) {
                        SheetStage.SETUP -> {
                            SetupContent(
                                uiState = uiState,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                nameInput = nameInput,
                                onNameInputChange = { nameInput = it },
                                isEditingName = isEditingName,
                                onIsEditingNameChange = { isEditingName = it },
                                effectiveName = effectiveName,
                                code = code,
                                onCodeChange = { code = it },
                                currentUserAvatarUrl = currentUserAvatarUrl,
                                colors = colors,
                                isBusy = isBusy,
                                isCreating = isCreating,
                                isJoining = isJoining,
                                onStartHosting = {
                                    persistUserName(effectiveName)
                                    viewModel.startHostingSession(effectiveName)
                                },
                                onJoinSession = {
                                    persistUserName(effectiveName)
                                    viewModel.joinListenTogetherSession(code, effectiveName)
                                }
                            )
                        }
                        SheetStage.HOSTING -> {
                            val hosting = uiState as? ListenTogetherUiState.Hosting
                            if (hosting != null) {
                                HostingContent(
                                    hosting = hosting,
                                    connectionState = connectionState,
                                    currentSong = currentSong,
                                    requests = requests,
                                    requestText = requestText,
                                    onRequestTextChange = { requestText = it },
                                    activeMessagesByUser = activeMessagesByUser,
                                    compactMode = compactMode,
                                    showSocial = showSocial,
                                    remoteState = remoteState,
                                    viewModel = viewModel,
                                    colors = colors,
                                    context = context,
                                    hapticFeedback = hapticFeedback
                                )
                            }
                        }
                        SheetStage.GUEST -> {
                            val guest = uiState as? ListenTogetherUiState.Guest
                            if (guest != null) {
                                GuestContent(
                                    guest = guest,
                                    connectionState = connectionState,
                                    syncDriftMs = syncDriftMs,
                                    requests = requests,
                                    requestText = requestText,
                                    onRequestTextChange = { requestText = it },
                                    activeMessagesByUser = activeMessagesByUser,
                                    compactMode = compactMode,
                                    showSocial = showSocial,
                                    remoteState = remoteState,
                                    viewModel = viewModel,
                                    colors = colors,
                                    context = context,
                                    hapticFeedback = hapticFeedback
                                )
                            }
                        }
                    }
                }
            }

            // Floating reactions overlay
            if (animatedReactions && (stage == SheetStage.HOSTING || stage == SheetStage.GUEST)) {
                FloatingReactionsOverlay(
                    events = reactionEvents,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun SetupContent(
    uiState: ListenTogetherUiState,
    selectedTab: PreSessionTab,
    onTabSelected: (PreSessionTab) -> Unit,
    nameInput: String,
    onNameInputChange: (String) -> Unit,
    isEditingName: Boolean,
    onIsEditingNameChange: (Boolean) -> Unit,
    effectiveName: String,
    code: String,
    onCodeChange: (String) -> Unit,
    currentUserAvatarUrl: String,
    colors: ColorScheme,
    isBusy: Boolean,
    isCreating: Boolean,
    isJoining: Boolean,
    onStartHosting: () -> Unit,
    onJoinSession: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

        if (uiState is ListenTogetherUiState.Error) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.message,
                    color = colors.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        SegmentedModeSwitcher(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            colors = colors
        )

        if (effectiveName.isNotBlank() && !isEditingName) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (currentUserAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentUserAvatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = effectiveName.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = colors.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == PreSessionTab.HOST) "Hosting as $effectiveName" else "Joining as $effectiveName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Tap edit to change name",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            onNameInputChange(effectiveName)
                            onIsEditingNameChange(true)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit name",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameInputChange,
                label = {
                    Text(
                        if (selectedTab == PreSessionTab.HOST) stringResource(R.string.listen_together_your_name)
                        else stringResource(R.string.listen_together_guest_name)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = colors.onSurfaceVariant,
                    focusedBorderColor = colors.primary
                ),
                trailingIcon = if (effectiveName.isNotBlank() && isEditingName) {
                    {
                        IconButton(onClick = { onIsEditingNameChange(false) }) {
                            Icon(Icons.Rounded.Check, contentDescription = "Done", tint = colors.primary)
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (selectedTab == PreSessionTab.HOST) {
            Button(
                onClick = onStartHosting,
                enabled = effectiveName.isNotBlank() && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isCreating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = colors.onPrimary
                        )
                        Text(
                            text = stringResource(R.string.listen_together_creating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.listen_together_start),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.listen_together_room_code),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                SegmentedRoomCodeInput(
                    code = code,
                    onCodeChange = onCodeChange,
                    colors = colors
                )

                Button(
                    onClick = onJoinSession,
                    enabled = code.length == 7 && effectiveName.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isJoining) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = colors.onPrimary
                            )
                            Text(
                                text = stringResource(R.string.listen_together_joining),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.listen_together_join),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostingContent(
    hosting: ListenTogetherUiState.Hosting,
    connectionState: ListenTogetherConnectionState,
    currentSong: Song?,
    requests: List<SongRequest>,
    requestText: String,
    onRequestTextChange: (String) -> Unit,
    activeMessagesByUser: Map<String, ChatMessage>,
    compactMode: Boolean,
    showSocial: Boolean,
    remoteState: SessionTrack?,
    viewModel: PlayerViewModel,
    colors: ColorScheme,
    context: Context,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingLiveBadge(listenerCount = hosting.members.size, colors = colors)
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (connectionState) {
                    ListenTogetherConnectionState.CONNECTED -> "Sync live"
                    ListenTogetherConnectionState.RECONNECTING -> "Reconnecting…"
                    ListenTogetherConnectionState.CONNECTING -> "Connecting…"
                    ListenTogetherConnectionState.DISCONNECTED -> "Offline"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (connectionState) {
                    ListenTogetherConnectionState.CONNECTED -> colors.primary
                    ListenTogetherConnectionState.RECONNECTING -> colors.error
                    else -> colors.onSurfaceVariant
                }
            )
            Spacer(Modifier.weight(1f))
            RoomCodeChip(
                code = hosting.code,
                onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("room code", hosting.code))
                    viewModel.sendToast(context.getString(R.string.listen_together_code_copied))
                },
                colors = colors
            )
        }

        if (currentSong != null) {
            NowPlayingAnchorPreviewCard(song = currentSong, colors = colors)
        }

        if (hosting.members.size <= 1) {
            HeroCodeDisplayCard(
                code = hosting.code,
                colors = colors,
                context = context,
                onToast = viewModel::sendToast
            )

            val hostPhoto = hosting.members.firstOrNull()?.photoUrl ?: viewModel.currentUserAvatarUrl.collectAsStateWithLifecycle().value.ifBlank { null }
            Surface(
                shape = CircleShape,
                color = avatarColorFor(hosting.hostName),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(68.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!hostPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = hostPhoto,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = hosting.hostName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                }
            }

            Text(
                text = "Share the room code to start listening together",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = context.getString(R.string.listen_together_listeners, hosting.members.size),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant
            )

            if (compactMode) {
                CompactMemberList(
                    members = hosting.members,
                    activeMessagesByUser = activeMessagesByUser,
                    hostName = hosting.hostName,
                    targetVideoId = remoteState?.videoId,
                    colors = colors
                )
            } else {
                hosting.members.forEach { member ->
                    MemberRow(
                        member = member,
                        activeMessage = activeMessagesByUser[member.name.lowercase().trim()],
                        colors = colors,
                        isHost = member.name == hosting.hostName,
                        targetVideoId = remoteState?.videoId
                    )
                }
            }

            HostControlBar(
                isPlaying = remoteState?.isPlaying == true || viewModel.stablePlayerState.collectAsStateWithLifecycle().value.isPlaying,
                currentSong = currentSong,
                onPrevious = viewModel::previousSong,
                onPlayPause = viewModel::playPause,
                onNext = viewModel::nextSong,
                colors = colors
            )

            SongRequestsSection(
                requests = requests,
                requestText = requestText,
                onRequestTextChange = onRequestTextChange,
                onSubmit = {
                    viewModel.sendListenTogetherSongRequest(requestText)
                    onRequestTextChange("")
                },
                onVote = viewModel::voteListenTogetherRequest,
                isHost = true,
                onQueueNext = { req -> viewModel.queueNextListenTogetherRequest(req) },
                onDismissRequest = viewModel::dismissListenTogetherRequest,
                colors = colors
            )

            if (showSocial) {
                SocialSection(
                    onReaction = { viewModel.sendListenTogetherReaction(it) },
                    onLoved = { viewModel.sendLovedReaction() },
                    onMessage = { viewModel.sendListenTogetherMessage(it) },
                    colors = colors,
                    hapticFeedback = hapticFeedback
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.endListenTogetherSession() },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.error,
                contentColor = colors.onError
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = stringResource(R.string.listen_together_end_session),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuestContent(
    guest: ListenTogetherUiState.Guest,
    connectionState: ListenTogetherConnectionState,
    syncDriftMs: Long,
    requests: List<SongRequest>,
    requestText: String,
    onRequestTextChange: (String) -> Unit,
    activeMessagesByUser: Map<String, ChatMessage>,
    compactMode: Boolean,
    showSocial: Boolean,
    remoteState: SessionTrack?,
    viewModel: PlayerViewModel,
    colors: ColorScheme,
    context: Context,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingLiveBadge(listenerCount = guest.members.size, colors = colors)
            Spacer(Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.listen_together_listening_with, guest.hostName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.weight(1f))
            RoomCodeChip(
                code = guest.code,
                onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("room code", guest.code))
                    viewModel.sendToast(context.getString(R.string.listen_together_code_copied))
                },
                colors = colors
            )
        }

        if (connectionState != ListenTogetherConnectionState.CONNECTED) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (connectionState) {
                        ListenTogetherConnectionState.RECONNECTING -> "Reconnecting to the session…"
                        ListenTogetherConnectionState.CONNECTING -> "Connecting to the session…"
                        else -> "Session connection unavailable"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        GuestSyncActionBar(
            syncDriftMs = syncDriftMs,
            onSync = { viewModel.syncListenTogetherNow() },
            colors = colors
        )

        if (guest.members.isNotEmpty()) {
            Text(
                text = context.getString(R.string.listen_together_listeners, guest.members.size),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant
            )

            if (compactMode) {
                CompactMemberList(
                    members = guest.members,
                    activeMessagesByUser = activeMessagesByUser,
                    hostName = guest.hostName,
                    targetVideoId = remoteState?.videoId,
                    colors = colors
                )
            } else {
                guest.members.forEach { member ->
                    MemberRow(
                        member = member,
                        activeMessage = activeMessagesByUser[member.name.lowercase().trim()],
                        colors = colors,
                        isHost = member.name == guest.hostName,
                        targetVideoId = remoteState?.videoId
                    )
                }
            }

            SongRequestsSection(
                requests = requests,
                requestText = requestText,
                onRequestTextChange = onRequestTextChange,
                onSubmit = {
                    viewModel.sendListenTogetherSongRequest(requestText)
                    onRequestTextChange("")
                },
                onVote = viewModel::voteListenTogetherRequest,
                isHost = false,
                onQueueNext = {},
                onDismissRequest = {},
                colors = colors
            )

            if (showSocial) {
                SocialSection(
                    onReaction = { viewModel.sendListenTogetherReaction(it) },
                    onLoved = { viewModel.sendLovedReaction() },
                    onMessage = { viewModel.sendListenTogetherMessage(it) },
                    colors = colors,
                    hapticFeedback = hapticFeedback
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { viewModel.leaveListenTogetherSession() },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = stringResource(R.string.listen_together_leave),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Segmented Mode Switcher: [ Host Session | Join Room ] */
@Composable
private fun SegmentedModeSwitcher(
    selectedTab: PreSessionTab,
    onTabSelected: (PreSessionTab) -> Unit,
    colors: ColorScheme
) {
    val hostBg by animateColorAsState(
        if (selectedTab == PreSessionTab.HOST) colors.primary else Color.Transparent,
        tween(250),
        label = "hostBg"
    )
    val hostTextColor by animateColorAsState(
        if (selectedTab == PreSessionTab.HOST) colors.onPrimary else colors.onSurfaceVariant,
        tween(250),
        label = "hostText"
    )
    val joinBg by animateColorAsState(
        if (selectedTab == PreSessionTab.JOIN) colors.primary else Color.Transparent,
        tween(250),
        label = "joinBg"
    )
    val joinTextColor by animateColorAsState(
        if (selectedTab == PreSessionTab.JOIN) colors.onPrimary else colors.onSurfaceVariant,
        tween(250),
        label = "joinText"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = colors.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(hostBg)
                    .clickable { onTabSelected(PreSessionTab.HOST) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Host Session",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = hostTextColor
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(joinBg)
                    .clickable { onTabSelected(PreSessionTab.JOIN) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Join Room",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = joinTextColor
                )
            }
        }
    }
}

/** 7-character segmented PIN-style room code input */
@Composable
private fun SegmentedRoomCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    colors: ColorScheme
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 7) {
                val char = code.getOrNull(i)?.toString() ?: ""
                val isFocused = code.length == i || (i == 6 && code.length == 7)
                val borderColor by animateColorAsState(
                    if (isFocused) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
                    animationSpec = tween(200),
                    label = "boxBorder_$i"
                )
                val boxScale by animateFloatAsState(
                    if (isFocused) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "boxScale_$i"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (char.isNotEmpty()) colors.surfaceContainerHighest else colors.surfaceContainerLow,
                    border = BorderStroke(if (isFocused) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = boxScale
                            scaleY = boxScale
                        }
                        .size(width = 40.dp, height = 54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (char.isNotEmpty()) {
                            Text(
                                text = char,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = GoogleSansRounded,
                                color = colors.primary
                            )
                        } else if (isFocused) {
                            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                            val cursorAlpha by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "cursorAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .alpha(cursorAlpha)
                                    .background(colors.primary, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        BasicTextField(
            value = code,
            onValueChange = { newText ->
                val filtered = newText.uppercase().filter { c -> c in 'A'..'Z' || c in '0'..'9' }.take(7)
                onCodeChange(filtered)
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            textStyle = TextStyle(color = Color.Transparent),
            modifier = Modifier
                .matchParentSize()
                .alpha(0.01f)
                .focusRequester(focusRequester)
        )
    }
}

/** Now Playing Anchor Preview at top of sheet */
@Composable
private fun NowPlayingAnchorPreviewCard(
    song: Song?,
    colors: ColorScheme
) {
    if (song == null) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = song.title,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CURRENT TRACK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = colors.primary
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface
                )
                Text(
                    text = song.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Hero Code Display Card with glow, 1-tap copy checkmark feedback, and share */
@Composable
private fun HeroCodeDisplayCard(
    code: String,
    colors: ColorScheme,
    context: Context,
    onToast: (String) -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 4.dp,
        border = BorderStroke(1.5.dp, colors.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.listen_together_share_code),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant
            )
            Text(
                text = code,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                fontFamily = GoogleSansRounded,
                color = colors.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("room code", code))
                        onToast(context.getString(R.string.listen_together_code_copied))
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copied) "Copied!" else stringResource(R.string.listen_together_copy_code))
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my Listen Together session on Pixel Music! Room Code: $code"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Listen Together Invite"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

/** Breathing Live Badge with pulsating beacon dot (slowed down for smooth presence) */
@Composable
private fun BreathingLiveBadge(
    listenerCount: Int,
    colors: ColorScheme
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathingBeacon")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = colors.primaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            scaleX = beaconPulse
                            scaleY = beaconPulse
                        }
                        .alpha(beaconAlpha)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
            Text(
                text = "LIVE • $listenerCount/${ListenTogetherManager.MAX_SESSION_MEMBERS}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer
            )
        }
    }
}

/** Compact horizontal strip for member list when compact mode is active */
@Composable
private fun CompactMemberList(
    members: List<SessionMember>,
    activeMessagesByUser: Map<String, ChatMessage>,
    hostName: String,
    targetVideoId: String?,
    colors: ColorScheme
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(550, easing = FastOutSlowInEasing)),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        items(members, key = { it.name }) { member ->
            val isHost = member.name == hostName
            val isSynced = member.isLive && (!member.syncVideoId.isNullOrBlank() && (targetVideoId == null || member.syncVideoId == targetVideoId))
            val isBuffering = member.isLive && targetVideoId != null && member.syncVideoId != targetVideoId
            val borderColor = when {
                isSynced -> colors.primary
                isBuffering -> Color(0xFFFFB300)
                else -> colors.outlineVariant
            }
            val userMsg = activeMessagesByUser[member.name.lowercase().trim()]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.animateContentSize(animationSpec = tween(550, easing = FastOutSlowInEasing))
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, borderColor, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(avatarColorFor(member.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!member.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = member.photoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = member.name.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    if (isHost) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(15.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Host",
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface
                )
                AnimatedVisibility(
                    visible = userMsg != null,
                    enter = slideInVertically(
                        animationSpec = tween(550, easing = FastOutSlowInEasing),
                        initialOffsetY = { -it }
                    ) + expandVertically(
                        animationSpec = tween(550, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(450)),
                    exit = slideOutVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                        targetOffsetY = { -it }
                    ) + shrinkVertically(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(400))
                ) {
                    if (userMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceContainerHigh,
                            border = BorderStroke(0.5.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                            tonalElevation = 2.dp,
                            modifier = Modifier.widthIn(max = 90.dp)
                        ) {
                            Text(
                                text = userMsg.text,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One guest card with avatar border sync ring, vector crown badge, and smooth animated speech bubble */
@Composable
private fun MemberRow(
    member: SessionMember,
    activeMessage: ChatMessage?,
    colors: ColorScheme,
    isHost: Boolean = false,
    targetVideoId: String? = null
) {
    val hostLabel = stringResource(R.string.listen_together_host)

    val isSynced = member.isLive && (!member.syncVideoId.isNullOrBlank() && (targetVideoId == null || member.syncVideoId == targetVideoId))
    val isBuffering = member.isLive && targetVideoId != null && member.syncVideoId != targetVideoId
    val isOffline = !member.isLive

    val borderColor = when {
        isSynced -> colors.primary
        isBuffering -> Color(0xFFFFB300)
        else -> colors.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(550, easing = FastOutSlowInEasing)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, borderColor, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(avatarColorFor(member.name)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!member.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = member.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
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
                if (isHost) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = hostLabel,
                            modifier = Modifier.padding(2.5.dp)
                        )
                    }
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
                    text = when {
                        isOffline -> stringResource(R.string.listen_together_reconnecting)
                        member.syncVideoId.isNullOrBlank() -> "Waiting for sync…"
                        isBuffering -> "Buffering track…"
                        else -> "Synced · live"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isOffline -> colors.error
                        isBuffering -> Color(0xFFFFB300)
                        else -> colors.primary
                    }
                )
            }

            if (isSynced) {
                MiniEqualizerBars(color = colors.primary)
            }
        }

        // Overlay speech bubble with slow, smooth slide up/down and expand/shrink animation
        AnimatedVisibility(
            visible = activeMessage != null,
            enter = slideInVertically(
                animationSpec = tween(550, easing = FastOutSlowInEasing),
                initialOffsetY = { -it / 2 }
            ) + expandVertically(
                animationSpec = tween(550, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) + fadeIn(
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 2 }
            ) + shrinkVertically(
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        ) {
            if (activeMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .padding(start = 56.dp, top = 2.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDCAC",
                            fontSize = 14.sp
                        )
                        Text(
                            text = activeMessage.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** Mini 3-bar animated equalizer with calm, slowed rhythm */
@Composable
private fun MiniEqualizerBars(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniEq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(16.dp), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.width(2.5.dp).height(h1.dp).clip(RoundedCornerShape(1.dp)).background(color))
        }
        Box(modifier = Modifier.width(2.5.dp).height(16.dp), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.width(2.5.dp).height(h2.dp).clip(RoundedCornerShape(1.dp)).background(color))
        }
        Box(modifier = Modifier.width(2.5.dp).height(16.dp), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.width(2.5.dp).height(h3.dp).clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}

/** Compact host-only transport card with track progress and dynamic play/pause */
@Composable
private fun HostControlBar(
    isPlaying: Boolean,
    currentSong: Song?,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    colors: ColorScheme
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No track playing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                    Text(
                        text = currentSong?.displayArtist ?: "Host playback controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) + scaleIn() togetherWith
                                fadeOut(animationSpec = tween(150)) + scaleOut()
                        },
                        label = "playPauseToggle"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/** Guest sync action bar with spinning sync icon and micro-interaction pill */
@Composable
private fun GuestSyncActionBar(
    syncDriftMs: Long,
    onSync: () -> Unit,
    colors: ColorScheme
) {
    var isSpinning by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isSpinning) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        finishedListener = { isSpinning = false },
        label = "syncRotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = {
                isSpinning = true
                onSync()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
            Spacer(Modifier.width(6.dp))
            Text("Sync Now")
        }
        SyncStatusPill(driftMs = syncDriftMs, colors = colors)
    }
}

/** Small live drift indicator with checkmark micro-interaction */
@Composable
private fun SyncStatusPill(driftMs: Long, colors: ColorScheme) {
    val inSync = driftMs <= 250L
    val label = when {
        inSync -> "In sync"
        driftMs <= 750L -> "~${driftMs}ms"
        driftMs <= 2_000L -> "${driftMs / 1000.0}s behind"
        else -> "Needs sync"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (inSync) colors.primaryContainer else colors.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (inSync) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (inSync) colors.onPrimaryContainer else colors.onTertiaryContainer
            )
        }
    }
}

/** Song-request queue with animated upvote pill and host Queue Next button */
@Composable
private fun SongRequestsSection(
    requests: List<SongRequest>,
    requestText: String,
    onRequestTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVote: (String) -> Unit,
    isHost: Boolean,
    onQueueNext: (SongRequest) -> Unit,
    onDismissRequest: (String) -> Unit,
    colors: ColorScheme
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isHost) "Song requests" else "Request a song",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = requestText,
                onValueChange = onRequestTextChange,
                placeholder = { Text("Song or artist…") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onSubmit,
                enabled = requestText.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Request")
            }
        }
        requests.forEach { request ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "From ${request.from}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (request.votedByMe) colors.primaryContainer else colors.surfaceContainerHighest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onVote(request.key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (request.votedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Vote",
                                tint = if (request.votedByMe) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            AnimatedContent(
                                targetState = request.votes,
                                transitionSpec = {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                                },
                                label = "voteCounter"
                            ) { count ->
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (request.votedByMe) colors.onPrimaryContainer else colors.onSurface
                                )
                            }
                        }
                    }

                    if (isHost) {
                        Button(
                            onClick = { onQueueNext(request) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Queue Next", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onDismissRequest(request.key) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Done", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Emoji reaction bar + preset message chips with bouncy spring effect */
@Composable
private fun SocialSection(
    onReaction: (String) -> Unit,
    onLoved: () -> Unit,
    onMessage: (String) -> Unit,
    colors: ColorScheme,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val presets = stringArrayResource(R.array.listen_together_preset_messages)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("\u2764\uFE0F", "\uD83D\uDD25", "\uD83D\uDE2E", "\uD83D\uDC4F").forEach { emoji ->
                BouncyReactionButton(
                    emoji = emoji,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReaction(emoji)
                    }
                )
            }
            BouncyLovedButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLoved()
                }
            )
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
private fun BouncyReactionButton(emoji: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { isPressed = false },
        label = "reactionBounce"
    )

    Surface(
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable {
                isPressed = true
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 22.sp)
        }
    }
}

/** The special once-per-song "loved this" reaction button with spring bounce */
@Composable
private fun BouncyLovedButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { isPressed = false },
        label = "lovedBounce"
    )

    Surface(
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable {
                isPressed = true
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "\uD83D\uDC9C", fontSize = 22.sp)
        }
    }
}

/** Floats received reactions upward inside frosted pills with natural curved drift */
@Composable
private fun FloatingReactionsOverlay(
    events: List<ReactionEvent>,
    modifier: Modifier = Modifier
) {
    val shownKeys = remember { mutableSetOf<String>() }
    val floating = remember { mutableStateListOf<ReactionEvent>() }
    LaunchedEffect(events) {
        val now = System.currentTimeMillis()
        events.forEach { event ->
            if (shownKeys.add(event.key)) {
                if (event.ts == 0L || now - event.ts < 4000L) {
                    floating.add(event)
                }
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
    event: ReactionEvent,
    onDone: () -> Unit
) {
    val rise = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val xSpread = remember(event.key) { (-60..60).random().toFloat() }

    LaunchedEffect(event.key) {
        coroutineScope {
            launch {
                rise.animateTo(
                    targetValue = -380f,
                    animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(1200)
                alpha.animateTo(0f, tween(1200))
            }
        }
        onDone()
    }

    val progress = (-rise.value / 380f).coerceIn(0f, 1f)
    val curvedX = xSpread * sin(progress * PI.toFloat())

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .offset(x = curvedX.dp, y = rise.value.dp)
            .alpha(alpha.value)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.emoji,
                fontSize = if (event.isLoved) 28.sp else 22.sp
            )
            Text(
                text = event.from,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
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

/** Room code chip with copy trigger */
@Composable
private fun RoomCodeChip(
    code: String,
    onCopy: () -> Unit,
    colors: ColorScheme
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier.clickable(onClick = onCopy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = colors.onSurface
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(R.string.listen_together_copy_code),
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
