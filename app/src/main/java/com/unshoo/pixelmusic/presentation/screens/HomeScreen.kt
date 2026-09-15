@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.unshoo.pixelmusic.presentation.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.preferences.CollagePattern
import com.unshoo.pixelmusic.data.remote.youtube.toNativeSong
import com.unshoo.pixelmusic.presentation.components.AlbumArtCollage
import com.unshoo.pixelmusic.presentation.components.BetaInfoBottomSheet
import com.unshoo.pixelmusic.presentation.components.ChangelogBottomSheet
import com.unshoo.pixelmusic.presentation.components.DailyMixSection
import com.unshoo.pixelmusic.presentation.components.FavoriteArtistReleasesSection
import com.unshoo.pixelmusic.presentation.components.HomeOptionsBottomSheet
import com.unshoo.pixelmusic.presentation.components.HomeShuffleFab
import com.unshoo.pixelmusic.presentation.components.InstagramPromoDialog
import com.unshoo.pixelmusic.presentation.components.MiniPlayerHeight
import com.unshoo.pixelmusic.presentation.components.MusicRecognitionOverlay
import com.unshoo.pixelmusic.presentation.components.QuickPicksSection
import com.unshoo.pixelmusic.presentation.components.RecentlyPlayedSection
import com.unshoo.pixelmusic.presentation.components.RecentlyPlayedSectionMinSongsToShow
import com.unshoo.pixelmusic.presentation.components.SmartImage
import com.unshoo.pixelmusic.presentation.components.StatsOverviewCard
import com.unshoo.pixelmusic.presentation.components.StreamingProviderSheet
import com.unshoo.pixelmusic.presentation.components.UpdateNotificationSheet
import com.unshoo.pixelmusic.presentation.components.resolveMainScreenBottomGradientHeight
import com.unshoo.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.unshoo.pixelmusic.presentation.components.subcomps.PlayingEqIcon
import com.unshoo.pixelmusic.presentation.components.subcomps.SineWaveLine
import com.unshoo.pixelmusic.presentation.model.collectRecentlyPlayedSongIds
import com.unshoo.pixelmusic.presentation.model.mapRecentlyPlayedSongs
import com.unshoo.pixelmusic.presentation.navigation.Screen
import com.unshoo.pixelmusic.presentation.navigation.navigateSafely
import com.unshoo.pixelmusic.presentation.navigation.navigateSafelyReplacing
import com.unshoo.pixelmusic.presentation.viewmodel.AccountsViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.ExternalServiceAccount
import com.unshoo.pixelmusic.presentation.viewmodel.FavoriteArtistReleasesViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.QuickPicksViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.SettingsViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.StatsViewModel
import com.unshoo.pixelmusic.ui.effects.successSweepEffect
import com.unshoo.pixelmusic.ui.theme.ExpTitleTypography
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
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
import com.unshoo.pixelmusic.ui.modifiers.scrollMotionBlur
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.unshoo.pixelmusic.data.preferences.AppBackgroundStyle


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
    onOpenSidebar: () -> Unit
) {
    val context = LocalContext.current
    val isBenchmarkMode = remember {
        (context as? android.app.Activity)?.intent?.getBooleanExtra("is_benchmark", false) ?: false
    }
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val accountsUiState by accountsViewModel.uiState.collectAsStateWithLifecycle()
    val userName = remember(accountsUiState) {
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
    val dailyMixSongs by playerViewModel.dailyMixSongs.collectAsStateWithLifecycle()
    val curatedYourMixSongs by playerViewModel.yourMixSongs.collectAsStateWithLifecycle()
    val homeMixPreviewSongs by playerViewModel.homeMixPreviewSongs.collectAsStateWithLifecycle()
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()
    val quickPicksDisplayMode by playerViewModel.quickPicksDisplayMode.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTestBuild = context.packageName.endsWith(".test")

    val usesFallbackHomeMix = remember(curatedYourMixSongs, dailyMixSongs) {
        curatedYourMixSongs.isEmpty() && dailyMixSongs.isEmpty()
    }
    val yourMixSongs = remember(curatedYourMixSongs, dailyMixSongs, homeMixPreviewSongs) {
        when {
            dailyMixSongs.isNotEmpty() -> dailyMixSongs
            curatedYourMixSongs.isNotEmpty() -> curatedYourMixSongs
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

    val recentlyPlayedSongs = latestRecentlyPlayedSongs

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
    val sheetState = rememberModalBottomSheetState()
    val betaSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val homeStatsOverview by statsViewModel.homeOverview.collectAsStateWithLifecycle()
    val quickPicks by quickPicksViewModel.quickPicks.collectAsStateWithLifecycle()
    val artistReleases by favoriteArtistReleasesViewModel.releases.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scrollThresholdPx = remember(density) { with(density) { 180.dp.toPx() } }
    val isScrolledPastThreshold = remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > scrollThresholdPx }
    }

    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    var needsScrollRestore by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        val currentAppVersion = com.unshoo.pixelmusic.BuildConfig.VERSION_NAME
        val lastPrompt = userPrefs.lastUpdatePromptTimeFlow.first()
        val lastVersionSeen = userPrefs.lastSeenChangelogVersionFlow.first()
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val updateState = com.unshoo.pixelmusic.utils.InAppUpdater.checkForUpdate(currentAppVersion)

        when (updateState) {
            is com.unshoo.pixelmusic.utils.UpdateState.Available -> {
                val cleanLatest = updateState.versionName.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                val cleanCurrent = currentAppVersion.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

                if (cleanLatest > cleanCurrent) {
                    if (now - lastPrompt > oneDayMs) {
                        isUpdateAvailableState = true
                        sheetVersionName = updateState.versionName
                        sheetChangelog = updateState.changelog
                        delay(2500)
                        showUpdateSheet = true
                    }
                }
            }
            is com.unshoo.pixelmusic.utils.UpdateState.UpToDate -> {
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

    // Status-bar height + title alpha — used by the top scrim and "PixelMusic" title
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val homeTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastThreshold.value) 0f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "homeTitleAlpha"
    )

    // Tinted top scrim that matches Explore's Material You expressive style in light mode
    val homeScrimTopColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            .compositeOver(MaterialTheme.colorScheme.background)
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sweepModifier)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    homePlaceholderRefreshGeneration++
                    quickPicksViewModel.refresh()
                    playerViewModel.forceUpdateDailyMix()
                    scope.launch {
                        delay(2000)
                        isRefreshing = false
                    }
                },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .scrollMotionBlur(
                            lazyListState = listState,
                            enabled = settingsUiState.isUiMotionBlurEnabled
                        ),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding()
                                + statusBarHeight
                                + 58.dp,
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

                    if (quickPicks.isNotEmpty()) {
                        item(
                            key = "quick_picks_section",
                            contentType = "quick_picks_section"
                        ) {
                            QuickPicksSection(
                                songs = quickPicks,
                                onSongClick = { song ->
                                    playerViewModel.showAndPlaySong(song, quickPicks, "Quick Picks")
                                },
                                onSeeAllClick = {
                                    navController.navigateSafely(Screen.QuickPicksAll.route)
                                },
                                currentSongId = currentSong?.id,
                                displayMode = quickPicksDisplayMode
                            )
                        }
                    }

                    if (yourMixSongs.isEmpty()) {
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
                    } else {
                        item(
                            key = "your_mix_header",
                            contentType = "your_mix_header"
                        ) {
                            YourMixHeader(
                                subtitle = yourMixSong,
                                featuredSong = yourMixSongs.firstOrNull(),
                                onSongClick = {
                                    yourMixSongs.firstOrNull()?.let { song ->
                                        if (usesFallbackHomeMix) {
                                            playerViewModel.showAndPlaySongFromLibrary(song, queueName = "Your Mix")
                                        } else {
                                            playerViewModel.showAndPlaySong(song, yourMixSongs, "Your Mix")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (yourMixSongs.isNotEmpty()) {
                        item(
                            key = "album_art_collage",
                            contentType = "album_art_collage"
                        ) {
                            val basePattern = settingsUiState.collagePattern
                            val isAutoRotate = settingsUiState.collageAutoRotate
                            val patterns = remember { CollagePattern.entries }

                            val activePattern = if (isAutoRotate) {
                                var rotationIndex by rememberSaveable { mutableIntStateOf(-1) }
                                LaunchedEffect(Unit) { rotationIndex++ }
                                remember(rotationIndex) {
                                    patterns[rotationIndex.coerceAtLeast(0) % patterns.size]
                                }
                            } else {
                                basePattern
                            }

                            AlbumArtCollage(
                                modifier = Modifier.fillMaxWidth(),
                                songs = yourMixSongs,
                                padding = 14.dp,
                                height = 400.dp,
                                pattern = activePattern,
                                onSongClick = { song ->
                                    if (usesFallbackHomeMix) {
                                        playerViewModel.showAndPlaySongFromLibrary(song, queueName = "Your Mix")
                                    } else {
                                        playerViewModel.showAndPlaySong(song, yourMixSongs, "Your Mix")
                                    }
                                }
                            )
                        }
                    }

                    if (dailyMixSongs.isNotEmpty()) {
                        item(
                            key = "daily_mix_section",
                            contentType = "daily_mix_section"
                        ) {
                            DailyMixSection(
                                songs = dailyMixSongs,
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
                                    navController.navigateSafely(Screen.AlbumDetail.createRoute(albumItem.playlistId))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(bottomGradientHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.2f to Color.Transparent,
                            0.8f to androidx.compose.material3.MaterialTheme.colorScheme.background,
                            1.0f to androidx.compose.material3.MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Top scrim — Material You expressive tint in light mode, matches Explore
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(statusBarHeight + 64.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to homeScrimTopColor.copy(alpha = 0.95f),
                            0.18f to homeScrimTopColor.copy(alpha = 0.86f),
                            0.36f to homeScrimTopColor.copy(alpha = 0.68f),
                            0.54f to homeScrimTopColor.copy(alpha = 0.48f),
                            0.72f to homeScrimTopColor.copy(alpha = 0.28f),
                            0.88f to homeScrimTopColor.copy(alpha = 0.11f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // "PixelMusic" title + icon — sits above the scrim, fades out on scroll
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = statusBarHeight + 14.dp)
                .graphicsLayer { alpha = homeTitleAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.pixelmusic_base_monochrome),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "PixelMusic",
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                letterSpacing = 0.5.sp
            )
        }

        HomeShuffleFab(
            isShuffleEnabled = isShuffleEnabled,
            isPlayerActive = currentSong != null,
            onClick = {
                val songsToUse = quickPicks.ifEmpty { yourMixSongs }
                if (songsToUse.isNotEmpty()) {
                    playerViewModel.playSongsShuffled(songsToUse, "Your Mix")
                }
            },
            onLongClick = { showRecognitionDialog = true },
            onSwipeUp = { showRecognitionDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    if (showOptionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsBottomSheet = false },
            sheetState = sheetState
        ) {
            HomeOptionsBottomSheet(
                onNavigateToMashup = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showOptionsBottomSheet = false
                            navController.navigateSafely(Screen.DJSpace.route)
                        }
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
            isTestBuild = isTestBuild,
            onDismiss = { showUpdateSheet = false },
            onConfirmClick = {
                if (isUpdateAvailableState) {
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
        ModalBottomSheet(
            onDismissRequest = { showChangelogBottomSheet = false },
            sheetState = sheetState
        ) {
            ChangelogBottomSheet()
        }
    }
    if (showBetaInfoBottomSheet) {
        ModalBottomSheet(
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
                navController.navigateSafely(Screen.YoutubeAuth.route)
            }
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

AnimatedVisibility(
    visible = showRecognitionDialog,
    enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)),
    exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
) {
    MusicRecognitionOverlay(
        isExternalWindow = false,
        onDismiss = { showRecognitionDialog = false },
        onPlayMusic = { recognizedSong ->
            showRecognitionDialog = false
            scope.launch {
                val songToPlay = withContext(Dispatchers.IO) {
                    val query = "${recognizedSong.title} ${recognizedSong.artist}"
                    val searchResult = YouTube.search(
                        query,
                        YouTube.SearchFilter.FILTER_SONG
                    ).getOrNull()

                    val topResult = searchResult?.items
                        ?.firstOrNull { it is SongItem } as? SongItem

                    val nativeSong = topResult?.toNativeSong()
                    nativeSong?.copy(
                        albumArtUriString = recognizedSong.coverArtHqUrl
                            ?: recognizedSong.coverArtUrl
                            ?: nativeSong.albumArtUriString
                    )
                }

                if (songToPlay != null) {
                    playerViewModel.playWithArchiveTuneQueueBuilder(
                        song = songToPlay,
                        queueName = "Recognized Music"
                    )
                } else {
                    playerViewModel.sendToast("Could not find this track on YouTube Music.")
                }
            }
        }
    )
    }
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
                        imageVector = Icons.Rounded.MusicNote,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YourMixHeader(
    subtitle: String,
    featuredSong: Song?,
    onSongClick: () -> Unit
) {
    val titleStyle = rememberYourMixTitleStyle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 8.dp)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (featuredSong != null) {
            Surface(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(86.dp),
                shape = racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 32.dp, smoothnessAsPercentTL = 100,
                    cornerRadiusTR = 12.dp, smoothnessAsPercentTR = 60,
                    cornerRadiusBL = 12.dp, smoothnessAsPercentBL = 60,
                    cornerRadiusBR = 32.dp, smoothnessAsPercentBR = 100
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClick = onSongClick
            ) {
                SmartImage(
                    model = featuredSong.albumArtUriString,
                    contentDescription = featuredSong.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
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
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()

    val isThisSongPlaying = remember(song.id, stablePlayerState.currentSong?.id, stablePlayerState.isPlaying) {
        song.id == stablePlayerState.currentSong?.id
    }

    SongListItemFavs(
        modifier = modifier,
        cardCorners = 0.dp,
        title = song.title,
        artist = song.displayArtist,
        albumArtUrl = song.albumArtUriString,
        isPlaying = stablePlayerState.isPlaying,
        isCurrentSong = song.id == stablePlayerState.currentSong?.id,
        onClick = onClick
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberYourMixTitleStyle(): TextStyle {
    return remember {
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
