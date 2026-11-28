package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.ui.blur.LocalScreenGlassState
import com.saurav.pixelmusic.ui.blur.ProgressiveHeaderBackdrop
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.blur.rememberGlassState

import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.components.BackupModuleSelectionDialog
import com.saurav.pixelmusic.presentation.components.expressiveListItemScrollAnimation
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.saurav.pixelmusic.data.preferences.AiPreferencesRepository
import com.saurav.pixelmusic.data.preferences.StreamingAudioQuality
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.text.format.Formatter
import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.backup.model.BackupHistoryEntry
import com.saurav.pixelmusic.data.backup.model.BackupOperationType
import com.saurav.pixelmusic.data.backup.model.BackupSection
import com.saurav.pixelmusic.data.backup.model.BackupTransferProgressUpdate
import com.saurav.pixelmusic.data.backup.model.ModuleRestoreDetail
import com.saurav.pixelmusic.data.backup.model.RestorePlan
import com.saurav.pixelmusic.data.preferences.AppLanguage
import com.saurav.pixelmusic.data.preferences.AppThemeMode
import com.saurav.pixelmusic.data.preferences.AppFontMode
import com.saurav.pixelmusic.data.preferences.LaunchTab
import com.saurav.pixelmusic.data.preferences.LibraryNavigationMode
import com.saurav.pixelmusic.data.preferences.NavBarStyle
import com.saurav.pixelmusic.data.preferences.ThemePreference
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.model.LyricsSourcePreference
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.ExpressiveTopBarContent
import com.saurav.pixelmusic.presentation.components.FileExplorerDialog
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.model.SettingsCategory
// LocalSettingsHighlightTitle lives in the same package (SettingsComponents.kt)
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.viewmodel.LyricsRefreshProgress
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.SettingsViewModel
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.saurav.pixelmusic.ui.theme.LocalThemeTransitionController
import com.saurav.pixelmusic.utils.AndroidVersionCompat

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsCategoryScreen(
    categoryId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    statsViewModel: com.saurav.pixelmusic.presentation.viewmodel.StatsViewModel = hiltViewModel(),
    highlightTitle: String? = null,
    onBackClick: () -> Unit
) {
    val category = SettingsCategory.fromId(categoryId) ?: return
    val context = LocalContext.current
    val view = LocalView.current
    val themeTransitionController = LocalThemeTransitionController.current

    // State Collection (Duplicated from SettingsScreen for now to ensure functionality)
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val currentAiApiKey by settingsViewModel.currentAiApiKey.collectAsStateWithLifecycle()
    val currentAiModel by settingsViewModel.currentAiModel.collectAsStateWithLifecycle()
    val currentAiSystemPrompt by settingsViewModel.currentAiSystemPrompt.collectAsStateWithLifecycle()
    val aiProvider by settingsViewModel.aiProvider.collectAsStateWithLifecycle()
    val currentPath by settingsViewModel.currentPath.collectAsStateWithLifecycle()
    val directoryChildren by settingsViewModel.currentDirectoryChildren.collectAsStateWithLifecycle()
    val availableStorages by settingsViewModel.availableStorages.collectAsStateWithLifecycle()
    val selectedStorageIndex by settingsViewModel.selectedStorageIndex.collectAsStateWithLifecycle()
    val isLoadingDirectories by settingsViewModel.isLoadingDirectories.collectAsStateWithLifecycle()
    val isExplorerPriming by settingsViewModel.isExplorerPriming.collectAsStateWithLifecycle()
    val isExplorerReady by settingsViewModel.isExplorerReady.collectAsStateWithLifecycle()
    val isCurrentDirectoryResolved by settingsViewModel.isCurrentDirectoryResolved.collectAsStateWithLifecycle()
    val isSyncing by settingsViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by settingsViewModel.syncProgress.collectAsStateWithLifecycle()
    val dataTransferProgress by settingsViewModel.dataTransferProgress.collectAsStateWithLifecycle()
    val paletteRegenerateTargets by if (category == SettingsCategory.DEVELOPER) {
        playerViewModel.paletteRegenerationTargets.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val explorerRoot = settingsViewModel.explorerRoot()

    // Local State
    var showExplorerSheet by remember { mutableStateOf(false) }
    var refreshRequested by remember { mutableStateOf(false) }
    var syncRequestObservedRunning by remember { mutableStateOf(false) }
    var syncIndicatorLabel by remember { mutableStateOf<String?>(null) }
    var showClearLyricsDialog by remember { mutableStateOf(false) }
    var showRebuildDatabaseWarning by remember { mutableStateOf(false) }
    var showRegenerateDailyMixDialog by remember { mutableStateOf(false) }
    var showRegenerateStatsDialog by remember { mutableStateOf(false) }
    var showRegenerateAllPalettesDialog by remember { mutableStateOf(false) }
    var showExportDataDialog by remember { mutableStateOf(false) }
    var showImportFlow by remember { mutableStateOf(false) }
    var exportSections by remember { mutableStateOf(BackupSection.defaultSelection) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    var minSongDurationDraft by remember(uiState.minSongDuration) {
        mutableStateOf(uiState.minSongDuration.toFloat())
    }
    var minTracksPerAlbumDraft by remember(uiState.minTracksPerAlbum) {
        mutableStateOf(uiState.minTracksPerAlbum.toFloat())
    }
    var albumArtCacheLimitDraft by remember(uiState.albumArtCacheLimitMb) {
        mutableStateOf(uiState.albumArtCacheLimitMb.toFloat())
    }
    var storageLimitDraft by remember(uiState.storageLimitMb) {
        mutableStateOf(uiState.storageLimitMb.toFloat())
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            settingsViewModel.exportAppData(uri, exportSections)
        }
    }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            importFileUri = uri
            settingsViewModel.inspectBackupFile(uri)
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Persist the URI permission so the app can load it after a restart
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
                settingsViewModel.setAppBackgroundCustomUri(uri.toString())
                settingsViewModel.setAppBackgroundStyle(
                    com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.CUSTOM
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image. Try another gallery app.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.dataTransferEvents.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isSyncing, refreshRequested) {
        if (!refreshRequested) return@LaunchedEffect
        if (isSyncing) {
            syncRequestObservedRunning = true
        } else if (syncRequestObservedRunning) {
            Toast.makeText(context, context.getString(R.string.toast_library_sync_finished), Toast.LENGTH_SHORT).show()
            refreshRequested = false
            syncRequestObservedRunning = false
            syncIndicatorLabel = null
        }
    }

    var showPaletteRegenerateSheet by remember { mutableStateOf(false) }
    var isPaletteRegenerateRunning by remember { mutableStateOf(false) }
    var isPaletteBulkRegenerateRunning by remember { mutableStateOf(false) }
    var paletteBulkCompletedCount by remember { mutableStateOf(0) }
    var paletteBulkTotalCount by remember { mutableStateOf(0) }
    var paletteSongSearchQuery by remember { mutableStateOf("") }
    val paletteRegenerateSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val isAnyPaletteRegenerateRunning = isPaletteRegenerateRunning || isPaletteBulkRegenerateRunning

    val filteredPaletteSongs = remember(paletteRegenerateTargets, paletteSongSearchQuery) {
        val query = paletteSongSearchQuery.trim()
        if (query.isBlank()) {
            paletteRegenerateTargets
        } else {
            paletteRegenerateTargets.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.displayArtist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        }
    }

    // Auto-load models when entering the AI settings page or when provider/key changes
    LaunchedEffect(category, aiProvider) {
        if (category == SettingsCategory.AI_INTEGRATION) {
            // Small delay to let StateFlows settle after navigation
            kotlinx.coroutines.delay(200)
            settingsViewModel.loadModelsForCurrentProvider()
        }
    }

    // TopBar Animations (identical to SettingsScreen)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val categoryTitle = stringResource(category.titleRes)
    val isLongTitle = categoryTitle.length > 13
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = if (isLongTitle) 200.dp else 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val titleMaxLines = if (isLongTitle) 2 else 1
    val topBarHeight = remember(maxTopBarHeightPx) { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value, maxTopBarHeightPx) {
        collapseFraction =
            1f -
                ((topBarHeight.value - minTopBarHeightPx) /
                    (maxTopBarHeightPx - minTopBarHeightPx))
                    .coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (!isScrollingDown &&
                    (lazyListState.firstVisibleItemIndex > 0 ||
                        lazyListState.firstVisibleItemScrollOffset > 0)
                ) {
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
            val canExpand =
                lazyListState.firstVisibleItemIndex == 0 &&
                    lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val categoryGlassState = rememberGlassState()
    CompositionLocalProvider(
        LocalSettingsHighlightTitle provides highlightTitle,
        LocalScreenGlassState provides categoryGlassState
    ) {
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
                    .glassRoot(categoryGlassState)
                    .scrollMotionBlur(
                        lazyListState = lazyListState,
                        enabled = uiState.isUiMotionBlurEnabled
                    ),
                contentPadding = PaddingValues(
                    top = currentTopBarHeightDp + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    Column(
                        modifier = Modifier.background(Color.Transparent)
                    ) {
                        when (category) {
                            SettingsCategory.LIBRARY, SettingsCategory.CONTENT -> {
                                SettingsSubsection(title = stringResource(R.string.setcat_library_structure)) {
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_excluded_directories_title),
                                        subtitle = stringResource(R.string.setcat_excluded_directories_subtitle),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_folder_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = {
                                            showExplorerSheet = true
                                            settingsViewModel.openExplorer()
                                        }
                                    )
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_artists_title),
                                        subtitle = stringResource(R.string.setcat_artists_subtitle),
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { navController.navigateSafely(Screen.ArtistSettings.route) }
                                    )
                                }

                                val ytOnlineEnabled = !uiState.localModeEnabled
                                val showLocalModeLockedToast: () -> Unit = {
                                    Toast.makeText(
                                        context,
                                        "This feature requires YouTube Music integration. Turn off Local Mode to use it.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                SettingsSubsection(title = "Online Content & Sources") {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_content_language_title),
                                        description = stringResource(R.string.settings_content_language_desc),
                                        options = com.saurav.pixelmusic.data.preferences.LanguageCodeToName,
                                        selectedKey = uiState.contentLanguage,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setContentLanguage(key)
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_translate_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_content_country_title),
                                        description = stringResource(R.string.settings_content_country_desc),
                                        options = com.saurav.pixelmusic.data.preferences.CountryCodeToName,
                                        selectedKey = uiState.contentCountry,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setContentCountry(key)
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_public_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_playlist_suggestion_title),
                                        description = stringResource(R.string.settings_playlist_suggestion_desc),
                                        options = mapOf(
                                            com.saurav.pixelmusic.data.preferences.PlaylistSuggestionSource.PLAYLIST_TITLE.name to "Playlist Title",
                                            com.saurav.pixelmusic.data.preferences.PlaylistSuggestionSource.PLAYLIST_CONTENT.name to "Playlist Content",
                                            com.saurav.pixelmusic.data.preferences.PlaylistSuggestionSource.BOTH.name to "Both"
                                        ),
                                        selectedKey = uiState.playlistSuggestionSource.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setPlaylistSuggestionSource(
                                                com.saurav.pixelmusic.data.preferences.PlaylistSuggestionSource.valueOf(key)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.settings_hide_explicit_title),
                                        subtitle = stringResource(R.string.settings_hide_explicit_desc),
                                        checked = uiState.hideExplicit,
                                        onCheckedChange = { settingsViewModel.setHideExplicit(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_explicit_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.settings_hide_video_title),
                                        subtitle = stringResource(R.string.settings_hide_video_desc),
                                        checked = uiState.hideVideo,
                                        onCheckedChange = { settingsViewModel.setHideVideo(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_videocam_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_my_top_size_title),
                                        description = stringResource(R.string.settings_my_top_size_desc),
                                        options = mapOf(
                                            "20" to "20 songs",
                                            "50" to "50 songs",
                                            "100" to "100 songs"
                                        ),
                                        selectedKey = uiState.topSize,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setTopSize(key)
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_trending_up_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_set_quick_picks_title),
                                        description = stringResource(R.string.settings_set_quick_picks_desc),
                                        options = mapOf(
                                            com.saurav.pixelmusic.data.preferences.QuickPicks.QUICK_PICKS.name to "Quick Picks (Discover)",
                                            com.saurav.pixelmusic.data.preferences.QuickPicks.LAST_LISTEN.name to "Last Listen",
                                            com.saurav.pixelmusic.data.preferences.QuickPicks.DONT_SHOW.name to "Don't Show"
                                        ),
                                        selectedKey = uiState.discover.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setDiscover(
                                                com.saurav.pixelmusic.data.preferences.QuickPicks.valueOf(key)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_auto_awesome_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.settings_quick_picks_mode_title),
                                        description = stringResource(R.string.settings_quick_picks_mode_desc),
                                        options = mapOf(
                                            com.saurav.pixelmusic.data.preferences.QuickPicksDisplayMode.CARD.name to "Card Carousel",
                                            com.saurav.pixelmusic.data.preferences.QuickPicksDisplayMode.LIST.name to "List Grid"
                                        ),
                                        selectedKey = uiState.quickPicksDisplayMode.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setQuickPicksDisplayMode(
                                                com.saurav.pixelmusic.data.preferences.QuickPicksDisplayMode.valueOf(key)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_grid_view_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = ytOnlineEnabled,
                                        onDisabledClick = showLocalModeLockedToast
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_filtering)) {
                                    SliderSettingsItem(
                                        label = stringResource(R.string.setcat_min_song_duration),
                                        value = minSongDurationDraft,
                                        valueRange = 0f..120000f,
                                        steps = 23,
                                        onValueChange = { minSongDurationDraft = it },
                                        onValueChangeFinished = {
                                            val selectedDuration = minSongDurationDraft.toInt()
                                            if (selectedDuration != uiState.minSongDuration) {
                                                settingsViewModel.setMinSongDuration(selectedDuration)
                                            }
                                        },
                                        valueText = { value -> "${(value / 1000).toInt()}s" }
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.setcat_min_tracks_per_album),
                                        value = minTracksPerAlbumDraft,
                                        valueRange = 1f..5f,
                                        steps = 3,
                                        onValueChange = { minTracksPerAlbumDraft = it },
                                        onValueChangeFinished = {
                                            val selectedTracks = minTracksPerAlbumDraft.toInt()
                                            if (selectedTracks != uiState.minTracksPerAlbum) {
                                                settingsViewModel.setMinTracksPerAlbum(selectedTracks)
                                            }
                                        },
                                        valueText = { value -> "${value.toInt()}" }
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.setcat_album_art_cache_limit),
                                        value = albumArtCacheLimitDraft,
                                        valueRange = 50f..1500f,
                                        steps = 28,
                                        onValueChange = { albumArtCacheLimitDraft = it },
                                        onValueChangeFinished = {
                                            val selectedLimit = albumArtCacheLimitDraft.toInt()
                                            if (selectedLimit != uiState.albumArtCacheLimitMb) {
                                                settingsViewModel.setAlbumArtCacheLimitMb(selectedLimit)
                                            }
                                        },
                                        valueText = { value -> "${value.toInt()} MB" }
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.setcat_music_storage_limit_desc),
                                        value = storageLimitDraft,
                                        valueRange = 0f..10240f,
                                        steps = 20,
                                        onValueChange = { storageLimitDraft = it },
                                        onValueChangeFinished = {
                                            val selectedLimit = storageLimitDraft.toInt()
                                            if (selectedLimit != uiState.storageLimitMb) {
                                                settingsViewModel.setStorageLimitMb(selectedLimit)
                                            }
                                        },
                                        valueText = { value ->
                                            if (value.toInt() == 0) "Unlimited"
                                            else if (value >= 1024) "${(value / 1024).toInt()} GB"
                                            else "${value.toInt()} MB"
                                        }
                                    )
                                    ActionSettingsItem(
                                        title = "Clear Streaming Cache",
                                        subtitle = "Delete all temporary streaming data from your device.",
                                        icon = {
                                            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        primaryActionLabel = "Clear Cache",
                                        onPrimaryAction = {
                                            settingsViewModel.clearExoPlayerCache()
                                            Toast.makeText(context, "Streaming cache cleared!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_sync_scanning)) {
                                    RefreshLibraryItem(
                                        isSyncing = isSyncing,
                                        syncProgress = syncProgress,
                                        activeOperationLabel = if (isSyncing) syncIndicatorLabel else null,
                                        onFullSync = {
                                            if (isSyncing) return@RefreshLibraryItem
                                            refreshRequested = true
                                            syncRequestObservedRunning = false
                                            syncIndicatorLabel = context.getString(R.string.setcat_sync_full_rescan_label)
                                            Toast.makeText(context, context.getString(R.string.toast_full_rescan_started), Toast.LENGTH_SHORT).show()
                                            settingsViewModel.fullSyncLibrary()
                                        },
                                        onRebuild = {
                                            if (isSyncing) return@RefreshLibraryItem
                                            showRebuildDatabaseWarning = true
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_auto_scan_lrc_title),
                                        subtitle = stringResource(R.string.setcat_auto_scan_lrc_subtitle),
                                        checked = uiState.autoScanLrcFiles,
                                        onCheckedChange = { settingsViewModel.setAutoScanLrcFiles(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_folder_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                }
                            }

                            SettingsCategory.APPEARANCE -> {
                                val useSmoothCorners = uiState.useSmoothCorners
                                val playerDesignStyle by playerViewModel.playerDesignStyle.collectAsStateWithLifecycle()
                                val nowPlayingLyricsStyle = uiState.nowPlayingLyricsStyle
                                val currentBgStyle = uiState.appBackgroundStyle
                                val currentBgOpacity = uiState.appBackgroundOpacity

                                SettingsSubsection(title = "App Background") {
                                    val bgOptions = buildMap {
                                        put(com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.DEFAULT.name, "Default (Solid Color)")
                                        put(com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.GRADIENT_BLUR.name, "Gradient Blur (Material You)")
                                        put(com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.MUSIC_NOTES.name, "Music Notes (Auto Dark/Light)")
                                        if (AndroidVersionCompat.supportsHardwareBlur) {
                                            put(com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.LIVE_BLUR.name, "Live Blur (Now Playing Art)")
                                        }
                                        put(com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.CUSTOM.name, "Custom Image from Gallery")
                                    }
                                    ThemeSelectorItem(
                                        label = "Background Wallpaper",
                                        showBetaBadge = true,
                                        description = "Choose a top-half ambient background for Home and Explore screens",
                                        options = bgOptions,
                                        selectedKey = currentBgStyle.name,
                                        onSelectionChanged = { key ->
                                            val selectedStyle = com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.valueOf(key)
                                            if (selectedStyle == com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.CUSTOM) {
                                                backgroundLauncher.launch("image/*")
                                            } else {
                                                settingsViewModel.setAppBackgroundStyle(selectedStyle)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_palette_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    AnimatedVisibility(
                                        visible = currentBgStyle != com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.DEFAULT,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        SliderSettingsItem(
                                            label = "Wallpaper Opacity",
                                            value = currentBgOpacity,
                                            valueRange = 0.1f..1.0f,
                                            steps = 0,
                                            onValueChange = { settingsViewModel.setAppBackgroundOpacity(it) },
                                            valueText = { "${(it * 100).toInt()}%" }
                                        )
                                        if (AndroidVersionCompat.supportsHardwareBlur) {
                                            SliderSettingsItem(
                                                label = "Wallpaper Blur",
                                                value = uiState.appBackgroundBlur,
                                                valueRange = 0f..100f,
                                                steps = 0,
                                                onValueChange = { settingsViewModel.setAppBackgroundBlur(it) },
                                                valueText = { "${it.toInt()}%" }
                                            )
                                        }
                                    }
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_global_theme)) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_language_label),
                                        description = stringResource(R.string.setcat_language_desc),
                                        options = AppLanguage.getLanguageOptions(context),
                                        selectedKey = uiState.appLanguageTag,
                                        onSelectionChanged = {
                                            settingsViewModel.setAppLanguage(it)
                                            (context as? Activity)?.recreate()
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_language_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    ThemeModeSelectorCard(
                                        selectedMode = uiState.appThemeMode,
                                        onModeSelected = { newMode, origin ->
                                            if (newMode != uiState.appThemeMode) {
                                                if (themeTransitionController != null) {
                                                    themeTransitionController.transitionThemeChange(
                                                        view = view,
                                                        origin = origin,
                                                        scope = coroutineScope
                                                    ) {
                                                        settingsViewModel.setAppThemeMode(newMode)
                                                    }
                                                } else {
                                                    settingsViewModel.setAppThemeMode(newMode)
                                                }
                                            }
                                        }
                                    )
                                    AppFontSelectorCard(
                                        selectedFontMode = uiState.appFontMode,
                                        onFontModeSelected = { settingsViewModel.setAppFontMode(it) }
                                    )
                                    val colorPaletteOptions = buildMap {
                                        if (AndroidVersionCompat.supportsDynamicColor) {
                                            put("DYNAMIC", stringResource(R.string.setcat_color_palette_dynamic))
                                        }
                                        put("ALBUM_ART", stringResource(R.string.setcat_color_palette_album_art))
                                        put("BLACK_AND_WHITE", stringResource(R.string.setcat_color_palette_bw))
                                        put("YELLOW", stringResource(R.string.setcat_color_palette_yellow))
                                        put("PEACH", "Peach Coral")
                                        put("SAGE", "Sage Mint")
                                        put("OCEAN", "Ocean Blue")
                                        put("LAVENDER", "Lavender")
                                        put("ROSE", "Rose Quartz")
                                        put("CRIMSON", "Crimson")
                                        put("TEAL", "Deep Teal")
                                        put("ESPRESSO", "Mocha")
                                    }
                                    ColorPaletteSelectorCard(
                                        selectedPalette = uiState.colorPalette,
                                        options = colorPaletteOptions,
                                        onPaletteSelected = { newPalette, origin ->
                                            if (newPalette != uiState.colorPalette) {
                                                if (themeTransitionController != null) {
                                                    themeTransitionController.transitionThemeChange(
                                                        view = view,
                                                        origin = origin,
                                                        scope = coroutineScope
                                                    ) {
                                                        settingsViewModel.setColorPalette(newPalette)
                                                    }
                                                } else {
                                                    settingsViewModel.setColorPalette(newPalette)
                                                }
                                            }
                                        }
                                    )
                                    AppIconSelectorCard()
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_now_playing)) {
                                    val playerThemeOptions = buildMap {
                                        put(ThemePreference.ALBUM_ART, stringResource(R.string.setcat_player_theme_album_art))
                                        if (AndroidVersionCompat.supportsDynamicColor) {
                                            put(ThemePreference.DYNAMIC, stringResource(R.string.setcat_player_theme_dynamic))
                                        }
                                    }
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_player_theme_label),
                                        description = stringResource(R.string.setcat_player_theme_desc),
                                        options = playerThemeOptions,
                                        selectedKey = uiState.playerThemePreference,
                                        onSelectionChanged = { settingsViewModel.setPlayerThemePreference(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_play_circle_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    ThemeSelectorItem(
                                        label = "Player Design Style",
                                        description = "Choose the layout and style of the Now Playing screen",
                                        options = mapOf(
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.DEFAULT.name to "Default",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.IMMERSIVE.name to "Immersive",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.IMMERSIVE_EXTENDED.name to "Immersive Extended",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.COMPLEX.name to "Complex",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.WAVE.name to "Wave",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.PILLARS.name to "Pillars",
                                            com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.EXPRESSIVE.name to "Expressive"
                                        ),
                                        selectedKey = playerDesignStyle.name,
                                        onSelectionChanged = { key ->
                                            coroutineScope.launch {
                                                playerViewModel.userPreferencesRepository.setPlayerDesignStyle(
                                                    com.saurav.pixelmusic.data.preferences.PlayerDesignStyle.valueOf(key)
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.rounded_imagesmode_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_show_player_file_info_title),
                                        subtitle = stringResource(R.string.setcat_show_player_file_info_subtitle),
                                        checked = uiState.showPlayerFileInfo,
                                        onCheckedChange = { settingsViewModel.setShowPlayerFileInfo(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_attach_file_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_album_art_palette_title),
                                        subtitle = stringResource(R.string.setcat_album_art_palette_subtitle, uiState.albumArtPaletteStyle.label),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_style_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { navController.navigateSafely(Screen.PaletteStyle.route) }
                                    )
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_app_navigation_section),
                                    addBottomSpace = false
                                ) {
                                    val defaultTabIconRes = when (uiState.launchTab) {
                                        LaunchTab.EXPLORE -> R.drawable.rounded_album_24
                                        LaunchTab.LIBRARY -> R.drawable.rounded_library_music_24
                                        LaunchTab.SEARCH -> R.drawable.rounded_search_24
                                        else -> R.drawable.rounded_home_24
                                    }
                                    ThemeSelectorItem(
                                        label = "Default Tab",
                                        description = "Choose which tab opens when PixelMusic starts",
                                        options = linkedMapOf(
                                            LaunchTab.HOME to "Home",
                                            LaunchTab.EXPLORE to "Explore",
                                            LaunchTab.LIBRARY to "Library",
                                            LaunchTab.SEARCH to "Search"
                                        ),
                                        selectedKey = when (uiState.launchTab) {
                                            LaunchTab.EXPLORE, LaunchTab.LIBRARY, LaunchTab.SEARCH -> uiState.launchTab
                                            else -> LaunchTab.HOME
                                        },
                                        onSelectionChanged = { settingsViewModel.setLaunchTab(it) },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(defaultTabIconRes),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_library_navigation_label),
                                        description = stringResource(R.string.setcat_library_navigation_desc),
                                        options = mapOf(
                                            LibraryNavigationMode.TAB_ROW to stringResource(R.string.setcat_library_nav_tab_row),
                                            LibraryNavigationMode.COMPACT_PILL to stringResource(R.string.setcat_library_nav_compact_pill)
                                        ),
                                        selectedKey = uiState.libraryNavigationMode,
                                        onSelectionChanged = { settingsViewModel.setLibraryNavigationMode(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_library_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = "Swipe to Change Track",
                                        subtitle = "Swipe left or right on the Now Playing screen to skip to the next or previous track.",
                                        checked = uiState.nowPlayingSwipeToChangeTrack,
                                        onCheckedChange = { settingsViewModel.setNowPlayingSwipeToChangeTrack(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_swap_horiz_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    if (AndroidVersionCompat.supportsMotionBlur) {
                                        SwitchSettingItem(
                                            title = "Motion Blur",
                                            subtitle = "Applies a cinematic directional blur when scrolling lists.",
                                            checked = uiState.isUiMotionBlurEnabled,
                                            onCheckedChange = { settingsViewModel.setUiMotionBlurEnabled(it) },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_auto_awesome_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        )
                                        if (uiState.isUiMotionBlurEnabled) {
                                            SliderSettingsItem(
                                                label = "Blur Intensity",
                                                value = uiState.uiMotionBlurIntensity,
                                                valueRange = 0f..2f,
                                                steps = 7,
                                                onValueChange = { settingsViewModel.setUiMotionBlurIntensity(it) },
                                                valueText = { v ->
                                                    when {
                                                        v < 0.25f -> "Off"
                                                        v < 0.75f -> "Subtle"
                                                        v < 1.25f -> "Normal"
                                                        v < 1.75f -> "Strong"
                                                        else -> "Extreme"
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_lyrics_management)
                                ) {
                                    var showLyricsProvidersSheet by remember { mutableStateOf(false) }
                                    val lyricsProviderOrder by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.providerOrderFlow(context)
                                    }.collectAsStateWithLifecycle(
                                        initialValue = com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.DefaultProviderOrder
                                    )
                                    val autoRomanize by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.autoRomanizeFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = false)
                                    val romanizationAsMain by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.romanizationAsMainFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = false)
                                    val autoTranslate by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.autoTranslateFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = false)
                                    val translateLang by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.translateLanguageFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = "en")
                                    val useAiTranslation by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.useAiTranslationFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = false)
                                    val lyricsTextSize by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.textSizeFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = 26f)
                                    val lyricsLineSpacing by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.lineSpacingFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = 1.25f)
                                    val lyricsSoftLight by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.softLightFlow(context)
                                    }.collectAsStateWithLifecycle(initialValue = true)

                                    if (showLyricsProvidersSheet) {
                                        com.saurav.pixelmusic.presentation.components.lyrics.LyricsProvidersReorderSheet(
                                            onDismiss = { showLyricsProvidersSheet = false }
                                        )
                                    }

                                    val lyricsStyle by remember(context) {
                                        com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.styleFlow(context)
                                    }.collectAsStateWithLifecycle(
                                        initialValue = com.saurav.pixelmusic.lyrics.PixelMusicLyricsStyle.MOTION
                                    )
                                    ThemeSelectorItem(
                                        label = "Lyrics Style",
                                        description = lyricsStyle.description,
                                        options = com.saurav.pixelmusic.lyrics.PixelMusicLyricsStyle.values()
                                            .associate { it.name to it.displayName },
                                        selectedKey = lyricsStyle.name,
                                        onSelectionChanged = { key ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setStyle(
                                                    context,
                                                    com.saurav.pixelmusic.lyrics.PixelMusicLyricsStyle.fromName(key)
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SettingsItem(
                                        title = "Lyrics Providers",
                                        subtitle = "Drag to reorder priority · ${
                                            lyricsProviderOrder.take(3).joinToString(" → ") {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsEngine.getDisplayName(it)
                                            }
                                        }",
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { showLyricsProvidersSheet = true }
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_auto_hide_delay_label),
                                        description = stringResource(R.string.setcat_auto_hide_delay_desc),
                                        options = mapOf(
                                            "3000" to stringResource(R.string.setcat_lyrics_delay_3s),
                                            "4000" to stringResource(R.string.setcat_lyrics_delay_4s),
                                            "5000" to stringResource(R.string.setcat_lyrics_delay_5s),
                                            "6000" to stringResource(R.string.setcat_lyrics_delay_6s)
                                        ),
                                        selectedKey = uiState.immersiveLyricsTimeout.toString(),
                                        onSelectionChanged = { settingsViewModel.setImmersiveLyricsTimeout(it.toLong()) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_timer_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = "Romanisation",
                                        subtitle = "Show Latin script readings for non-Latin lyrics",
                                        checked = autoRomanize,
                                        onCheckedChange = { enabled ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setAutoRomanize(context, enabled)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_text_format_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    if (autoRomanize) {
                                        SwitchSettingItem(
                                            title = "Romanisation as the main line",
                                            subtitle = "Put romanised lyrics on top and original script underneath",
                                            checked = romanizationAsMain,
                                            onCheckedChange = { enabled ->
                                                coroutineScope.launch {
                                                    com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setRomanizationAsMain(context, enabled)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_text_format_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        )
                                    }
                                    SwitchSettingItem(
                                        title = "Translation",
                                        subtitle = "Translate lyrics into your preferred language",
                                        checked = autoTranslate,
                                        onCheckedChange = { enabled ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setAutoTranslate(context, enabled)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_translate_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    if (autoTranslate) {
                                        ThemeSelectorItem(
                                            label = "Translation Language",
                                            description = "Choose the language to translate lyrics into",
                                            options = com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.SupportedTranslateLanguages.toMap(),
                                            selectedKey = translateLang,
                                            onSelectionChanged = { code ->
                                                coroutineScope.launch {
                                                    com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setTranslateLanguage(context, code)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_translate_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        )
                                        SwitchSettingItem(
                                            title = "Use AI for translation",
                                            subtitle = "Try your AI provider first, fall back to Google",
                                            checked = useAiTranslation,
                                            onCheckedChange = { enabled ->
                                                coroutineScope.launch {
                                                    com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setUseAiTranslation(context, enabled)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_auto_awesome_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        )
                                    }
                                    SliderSettingsItem(
                                        label = "Text Size",
                                        value = lyricsTextSize,
                                        valueRange = 18f..36f,
                                        steps = 17,
                                        onValueChange = { sizeSp ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setTextSize(context, sizeSp)
                                            }
                                        },
                                        valueText = { "${it.roundToInt()} sp" }
                                    )
                                    SliderSettingsItem(
                                        label = "Line Spacing",
                                        value = lyricsLineSpacing,
                                        valueRange = 1.0f..1.7f,
                                        steps = 13,
                                        onValueChange = { spacing ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setLineSpacing(context, spacing)
                                            }
                                        },
                                        valueText = { "${(it * 100).roundToInt()}%" }
                                    )
                                    if (AndroidVersionCompat.supportsHardwareBlur) {
                                        SliderSettingsItem(
                                            label = stringResource(R.string.presentation_batch_f_exp_blur_strength),
                                            value = uiState.animatedLyricsBlurStrength.coerceIn(0.5f, 5.0f),
                                            valueRange = 0.5f..5.0f,
                                            steps = 8,
                                            onValueChange = { settingsViewModel.setAnimatedLyricsBlurStrength(it) },
                                            valueText = { String.format(java.util.Locale.US, "%.1fx", it) }
                                        )
                                    }
                                    SwitchSettingItem(
                                        title = "Soft Light & Depth Blur",
                                        subtitle = "Subtly blur inactive lines to keep focus on the active lyric",
                                        checked = lyricsSoftLight,
                                        onCheckedChange = { enabled ->
                                            coroutineScope.launch {
                                                com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences.setSoftLight(context, enabled)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_blur_on_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_reset_imported_lyrics_title),
                                        subtitle = stringResource(R.string.setcat_reset_imported_lyrics_subtitle),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_clear_all_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        onClick = { showClearLyricsDialog = true }
                                    )
                                }

                                SettingsSubsection(
                                    title = "Performance & Battery",
                                    addBottomSpace = false
                                ) {
                                    SwitchSettingItem(
                                        title = "Performance Mode (Low-End & Battery Saver)",
                                        subtitle = "Optimizes animations, blurs, and downsamples images to keep the app smooth and light on budget devices.",
                                        checked = uiState.performanceModeEnabled,
                                        onCheckedChange = { settingsViewModel.setPerformanceModeEnabled(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_speed_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = "Hardware Audio Offload",
                                        subtitle = "Delegates audio decoding to hardware DSP to allow the CPU to sleep during playback, saving battery.",
                                        checked = uiState.audioOffloadEnabled,
                                        onCheckedChange = { settingsViewModel.setAudioOffloadEnabled(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_battery_charging_full_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                }
                            }

                            SettingsCategory.PLAYBACK -> {
                                SettingsSubsection(title = stringResource(R.string.setcat_background_playback)) {
                                    SwitchSettingItem(
                                        title = "Keep Playing in Background",
                                        subtitle = "Continue audio playback when the app is minimized or the screen is turned off.",
                                        checked = uiState.keepPlayingInBackground,
                                        onCheckedChange = { settingsViewModel.setKeepPlayingInBackground(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_play_circle_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    if (AndroidVersionCompat.supportsLiveNotificationOrIsland()) {
                                        val isDynamicIslandEnabled = uiState.dynamicIslandEnabled
                                        val dynamicIslandStyle = uiState.dynamicIslandStyle

                                        SwitchSettingItem(
                                            title = if (Build.VERSION.SDK_INT >= 36) "Live Activity / Dynamic Island" else "Dynamic Island (Origin OS special)",
                                            subtitle = "Displays real-time track progress and controls in the status bar pill. Turn off to save battery.",
                                            checked = isDynamicIslandEnabled,
                                            onCheckedChange = { enabled ->
                                                coroutineScope.launch {
                                                    playerViewModel.userPreferencesRepository.setDynamicIslandEnabled(enabled)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_auto_awesome_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        )

                                        if (isDynamicIslandEnabled) {
                                            ThemeSelectorItem(
                                                label = "Island Visualizer Style",
                                                description = "Choose what appears in the dynamic island pill",
                                                options = mapOf(
                                                    com.saurav.pixelmusic.data.preferences.DynamicIslandStyle.ANIMATED_NOTES.name to "Animated Notes",
                                                    com.saurav.pixelmusic.data.preferences.DynamicIslandStyle.PROGRESS_TIME.name to "Progress Time",
                                                    com.saurav.pixelmusic.data.preferences.DynamicIslandStyle.STATIC_ICON.name to "Static Icon"
                                                ),
                                                selectedKey = dynamicIslandStyle.name,
                                                onSelectionChanged = { key ->
                                                    coroutineScope.launch {
                                                        playerViewModel.userPreferencesRepository.setDynamicIslandStyle(
                                                            com.saurav.pixelmusic.data.preferences.DynamicIslandStyle.valueOf(key)
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(painterResource(R.drawable.rounded_music_note_24), null, tint = MaterialTheme.colorScheme.secondary)
                                                }
                                            )
                                        }
                                    }

                                    ThemeSelectorItem(
                                        label = "AOD Screen",
                                        description = "AMOLED ambient view via long-press on artwork or screen-off. Double-tap to exit.",
                                        options = mapOf(
                                            com.saurav.pixelmusic.data.preferences.AodScreenStyle.DISABLED.name to "Disabled",
                                            com.saurav.pixelmusic.data.preferences.AodScreenStyle.DEFAULT.name to "Ambient Artwork",
                                            com.saurav.pixelmusic.data.preferences.AodScreenStyle.LYRICS.name to "Live Lyrics"
                                        ),
                                        selectedKey = uiState.aodScreenStyle.name,
                                        onSelectionChanged = { key ->
                                            val selectedStyle = runCatching {
                                                com.saurav.pixelmusic.data.preferences.AodScreenStyle.valueOf(key)
                                            }.getOrDefault(com.saurav.pixelmusic.data.preferences.AodScreenStyle.DEFAULT)
                                            settingsViewModel.setAodScreenStyle(selectedStyle)
                                            if (selectedStyle != com.saurav.pixelmusic.data.preferences.AodScreenStyle.DISABLED &&
                                                android.os.Build.VERSION.SDK_INT >= 34
                                            ) {
                                                try {
                                                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                                                    if (nm != null && !nm.canUseFullScreenIntent() && !Settings.canDrawOverlays(context)) {
                                                        context.startActivity(
                                                            Intent(
                                                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                                "package:${context.packageName}".toUri()
                                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        )
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_dark_mode_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )

                                    

                                    SettingsItem(
                                        title = stringResource(R.string.setcat_battery_optimization_title),
                                        subtitle = stringResource(R.string.setcat_battery_optimization_subtitle),
                                        onClick = {
                                            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                                            if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                                Toast.makeText(context, context.getString(R.string.toast_battery_already_disabled), Toast.LENGTH_SHORT).show()
                                                return@SettingsItem
                                            }
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = "package:${context.packageName}".toUri()
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                    context.startActivity(fallbackIntent)
                                                } catch (e2: Exception) {
                                                    Toast.makeText(context, context.getString(R.string.toast_battery_settings_unavailable), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_all_inclusive_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                }

                                val playbackYtOnlineEnabled = !uiState.localModeEnabled
                                val showPlaybackLocalModeLockedToast: () -> Unit = {
                                    Toast.makeText(
                                        context,
                                        "This feature requires YouTube Music integration. Turn off Local Mode to use it.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                SettingsSubsection(title = stringResource(R.string.setcat_streaming_title)) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_streaming_audio_quality_wifi_title),
                                        description = stringResource(R.string.setcat_streaming_audio_quality_wifi_desc),
                                        options = StreamingAudioQuality.entries.associate { it.name to it.label },
                                        selectedKey = uiState.streamingAudioQualityWifi.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setStreamingAudioQualityWifi(StreamingAudioQuality.valueOf(key))
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_wifi_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_streaming_audio_quality_mobile_title),
                                        description = stringResource(R.string.setcat_streaming_audio_quality_mobile_desc),
                                        options = StreamingAudioQuality.entries.associate { it.name to it.label },
                                        selectedKey = uiState.streamingAudioQualityMobile.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setStreamingAudioQualityMobile(StreamingAudioQuality.valueOf(key))
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_mobile_speaker_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_force_high_quality_mobile_title),
                                        subtitle = stringResource(R.string.setcat_force_high_quality_mobile_desc),
                                        checked = uiState.forceHighQualityOnMobile,
                                        onCheckedChange = { settingsViewModel.setForceHighQualityOnMobile(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.outline_high_quality_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    SwitchSettingItem(
                                        title = "Download on like",
                                        subtitle = "Automatically download audio of liked YouTube songs to the public Music folder.",
                                        checked = uiState.cacheLikedSongsOffline,
                                        onCheckedChange = { settingsViewModel.setCacheLikedSongsOffline(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.round_favorite_border_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.presentation_batch_f_exp_visual_quality)
                                ) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_album_art_quality_wifi_title),
                                        description = stringResource(R.string.setcat_album_art_quality_wifi_desc),
                                        options = mapOf(
                                            AlbumArtQuality.LOW.name to stringResource(R.string.presentation_batch_f_album_art_quality_low_line),
                                            AlbumArtQuality.HIGH.name to stringResource(R.string.presentation_batch_f_album_art_quality_high_line)
                                        ),
                                        selectedKey = uiState.albumArtQuality.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setAlbumArtQuality(AlbumArtQuality.valueOf(key))
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_wifi_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_album_art_quality_mobile_title),
                                        description = stringResource(R.string.setcat_album_art_quality_mobile_desc),
                                        options = mapOf(
                                            AlbumArtQuality.LOW.name to stringResource(R.string.presentation_batch_f_album_art_quality_low_line),
                                            AlbumArtQuality.HIGH.name to stringResource(R.string.presentation_batch_f_album_art_quality_high_line)
                                        ),
                                        selectedKey = uiState.albumArtQualityMobile.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setAlbumArtQualityMobile(AlbumArtQuality.valueOf(key))
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_mobile_speaker_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                }

                                SettingsSubsection(title = "Queue Settings") {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_persistent_shuffle_title),
                                        subtitle = stringResource(R.string.setcat_persistent_shuffle_subtitle),
                                        checked = uiState.persistentShuffleEnabled,
                                        onCheckedChange = { settingsViewModel.setPersistentShuffleEnabled(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_shuffle_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_show_queue_history_title),
                                        subtitle = stringResource(R.string.setcat_show_queue_history_subtitle),
                                        checked = uiState.showQueueHistory,
                                        onCheckedChange = { settingsViewModel.setShowQueueHistory(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_auto_queue_enabled_title),
                                        subtitle = stringResource(R.string.setcat_auto_queue_enabled_desc),
                                        checked = uiState.autoQueueEnabled,
                                        onCheckedChange = { settingsViewModel.setAutoQueueEnabled(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_avoid_repetitive_songs_title),
                                        subtitle = stringResource(R.string.setcat_avoid_repetitive_songs_desc),
                                        checked = uiState.avoidRepetitiveSongs,
                                        onCheckedChange = { settingsViewModel.setAvoidRepetitiveSongs(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_shuffle_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_preload_queue_enabled_title),
                                        subtitle = stringResource(R.string.setcat_preload_queue_enabled_desc),
                                        checked = uiState.preloadQueueEnabled,
                                        onCheckedChange = { settingsViewModel.setPreloadQueueEnabled(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    AnimatedVisibility(
                                        visible = uiState.preloadQueueEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        ThemeSelectorItem(
                                            label = stringResource(R.string.setcat_preload_queue_size_title),
                                            description = stringResource(R.string.setcat_preload_queue_size_desc),
                                            options = mapOf(
                                                "1" to "1 song",
                                                "2" to "2 songs",
                                                "3" to "3 songs",
                                                "5" to "5 songs",
                                                "10" to "10 songs"
                                            ),
                                            selectedKey = uiState.preloadQueueSize.toString(),
                                            onSelectionChanged = { key ->
                                                settingsViewModel.setPreloadQueueSize(key.toInt())
                                            },
                                            leadingIcon = {
                                                Icon(painterResource(R.drawable.rounded_queue_music_24), null, tint = MaterialTheme.colorScheme.secondary)
                                            },
                                            enabled = playbackYtOnlineEnabled,
                                            onDisabledClick = showPlaybackLocalModeLockedToast
                                        )
                                    }
                                }

                                SettingsSubsection(title = "Download Management") {
                                    ThemeSelectorItem(
                                        label = "Download Audio Quality",
                                        description = "Select the default audio quality for offline music downloads.",
                                        options = StreamingAudioQuality.entries.associate { it.name to it.label },
                                        selectedKey = uiState.downloadAudioQuality.name,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setDownloadAudioQuality(StreamingAudioQuality.valueOf(key))
                                        },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_download_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    SwitchSettingItem(
                                        title = "Embed Rich Metadata & Lyrics",
                                        subtitle = "Embed full song tags, high-resolution artwork, and synchronized lyrics directly into downloaded files (increases file size)",
                                        checked = uiState.embedFullMetadataOnDownload,
                                        onCheckedChange = { settingsViewModel.setEmbedFullMetadataOnDownload(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_lyrics_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.setcat_music_storage_limit_desc),
                                        value = storageLimitDraft,
                                        valueRange = 0f..10240f,
                                        steps = 20,
                                        onValueChange = { storageLimitDraft = it },
                                        onValueChangeFinished = {
                                            val selectedLimit = storageLimitDraft.toInt()
                                            if (selectedLimit != uiState.storageLimitMb) {
                                                settingsViewModel.setStorageLimitMb(selectedLimit)
                                            }
                                        },
                                        valueText = { value ->
                                            if (value.toInt() == 0) "Unlimited"
                                            else if (value >= 1024) "${(value / 1024).toInt()} GB"
                                            else "${value.toInt()} MB"
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                }

                                SettingsSubsection(
                                    title = "Recommendations & YouTube Content",
                                    addBottomSpace = false
                                ) {
                                    SwitchSettingItem(
                                        title = "Pure YouTube Music",
                                        subtitle = "Filter out non-music video content and show only audio songs.",
                                        checked = uiState.pureYtMusicOnly,
                                        onCheckedChange = { settingsViewModel.setPureYtMusicOnly(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_music_note_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        enabled = playbackYtOnlineEnabled,
                                        onDisabledClick = showPlaybackLocalModeLockedToast
                                    )
                                }
                            }

                            SettingsCategory.AI_INTEGRATION -> {
                                SettingsSubsection(title = stringResource(R.string.setcat_ai_provider_section)) {
                                    ThemeSelectorItem(
                                        label = stringResource(R.string.setcat_ai_provider_label),
                                        description = stringResource(R.string.setcat_ai_provider_desc),
                                        options = com.saurav.pixelmusic.data.ai.provider.AiProvider.entries.associate { it.name to it.displayName },
                                        selectedKey = aiProvider,
                                        onSelectionChanged = { settingsViewModel.onAiProviderChange(it) },
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_science_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.setcat_safe_token_title),
                                        subtitle = if (uiState.isSafeTokenLimitEnabled) {
                                            stringResource(R.string.setcat_safe_token_on)
                                        } else {
                                            stringResource(R.string.setcat_safe_token_off)
                                        },
                                        checked = uiState.isSafeTokenLimitEnabled,
                                        onCheckedChange = { settingsViewModel.setSafeTokenLimitEnabled(it) },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.rounded_monitoring_24),
                                                null,
                                                tint = if (uiState.isSafeTokenLimitEnabled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_credentials)) {
                                    val provider = com.saurav.pixelmusic.data.ai.provider.AiProvider.fromString(aiProvider)
                                    val sourceLabel = when (provider) {
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.GEMINI -> stringResource(R.string.setcat_ai_source_gemini)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.DEEPSEEK -> stringResource(R.string.setcat_ai_source_deepseek)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.GROQ -> stringResource(R.string.setcat_ai_source_groq)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.MISTRAL -> stringResource(R.string.setcat_ai_source_mistral)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.NVIDIA -> stringResource(R.string.setcat_ai_source_nvidia)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.KIMI -> stringResource(R.string.setcat_ai_source_kimi)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.GLM -> stringResource(R.string.setcat_ai_source_glm)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.OPENAI -> stringResource(R.string.setcat_ai_source_openai)
                                        com.saurav.pixelmusic.data.ai.provider.AiProvider.OPENROUTER -> "OpenRouter (openrouter.ai)"
                                    }
                                    AiApiKeyItem(
                                        apiKey = currentAiApiKey,
                                        onApiKeySave = { settingsViewModel.onAiApiKeyChange(it) },
                                        title = stringResource(R.string.setcat_ai_api_key_title, provider.displayName),
                                        subtitle = stringResource(R.string.setcat_ai_api_key_subtitle, sourceLabel)
                                    )
                                }

                                if (currentAiApiKey.isNotBlank()) {
                                    SettingsSubsection(title = stringResource(R.string.setcat_model_selection)) {
                                        if (uiState.isLoadingModels) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.setcat_loading_models),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else if (uiState.modelsFetchError != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = uiState.modelsFetchError ?: stringResource(R.string.models_fetch_failed),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        } else if (uiState.availableModels.isNotEmpty()) {
                                            ThemeSelectorItem(
                                                label = stringResource(R.string.setcat_ai_model_label),
                                                description = stringResource(R.string.setcat_ai_model_desc),
                                                options = uiState.availableModels.associate { it.name to it.displayName },
                                                selectedKey = currentAiModel.ifEmpty {
                                                    uiState.availableModels.firstOrNull { model ->
                                                        val combined = (model.name + " " + model.displayName).lowercase()
                                                        combined.contains("3.5") && combined.contains("flash") && combined.contains("lite")
                                                    }?.name
                                                        ?: uiState.availableModels.firstOrNull { model ->
                                                            val combined = (model.name + " " + model.displayName).lowercase()
                                                            combined.contains("flash") && combined.contains("lite")
                                                        }?.name
                                                        ?: uiState.availableModels.firstOrNull { model ->
                                                            val combined = (model.name + " " + model.displayName).lowercase()
                                                            combined.contains("flash") && !combined.contains("tts")
                                                        }?.name
                                                        ?: uiState.availableModels.firstOrNull()?.name ?: ""
                                                },
                                                onSelectionChanged = { settingsViewModel.onAiModelChange(it) },
                                                leadingIcon = {
                                                    Icon(painterResource(R.drawable.rounded_science_24), null, tint = MaterialTheme.colorScheme.secondary)
                                                }
                                            )
                                        }
                                    }
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_prompt_behavior),
                                    addBottomSpace = false
                                ) {
                                    AiSystemPromptItem(
                                        systemPrompt = currentAiSystemPrompt,
                                        defaultPrompt = AiPreferencesRepository.DEFAULT_SYSTEM_PROMPT,
                                        onSystemPromptSave = { settingsViewModel.onAiSystemPromptChange(it) },
                                        onReset = { settingsViewModel.resetAiSystemPrompt() },
                                        title = stringResource(R.string.setcat_system_prompt_title),
                                        subtitle = stringResource(R.string.setcat_system_prompt_subtitle)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                SettingsSubsection(title = stringResource(R.string.setcat_ai_usage_report)) {
                                    val recentAiUsage by settingsViewModel.recentAiUsage.collectAsStateWithLifecycle()
                                    val totalPromptTokens by settingsViewModel.totalPromptTokens.collectAsStateWithLifecycle()
                                    val totalOutputTokens by settingsViewModel.totalOutputTokens.collectAsStateWithLifecycle()
                                    val totalThoughtTokens by settingsViewModel.totalThoughtTokens.collectAsStateWithLifecycle()
                                    val totalTokens = totalPromptTokens + totalOutputTokens + totalThoughtTokens
                                    val totalTokStr = String.format(Locale.US, "%,d", totalTokens)
                                    val promptTokStr = String.format(Locale.US, "%,d", totalPromptTokens)
                                    val outputTokStr = String.format(Locale.US, "%,d", totalOutputTokens)
                                    val thoughtTokStr = String.format(Locale.US, "%,d", totalThoughtTokens)

                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_total_consumption_title),
                                        subtitle = stringResource(
                                            R.string.setcat_ai_usage_tokens_subtitle,
                                            totalTokStr,
                                            promptTokStr,
                                            outputTokStr,
                                            thoughtTokStr
                                        ),
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_monitoring_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        },
                                        primaryActionLabel = stringResource(R.string.setcat_ai_clear_logs),
                                        onPrimaryAction = { settingsViewModel.clearAiUsageData() }
                                    )

                                    if (recentAiUsage.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        var expanded by remember { mutableStateOf(false) }
                                        val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expanded = !expanded },
                                            color = Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_monitoring_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = stringResource(R.string.setcat_ai_activity_log_title, recentAiUsage.size),
                                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = GoogleSansRounded),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_expand_more_24),
                                                    contentDescription = if (expanded) stringResource(R.string.cd_hide) else stringResource(R.string.cd_show),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.rotate(rotation)
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = expanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 8.dp)
                                            ) {
                                                val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                                val groupedUsage = recentAiUsage.groupBy {
                                                    dateFormat.format(Date(it.timestamp))
                                                }
                                                groupedUsage.forEach { (date, items) ->
                                                    AiUsageDateHeader(date = date)
                                                    items.forEach { usage ->
                                                        AiUsageLogItem(usage = usage)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.BACKUP_RESTORE -> {
                                if (!uiState.backupInfoDismissed) {
                                    BackupInfoNoticeCard(
                                        onDismiss = { settingsViewModel.setBackupInfoDismissed(true) }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_create_backup)) {
                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_export_backup_title),
                                        subtitle = stringResource(
                                            R.string.setcat_export_backup_subtitle,
                                            buildBackupSelectionSummary(context, exportSections)
                                        ),
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.outline_save_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        primaryActionLabel = stringResource(R.string.setcat_select_export),
                                        onPrimaryAction = { showExportDataDialog = true },
                                        enabled = !uiState.isDataTransferInProgress
                                    )
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_restore_backup_section),
                                    addBottomSpace = false
                                ) {
                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_import_backup_title),
                                        subtitle = stringResource(R.string.setcat_import_backup_subtitle),
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_restore_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        primaryActionLabel = stringResource(R.string.setcat_select_restore),
                                        onPrimaryAction = { showImportFlow = true },
                                        enabled = !uiState.isDataTransferInProgress
                                    )
                                }
                            }

                            SettingsCategory.DEVELOPER -> {
                                SettingsSubsection(title = "Network & Local Mode") {
                                    val toggleLocalModeAndRestart: (Boolean) -> Unit = { targetEnabled ->
                                        settingsViewModel.setLocalModeEnabled(targetEnabled) {
                                            Toast.makeText(
                                                context,
                                                if (targetEnabled) "Switching to Local Mode…" else "Switching to Online Mode…",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            val launchIntent = context.packageManager
                                                .getLaunchIntentForPackage(context.packageName)
                                                ?.apply {
                                                    addFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    )
                                                }
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                                Runtime.getRuntime().exit(0)
                                            }
                                        }
                                    }
                                    SwitchSettingItem(
                                        title = "Local Mode",
                                        subtitle = "Offline & downloaded music only",
                                        checked = uiState.localModeEnabled,
                                        onCheckedChange = { enabled -> toggleLocalModeAndRestart(enabled) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_public_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                    AnimatedVisibility(
                                        visible = uiState.localModeEnabled,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            SwitchSettingItem(
                                                title = "Internet for lyrics",
                                                subtitle = "Fetch online lyrics if not embedded",
                                                checked = uiState.localModeAllowOnlineLyrics,
                                                onCheckedChange = { allowed ->
                                                    settingsViewModel.setLocalModeAllowOnlineLyrics(allowed)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_lyrics_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            )
                                            SwitchSettingItem(
                                                title = "Auto-Queue",
                                                subtitle = "Queue similar local tracks",
                                                checked = uiState.autoQueueEnabled,
                                                onCheckedChange = { enabled ->
                                                    settingsViewModel.setAutoQueueEnabled(enabled)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_queue_music_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    SettingsItem(
                                        title = "Rescan library",
                                        subtitle = "Scan local storage for new or updated audio files",
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_sync_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        onClick = {
                                            settingsViewModel.refreshLibrary()
                                            Toast.makeText(context, "Scanning library…", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_experiments)) {
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_experimental_title),
                                        subtitle = stringResource(R.string.setcat_experimental_subtitle),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_science_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { navController.navigateSafely(Screen.Experimental.route) }
                                    )
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_test_setup_title),
                                        subtitle = stringResource(R.string.setcat_test_setup_subtitle),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_science_24), null, tint = MaterialTheme.colorScheme.tertiary)
                                        },
                                        onClick = {
                                            settingsViewModel.resetSetupFlow()
                                        }
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.setcat_maintenance)) {
                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_force_daily_mix_title),
                                        subtitle = stringResource(R.string.setcat_force_daily_mix_subtitle),
                                        icon = {
                                            Icon(painterResource(R.drawable.rounded_instant_mix_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        primaryActionLabel = stringResource(R.string.setcat_regenerate_daily_mix_action),
                                        onPrimaryAction = { showRegenerateDailyMixDialog = true }
                                    )
                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_force_stats_title),
                                        subtitle = stringResource(R.string.setcat_force_stats_subtitle),
                                        icon = {
                                            Icon(painterResource(R.drawable.rounded_monitoring_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        primaryActionLabel = stringResource(R.string.setcat_regenerate_stats_action),
                                        onPrimaryAction = { showRegenerateStatsDialog = true }
                                    )
                                    ActionSettingsItem(
                                        title = stringResource(R.string.setcat_force_palette_title),
                                        subtitle = if (paletteRegenerateTargets.isEmpty()) {
                                            stringResource(R.string.setcat_force_palette_empty)
                                        } else {
                                            stringResource(R.string.setcat_force_palette_subtitle)
                                        },
                                        icon = {
                                            Icon(painterResource(R.drawable.rounded_style_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        primaryActionLabel = if (isPaletteBulkRegenerateRunning) stringResource(R.string.setcat_regenerating) else stringResource(R.string.setcat_regenerate_all),
                                        onPrimaryAction = { showRegenerateAllPalettesDialog = true },
                                        secondaryActionLabel = stringResource(R.string.setcat_choose_song),
                                        onSecondaryAction = { showPaletteRegenerateSheet = true },
                                        enabled = paletteRegenerateTargets.isNotEmpty() && !isAnyPaletteRegenerateRunning
                                    )
                                }

                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_diagnostics),
                                    addBottomSpace = false
                                ) {
                                    SettingsItem(
                                        title = stringResource(R.string.settings_category_device_capabilities_title),
                                        subtitle = stringResource(R.string.settings_category_device_capabilities_subtitle),
                                        leadingIcon = {
                                            Icon(painterResource(R.drawable.rounded_developer_board_24), null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cd_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { navController.navigateSafely(Screen.DeviceCapabilities.route) }
                                    )
                                    SwitchSettingItem(
                                        title = "Universal App Logging",
                                        subtitle = "Record all app activity (verbose, debug, info, warn, error, fatal) to Logcat and internal log file. Turn off for zero logging.",
                                        checked = uiState.verboseLoggingEnabled,
                                        onCheckedChange = { settingsViewModel.setVerboseLoggingEnabled(it) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    )
                                    if (uiState.verboseLoggingEnabled) {
                                        SettingsItem(
                                            title = "Export / Share App Logs",
                                            subtitle = "Share or save the recorded log file (pixelmusic.log)",
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Share, null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            onClick = {
                                                val logPath = com.saurav.pixelmusic.utils.PixelLogger.logFilePath()
                                                if (logPath != null) {
                                                    val file = java.io.File(logPath)
                                                    if (file.exists() && file.length() > 0) {
                                                        val cacheFile = java.io.File(context.cacheDir, "pixelmusic_logs_${System.currentTimeMillis()}.txt")
                                                        file.copyTo(cacheFile, overwrite = true)
                                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            cacheFile
                                                        )
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(android.content.Intent.createChooser(intent, "Share PixelMusic Logs"))
                                                    } else {
                                                        Toast.makeText(context, "Log file is currently empty", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Log file not initialized", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        SettingsItem(
                                            title = "Clear App Logs",
                                            subtitle = "Wipe the current log buffer and log file",
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                                            },
                                            onClick = {
                                                com.saurav.pixelmusic.utils.PixelLogger.clear()
                                                Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_trigger_crash_title),
                                        subtitle = stringResource(R.string.setcat_trigger_crash_subtitle),
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = { settingsViewModel.triggerTestCrash() }
                                    )
                                }
                            }

                            SettingsCategory.ABOUT -> {
                                SettingsSubsection(
                                    title = stringResource(R.string.setcat_application),
                                    addBottomSpace = false
                                ) {
                                    SettingsItem(
                                        title = stringResource(R.string.setcat_about_pixelmusicer_title),
                                        subtitle = stringResource(R.string.setcat_about_pixelmusicer_subtitle),
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        trailingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        },
                                        onClick = { navController.navigateSafely("about") }
                                    )
                                }
                            }

                            SettingsCategory.PIXEL_JAM -> {
                                // PixelJam has its own dedicated screen
                            }

                            SettingsCategory.EQUALIZER -> {
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    navController.navigateSafely(Screen.Equalizer.route) {
                                        popUpTo(Screen.SettingsCategory.route) { inclusive = true }
                                    }
                                }
                            }

                            SettingsCategory.DEVICE_CAPABILITIES -> {
                                // Device Capabilities has its own screen
                            }

                            SettingsCategory.LASTFM -> {
                                var isStartingBrowserAuth by remember { mutableStateOf(false) }
                                var pendingWebToken by remember { mutableStateOf<String?>(null) }
                                val loginCoroutineScope = rememberCoroutineScope()

                                LaunchedEffect(pendingWebToken) {
                                    val token = pendingWebToken ?: return@LaunchedEffect
                                    while (pendingWebToken == token) {
                                        delay(3000L)
                                        com.saurav.pixelmusic.data.lastfm.LastFM.initialize(
                                            com.saurav.pixelmusic.BuildConfig.LASTFM_API_KEY,
                                            com.saurav.pixelmusic.BuildConfig.LASTFM_SECRET
                                        )
                                        val sessionResult = com.saurav.pixelmusic.data.lastfm.LastFM.getSession(token)
                                        sessionResult.fold(
                                            onSuccess = { authSession ->
                                                val sk = authSession.session.key
                                                val name = authSession.session.name
                                                settingsViewModel.setLastfmSession(sk)
                                                settingsViewModel.setLastfmUsername(name)
                                                com.saurav.pixelmusic.data.lastfm.LastFM.sessionKey = sk
                                                pendingWebToken = null
                                                Toast.makeText(context, context.getString(R.string.lastfm_login_success), Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { throwable ->
                                                val lastFmErr = throwable as? com.saurav.pixelmusic.data.lastfm.LastFM.LastFmException
                                                if (lastFmErr != null && lastFmErr.code != 14) {
                                                    pendingWebToken = null
                                                    Toast.makeText(context, lastFmErr.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }

                                SettingsSubsection(title = stringResource(R.string.settings_accounts_row_title)) {
                                    val isLoggedIn = uiState.lastfmSession.isNotEmpty()
                                    SettingsItem(
                                        title = when {
                                            isLoggedIn -> stringResource(R.string.lastfm_logged_in_as, uiState.lastfmUsername)
                                            isStartingBrowserAuth -> "Opening Browser…"
                                            pendingWebToken != null -> "Waiting for Browser Approval…"
                                            else -> stringResource(R.string.lastfm_not_logged_in)
                                        },
                                        subtitle = when {
                                            isLoggedIn -> "Click to Log Out"
                                            pendingWebToken != null -> "Tap 'Yes, allow access' in your browser (tap to reopen)"
                                            else -> "Click to Log In and authorize scrobbling"
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        },
                                        trailingIcon = {
                                            if (!isLoggedIn && (isStartingBrowserAuth || pendingWebToken != null)) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (isLoggedIn) {
                                                pendingWebToken = null
                                                settingsViewModel.setLastfmSession("")
                                                settingsViewModel.setLastfmUsername("")
                                                com.saurav.pixelmusic.data.lastfm.LastFM.sessionKey = null
                                                Toast.makeText(context, "Logged out from Last.fm", Toast.LENGTH_SHORT).show()
                                            } else if (!isStartingBrowserAuth) {
                                                isStartingBrowserAuth = true
                                                loginCoroutineScope.launch {
                                                    com.saurav.pixelmusic.data.lastfm.LastFM.initialize(
                                                        com.saurav.pixelmusic.BuildConfig.LASTFM_API_KEY,
                                                        com.saurav.pixelmusic.BuildConfig.LASTFM_SECRET
                                                    )
                                                    val tokenResult = com.saurav.pixelmusic.data.lastfm.LastFM.getToken()
                                                    tokenResult.fold(
                                                        onSuccess = { tokenResponse ->
                                                            isStartingBrowserAuth = false
                                                            pendingWebToken = tokenResponse.token
                                                            val authUrl = com.saurav.pixelmusic.data.lastfm.LastFM.getAuthUrl(tokenResponse.token)
                                                            runCatching {
                                                                context.startActivity(Intent(Intent.ACTION_VIEW, authUrl.toUri()))
                                                            }.onFailure {
                                                                pendingWebToken = null
                                                                Toast.makeText(context, "Could not open browser for Last.fm authorization", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        onFailure = { throwable ->
                                                            isStartingBrowserAuth = false
                                                            Toast.makeText(
                                                                context,
                                                                throwable.localizedMessage ?: "Failed to start browser login",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }

                                SettingsSubsection(title = "Options") {
                                    SwitchSettingItem(
                                        title = stringResource(R.string.lastfm_enable_scrobbling),
                                        subtitle = stringResource(R.string.lastfm_enable_scrobbling_desc),
                                        checked = uiState.lastfmScrobblingEnabled,
                                        onCheckedChange = { settingsViewModel.setLastfmScrobblingEnabled(it) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_music_note_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                    SwitchSettingItem(
                                        title = stringResource(R.string.lastfm_now_playing),
                                        subtitle = stringResource(R.string.lastfm_now_playing_desc),
                                        checked = uiState.lastfmUseNowPlaying,
                                        onCheckedChange = { settingsViewModel.setLastfmUseNowPlaying(it) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_touch_app_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                }

                                SettingsSubsection(title = "Smart Mix Playlists") {
                                    ThemeSelectorItem(
                                        label = "Generated Playlists Retention",
                                        description = "Choose how long to keep generated Last.fm mix playlists in your library.",
                                        options = mapOf(
                                            "permanent" to "Permanently Store",
                                            "24_hours" to "Delete after 24 hours",
                                            "7_days" to "Delete after 7 days",
                                            "30_days" to "Delete after 30 days"
                                        ),
                                        selectedKey = uiState.generatedPlaylistsRetentionPeriod,
                                        onSelectionChanged = { key ->
                                            settingsViewModel.setGeneratedPlaylistsRetentionPeriod(key)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_timer_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                }

                                SettingsSubsection(title = stringResource(R.string.lastfm_scrobbling_configuration)) {
                                    SliderSettingsItem(
                                        label = stringResource(R.string.lastfm_scrobble_min_track_duration, uiState.scrobbleMinSongDuration),
                                        value = uiState.scrobbleMinSongDuration.toFloat(),
                                        valueRange = 10f..120f,
                                        steps = 11,
                                        onValueChange = { settingsViewModel.setScrobbleMinSongDuration(it.toInt()) },
                                        valueText = { "${it.toInt()}s" }
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.lastfm_scrobble_delay_percent, (uiState.scrobbleDelayPercent * 100).toInt()),
                                        value = uiState.scrobbleDelayPercent,
                                        valueRange = 0.3f..0.95f,
                                        steps = 0,
                                        onValueChange = { settingsViewModel.setScrobbleDelayPercent(it) },
                                        valueText = { "${(it * 100).toInt()}%" }
                                    )
                                    SliderSettingsItem(
                                        label = stringResource(R.string.lastfm_scrobble_delay_seconds, uiState.scrobbleDelaySeconds),
                                        value = uiState.scrobbleDelaySeconds.toFloat(),
                                        valueRange = 30f..360f,
                                        steps = 11,
                                        onValueChange = { settingsViewModel.setScrobbleDelaySeconds(it.toInt()) },
                                        valueText = { "${it.toInt()}s" }
                                    )
                                }


                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(1.dp))
                }
            }

            CollapsibleCommonTopBar(
                collapseFraction = collapseFraction,
                headerHeight = currentTopBarHeightDp,
                onBackClick = onBackClick,
                title = categoryTitle,
                maxLines = titleMaxLines
            )

            // Block interaction during transition
            var isTransitioning by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(com.saurav.pixelmusic.presentation.navigation.TRANSITION_DURATION.toLong())
                isTransitioning = false
            }
            if (isTransitioning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                )
            }
        }
    }

    BackupTransferProgressDialogHost(progress = dataTransferProgress)

    // Dialogs
    FileExplorerDialog(
        visible = showExplorerSheet,
        currentPath = currentPath,
        directoryChildren = directoryChildren,
        availableStorages = availableStorages,
        selectedStorageIndex = selectedStorageIndex,
        isLoading = isLoadingDirectories,
        isPriming = isExplorerPriming,
        isReady = isExplorerReady,
        isCurrentDirectoryResolved = isCurrentDirectoryResolved,
        isAtRoot = settingsViewModel.isAtRoot(),
        rootDirectory = explorerRoot,
        onNavigateTo = settingsViewModel::loadDirectory,
        onNavigateUp = settingsViewModel::navigateUp,
        onNavigateHome = { settingsViewModel.loadDirectory(explorerRoot) },
        onToggleAllowed = settingsViewModel::toggleDirectoryAllowed,
        onRefresh = settingsViewModel::refreshExplorer,
        onStorageSelected = settingsViewModel::selectStorage,
        onDone = {
            settingsViewModel.applyPendingDirectoryRuleChanges()
            showExplorerSheet = false
        },
        onDismiss = {
            settingsViewModel.applyPendingDirectoryRuleChanges()
            showExplorerSheet = false
        }
    )

    if (showPaletteRegenerateSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = {
                if (!isAnyPaletteRegenerateRunning) {
                    showPaletteRegenerateSheet = false
                    paletteSongSearchQuery = ""
                }
            },
            sheetState = paletteRegenerateSheetState
        ) {
            PaletteRegenerateSongSheetContent(
                songs = filteredPaletteSongs,
                isRunning = isPaletteRegenerateRunning,
                searchQuery = paletteSongSearchQuery,
                onSearchQueryChange = { paletteSongSearchQuery = it },
                onClearSearch = { paletteSongSearchQuery = "" },
                onSongClick = { song ->
                    if (isAnyPaletteRegenerateRunning) return@PaletteRegenerateSongSheetContent
                    isPaletteRegenerateRunning = true
                    coroutineScope.launch {
                        val success = playerViewModel.forceRegenerateAlbumPaletteForSong(song)
                        isPaletteRegenerateRunning = false
                        if (success) {
                            paletteSongSearchQuery = ""
                            Toast.makeText(
                                context,
                                context.getString(R.string.dialog_palette_regenerated, song.title),
                                Toast.LENGTH_SHORT
                            ).show()
                            paletteRegenerateSheetState.hide()
                            showPaletteRegenerateSheet = false
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.dialog_palette_regenerate_failed, song.title),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    if (showRegenerateAllPalettesDialog) {
        AlertDialog(
            icon = {
                Icon(
                    painterResource(R.drawable.rounded_style_24),
                    null,
                    tint = if (isPaletteBulkRegenerateRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            },
            title = {
                Text(
                    if (isPaletteBulkRegenerateRunning) {
                        stringResource(R.string.dialog_regenerating_palettes_title)
                    } else {
                        stringResource(R.string.dialog_regenerate_palettes_title)
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isPaletteBulkRegenerateRunning) {
                            stringResource(R.string.dialog_regenerate_palettes_progress_body, paletteBulkTotalCount)
                        } else {
                            stringResource(R.string.dialog_regenerate_palettes_confirm_body, paletteRegenerateTargets.size)
                        }
                    )
                    if (isPaletteBulkRegenerateRunning) {
                        val progress = if (paletteBulkTotalCount > 0) {
                            paletteBulkCompletedCount.toFloat() / paletteBulkTotalCount.toFloat()
                        } else {
                            0f
                        }
                        LinearWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = stringResource(R.string.dialog_palette_progress_format, paletteBulkCompletedCount, paletteBulkTotalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onDismissRequest = {
                if (!isPaletteBulkRegenerateRunning) {
                    showRegenerateAllPalettesDialog = false
                }
            },
            confirmButton = {
                if (isPaletteBulkRegenerateRunning) {
                    TextButton(
                        onClick = {},
                        enabled = false
                    ) {
                        Text(stringResource(R.string.working_ellipsis), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    TextButton(
                        onClick = {
                            isPaletteBulkRegenerateRunning = true
                            paletteBulkCompletedCount = 0
                            paletteBulkTotalCount = paletteRegenerateTargets.size
                            coroutineScope.launch {
                                var successCount = 0
                                paletteRegenerateTargets.forEachIndexed { index, song ->
                                    if (playerViewModel.forceRegenerateAlbumPaletteForSong(song)) {
                                        successCount++
                                    }
                                    paletteBulkCompletedCount = index + 1
                                }
                                isPaletteBulkRegenerateRunning = false
                                showRegenerateAllPalettesDialog = false
                                val totalCount = paletteRegenerateTargets.size
                                Toast.makeText(
                                    context,
                                    if (successCount == totalCount) {
                                        context.getString(R.string.toast_regenerated_palettes_all, successCount)
                                    } else {
                                        context.getString(R.string.toast_regenerated_palettes_partial, successCount, totalCount)
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.dialog_regenerate))
                    }
                }
            },
            dismissButton = {
                if (!isPaletteBulkRegenerateRunning) {
                    TextButton(
                        onClick = { showRegenerateAllPalettesDialog = false }
                    ) {
                        Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        )
    }

    if (showClearLyricsDialog) {
        AlertDialog(
            icon = { Icon(Icons.Outlined.Warning, null) },
            title = { Text(stringResource(R.string.dialog_reset_lyrics_title)) },
            text = { Text(stringResource(R.string.dialog_cannot_undo)) },
            onDismissRequest = { showClearLyricsDialog = false },
            confirmButton = {
                TextButton(onClick = { showClearLyricsDialog = false; playerViewModel.resetAllLyrics() }) {
                    Text(stringResource(R.string.confirm), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLyricsDialog = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    if (showRebuildDatabaseWarning) {
        AlertDialog(
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.dialog_rebuild_database_title)) },
            text = { Text(stringResource(R.string.dialog_rebuild_database_message)) },
            onDismissRequest = { showRebuildDatabaseWarning = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebuildDatabaseWarning = false
                        refreshRequested = true
                        syncRequestObservedRunning = false
                        syncIndicatorLabel = context.getString(R.string.sync_indicator_rebuilding)
                        Toast.makeText(context, context.getString(R.string.toast_rebuilding_database), Toast.LENGTH_SHORT).show()
                        settingsViewModel.rebuildDatabase()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.rebuild))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildDatabaseWarning = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    if (showRegenerateDailyMixDialog) {
        AlertDialog(
            icon = { Icon(painterResource(R.drawable.rounded_instant_mix_24), null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.dialog_regenerate_daily_mix_title)) },
            text = { Text(stringResource(R.string.dialog_regenerate_daily_mix_body)) },
            onDismissRequest = { showRegenerateDailyMixDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateDailyMixDialog = false
                        playerViewModel.forceUpdateDailyMix()
                        Toast.makeText(context, context.getString(R.string.toast_daily_mix_regeneration_started), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.dialog_regenerate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDailyMixDialog = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    if (showRegenerateStatsDialog) {
        AlertDialog(
            icon = { Icon(painterResource(R.drawable.rounded_monitoring_24), null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.dialog_regenerate_stats_title)) },
            text = { Text(stringResource(R.string.dialog_regenerate_stats_body)) },
            onDismissRequest = { showRegenerateStatsDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateStatsDialog = false
                        statsViewModel.forceRegenerateStats()
                        Toast.makeText(context, context.getString(R.string.toast_stats_regeneration_started), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.dialog_regenerate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateStatsDialog = false }) {
                    Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }

    if (showExportDataDialog) {
        BackupSectionSelectionDialog(
            operation = BackupOperationType.EXPORT,
            title = stringResource(R.string.setcat_export_backup_title),
            supportingText = stringResource(R.string.backup_dialog_supporting_export),
            selectedSections = exportSections,
            confirmLabel = stringResource(R.string.backup_confirm_export_pxpl),
            inProgress = uiState.isDataTransferInProgress,
            onDismiss = { showExportDataDialog = false },
            onSelectionChanged = { exportSections = it },
            onConfirm = {
                showExportDataDialog = false
                val fileName = context.getString(R.string.backup_file_name_format, System.currentTimeMillis())
                exportLauncher.launch(fileName)
            }
        )
    }

    if (showImportFlow) {
        val restorePlan = uiState.restorePlan
        if (restorePlan != null && importFileUri != null) {
            ImportModuleSelectionDialog(
                plan = restorePlan,
                inProgress = uiState.isDataTransferInProgress,
                onDismiss = {
                    showImportFlow = false
                    importFileUri = null
                    settingsViewModel.clearRestorePlan()
                },
                onBack = {
                    importFileUri = null
                    settingsViewModel.clearRestorePlan()
                },
                onSelectionChanged = { settingsViewModel.updateRestorePlanSelection(it) },
                onConfirm = {
                    settingsViewModel.restoreFromPlan(importFileUri!!)
                    showImportFlow = false
                    importFileUri = null
                }
            )
        } else {
            ImportFileSelectionDialog(
                backupHistory = uiState.backupHistory,
                isInspecting = uiState.isInspectingBackup,
                onDismiss = {
                    showImportFlow = false
                    importFileUri = null
                    settingsViewModel.clearRestorePlan()
                },
                onBrowseFile = { importFilePicker.launch("*/*") },
                onHistoryItemSelected = { entry ->
                    val uri = entry.uri.toUri()
                    importFileUri = uri
                    settingsViewModel.inspectBackupFile(uri)
                },
                onRemoveHistoryEntry = { settingsViewModel.removeBackupHistoryEntry(it) }
            )
        }
    }
}

private fun buildBackupSelectionSummary(context: Context, selected: Set<BackupSection>): String {
    if (selected.isEmpty()) return context.getString(R.string.backup_summary_none)
    val total = BackupSection.entries.size
    return if (selected.size == total) {
        context.getString(R.string.backup_summary_all)
    } else {
        context.getString(R.string.backup_summary_partial, selected.size, total)
    }
}

private fun backupSectionIconRes(section: BackupSection): Int {
    return section.iconRes
}

@Composable
private fun BackupInfoNoticeCard(
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_upload_file_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_how_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.backup_how_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_close_24),
                    contentDescription = stringResource(R.string.cd_close_notice),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupSectionSelectionDialog(
    operation: BackupOperationType,
    title: String,
    supportingText: String,
    selectedSections: Set<BackupSection>,
    confirmLabel: String,
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onSelectionChanged: (Set<BackupSection>) -> Unit,
    onConfirm: () -> Unit
) {
    val listState = rememberLazyListState()
    val selectedCount = selectedSections.size
    val totalCount = BackupSection.entries.size
    val transitionState = remember { MutableTransitionState(false) }
    var shouldShowDialog by remember { mutableStateOf(true) }
    var onDialogHiddenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var initialOpenComplete by remember { mutableStateOf(false) }
    transitionState.targetState = shouldShowDialog

    LaunchedEffect(Unit) {
        delay(320)
        initialOpenComplete = true
    }

    fun closeDialog(afterClose: () -> Unit) {
        if (!shouldShowDialog) return
        onDialogHiddenAction = afterClose
        shouldShowDialog = false
    }

    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (!transitionState.currentState && !transitionState.targetState) {
            onDialogHiddenAction?.let { action ->
                onDialogHiddenAction = null
                action()
            }
        }
    }

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = { closeDialog(onDismiss) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(
                    initialOffsetY = { it / 10 },
                    animationSpec = tween(360, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing)
                ) + fadeIn(tween(260)) + androidx.compose.animation.scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(340, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 10 },
                    animationSpec = tween(240, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing)
                ) + fadeOut(tween(200)) + androidx.compose.animation.scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(240)
                ),
                label = "backup_section_dialog"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentWindowInsets = WindowInsets.systemBars,
                        topBar = {
                            Box {
                                ProgressiveHeaderBackdrop(
                                    scrimColor = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.matchParentSize()
                                )
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 24.sp,
                                                textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                                            ),
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    navigationIcon = {
                                        FilledIconButton(
                                            modifier = Modifier.padding(start = 6.dp),
                                            onClick = { closeDialog(onDismiss) },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.cd_close)
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        scrolledContainerColor = Color.Transparent
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            BottomAppBar(
                                windowInsets = WindowInsets.navigationBars,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledIconButton(
                                            onClick = { onSelectionChanged(BackupSection.entries.toSet()) },
                                            enabled = !inProgress,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.round_select_all_24),
                                                contentDescription = stringResource(R.string.cd_select_all)
                                            )
                                        }
                                        FilledIconButton(
                                            onClick = { onSelectionChanged(emptySet()) },
                                            enabled = !inProgress,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_deselect_24),
                                                contentDescription = stringResource(R.string.cd_clear_selection)
                                            )
                                        }
                                    }
                                    ExtendedFloatingActionButton(
                                        onClick = { closeDialog(onConfirm) },
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .height(48.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        containerColor = if (operation == BackupOperationType.EXPORT) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        contentColor = if (operation == BackupOperationType.EXPORT) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    ) {
                                        if (inProgress) {
                                            LoadingIndicator(modifier = Modifier.height(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (operation == BackupOperationType.EXPORT) stringResource(R.string.backup_exporting) else stringResource(R.string.backup_importing),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (operation == BackupOperationType.EXPORT) {
                                                            R.drawable.outline_save_24
                                                        } else {
                                                            R.drawable.rounded_upload_file_24
                                                        }
                                                    ),
                                                    contentDescription = confirmLabel
                                                )
                                                Text(
                                                    text = if (operation == BackupOperationType.EXPORT) {
                                                        stringResource(R.string.setcat_export_backup_title)
                                                    } else {
                                                        stringResource(R.string.setcat_import_backup_title)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        val allSections = BackupSection.entries
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item(key = "backup_export_header") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = supportingText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AnimatedContent(
                                            targetState = selectedCount,
                                            transitionSpec = {
                                                (fadeIn(tween(160)) + slideInVertically { it / 2 })
                                                    .togetherWith(fadeOut(tween(120)) + slideOutVertically { -it / 2 })
                                            },
                                            label = "export_selected_count"
                                        ) { count ->
                                            Text(
                                                text = stringResource(R.string.backup_sections_selected_format, count, totalCount),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = GoogleSansRounded,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (inProgress) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                LoadingIndicator(modifier = Modifier.height(24.dp))
                                                Text(
                                                    text = stringResource(R.string.backup_transfer_in_progress),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(allSections, key = { _, it -> it.key }) { index, section ->
                                val isSelected = section in selectedSections
                                val entrance = remember {
                                    Animatable(if (initialOpenComplete) 1f else 0f)
                                }
                                LaunchedEffect(Unit) {
                                    if (entrance.value < 1f) {
                                        delay(30L + (index.coerceAtMost(7) * 30L))
                                        entrance.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                                BackupSectionSelectableCard(
                                    section = section,
                                    selected = isSelected,
                                    enabled = !inProgress,
                                    isFirst = index == 0,
                                    isLast = index == allSections.lastIndex,
                                    modifier = Modifier
                                        .animateItem()
                                        .expressiveListItemScrollAnimation(listState)
                                        .graphicsLayer {
                                            val p = entrance.value
                                            alpha = p.coerceIn(0f, 1f)
                                            translationY = (1f - p) * 24f
                                        },
                                    onToggle = {
                                        onSelectionChanged(
                                            if (isSelected) selectedSections - section else selectedSections + section
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupSectionSelectableCard(
    section: BackupSection,
    selected: Boolean,
    enabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    detail: ModuleRestoreDetail? = null,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backup_export_press_scale"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (selected) 16.dp else 4.dp,
        animationSpec = tween(260, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing),
        label = "backup_export_inner_corner"
    )
    val topRadius = if (isFirst) 24.dp else innerCorner
    val bottomRadius = if (isLast) 24.dp else innerCorner

    val cardColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(240, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing),
        label = "backup_export_card_bg"
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(220, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing),
        label = "backup_section_icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing),
        label = "backup_section_icon_tint"
    )
    val iconCorner by animateDpAsState(
        targetValue = if (selected) 24.dp else 14.dp,
        animationSpec = tween(260, easing = com.saurav.pixelmusic.presentation.components.AiSmoothMorphEasing),
        label = "backup_export_icon_corner"
    )

    Surface(
        onClick = onToggle,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        ),
        color = cardColor,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = iconContainerColor,
                shape = RoundedCornerShape(iconCorner),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(backupSectionIconRes(section)),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansRounded
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (detail != null && detail.entryCount > 0) {
                    Text(
                        text = stringResource(R.string.backup_entries_will_replace, detail.entryCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                thumbContent = {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                        label = "switch_thumb_icon"
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

private const val BackupTransferDialogMinimumVisibilityMs = 1500L

@Composable
private fun BackupTransferProgressDialogHost(progress: BackupTransferProgressUpdate?) {
    var visibleProgress by remember { mutableStateOf<BackupTransferProgressUpdate?>(null) }
    var visibleSinceMs by remember { mutableStateOf(0L) }
    var isHoldingForMinimumTime by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (progress != null) {
            if (visibleProgress == null || isHoldingForMinimumTime) {
                visibleSinceMs = SystemClock.elapsedRealtime()
            }
            isHoldingForMinimumTime = false
            visibleProgress = progress
            return@LaunchedEffect
        }
        val currentVisibleProgress = visibleProgress ?: return@LaunchedEffect
        isHoldingForMinimumTime = true
        val elapsed = SystemClock.elapsedRealtime() - visibleSinceMs
        val remaining = BackupTransferDialogMinimumVisibilityMs - elapsed
        if (remaining > 0) {
            delay(remaining)
        }
        if (visibleProgress == currentVisibleProgress) {
            visibleProgress = null
            visibleSinceMs = 0L
        }
        isHoldingForMinimumTime = false
    }

    visibleProgress?.let { currentProgress ->
        BackupTransferProgressDialog(progress = currentProgress)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupTransferProgressDialog(progress: BackupTransferProgressUpdate) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "BackupTransferProgress"
    )
    val progressPercent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)
    val statusText = when (progress.operation) {
        BackupOperationType.EXPORT -> stringResource(R.string.backup_exporting)
        else -> stringResource(R.string.backup_importing)
    }
    val stepText = stringResource(
        R.string.backup_step_format,
        progress.step.coerceAtLeast(1),
        progress.totalSteps
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (progress.operation == BackupOperationType.EXPORT) {
                        stringResource(R.string.backup_dialog_creating)
                    } else {
                        stringResource(R.string.backup_dialog_restoring)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.84f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.presentation_batch_f_percent, progressPercent),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * 1.4f
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    text = progress.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.presentation_batch_f_status_bullet_step, statusText, stepText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                AnimatedContent(
                    targetState = progress.detail,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "BackupStepDetail"
                ) { animatedDetail ->
                    Text(
                        text = animatedDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                progress.section?.let { section ->
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportFileSelectionDialog(
    backupHistory: List<BackupHistoryEntry>,
    isInspecting: Boolean,
    onDismiss: () -> Unit,
    onBrowseFile: () -> Unit,
    onHistoryItemSelected: (BackupHistoryEntry) -> Unit,
    onRemoveHistoryEntry: (BackupHistoryEntry) -> Unit
) {
    val context = LocalContext.current
    val transitionState = remember { MutableTransitionState(false) }
    var shouldShowDialog by remember { mutableStateOf(true) }
    var onDialogHiddenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    transitionState.targetState = shouldShowDialog

    fun closeDialog(afterClose: () -> Unit) {
        if (!shouldShowDialog) return
        onDialogHiddenAction = afterClose
        shouldShowDialog = false
    }

    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (!transitionState.currentState && !transitionState.targetState) {
            onDialogHiddenAction?.let { action ->
                onDialogHiddenAction = null
                action()
            }
        }
    }

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = { closeDialog(onDismiss) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(animationSpec = tween(200)),
                label = "import_file_selection_dialog"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentWindowInsets = WindowInsets.systemBars,
                        topBar = {
                            Box {
                                ProgressiveHeaderBackdrop(
                                    scrimColor = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.matchParentSize()
                                )
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = stringResource(R.string.import_backup_toolbar_title),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 24.sp,
                                                textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                                            ),
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    navigationIcon = {
                                        FilledIconButton(
                                            modifier = Modifier.padding(start = 6.dp),
                                            onClick = { closeDialog(onDismiss) },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.cd_close)
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        scrolledContainerColor = Color.Transparent
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            BottomAppBar(
                                windowInsets = WindowInsets.navigationBars,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExtendedFloatingActionButton(
                                        onClick = onBrowseFile,
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        if (isInspecting) {
                                            LoadingIndicator(modifier = Modifier.height(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.import_inspecting),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_upload_file_24),
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.import_browse_file),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.import_backup_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (backupHistory.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.import_recent_backups),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (backupHistory.isEmpty()) {
                                    item {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_restore_24),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = stringResource(R.string.import_no_recent_title),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(R.string.import_no_recent_subtitle),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(backupHistory, key = { it.uri }) { entry ->
                                        BackupHistoryCard(
                                            entry = entry,
                                            context = context,
                                            onSelect = { onHistoryItemSelected(entry) },
                                            onRemove = { onRemoveHistoryEntry(entry) }
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
}

@Composable
private fun BackupHistoryCard(
    entry: BackupHistoryEntry,
    context: Context,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    val unknownShort = stringResource(R.string.presentation_batch_f_unknown_short)
    val dateText = remember(entry.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        sdf.format(Date(entry.createdAt))
    }
    val sizeText = remember(entry.sizeBytes) {
        if (entry.sizeBytes > 0) Formatter.formatShortFileSize(context, entry.sizeBytes) else ""
    }
    val moduleCount = entry.modules.size

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_upload_file_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = GoogleSansRounded
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateText +
                            if (sizeText.isNotEmpty()) {
                                stringResource(R.string.presentation_batch_f_dot_space_suffix, sizeText)
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.cd_remove_from_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.backup_history_modules_line,
                        moduleCount,
                        entry.appVersion.ifEmpty { unknownShort },
                        entry.schemaVersion
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportModuleSelectionDialog(
    plan: RestorePlan,
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSelectionChanged: (Set<BackupSection>) -> Unit,
    onConfirm: () -> Unit
) {
    BackupModuleSelectionDialog(
        plan = plan,
        inProgress = inProgress,
        onDismiss = onDismiss,
        onBack = onBack,
        onSelectionChanged = onSelectionChanged,
        onConfirm = onConfirm
    )
}

@Composable
private fun PaletteRegenerateSongSheetContent(
    songs: List<Song>,
    isRunning: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.palette_force_regenerate_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.palette_force_regenerate_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isRunning,
            placeholder = { Text(stringResource(R.string.search_songs_by_title_artist_album)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = onClearSearch,
                        enabled = !isRunning
                    ) {
                        Icon(painterResource(R.drawable.rounded_clear_all_24), contentDescription = stringResource(R.string.cd_clear_search))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.palette_regenerating_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (songs.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.search_no_songs_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRunning) { onSongClick(song) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.displayArtist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (song.album.isNotBlank()) {
                                Text(
                                    text = song.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSubsectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSubsection(
    title: String,
    addBottomSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    SettingsSubsectionHeader(title)
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
