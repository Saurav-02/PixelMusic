package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ListenTogetherSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    object Keys {
        val COMPACT = booleanPreferencesKey("listen_together_compact_members")
        val ANIMATIONS = booleanPreferencesKey("listen_together_animated_reactions")
        val SOCIAL = booleanPreferencesKey("listen_together_show_social")
        val KEEP_ALIVE = booleanPreferencesKey("listen_together_keep_alive")
    }

    val compact: StateFlow<Boolean> = dataStore.data.map { it[Keys.COMPACT] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val animations: StateFlow<Boolean> = dataStore.data.map { it[Keys.ANIMATIONS] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val social: StateFlow<Boolean> = dataStore.data.map { it[Keys.SOCIAL] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val keepAlive: StateFlow<Boolean> = dataStore.data.map { it[Keys.KEEP_ALIVE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setCompact(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.COMPACT] = value } }
    }

    fun setAnimations(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.ANIMATIONS] = value } }
    }

    fun setSocial(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.SOCIAL] = value } }
    }

    fun setKeepAlive(value: Boolean) {
        viewModelScope.launch { dataStore.edit { it[Keys.KEEP_ALIVE] = value } }
    }
}

@Composable
fun ListenTogetherSettingsScreen(
    navController: NavController,
    vm: ListenTogetherSettingsViewModel = hiltViewModel()
) {
    val compact by vm.compact.collectAsStateWithLifecycle()
    val animations by vm.animations.collectAsStateWithLifecycle()
    val social by vm.social.collectAsStateWithLifecycle()
    val keepAlive by vm.keepAlive.collectAsStateWithLifecycle()

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
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
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

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .scrollMotionBlur(lazyListState),
            contentPadding = PaddingValues(top = currentTopBarHeightDp + 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Session Experience Section
            item {
                SettingsSection(
                    title = "Session Experience",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SwitchSettingItem(
                            title = "Compact member list",
                            subtitle = "Collapse participants into a dense horizontal avatar strip",
                            checked = compact,
                            onCheckedChange = { vm.setCompact(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ViewDay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        SwitchSettingItem(
                            title = "Animated reactions",
                            subtitle = "Display floating emoji reaction animations across the screen",
                            checked = animations,
                            onCheckedChange = { vm.setAnimations(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mood,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        SwitchSettingItem(
                            title = "Chat & social bar",
                            subtitle = "Show quick chat reaction chips and song request controls",
                            checked = social,
                            onCheckedChange = { vm.setSocial(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }

            // Connection & Sync Section
            item {
                SettingsSection(
                    title = "Connection & Sync",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SwitchSettingItem(
                            title = "Keep session alive in background",
                            subtitle = "Maintain realtime synchronization when Pixel Music is minimized",
                            checked = keepAlive,
                            onCheckedChange = { vm.setKeepAlive(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        SettingsItem(
                            title = "Automatic Drift Correction",
                            subtitle = "Dynamic playback rate adjustment ensures all listeners stay in frame-accurate sync",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {}
                        )
                    }
                }
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.listen_together),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() }
        )
    }
}
