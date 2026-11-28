@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.presentation.components.TopHalfBackgroundLayer
import com.saurav.pixelmusic.ui.blur.ProgressiveHeaderBackdrop
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.blur.rememberGlassState

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import com.saurav.pixelmusic.utils.AndroidVersionCompat
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.CollagePattern
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.presentation.components.AlbumArtCollage
import com.saurav.pixelmusic.presentation.components.BetaInfoBottomSheet
import com.saurav.pixelmusic.presentation.components.ChangelogBottomSheet
import com.saurav.pixelmusic.presentation.components.DailyMixSection
import com.saurav.pixelmusic.presentation.components.FavoriteArtistReleasesSection
import com.saurav.pixelmusic.presentation.components.HomeOptionsBottomSheet
import com.saurav.pixelmusic.presentation.components.HomeShuffleFab
import com.saurav.pixelmusic.presentation.components.InstagramPromoDialog
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.MusicRecognitionOverlay
import androidx.compose.foundation.layout.navigationBars
import com.saurav.pixelmusic.presentation.components.QuickPicksSection
import com.saurav.pixelmusic.presentation.components.RecentlyPlayedSection
import com.saurav.pixelmusic.presentation.components.SongInfoBottomSheet
import com.saurav.pixelmusic.presentation.components.PlaylistBottomSheet
import com.saurav.pixelmusic.presentation.components.RecentlyPlayedSectionMinSongsToShow
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.StatsOverviewCard
import com.saurav.pixelmusic.presentation.components.StreamingProviderSheet
import com.saurav.pixelmusic.presentation.components.UpdateNotificationSheet
import com.saurav.pixelmusic.presentation.components.resolveMainScreenBottomGradientHeight
import com.saurav.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.saurav.pixelmusic.presentation.components.subcomps.PlayingEqIcon
import com.saurav.pixelmusic.presentation.components.subcomps.SineWaveLine
import com.saurav.pixelmusic.presentation.model.collectRecentlyPlayedSongIds
import com.saurav.pixelmusic.presentation.model.mapRecentlyPlayedSongs
import com.saurav.pixelmusic.presentation.components.PixelToast as Toast
import androidx.compose.material3.ButtonDefaults
import com.saurav.pixelmusic.presentation.components.AiMixCookieButton
import com.saurav.pixelmusic.presentation.components.CreateAiPlaylistDialog
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.saurav.pixelmusic.presentation.navigation.navigateSafelyReplacing
import com.saurav.pixelmusic.presentation.viewmodel.AccountsViewModel
import com.saurav.pixelmusic.presentation.viewmodel.ExternalServiceAccount
import com.saurav.pixelmusic.presentation.viewmodel.FavoriteArtistReleasesViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import com.saurav.pixelmusic.presentation.viewmodel.QuickPicksViewModel
import com.saurav.pixelmusic.presentation.viewmodel.SettingsViewModel
import com.saurav.pixelmusic.presentation.viewmodel.StatsViewModel
import com.saurav.pixelmusic.ui.effects.successSweepEffect
import com.saurav.pixelmusic.ui.theme.ExpTitleTypography
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.saurav.pixelmusic.data.preferences.AppBackgroundStyle
import androidx.compose.ui.draw.blur



private const val HomeLoadingPlaceholderMinDurationMillis = 1200L

// Tracks cold launch in-memory across the app process lifetime
private var isColdLaunchSweepPlayed = false

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValuesParent: PaddingValues,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    statsViewModel: StatsViewModel = hiltViewModel(),
    quickPicksViewModel: QuickPicksViewModel = hiltViewModel(),
    favoriteArtistReleasesViewModel: FavoriteArtistReleasesViewModel = hiltViewModel(),
    accountsViewModel: AccountsViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    onOpenSidebar: () -> Unit
) {
    val context = LocalContext.current
    val isBenchmarkMode = remember {
        (context as? android.app.Activity)?.intent?.getBooleanExtra("is_benchmark", false) ?: false
    }
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val accountsUiState by accountsViewModel.uiState.collectAsStateWithLifecycle()
    val userName = remember(settingsUiState.userProfile.displayName, accountsUiState) {
        val customProfileName = settingsUiState.userProfile.displayName.trim()
        if (customProfileName.isNotEmpty()) {
            customProfileName
        } else {
            val rawName = accountsUiState.userName
            if (!rawName.isNullOrBlank()) {
                val cleanName = if (rawName.startsWith("@")) rawName.substring(1) else rawName
                val baseName = if (!cleanName.contains("@")) {
                    cleanName
                } else {
                    cleanName.substringBefore("@")
                }
                val formattedName = baseName.split(".", "_", "-")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercase() }
                    }
                val firstName = formattedName.split(" ").firstOrNull()?.trim().orEmpty()
                if (firstName.length >= 3) {
                    formattedName.trim()
                } else {
                    if (firstName.isNotEmpty()) firstName else formattedName.trim()
                }
            } else {
                null
            }
        }
    }
    val dailyMixSongs by playerViewModel.dailyMixSongs.collectAsStateWithLifecycle()
    val curatedYourMixSongs by playerViewModel.yourMixSongs.collectAsStateWithLifecycle()
    val homeMixPreviewSongs by playerViewModel.homeMixPreviewSongs.collectAsStateWithLifecycle()
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()
    val quickPicksDisplayMode by playerViewModel.quickPicksDisplayMode.collectAsStateWithLifecycle()
    val backgroundStyle by playerViewModel.userPreferencesRepository.appBackgroundStyleFlow.collectAsStateWithLifecycle(initialValue = AppBackgroundStyle.DEFAULT)
    val isCustomBackground = backgroundStyle != AppBackgroundStyle.DEFAULT
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTestBuild = context.packageName.endsWith(".test")

    val usesFallbackHomeMix = remember(curatedYourMixSongs, dailyMixSongs) {
        curatedYourMixSongs.isEmpty() && dailyMixSongs.isEmpty()
    }
    val yourMixSongs = remember(curatedYourMixSongs, dailyMixSongs, homeMixPreviewSongs) {
        when {
            curatedYourMixSongs.isNotEmpty() -> curatedYourMixSongs
            dailyMixSongs.isNotEmpty() -> dailyMixSongs
            else -> homeMixPreviewSongs
        }
    }
    var homePlaceholderRefreshGeneration by rememberSaveable { mutableIntStateOf(0) }
    var hasHomeLoadingMinimumElapsed by rememberSaveable(homePlaceholderRefreshGeneration) {
        mutableStateOf(false)
    }

    LaunchedEffect(homePlaceholderRefreshGeneration, yourMixSongs.isEmpty()) {
        if (yourMixSongs.isEmpty()) {
            hasHomeLoadingMinimumElapsed = false
            delay(HomeLoadingPlaceholderMinDurationMillis)
            hasHomeLoadingMinimumElapsed = true
        } else {
            hasHomeLoadingMinimumElapsed = true
        }
    }

    val shouldShowYourMixLoadingPlaceholder = yourMixSongs.isEmpty() && !hasHomeLoadingMinimumElapsed
    val recentSongIds = remember(playbackHistory) {
        collectRecentlyPlayedSongIds(
            playbackHistory = playbackHistory,
            maxItems = 64
        )
    }
    val recentlyPlayedSourceSongsInitialValue = remember(recentSongIds) {
        if (recentSongIds.isEmpty()) persistentListOf<Song>() else null
    }
    val recentlyPlayedSourceSongs by remember(recentSongIds, playerViewModel) {
        playerViewModel.observeSongs(recentSongIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = recentlyPlayedSourceSongsInitialValue)
    val latestRecentlyPlayedSongs = remember(playbackHistory, recentlyPlayedSourceSongs) {
        val sourceSongs = recentlyPlayedSourceSongs ?: return@remember emptyList()
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = sourceSongs,
            maxItems = 64
        )
    }

    val localModeEnabled by playerViewModel.localModeEnabled.collectAsStateWithLifecycle()
    val recentlyPlayedSongs = remember(latestRecentlyPlayedSongs, localModeEnabled) {
        if (!localModeEnabled) latestRecentlyPlayedSongs
        else latestRecentlyPlayedSongs.filter { entry ->
            val s = entry.song
            (!s.contentUriString.startsWith("youtube://", ignoreCase = true) &&
                !s.contentUriString.startsWith("http://", ignoreCase = true) &&
                !s.contentUriString.startsWith("https://", ignoreCase = true)) ||
                (s.path.isNotBlank() && java.io.File(s.path).exists())
        }
    }

    val recentlyPlayedQueue = remember(recentlyPlayedSongs) {
        recentlyPlayedSongs.map { it.song }.toImmutableList()
    }

    ReportDrawnWhen {
        yourMixSongs.isNotEmpty() || hasHomeLoadingMinimumElapsed || isBenchmarkMode
    }

    val yourMixSong: String = "Today's Mix for you"

    val currentSong by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong }
    }.collectAsStateWithLifecycle(initialValue = null)

    val isShuffleEnabled by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState
            .map { it.isShuffleEnabled }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val density = LocalDensity.current
    val bottomPadding = if (currentSong != null) MiniPlayerHeight else 0.dp

    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val bottomGradientHeight = resolveMainScreenBottomGradientHeight(navBarCompactMode)

    var showOptionsBottomSheet by remember { mutableStateOf(false) }
    var showChangelogBottomSheet by remember { mutableStateOf(false) }
    var showBetaInfoBottomSheet by remember { mutableStateOf(false) }
    var showStreamingProviderSheet by remember { mutableStateOf(false) }
    var showRecognitionDialog by remember { mutableStateOf(false) }
    var cleanInstallDisclaimerDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val betaSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()

    val homeStatsOverview by statsViewModel.homeOverview.collectAsStateWithLifecycle()
    val quickPicks by quickPicksViewModel.quickPicks.collectAsStateWithLifecycle()
    val homeQuickPicksStyle by playerViewModel.homeQuickPicksStyle.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val interestedSongIds by playerViewModel.interestedSongIds.collectAsStateWithLifecycle()
    val notInterestedSongIds by playerViewModel.notInterestedSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var playlistSheetSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val artistReleases by favoriteArtistReleasesViewModel.releases.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    val hasActiveAiProviderApiKey by playerViewModel.hasActiveAiProviderApiKey.collectAsStateWithLifecycle()
    val isGeneratingAiPlaylist by playerViewModel.isGeneratingAiPlaylist.collectAsStateWithLifecycle()
    val lastCreatedAiPlaylistId by playerViewModel.lastCreatedAiPlaylistId.collectAsStateWithLifecycle()
    val aiStatus by playerViewModel.aiStatus.collectAsStateWithLifecycle()
    val aiSuccess by playerViewModel.aiSuccess.collectAsStateWithLifecycle()
    val aiError by playerViewModel.aiError.collectAsStateWithLifecycle()
    var showCreateAiPlaylistDialog by remember { mutableStateOf(false) }
    var aiGenerationRequestedFromDialog by remember { mutableStateOf(false) }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val isScrolledPastThreshold by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 }
    }

    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    var needsScrollRestore by rememberSaveable { mutableStateOf(false) }

    val activeCollagePattern = CollagePattern.FLUID_BLOB

    fun isStorageBackedHomeSong(song: Song): Boolean {
        if (!song.contentUriString.startsWith("youtube://", ignoreCase = true) &&
            !song.contentUriString.startsWith("http://", ignoreCase = true) &&
            !song.contentUriString.startsWith("https://", ignoreCase = true)
        ) return true
        return song.path.isNotBlank() && java.io.File(song.path).exists()
    }

    val filteredQuickPicks = remember(quickPicks, notInterestedSongIds, interestedSongIds, localModeEnabled) {
        if (localModeEnabled) emptyList()
        else quickPicks.filter { it.id !in notInterestedSongIds }
            .sortedByDescending { it.id in interestedSongIds }
    }
    val filteredYourMixSongs = remember(yourMixSongs, homeMixPreviewSongs, notInterestedSongIds, interestedSongIds, localModeEnabled) {
        val primary = yourMixSongs
            .filter { it.id !in notInterestedSongIds && (!localModeEnabled || isStorageBackedHomeSong(it)) }
        val resolved = if (localModeEnabled && primary.isEmpty()) {
            homeMixPreviewSongs.filter { it.id !in notInterestedSongIds && isStorageBackedHomeSong(it) }
        } else {
            primary
        }
        resolved
            .sortedByDescending { it.id in interestedSongIds }
            .toImmutableList()
    }
    val filteredDailyMixSongs = remember(dailyMixSongs, notInterestedSongIds, interestedSongIds, localModeEnabled) {
        if (localModeEnabled) persistentListOf()
        else dailyMixSongs.filter { it.id !in notInterestedSongIds }
            .sortedByDescending { it.id in interestedSongIds }
            .toImmutableList()
    }

    DisposableEffect(lifecycleOwner, listState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                savedScrollIndex = listState.firstVisibleItemIndex
                savedScrollOffset = listState.firstVisibleItemScrollOffset
                needsScrollRestore = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lastCreatedAiPlaylistId, aiSuccess) {
        if (lastCreatedAiPlaylistId != null && aiSuccess) {
            showCreateAiPlaylistDialog = false
            aiGenerationRequestedFromDialog = false
        }
    }

    LaunchedEffect(hasActiveAiProviderApiKey, showCreateAiPlaylistDialog) {
        if (!hasActiveAiProviderApiKey && showCreateAiPlaylistDialog) {
            showCreateAiPlaylistDialog = false
            aiGenerationRequestedFromDialog = false
            playerViewModel.clearAiPlaylistError()
        }
    }

    LaunchedEffect(
        needsScrollRestore,
        yourMixSongs.isNotEmpty(),
        dailyMixSongs.isNotEmpty(),
        recentlyPlayedSongs.size,
        homeStatsOverview
    ) {
        if (!needsScrollRestore) return@LaunchedEffect
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) return@LaunchedEffect
        val targetIndex = savedScrollIndex.coerceIn(0, (totalItems - 1).coerceAtLeast(0))
        listState.scrollToItem(targetIndex, savedScrollOffset)
        needsScrollRestore = false
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val userPrefs = playerViewModel.userPreferencesRepository
    val lastPromptTime by userPrefs.lastUpdatePromptTimeFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val lastSeenVersion by userPrefs.lastSeenChangelogVersionFlow.collectAsStateWithLifecycle(initialValue = "LOADING")

    var showUpdateSheet by remember { mutableStateOf(false) }
    var isUpdateAvailableState by remember { mutableStateOf(false) }
    var sheetVersionName by remember { mutableStateOf("") }
    var sheetChangelog by remember { mutableStateOf<String?>(null) }
    var sheetDownloadUrl by remember { mutableStateOf<String?>(null) }
    var sheetApkName by remember { mutableStateOf<String?>(null) }
    var sheetApkSizeBytes by remember { mutableStateOf<Long?>(null) }

    val openUpdateSheetEvent by com.saurav.pixelmusic.utils.InAppUpdater.openUpdateSheetEvent.collectAsStateWithLifecycle()

    LaunchedEffect(openUpdateSheetEvent) {
        if (openUpdateSheetEvent) {
            com.saurav.pixelmusic.utils.InAppUpdater.consumeOpenUpdateSheetEvent()
            val currentAppVersion = com.saurav.pixelmusic.BuildConfig.VERSION_NAME
            val cached = com.saurav.pixelmusic.utils.InAppUpdater.latestUpdateState.value
                ?: com.saurav.pixelmusic.utils.InAppUpdater.checkForUpdate(currentAppVersion)
            when (cached) {
                is com.saurav.pixelmusic.utils.UpdateState.Available -> {
                    isUpdateAvailableState = true
                    sheetVersionName = cached.versionName
                    sheetChangelog = cached.changelog
                    sheetDownloadUrl = cached.downloadUrl
                    sheetApkName = cached.apkName
                    sheetApkSizeBytes = cached.apkSizeBytes
                }
                is com.saurav.pixelmusic.utils.UpdateState.UpToDate -> {
                    isUpdateAvailableState = true
                    sheetVersionName = cached.versionName ?: com.saurav.pixelmusic.utils.InAppUpdater.getResolvedVersionName() ?: currentAppVersion
                    sheetChangelog = cached.changelog ?: com.saurav.pixelmusic.utils.InAppUpdater.getResolvedChangelog()
                    sheetDownloadUrl = cached.downloadUrl ?: com.saurav.pixelmusic.utils.InAppUpdater.getResolvedDownloadUrl()
                    sheetApkName = cached.apkName ?: com.saurav.pixelmusic.utils.InAppUpdater.getResolvedApkName()
                    sheetApkSizeBytes = cached.apkSizeBytes ?: com.saurav.pixelmusic.utils.InAppUpdater.getResolvedApkSizeBytes()
                }
                else -> {
                    isUpdateAvailableState = true
                    sheetVersionName = com.saurav.pixelmusic.utils.InAppUpdater.getResolvedVersionName() ?: currentAppVersion
                    sheetChangelog = com.saurav.pixelmusic.utils.InAppUpdater.getResolvedChangelog()
                    sheetDownloadUrl = com.saurav.pixelmusic.utils.InAppUpdater.getResolvedDownloadUrl()
                    sheetApkName = com.saurav.pixelmusic.utils.InAppUpdater.getResolvedApkName()
                    sheetApkSizeBytes = com.saurav.pixelmusic.utils.InAppUpdater.getResolvedApkSizeBytes()
                }
            }
            showUpdateSheet = true
        }
    }

    LaunchedEffect(Unit) {
        val currentAppVersion = com.saurav.pixelmusic.BuildConfig.VERSION_NAME
        val lastPrompt = userPrefs.lastUpdatePromptTimeFlow.first()
        val lastVersionSeen = userPrefs.lastSeenChangelogVersionFlow.first()
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val updateState = com.saurav.pixelmusic.utils.InAppUpdater.checkForUpdate(currentAppVersion)

        when (updateState) {
            is com.saurav.pixelmusic.utils.UpdateState.Available -> {
                val isNewer = com.saurav.pixelmusic.utils.InAppUpdater.isNewerVersion(updateState.versionName, currentAppVersion)

                if (isNewer) {
                    sheetVersionName = updateState.versionName
                    sheetChangelog = updateState.changelog
                    sheetDownloadUrl = updateState.downloadUrl
                    sheetApkName = updateState.apkName
                    sheetApkSizeBytes = updateState.apkSizeBytes
                    if (now - lastPrompt > oneDayMs) {
                        isUpdateAvailableState = true
                        delay(2500)
                        showUpdateSheet = true
                    }
                }
            }
            is com.saurav.pixelmusic.utils.UpdateState.UpToDate -> {
                sheetDownloadUrl = updateState.downloadUrl
                sheetApkName = updateState.apkName
                sheetApkSizeBytes = updateState.apkSizeBytes
                if (lastVersionSeen.isNotEmpty() && lastVersionSeen != currentAppVersion) {
                    isUpdateAvailableState = false
                    sheetVersionName = currentAppVersion
                    sheetChangelog = updateState.changelog ?: "Welcome to the latest version of PixelMusic! 🎉"
                    delay(2500)
                    showUpdateSheet = true
                    userPrefs.setLastSeenChangelogVersion(currentAppVersion)
                } else if (lastVersionSeen.isEmpty()) {
                    userPrefs.setLastSeenChangelogVersion(currentAppVersion)
                }
            }
            else -> {}
        }
    }

    val shouldShowCleanInstallDisclaimer =
        settingsUiState.beta05CleanInstallDisclaimerDismissed == false &&
            !cleanInstallDisclaimerDismissedThisSession

    // Drive the cold launch sweep animation once per process lifetime
    var triggerColdLaunchSweep by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isColdLaunchSweepPlayed) {
            isColdLaunchSweepPlayed = true
            delay(400L)
            triggerColdLaunchSweep = true
        }
    }

    val sweepModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.successSweepEffect(isTriggered = triggerColdLaunchSweep)
    } else {
        Modifier
    }

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    val backgroundBrush = remember(surfaceColor, primaryColor, isLightTheme) {
        if (isLightTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.15f),
                    surfaceColor.copy(alpha = 0.6f),
                    surfaceColor
                ),
                endY = 1000f
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(surfaceColor, surfaceColor),
                endY = 1000f
            )
        }
    }

    val scrimTopColor = if (isLightTheme) {
        primaryColor.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.background)
    } else {
        MaterialTheme.colorScheme.background
    }

    // Status-bar height + title alpha — used by the top scrim and "PixelMusic" title
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val homeTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastThreshold) 0f else 1f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "homeTitleAlpha"
    )

    val homeGlassState = rememberGlassState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sweepModifier)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .glassRoot(homeGlassState),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isCustomBackground) {
                    TopHalfBackgroundLayer(
                        userPreferencesRepository = playerViewModel.userPreferencesRepository,
                        currentArtUri = currentSong?.albumArtUriString,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    homePlaceholderRefreshGeneration++
                    if (!localModeEnabled) {
                        quickPicksViewModel.refresh()
                        playerViewModel.forceUpdateDailyMix()
                    }
                    scope.launch {
                        delay(1200)
                        isRefreshing = false
                    }
                },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusBarHeight + 56.dp)
                    )
                }
            ) {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCustomBackground) Modifier else Modifier.background(backgroundBrush))
                        .scrollMotionBlur(
                            lazyListState = listState,
                            enabled = settingsUiState.isUiMotionBlurEnabled
                        ),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding()
                                + statusBarHeight
                                + 64.dp,
                        bottom = paddingValuesParent.calculateBottomPadding()
                                + 38.dp + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item(
                        key = "home_greeting",
                        contentType = "home_greeting"
                    ) {
                        HomeGreetingHeader(userName = userName)
                    }

                    if (filteredQuickPicks.isNotEmpty()) {
                        item(
                            key = "quick_picks_section",
                            contentType = "quick_picks_section"
                        ) {
                            QuickPicksSection(
                                songs = filteredQuickPicks,
                                onSongClick = { song ->
                                    playerViewModel.showAndPlaySong(song, filteredQuickPicks, "Quick Picks")
                                },
                                onSeeAllClick = {
                                    navController.navigateSafely(Screen.QuickPicksAll.route)
                                },
                                onMoreOptionsClick = { song ->
                                    playerViewModel.selectSongForInfo(song)
                                    showSongInfoBottomSheet = true
                                },
                                currentSongId = currentSong?.id,
                                displayMode = quickPicksDisplayMode,
                                sectionStyle = homeQuickPicksStyle
                            )
                        }
                    }

                    if (filteredYourMixSongs.isEmpty()) {
                        item(
                            key = "your_mix_placeholder",
                            contentType = "your_mix_placeholder"
                        ) {
                            if (shouldShowYourMixLoadingPlaceholder) {
                                YourMixLoadingPlaceholder()
                            } else {
                                YourMixEmptyPlaceholder(
                                    onRefresh = {
                                        homePlaceholderRefreshGeneration++
                                        settingsViewModel.refreshLibrary()
                                        playerViewModel.forceUpdateDailyMix()
                                    }
                                )
                            }
                        }
                    } else if (isLandscape) {
                        item(
                            key = "your_mix_landscape_row",
                            contentType = "your_mix_landscape_row"
                        ) {
                            val onAiCreateMixClick: () -> Unit = {
                                if (localModeEnabled) {
                                    Toast.makeText(
                                        context,
                                        "AI Playlist requires YouTube Music integration. Turn off Local Mode to use it.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (hasActiveAiProviderApiKey) {
                                    playerViewModel.clearAiPlaylistError()
                                    showCreateAiPlaylistDialog = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_set_ai_provider_api_key_first),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigateSafely(Screen.SettingsCategory.createRoute("ai"))
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Box(modifier = Modifier.weight(0.42f)) {
                                    YourMixHeader(
                                        subtitle = yourMixSong,
                                        onAiCreateClick = onAiCreateMixClick,
                                        horizontalPadding = 0.dp
                                    )
                                }
                                AlbumArtCollage(
                                    modifier = Modifier.weight(0.58f),
                                    songs = filteredYourMixSongs,
                                    padding = 4.dp,
                                    height = 285.dp,
                                    pattern = activeCollagePattern,
                                    onSongClick = { song ->
                                        if (usesFallbackHomeMix) {
                                            playerViewModel.showAndPlaySongFromLibrary(song, queueName = "Your Mix")
                                        } else {
                                            playerViewModel.showAndPlaySong(song, filteredYourMixSongs, "Your Mix")
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        item(
                            key = "your_mix_header",
                            contentType = "your_mix_header"
                        ) {
                            YourMixHeader(
                                subtitle = yourMixSong,
                                onAiCreateClick = {
                                    if (localModeEnabled) {
                                        Toast.makeText(
                                            context,
                                            "AI Playlist requires YouTube Music integration. Turn off Local Mode to use it.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (hasActiveAiProviderApiKey) {
                                        playerViewModel.clearAiPlaylistError()
                                        showCreateAiPlaylistDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_set_ai_provider_api_key_first),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigateSafely(Screen.SettingsCategory.createRoute("ai"))
                                    }
                                }
                            )
                        }

                        item(
                            key = "album_art_collage",
                            contentType = "album_art_collage"
                        ) {
                            AlbumArtCollage(
                                modifier = Modifier.fillMaxWidth(),
                                songs = filteredYourMixSongs,
                                padding = 6.dp,
                                height = 440.dp,
                                pattern = activeCollagePattern,
                                onSongClick = { song ->
                                    if (usesFallbackHomeMix) {
                                        playerViewModel.showAndPlaySongFromLibrary(song, queueName = "Your Mix")
                                    } else {
                                        playerViewModel.showAndPlaySong(song, filteredYourMixSongs, "Your Mix")
                                    }
                                }
                            )
                        }
                    }

                    if (filteredDailyMixSongs.isNotEmpty()) {
                        item(
                            key = "daily_mix_section",
                            contentType = "daily_mix_section"
                        ) {
                            DailyMixSection(
                                songs = filteredDailyMixSongs,
                                onClickOpen = {
                                    navController.navigateSafely(Screen.DailyMixScreen.route)
                                },
                                onNavigateToAlbum = { song ->
                                    navController.navigateSafelyReplacing(
                                        route = Screen.AlbumDetail.createRoute(song.albumId),
                                        patternToPop = Screen.AlbumDetail.route
                                    )
                                },
                                onNavigateToArtist = { song ->
                                    navController.navigateSafelyReplacing(
                                        route = Screen.ArtistDetail.createRoute(song.artistId),
                                        patternToPop = Screen.ArtistDetail.route
                                    )
                                },
                                onNavigateToGenre = {},
                                playerViewModel = playerViewModel
                            )
                        }
                    }

                    if (recentlyPlayedSongs.size >= RecentlyPlayedSectionMinSongsToShow) {
                        item(
                            key = "recently_played_section",
                            contentType = "recently_played_section"
                        ) {
                            RecentlyPlayedSection(
                                songs = recentlyPlayedSongs,
                                onSongClick = { song ->
                                    if (recentlyPlayedQueue.isNotEmpty()) {
                                        playerViewModel.playSongs(
                                            songsToPlay = recentlyPlayedQueue,
                                            startSong = song,
                                            queueName = "Recently Played"
                                        )
                                    }
                                },
                                onOpenAllClick = {
                                    navController.navigateSafely(Screen.RecentlyPlayed.route)
                                },
                                onMoreOptionsClick = { song ->
                                    playerViewModel.selectSongForInfo(song)
                                    showSongInfoBottomSheet = true
                                },
                                themeStateHolder = playerViewModel.themeStateHolder,
                                currentSongId = currentSong?.id,
                                contentPadding = PaddingValues(start = 8.dp, end = 24.dp)
                            )
                        }
                    }

                    if (artistReleases.isNotEmpty()) {
                        item(
                            key = "favorite_artist_releases_section",
                            contentType = "favorite_artist_releases_section"
                        ) {
                            FavoriteArtistReleasesSection(
                                releases = artistReleases,
                                onSongClick = { songItem ->
                                    val nativeSong = songItem.toNativeSong()
                                    playerViewModel.showAndPlaySong(nativeSong)
                                },
                                onAlbumClick = { albumItem ->
                                    navController.navigateSafely(Screen.AlbumDetail.createRoute(albumItem.browseId))
                                }
                            )
                        }
                    }

                    if (homeStatsOverview != null) {
                        item(
                            key = "listening_stats_preview",
                            contentType = "listening_stats_preview"
                        ) {
                            StatsOverviewCard(
                                summary = homeStatsOverview,
                                onClick = { navController.navigateSafely(Screen.Stats.route) }
                            )
                        }
                    }
                }
            }
            }
        }

        // Top progressive blur backdrop (PixelMusic style)
        ProgressiveHeaderBackdrop(
            scrimColor = if (isCustomBackground) MaterialTheme.colorScheme.background.copy(alpha = 0.35f) else scrimTopColor,
            glassState = homeGlassState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(statusBarHeight + 76.dp)
        )

        // Top bar with PixelMusic pill on the left and Settings button on the right
        // Both vanish when scrolling (homeTitleAlpha fades out)
        val isDarkHeaderTheme = com.saurav.pixelmusic.ui.theme.LocalPixelMusicDarkTheme.current
        val headerPillColor = if (isDarkHeaderTheme) {
            androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.primaryContainer,
                0.22f
            ).copy(alpha = 0.90f)
        } else {
            androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surfaceContainerLowest,
                MaterialTheme.colorScheme.primaryContainer,
                0.22f
            ).copy(alpha = 0.90f)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = statusBarHeight + 14.dp)
                .graphicsLayer { alpha = homeTitleAlpha },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    if (homeTitleAlpha > 0.05f) {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                shape = CircleShape,
                color = headerPillColor,
                modifier = Modifier
                    .height(48.dp)
                    .graphicsLayer {
                        if (com.saurav.pixelmusic.presentation.screens.SplashTransitionState.isSplashActive) {
                            val p = com.saurav.pixelmusic.presentation.screens.SplashTransitionState.exitProgress
                            val raw = ((p - 0.54f) / 0.38f).coerceIn(0f, 1f)
                            val inv = 1f - raw
                            alpha = 1f - inv * inv * inv
                        } else {
                            alpha = 1f
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(26.dp)
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.isAttached) {
                                    val pos = coordinates.positionInRoot()
                                    val w = coordinates.size.width.toFloat()
                                    val h = coordinates.size.height.toFloat()
                                    if (w > 0f && h > 0f) {
                                        com.saurav.pixelmusic.presentation.screens.SplashTransitionState.updateTarget(
                                            centerInRoot = androidx.compose.ui.geometry.Offset(
                                                x = pos.x + w * 0.5f,
                                                y = pos.y + h * 0.5f
                                            ),
                                            sizePx = w
                                        )
                                    }
                                }
                            }
                            .graphicsLayer {
                                if (com.saurav.pixelmusic.presentation.screens.SplashTransitionState.isSplashActive) {
                                    val p = com.saurav.pixelmusic.presentation.screens.SplashTransitionState.exitProgress
                                    alpha = ((p - 0.82f) / 0.16f).coerceIn(0f, 1f)
                                } else {
                                    alpha = 1f
                                }
                            }
                    )
                    Text(
                        text = "PixelMusic",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.graphicsLayer {
                            if (com.saurav.pixelmusic.presentation.screens.SplashTransitionState.isSplashActive) {
                                val p = com.saurav.pixelmusic.presentation.screens.SplashTransitionState.exitProgress
                                val raw = ((p - 0.54f) / 0.38f).coerceIn(0f, 1f)
                                val inv = 1f - raw
                                val eased = 1f - inv * inv * inv
                                alpha = eased
                                translationX = -16.dp.toPx() * (1f - eased)
                                val s = 0.92f + 0.08f * eased
                                scaleX = s
                                scaleY = s
                            } else {
                                alpha = 1f
                                translationX = 0f
                                scaleX = 1f
                                scaleY = 1f
                            }
                        }
                    )
                }
            }

            val homeAvatarName = userName ?: settingsUiState.ytUsername.ifBlank { "Guest" }
            val homeAvatarSpec = settingsUiState.userProfile.effectiveLocalAvatar(
                if (settingsUiState.ytUsername.isNotEmpty()) settingsUiState.ytAvatarUrl else ""
            )
            Surface(
                onClick = {
                    if (homeTitleAlpha > 0.05f) {
                        navController.navigateSafely(Screen.Settings.route)
                    }
                },
                shape = CircleShape,
                color = headerPillColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    com.saurav.pixelmusic.presentation.components.ProfileAvatar(
                        avatarSpec = homeAvatarSpec,
                        displayName = homeAvatarName,
                        size = 40.dp,
                        cacheKey = settingsUiState.userProfile.updatedAt,
                        fallbackInitial = "G"
                    )
                }
            }
        }

    }

    if (showOptionsBottomSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = { showOptionsBottomSheet = false },
            sheetState = sheetState
        ) {
            val smoothDismiss = com.saurav.pixelmusic.presentation.components.LocalSmoothDismiss.current
            HomeOptionsBottomSheet(
                onNavigateToMashup = {
                    smoothDismiss {
                        navController.navigateSafely(Screen.DJSpace.route)
                    }
                }
            )
        }
    }

    if (showUpdateSheet && !shouldShowCleanInstallDisclaimer) {
        UpdateNotificationSheet(
            isUpdateAvailable = isUpdateAvailableState,
            versionName = sheetVersionName,
            changelog = sheetChangelog,
            downloadUrl = sheetDownloadUrl,
            apkName = sheetApkName,
            apkSizeBytes = sheetApkSizeBytes,
            userName = userName,
            isTestBuild = isTestBuild,
            onDismiss = { showUpdateSheet = false },
            onConfirmClick = {
                if (isUpdateAvailableState && sheetDownloadUrl.isNullOrBlank()) {
                    navController.navigateSafely("about")
                }
            },
            onMigrateClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/Saurav-02/PixelMusic/releases/latest")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                try { context.startActivity(intent) } catch (e: android.content.ActivityNotFoundException) {}
            },
            onSnoozeClick = {
                scope.launch {
                    userPrefs.setLastUpdatePromptTime(System.currentTimeMillis())
                }
            }
        )
    }

    if (showChangelogBottomSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = { showChangelogBottomSheet = false },
            sheetState = sheetState
        ) {
            ChangelogBottomSheet()
        }
    }
    if (showBetaInfoBottomSheet) {
        SmoothModalBottomSheet(
            onDismissRequest = { showBetaInfoBottomSheet = false },
            sheetState = betaSheetState,
        ) {
            BetaInfoBottomSheet()
        }
    }
    if (showStreamingProviderSheet) {
        StreamingProviderSheet(
            onDismissRequest = { showStreamingProviderSheet = false },
            onNavigateToYoutubeAuth = {
                navController.navigateSafely(Screen.YoutubeAuth.createRoute())
            }
        )
    }
    if (showSongInfoBottomSheet && selectedSongForInfo != null) {
        val song = selectedSongForInfo!!
        SongInfoBottomSheet(
            song = song,
            isFavorite = favoriteSongIds.contains(song.id),
            onToggleFavorite = {
                playerViewModel.toggleFavoriteSpecificSong(song)
            },
            onDismiss = {
                showSongInfoBottomSheet = false
            },
            onPlaySong = {
                if (quickPicks.isNotEmpty()) {
                    playerViewModel.playSongs(quickPicks, song, "Quick Picks")
                }
                showSongInfoBottomSheet = false
            },
            onAddToQueue = {
                playerViewModel.addSongToQueue(song)
                showSongInfoBottomSheet = false
            },
            onAddNextToQueue = {
                playerViewModel.addSongNextToQueue(song)
                showSongInfoBottomSheet = false
            },
            onAddToPlayList = {
                playlistSheetSongs = listOf(song)
                showSongInfoBottomSheet = false
                showPlaylistBottomSheet = true
            },
            onDeleteFromDevice = playerViewModel::deleteFromDevice,
            onNavigateToAlbum = {
                navController.navigateSafely(Screen.AlbumDetail.createRoute(song.albumId))
                showSongInfoBottomSheet = false
            },
            onNavigateToArtist = {
                navController.navigateSafely(Screen.ArtistDetail.createRoute(song.artistId))
                showSongInfoBottomSheet = false
            },
            onNavigateToArtistById = { artistId ->
                navController.navigateSafely(Screen.ArtistDetail.createRoute(artistId))
                showSongInfoBottomSheet = false
            },
            onNavigateToGenre = {},
            onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate ->
                playerViewModel.editSongMetadata(
                    song,
                    newTitle,
                    newArtist,
                    newAlbum,
                    newAlbumArtist,
                    newComposer,
                    newGenre,
                    newLyrics,
                    newTrackNumber,
                    newDiscNumber,
                    replayGainTrackGainDb,
                    replayGainAlbumGainDb,
                    coverArtUpdate
                )
            },
            generateAiMetadata = { fields ->
                playerViewModel.generateAiMetadata(song, fields)
            },
            removeFromListTrigger = {}
        )
    }
    if (showPlaylistBottomSheet && selectedSongForInfo != null) {
        val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = playlistSheetSongs,
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = systemNavBarInset,
            playerViewModel = playerViewModel,
        )
    }
    if (shouldShowCleanInstallDisclaimer) {
        InstagramPromoDialog(
            onDismiss = {
                cleanInstallDisclaimerDismissedThisSession = true
                settingsViewModel.setBeta05CleanInstallDisclaimerDismissed(true)
            }
        )
    }

    CreateAiPlaylistDialog(
        visible = showCreateAiPlaylistDialog && hasActiveAiProviderApiKey,
        isGenerating = isGeneratingAiPlaylist,
        isSuccess = aiSuccess,
        status = aiStatus,
        error = aiError,
        onDismiss = {
            showCreateAiPlaylistDialog = false
            aiGenerationRequestedFromDialog = false
            playerViewModel.clearAiPlaylistError()
            playerViewModel.clearLastCreatedAiPlaylistId()
        },
        onGenerate = { playlistName, prompt, minLength, maxLength ->
            showCreateAiPlaylistDialog = false
            aiGenerationRequestedFromDialog = true
            playerViewModel.generateAiPlaylist(
                prompt = prompt,
                minLength = minLength,
                maxLength = maxLength,
                saveAsPlaylist = true,
                playlistName = playlistName
            )
        }
    )


}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun YourMixLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(128.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun YourMixEmptyPlaceholder(
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 256.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 28.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBR = 28.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusBL = 28.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusTR = 28.dp,
                    smoothnessAsPercentBL = 60,
                ),
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_music_note_24),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_empty_placeholder_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.home_empty_placeholder_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 22.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusBR = 22.dp,
                    smoothnessAsPercentTL = 60,
                    cornerRadiusBL = 22.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusTR = 22.dp,
                    smoothnessAsPercentBL = 60,
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.home_empty_placeholder_refresh))
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private val YourMixTitleStyle: TextStyle by lazy {
    TextStyle(
        fontFamily = FontFamily(
            Font(
                resId = R.font.gflex_variable,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(636),
                    FontVariation.width(152f),
                    FontVariation.Setting("ROND", 50f),
                    FontVariation.Setting("XTRA", 520f),
                    FontVariation.Setting("YOPQ", 90f),
                    FontVariation.Setting("YTLC", 505f)
                )
            )
        ),
        fontWeight = FontWeight(760),
        fontSize = 42.sp,
        lineHeight = 44.sp
    )
}

private val YourMixFeaturedShape = racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape(
    cornerRadiusTL = 32.dp, smoothnessAsPercentTL = 100,
    cornerRadiusTR = 12.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBL = 12.dp, smoothnessAsPercentBL = 60,
    cornerRadiusBR = 32.dp, smoothnessAsPercentBR = 100
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YourMixHeader(
    subtitle: String,
    featuredSong: Song? = null,
    onSongClick: (() -> Unit)? = null,
    onAiCreateClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 24.dp
) {
    val titleStyle = YourMixTitleStyle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_your_mix_title),
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        if (onAiCreateClick != null) {
            AiMixCookieButton(
                onClick = onAiCreateClick
            )
        }
    }
}

@Composable
fun SongListItemFavs(
    modifier: Modifier = Modifier,
    cardCorners: Dp = 12.dp,
    title: String,
    artist: String,
    albumArtUrl: String?,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isCurrentSong) colors.primaryContainer.copy(alpha = 0.46f) else colors.surfaceContainer
    val contentColor = if (isCurrentSong) colors.primary else colors.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cardCorners),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(0.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartImage(
                    model = albumArtUrl,
                    contentDescription = stringResource(R.string.cd_album_art_for_title, title),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist, style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            if (isCurrentSong) {
                PlayingEqIcon(
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(start = 8.dp)
                        .size(width = 18.dp, height = 16.dp),
                    color = colors.primary,
                    isPlaying = isPlaying
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SongListItemFavsWrapper(
    song: Song,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val rowIsPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val isThisSongPlaying = remember(song.id, currentSongId) {
        song.id == currentSongId
    }

    SongListItemFavs(
        modifier = modifier,
        cardCorners = 0.dp,
        title = song.title,
        artist = song.displayArtist,
        albumArtUrl = song.albumArtUriString,
        isPlaying = rowIsPlaying,
        isCurrentSong = song.id == currentSongId,
        onClick = onClick
    )
}

@Composable
fun HomeGreetingHeader(userName: String?) {
    val greeting = remember(userName) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greetings = when (hour) {
            in 5..11 -> if (userName != null) {
                listOf(
                    "Good morning, $userName! ☀️",
                    "Glad you're awake, $userName. 🌅",
                    "Wakey wakey, $userName! 🎧",
                    "Hey, morning check, $userName! ⚡",
                    "Ready today, $userName? ☀️",
                    "Welcome back, $userName! 🌅",
                    "Up early, $userName? 🌅",
                    "Morning vibe check, $userName! ✨",
                    "Slept well, $userName? 🛌"
                )
            } else {
                listOf(
                    "Good morning! ☀️",
                    "Glad you're awake. 🌅",
                    "Wakey wakey, sunshine! 🎧",
                    "Hey, morning check! ⚡",
                    "Ready today? ☀️",
                    "Welcome back! 🌅",
                    "Up early? 🌅",
                    "Morning vibe check! ✨",
                    "Slept well? 🛌"
                )
            }
            in 12..16 -> if (userName != null) {
                listOf(
                    "Hey, how's your day, $userName? ☀️",
                    "Need a break, $userName? 💆",
                    "Glad to see you, $userName. 🍕",
                    "Listening under the sun, $userName? ☀️",
                    "Hope it's going well, $userName. 🌟",
                    "Slay the afternoon, $userName! 💅",
                    "Hey, what's playing, $userName? 🎧",
                    "Midday vibe check, $userName! ⚡",
                    "Hey, you got this, $userName! ⚡"
                )
            } else {
                listOf(
                    "Hey, how's your day? ☀️",
                    "Need a break? 💆",
                    "Glad to see you. 🍕",
                    "Listening under the sun? ☀️",
                    "Hope it's going well. 🌟",
                    "Slay the afternoon! 💅",
                    "Hey, what's playing? 🎧",
                    "Midday vibe check! ⚡",
                    "Hey, you got this! ⚡"
                )
            }
            in 17..21 -> if (userName != null) {
                listOf(
                    "Welcome home, $userName! 🏡",
                    "Unwinding, $userName? 🛋️",
                    "Glad you made it, $userName. 💛",
                    "Hey, let's relax, $userName. 🍵",
                    "Time to chill, $userName. 🌃",
                    "Sunset listening, $userName. 🌇",
                    "How was your day, $userName? ✨",
                    "Hope it was good, $userName! 💛",
                    "Ready to zone out, $userName? 🛋️"
                )
            } else {
                listOf(
                    "Welcome home! 🏡",
                    "Unwinding? 🛋️",
                    "Glad you made it. 💛",
                    "Hey, let's relax. 🍵",
                    "Time to chill. 🌃",
                    "Sunset listening. 🌇",
                    "How was your day? ✨",
                    "Hope it was good! 💛",
                    "Ready to zone out? 🛋️"
                )
            }
            else -> if (userName != null) {
                listOf(
                    "Under the stars, $userName 🌌",
                    "Insomnia club, $userName 🌌",
                    "Up late, $userName? 🌙",
                    "Can't sleep, $userName? 🌌",
                    "Still awake, $userName? 🌌",
                    "Quiet hours, $userName. 🕯️",
                    "Rest easy, $userName. 💤",
                    "In the quiet, $userName. 🤍",
                    "Midnight thoughts, $userName? 💭",
                    "Soft music now, $userName. 🎧"
                )
            } else {
                listOf(
                    "Under the stars 🌌",
                    "Insomnia club 🌌",
                    "Up late? 🌙",
                    "Can't sleep? 🌌",
                    "Still awake? 🌌",
                    "Quiet hours. 🕯️",
                    "Rest easy. 💤",
                    "In the quiet. 🤍",
                    "Midnight thoughts? 💭",
                    "Soft music now. 🎧"
                )
            }
        }
        greetings.random()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 0.dp)
    ) {
        Text(
            text = greeting,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
