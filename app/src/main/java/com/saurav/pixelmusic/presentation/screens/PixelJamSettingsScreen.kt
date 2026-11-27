package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.session.PixelJamConfig
import com.saurav.pixelmusic.data.session.PixelJamManager
import com.saurav.pixelmusic.data.session.PixelJamRoomMode
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.PixelToast
import com.saurav.pixelmusic.presentation.components.ProfileAvatar
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.ui.blur.LocalScreenGlassState
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.blur.rememberGlassState
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PixelJamSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val pixelJamManager: PixelJamManager,
    private val userPreferencesRepository: com.saurav.pixelmusic.data.preferences.UserPreferencesRepository,
    private val youtubeDatastoreRepository: com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository
) : ViewModel() {

    object Keys {
        val KEEP_ALIVE = booleanPreferencesKey("pixeljam_keep_alive")
    }

    val serverUrl: StateFlow<String> = pixelJamManager.serverUrl
    val notifyJoinLeave: StateFlow<Boolean> = pixelJamManager.notifyJoinLeave
    val notifyRoomAlerts: StateFlow<Boolean> = pixelJamManager.notifyRoomAlerts
    val autoQueueOnLeave: StateFlow<Boolean> = pixelJamManager.autoQueueOnLeave
    val defaultRoomMode: StateFlow<PixelJamRoomMode> = pixelJamManager.defaultRoomMode

    val userProfile: StateFlow<com.saurav.pixelmusic.data.model.UserProfile> =
        userPreferencesRepository.userProfileFlow
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                com.saurav.pixelmusic.data.model.UserProfile()
            )

    val ytUsername: StateFlow<String> = youtubeDatastoreRepository.ytUsername
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val ytAvatarUrl: StateFlow<String> = youtubeDatastoreRepository.ytAvatarUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val displayName: StateFlow<String> = dataStore.data
        .map { it[PixelJamManager.KEY_DISPLAY_NAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val keepAlive: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.KEEP_ALIVE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setServerUrl(url: String) {
        viewModelScope.launch { pixelJamManager.setServerUrl(url) }
    }

    fun resetServerUrl() {
        viewModelScope.launch { pixelJamManager.setServerUrl(PixelJamConfig.DEFAULT_SERVER_URL) }
    }

    fun setKeepAlive(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.KEEP_ALIVE] = value } }
    }

    fun setNotifyJoinLeave(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PixelJamManager.KEY_NOTIFY_JOIN_LEAVE] = value } }
    }

    fun setNotifyRoomAlerts(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PixelJamManager.KEY_NOTIFY_ROOM_ALERTS] = value } }
    }

    fun setAutoQueueOnLeave(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PixelJamManager.KEY_AUTO_QUEUE_ON_LEAVE] = value } }
    }

    fun setDefaultRoomMode(mode: PixelJamRoomMode) {
        viewModelScope.launch { dataStore.edit { it[PixelJamManager.KEY_DEFAULT_MODE] = mode.wireValue } }
    }
}

@Composable
fun PixelJamSettingsScreen(
    navController: NavController,
    vm: PixelJamSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val serverUrl by vm.serverUrl.collectAsStateWithLifecycle()
    val displayName by vm.displayName.collectAsStateWithLifecycle()
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val ytUsername by vm.ytUsername.collectAsStateWithLifecycle()
    val ytAvatarUrl by vm.ytAvatarUrl.collectAsStateWithLifecycle()
    val keepAlive by vm.keepAlive.collectAsStateWithLifecycle()
    val notifyJoinLeave by vm.notifyJoinLeave.collectAsStateWithLifecycle()
    val notifyRoomAlerts by vm.notifyRoomAlerts.collectAsStateWithLifecycle()
    val autoQueueOnLeave by vm.autoQueueOnLeave.collectAsStateWithLifecycle()
    val defaultRoomMode by vm.defaultRoomMode.collectAsStateWithLifecycle()

    val effectiveName = userProfile.effectiveDisplayName(ytUsername, displayName)
    val effectiveAvatar = userProfile.effectiveLocalAvatar(if (ytUsername.isNotEmpty()) ytAvatarUrl else "")
    val hasProfile = effectiveName.isNotBlank() || effectiveAvatar.isNotBlank()

    var showServerDialog by remember { mutableStateOf(false) }
    var serverDraft by remember(serverUrl) { mutableStateOf(serverUrl) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }
                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }
                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = colors.surfaceContainerHigh,
            title = { Text("PixelJam Server URL", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = serverDraft,
                    onValueChange = { serverDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Render / WebSocket Host") },
                    placeholder = { Text(PixelJamConfig.DEFAULT_SERVER_URL) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setServerUrl(serverDraft)
                    showServerDialog = false
                    PixelToast.show(context, "PixelJam server URL saved")
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.resetServerUrl()
                    showServerDialog = false
                    PixelToast.show(context, "Reset to default PixelJam server")
                }) { Text("Reset Default") }
            }
        )
    }

    val roomModeOptions = remember {
        PixelJamRoomMode.entries.associate { it.wireValue to it.label }
    }

    val avatarRingBrush = Brush.sweepGradient(
        colors = listOf(
            colors.primary,
            colors.tertiary,
            colors.secondary,
            colors.primaryContainer,
            colors.primary
        )
    )

    val settingsGlassState = rememberGlassState()
    CompositionLocalProvider(
        LocalScreenGlassState provides settingsGlassState
    ) {
        Box(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize()
        ) {
            val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
            val bottomInset = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .glassRoot(settingsGlassState)
                    .scrollMotionBlur(lazyListState),
                contentPadding = PaddingValues(
                    top = currentTopBarHeightDp + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = bottomInset
                )
            ) {
                // 1. Material You Expressive Profile Hero Card (matches ExpandableAccountCard hierarchy)
                item(key = "jam_profile_hero") {
                    Surface(
                        onClick = { navController.navigateSafely(Screen.Profile.route) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(62.dp)
                                    .then(
                                        if (hasProfile) Modifier.border(BorderStroke(2.5.dp, avatarRingBrush), CircleShape)
                                        else Modifier
                                    )
                                    .clip(CircleShape)
                            ) {
                                ProfileAvatar(
                                    avatarSpec = effectiveAvatar,
                                    displayName = effectiveName.ifBlank { "P" },
                                    size = 52.dp,
                                    cacheKey = userProfile.updatedAt,
                                    fallbackInitial = "P"
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = effectiveName.ifBlank { "Set up your profile" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Edit display name, photo & illustrated avatar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = colors.secondaryContainer
                            ) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Notifications & Toasts
                item(key = "jam_notifications") {
                    PixelJamSettingsSubsection(title = "Notifications & Toasts") {
                        SwitchSettingItem(
                            title = "Join & leave toasts",
                            subtitle = "Show a toast when someone joins or leaves your PixelJam room",
                            checked = notifyJoinLeave,
                            onCheckedChange = { vm.setNotifyJoinLeave(it) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_group_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            }
                        )
                        SwitchSettingItem(
                            title = "Host & suggestion alerts",
                            subtitle = "Show toasts for co-host promotions and incoming song suggestions",
                            checked = notifyRoomAlerts,
                            onCheckedChange = { vm.setNotifyRoomAlerts(it) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_forum_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            }
                        )
                    }
                }

                // 3. Playback & Room Defaults
                item(key = "jam_playback_behavior") {
                    PixelJamSettingsSubsection(title = "Playback & Room Defaults") {
                        SwitchSettingItem(
                            title = "Auto-continue queue on leave",
                            subtitle = "Automatically queue related tracks when you leave a PixelJam room",
                            checked = autoQueueOnLeave,
                            onCheckedChange = { vm.setAutoQueueOnLeave(it) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_queue_music_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            }
                        )
                        SwitchSettingItem(
                            title = "Keep session alive in background",
                            subtitle = "Maintain WebSocket synchronization when PixelMusic is minimized",
                            checked = keepAlive,
                            onCheckedChange = { vm.setKeepAlive(it) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_sync_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            }
                        )
                        ThemeSelectorItem(
                            label = "Default room mode",
                            description = "Choose who controls playback when you host a new PixelJam room",
                            options = roomModeOptions,
                            selectedKey = defaultRoomMode.wireValue,
                            onSelectionChanged = { key ->
                                val mode = PixelJamRoomMode.entries.firstOrNull { it.wireValue == key }
                                    ?: PixelJamRoomMode.HOST_ONLY
                                vm.setDefaultRoomMode(mode)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_music_note_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            }
                        )
                    }
                }

                // 4. Sync Server
                item(key = "jam_server") {
                    PixelJamSettingsSubsection(
                        title = "Sync Server",
                        addBottomSpace = false
                    ) {
                        SettingsItem(
                            title = "Sync server URL",
                            subtitle = serverUrl,
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_cloud_sync_24),
                                    contentDescription = null,
                                    tint = colors.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant
                                )
                            },
                            onClick = {
                                serverDraft = serverUrl
                                showServerDialog = true
                            }
                        )
                    }
                }
            }

            CollapsibleCommonTopBar(
                title = stringResource(R.string.pixeljam),
                collapseFraction = collapseFraction,
                headerHeight = currentTopBarHeightDp,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PixelJamSettingsSubsection(
    title: String,
    addBottomSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
    if (addBottomSpace) {
        Spacer(modifier = Modifier.height(10.dp))
    }
}
