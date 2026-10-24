package com.saurav.pixelmusic

import com.saurav.pixelmusic.presentation.navigation.navigateSafely

// import androidx.compose.ui.platform.LocalView // No longer needed for this
// import androidx.core.view.WindowInsetsCompat // No longer needed for this
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Trace
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.CallSuper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import com.saurav.pixelmusic.presentation.components.HomeShuffleFab
import com.saurav.pixelmusic.presentation.components.MusicRecognitionOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.Crossfade
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.saurav.pixelmusic.data.github.GitHubAnnouncementPropertiesService
import com.saurav.pixelmusic.data.github.PlayStoreAnnouncementRemoteConfig
import com.saurav.pixelmusic.data.preferences.AppThemeMode
import com.saurav.pixelmusic.data.preferences.AppFontMode
import com.saurav.pixelmusic.data.preferences.NavBarStyle
import com.saurav.pixelmusic.data.preferences.sanitizeNavBarCornerRadius
import com.saurav.pixelmusic.data.preferences.ThemePreferencesRepository
import com.saurav.pixelmusic.data.preferences.ThemePreference
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.service.MusicService
import com.saurav.pixelmusic.data.worker.SyncManager
import com.saurav.pixelmusic.data.worker.SyncProgress
import com.saurav.pixelmusic.presentation.components.AppSidebarDrawer
import com.saurav.pixelmusic.presentation.components.CrashReportDialog
import com.saurav.pixelmusic.presentation.components.DismissUndoBar
import com.saurav.pixelmusic.presentation.components.DrawerDestination
import com.saurav.pixelmusic.presentation.components.MiniPlayerBottomSpacer
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.PlayerInternalNavigationBar
import com.saurav.pixelmusic.presentation.components.PlayStoreAnnouncementDefaults
import com.saurav.pixelmusic.presentation.components.PlayStoreAnnouncementDialog
import com.saurav.pixelmusic.presentation.components.PlayStoreAnnouncementUiModel
import com.saurav.pixelmusic.presentation.components.UnifiedPlayerSheetV2
import com.saurav.pixelmusic.presentation.components.calculatePlayerSheetCollapsedTargetY
import com.saurav.pixelmusic.presentation.components.resolveNavBarOccupiedHeight
import com.saurav.pixelmusic.presentation.components.resolveNavBarSurfaceHeight
import com.saurav.pixelmusic.presentation.components.sanitizeNavigationBarBottomInset
import com.saurav.pixelmusic.presentation.navigation.AppNavigation
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.screens.SetupScreen
import com.saurav.pixelmusic.presentation.viewmodel.MainViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.ui.theme.PixelMusicTheme
import com.saurav.pixelmusic.ui.theme.rememberThemeTransitionController
import com.saurav.pixelmusic.ui.theme.LocalThemeTransitionController
import com.saurav.pixelmusic.ui.theme.CircularThemeRevealHost
import com.saurav.pixelmusic.utils.CrashHandler
import com.saurav.pixelmusic.utils.AppLocaleManager
import com.saurav.pixelmusic.utils.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.saurav.pixelmusic.presentation.utils.AppHapticsConfig
import com.saurav.pixelmusic.presentation.utils.LocalAppHapticsConfig
import com.saurav.pixelmusic.ui.modifiers.LocalMotionBlurEnabled
import com.saurav.pixelmusic.ui.modifiers.LocalMotionBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassBlurIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionIntensity
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import com.saurav.pixelmusic.utils.AndroidVersionCompat
import com.saurav.pixelmusic.presentation.utils.NoOpHapticFeedback
import com.saurav.pixelmusic.utils.CrashLogData
import javax.annotation.concurrent.Immutable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import com.saurav.pixelmusic.presentation.navigation.navigateToTopLevelSafely
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage





@Immutable
data class BottomNavItem(
    val label: String,
    @DrawableRes val iconResId: Int,
    @DrawableRes val selectedIconResId: Int? = null,
    val screen: Screen
)

private data class DismissUndoBarSlice(
    val isVisible: Boolean = false,
    val durationMillis: Long = 4000L
)

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private var isUIVisiblyReady = false
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository // Inject here
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository
    @Inject
    lateinit var syncManager: SyncManager
    // For handling shortcut navigation - using StateFlow so composables can observe changes
    private val _pendingPlaylistNavigation = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _pendingRouteNavigation = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _pendingShuffleAll = kotlinx.coroutines.flow.MutableStateFlow(false)
    /** URI of an M3U/M3U8 file shared/opened from another app, waiting to be imported. */
    val pendingM3uImportUri = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)


    /**
     * Asks the OS to run this window at the display's highest supported refresh
     * rate (120Hz+ where available) instead of leaving it capped at 60Hz.
     * Compose renders on the display vsync, so without this hint some OEMs keep
     * the app at 60Hz even when the system is set higher.
     */
    private fun requestHighRefreshRate() {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return
            val bestMode = display.supportedModes.maxByOrNull { it.refreshRate } ?: return
            if (bestMode.refreshRate > 61f) {
                val attrs = window.attributes
                attrs.preferredRefreshRate = bestMode.refreshRate
                window.attributes = attrs
                LogUtils.d(this, "Requested refresh rate: ${bestMode.refreshRate}Hz")
            }
        } catch (e: Exception) {
            LogUtils.e(this, e, "Failed to request high refresh rate")
        }
    }

    @CallSuper
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtils.d(this, "onCreate")
        val splashScreen = installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        requestHighRefreshRate()
        super.onCreate(savedInstanceState)

        // Suppress default system splash icon exit animation and release immediately to Compose splash.
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }
        splashScreen.setKeepOnScreenCondition { false }

        // LEER SEÑAL DE BENCHMARK
        val isBenchmarkMode = intent.getBooleanExtra("is_benchmark", false)
        val shouldBenchmarkRebuildDatabase =
            isBenchmarkMode && intent.getBooleanExtra("benchmark_rebuild_database", false)
        Log.i(
            "PixelMusicBenchmark",
            "onCreate benchmark=$isBenchmarkMode rebuildDatabase=$shouldBenchmarkRebuildDatabase"
        )
        if (shouldBenchmarkRebuildDatabase) {
            lifecycleScope.launch {
                userPreferencesRepository.setInitialSetupDone(true)
                Log.i("PixelMusicBenchmark", "Enqueueing benchmark database rebuild")
                syncManager.rebuildDatabase()
                delay(1_500L)
                playerViewModel.prepareBenchmarkPlayerFromLibrary()
            }
        }

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val themeTransitionController = rememberThemeTransitionController()
            val appThemeMode by themePreferencesRepository.appThemeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)
            val useDarkTheme = when (appThemeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                else -> systemDarkTheme
            }
            val playerThemePreference by themePreferencesRepository.playerThemePreferenceFlow.collectAsStateWithLifecycle(initialValue = ThemePreference.ALBUM_ART)
            val colorPalette by themePreferencesRepository.colorPalettePreferenceFlow.collectAsStateWithLifecycle(initialValue = "DYNAMIC")
            val appFontMode by themePreferencesRepository.appFontModeFlow.collectAsStateWithLifecycle(initialValue = AppFontMode.APP_DEFAULT)
            val isAmoledBlackEnabled by themePreferencesRepository.amoledBlackModeFlow.collectAsStateWithLifecycle(initialValue = false)
            val dynamicColorEnabled = (colorPalette == "DYNAMIC" || playerThemePreference == ThemePreference.DYNAMIC) && colorPalette != "BLACK_AND_WHITE"
            val currentAlbumArtColorSchemePair by playerViewModel.currentAlbumArtColorSchemePair.collectAsStateWithLifecycle()
            val colorSchemePairOverride = if (colorPalette == "ALBUM_ART") currentAlbumArtColorSchemePair else null
            val isSetupComplete by mainViewModel.isSetupComplete.collectAsStateWithLifecycle()
            
            // Crash report dialog state
            var showCrashReportDialog by remember { mutableStateOf(false) }
            var crashLogData by remember { mutableStateOf<CrashLogData?>(null) }
            
            // Permissions Logic
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            @OptIn(ExperimentalPermissionsApi::class)
            val permissionState = rememberMultiplePermissionsState(permissions = permissions)
            // Determine if we need to show Setup based on completion OR missing permissions
            val permissionsValid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionState.permissions.firstOrNull { it.permission == Manifest.permission.READ_MEDIA_AUDIO }?.status is PermissionStatus.Granted
            } else {
                permissionState.allPermissionsGranted
            }
            val showSetupScreen = remember(isSetupComplete, permissionsValid, isBenchmarkMode) {
                when {
                    isBenchmarkMode -> false
                    isSetupComplete == null -> null
                    else -> !isSetupComplete!! || !permissionsValid
                }
            }

            // Sync Trigger: When we are NOT showing setup (meaning permissions are good and setup is done)
            LaunchedEffect(showSetupScreen) {
                if (showSetupScreen == false) {
                     LogUtils.i(this, "Setup complete/skipped and permissions valid. Starting sync.")
                     mainViewModel.startSync()
                }
            }

            // Check for crash log when app starts
            LaunchedEffect(Unit) {
                if (!isBenchmarkMode && CrashHandler.hasCrashLog()) {
                    crashLogData = CrashHandler.getCrashLog()
                    showCrashReportDialog = true
                }
            }

            PixelMusicTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColorEnabled,
                colorSchemePairOverride = colorSchemePairOverride,
                colorPalette = colorPalette,
                useSystemFont = (appFontMode == AppFontMode.SYSTEM),
                isAmoledBlack = isAmoledBlackEnabled
            ) {
                CompositionLocalProvider(
                    LocalThemeTransitionController provides themeTransitionController
                ) {
                    CircularThemeRevealHost(
                        controller = themeTransitionController,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val shouldSkipSplash = remember {
                    val action = intent?.action
                    isBenchmarkMode ||
                        action == Intent.ACTION_VIEW ||
                        action == Intent.ACTION_SEND ||
                        action == MainActivityIntentContract.ACTION_SHUFFLE_ALL ||
                        action == MainActivityIntentContract.ACTION_OPEN_PLAYLIST ||
                        action == "com.saurav.pixelmusic.ACTION_PLAY_SONG" ||
                        intent?.getBooleanExtra("ACTION_SHOW_PLAYER", false) == true ||
                        intent?.getBooleanExtra("NAVIGATE_TO_ABOUT", false) == true
                }
                var showSplashScreen by remember { mutableStateOf(!shouldSkipSplash) }
                var contentVisible by remember { mutableStateOf(shouldSkipSplash) }
                
                val contentAlpha by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
                    label = "AppContentAlpha"
                )

                val contentScale by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0.82f,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                    label = "AppContentScale"
                )
                
                val contentOffset by animateDpAsState(
                    targetValue = if (contentVisible) 0.dp else 18.dp,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                    label = "AppContentOffset"
                )

                // The Box allows us to layer the Splash Screen ON TOP of the app
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = contentAlpha
                                scaleX = contentScale
                                scaleY = contentScale
                                translationY = contentOffset.toPx()
                            },
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSetupScreen == null) {
                        SetupGateLoadingScreen()
                    } else {
                        AnimatedContent(
                            targetState = showSetupScreen,
                            transitionSpec = {
                                if (targetState) {
                                    // Transition to Setup
                                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                                } else {
                                    // Transition from Setup to Main App
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(450)) + fadeIn(animationSpec = tween(450)) togetherWith
                                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(450)) + fadeOut(animationSpec = tween(450))
                                }
                            },
                            label = "SetupTransition"
                        ) { shouldShowSetup ->
                            if (shouldShowSetup) {
                                val setupNavController = rememberNavController()
                                NavHost(
                                    navController = setupNavController,
                                    startDestination = "setup_main"
                                ) {
                                    composable("setup_main") {
                                        SetupScreen(
                                            navController = setupNavController,
                                            onSetupComplete = {
                                                // Repository-backed setup completion updates the gate automatically.
                                            }
                                        )
                                    }
                                    composable(
                                        route = Screen.YoutubeAuth.route,
                                        arguments = listOf(navArgument("addAccount") {
                                            type = NavType.BoolType
                                            defaultValue = false
                                        })
                                    ) {
                                        com.saurav.pixelmusic.presentation.screens.youtube.AuthScreen(
                                            onBack = { setupNavController.popBackStack() }
                                        )
                                    }
                                }
                            } else {
                                MainAppContent(playerViewModel, mainViewModel)
                            }
                        }
                    }

                    // Show crash report dialog if needed
                    if (showCrashReportDialog && crashLogData != null) {
                        CrashReportDialog(
                            crashLog = crashLogData!!,
                            onDismiss = {
                                CrashHandler.clearCrashLog()
                                crashLogData = null
                                showCrashReportDialog = false
                            }
                        )
                    }
                } // End of Surface

                if (showSplashScreen) {
                    com.saurav.pixelmusic.presentation.screens.AnimatedSplashScreen(
                        onExitStarted = {
                            contentVisible = true
                        },
                        onSplashFinished = {
                            contentVisible = true
                            showSplashScreen = false
                        }
                    )
                }
                    }
                }
            }
        }
    }
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Catch intent from "Download on Like" notification
        handleDownloadIntent(intent, playerViewModel)

        // 2. Catch intent from "Update Ready" notification
        handleInstallIntent(intent)

        when {
            // Handle shuffle all shortcut / tile
            intent.action == MainActivityIntentContract.ACTION_SHUFFLE_ALL -> {
                android.util.Log.d("TileDebug", "handleIntent: ACTION_SHUFFLE_ALL received")
                playerViewModel.triggerShuffleAllFromTile()
                intent.action = null // Clear action to prevent re-triggering
            }
            
            // Handle playlist shortcut
            intent.action == MainActivityIntentContract.ACTION_OPEN_PLAYLIST -> {
                intent.getStringExtra(MainActivityIntentContract.EXTRA_PLAYLIST_ID)?.let { playlistId ->
                    _pendingPlaylistNavigation.value = playlistId
                }
                intent.action = null
            }

            intent.getBooleanExtra("NAVIGATE_TO_ABOUT", false) -> {
                _pendingRouteNavigation.value = Screen.About.route
                intent.removeExtra("NAVIGATE_TO_ABOUT")
            }

            intent.getBooleanExtra("ACTION_SHOW_PLAYER", false) -> {
                playerViewModel.showPlayer()
            }

            // Handle YouTube Share Intent (Sharing from YT/YT Music app)
            intent.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (sharedText != null && sharedText.contains("youtu")) {
                    val url = sharedText.split("\\s+".toRegex()).firstOrNull { it.contains("youtu") }
                    if (url != null) {
                        extractYoutubeVideoId(url)?.let { videoId ->
                            val endpoint = saurav.shru.pixelmusic.innertube.models.WatchEndpoint(
                                videoId = videoId,
                                playlistId = "RDAMVM$videoId"
                            )
                            playerViewModel.playRadio(endpoint, "Shared from YouTube")
                            playerViewModel.showPlayer()
                        }
                    }
                }
                clearExternalIntentPayload(intent)
            }

            intent.action == android.content.Intent.ACTION_VIEW && intent.data != null -> {
                intent.data?.let { uri ->
                    if (uri.scheme == "pixelmusic" && (uri.host == "listen_together" || uri.path?.contains("listen_together") == true)) {
                        val roomCode = uri.getQueryParameter("code")
                        if (!roomCode.isNullOrBlank()) {
                            playerViewModel.setPendingListenTogetherCode(roomCode)
                        }
                        playerViewModel.openListenTogetherSheet()
                    } else {
                        persistUriPermissionIfNeeded(intent, uri)
                        playerViewModel.playExternalUri(uri)
                    }
                }
                clearExternalIntentPayload(intent)
            }

            intent.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true -> {
                resolveStreamUri(intent)?.let { uri ->
                    persistUriPermissionIfNeeded(intent, uri)
                    playerViewModel.playExternalUri(uri)
                }
                clearExternalIntentPayload(intent)
            }
            
            intent.action == "com.saurav.pixelmusic.ACTION_PLAY_SONG" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                     intent.getParcelableExtra("song", com.saurav.pixelmusic.data.model.Song::class.java)?.let { song ->
                         playerViewModel.playSong(song)
                     }
                } else {
                     @Suppress("DEPRECATION")
                     intent.getParcelableExtra<com.saurav.pixelmusic.data.model.Song>("song")?.let { song ->
                         playerViewModel.playSong(song)
                     }
                }
                intent.action = null
            }
        }
    }

    private fun handleDownloadIntent(intent: Intent?, playerViewModel: PlayerViewModel) {
        if (intent?.action == "PLAY_DOWNLOADED_SONG") {
            val songId = intent.getStringExtra("song_id")
            if (!songId.isNullOrBlank()) {
                lifecycleScope.launch {
                    // Fetch the song from the database using the ID, then play it!
                    playerViewModel.observeSong(songId).firstOrNull()?.let { song ->
                        playerViewModel.showAndPlaySong(song)
                    }
                }
            }
        }
    }

    private fun handleInstallIntent(intent: Intent?) {
        if (intent?.action == "INSTALL_UPDATE") {
            val fileName = intent.getStringExtra("apk_file_name")
            if (!fileName.isNullOrBlank() && !fileName.contains("/") && !fileName.contains("\\") && !fileName.contains("..")) {
                val downloadDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                val apkFile = java.io.File(downloadDir, fileName)
                
                if (apkFile.exists() && apkFile.canonicalPath.startsWith(downloadDir?.canonicalPath ?: "")) {
                    val apkUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider", // Make sure this matches the FileProvider authority in your AndroidManifest.xml
                        apkFile
                    )
                    
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(installIntent)
                }
            }
        }
    }
    
    private fun resolveStreamUri(intent: Intent): android.net.Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)?.let { return it }
        } else {
            @Suppress("DEPRECATION")
            val legacyUri = intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (legacyUri != null) return legacyUri
        }

        intent.clipData?.let { clipData ->
            if (clipData.itemCount > 0) {
                return clipData.getItemAt(0).uri
            }
        }

        return intent.data
    }

    private fun persistUriPermissionIfNeeded(intent: Intent, uri: android.net.Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val hasPersistablePermission = intent.flags and android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
            if (hasPersistablePermission) {
                val takeFlags = intent.flags and (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (takeFlags != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (securityException: SecurityException) {
                        android.util.Log.w("MainActivity", "Unable to persist URI permission for $uri", securityException)
                    } catch (illegalArgumentException: IllegalArgumentException) {
                        android.util.Log.w("MainActivity", "Persistable URI permission not granted for $uri", illegalArgumentException)
                    }
                }
            }
        }
    }

    private fun clearExternalIntentPayload(intent: Intent) {
        intent.data = null
        intent.clipData = null
        intent.removeExtra(android.content.Intent.EXTRA_STREAM)
    }

    /** Returns true if the MIME type or file name indicates an M3U/M3U8 playlist. */
    private fun extractYoutubeVideoId(text: String): String? {
        val url = text.split("\\s+".toRegex()).firstOrNull { it.contains("youtu") } ?: text
        return try {
            when {
                url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/").substringBefore("&")
                url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&").substringBefore("#")
                url.contains("shorts/") -> url.substringAfter("shorts/").substringBefore("?").substringBefore("/").substringBefore("&")
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isM3uMimeOrExtension(mimeType: String?, fileName: String?): Boolean {
        val m3uMimeTypes = setOf(
            "audio/x-mpegurl",
            "audio/mpegurl",
            "application/vnd.apple.mpegurl"
        )
        if (mimeType != null && mimeType.lowercase() in m3uMimeTypes) return true
        val lower = fileName?.lowercase() ?: return false
        return lower.endsWith(".m3u") || lower.endsWith(".m3u8")
    }

    private fun openExternalUrl(url: String) {
        // Defense in depth: the announcement URL is fetched from a remote
        // properties file on GitHub. If that file is ever tampered with, we
        // must not let it launch arbitrary intents (`intent://...`,
        // `javascript:`, custom schemes, etc.). Allow only the Play Store host.
        val parsed = runCatching { url.toUri() }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()
        val isPlayStore = scheme == "https" &&
            (host == "play.google.com" || host == "market.android.com")
        if (!isPlayStore) {
            LogUtils.w(this, "Refusing to open non-Play-Store announcement URL: $url")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, parsed)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            LogUtils.w(this, "No activity available to open URL: $url")
        }
    }

    private fun PlayStoreAnnouncementRemoteConfig.toUiModel(context: Context): PlayStoreAnnouncementUiModel {
        val fallback = PlayStoreAnnouncementDefaults.localizedTemplate(context)
        return fallback.copy(
            enabled = enabled,
            playStoreUrl = playStoreUrl ?: fallback.playStoreUrl,
            title = title ?: fallback.title,
            body = body ?: fallback.body,
            primaryActionLabel = primaryActionLabel ?: fallback.primaryActionLabel,
            dismissActionLabel = dismissActionLabel ?: fallback.dismissActionLabel,
            linkPendingMessage = linkPendingMessage ?: fallback.linkPendingMessage,
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SetupGateLoadingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularWavyProgressIndicator()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Preparing setup…",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun MainAppContent(playerViewModel: PlayerViewModel, mainViewModel: MainViewModel) {
        Trace.beginSection("MainActivity.MainAppContent")
        val navController = rememberNavController()
        val isSyncing by mainViewModel.isSyncing.collectAsStateWithLifecycle()
        val isLibraryEmpty by mainViewModel.isLibraryEmpty.collectAsStateWithLifecycle()
        val hasCompletedInitialSync by mainViewModel.hasCompletedInitialSync.collectAsStateWithLifecycle()
        val syncProgress by mainViewModel.syncProgress.collectAsStateWithLifecycle()
        
        // isMediaControllerReady used below for playlist navigation gate
        val isMediaControllerReady by playerViewModel.isMediaControllerReady.collectAsStateWithLifecycle()
        
        // Observe pending playlist navigation
        val pendingPlaylistNav by _pendingPlaylistNavigation.collectAsStateWithLifecycle()
        val pendingRouteNav by _pendingRouteNavigation.collectAsStateWithLifecycle()
        LaunchedEffect(pendingRouteNav) {
            pendingRouteNav?.let { route ->
                navController.navigateSafely(route)
                _pendingRouteNavigation.value = null
            }
        }
        var processedPlaylistId by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(pendingPlaylistNav, isMediaControllerReady) {
            val playlistId = pendingPlaylistNav
            // Only process if we have a new playlist ID that hasn't been processed yet
            if (playlistId != null && playlistId != processedPlaylistId && isMediaControllerReady) {
                processedPlaylistId = playlistId
                // Wait for navigation graph to be ready (retry with delay)
                var success = false
                var attempts = 0
                while (!success && attempts < 50) { // 5 seconds max
                    try {
                        success = navController.navigateSafely(Screen.PlaylistDetail.createRoute(playlistId))
                        if (success) {
                            _pendingPlaylistNavigation.value = null
                        } else {
                            delay(100)
                            attempts++
                        }
                    } catch (e: IllegalArgumentException) {
                        delay(100)
                        attempts++
                    }
                }
            } else if (playlistId == null) {
                // Reset so the same playlist can be opened again
                processedPlaylistId = null
            }
        }

        // Estado para controlar si el indicador de carga puede mostrarse después de un delay
        var canShowLoadingIndicator by remember { mutableStateOf(false) }
        // Track when the loading indicator was first shown for minimum display time
        var loadingShownTimestamp by remember { mutableStateOf(0L) }
        val minimumDisplayDuration = 1500L // Show loading for at least 1.5 seconds

        val shouldPotentiallyShowLoading = isSyncing && isLibraryEmpty && !hasCompletedInitialSync

        LaunchedEffect(shouldPotentiallyShowLoading) {
            if (shouldPotentiallyShowLoading) {
                // Espera un breve período antes de permitir que se muestre el indicador de carga
                // Ajusta este valor según sea necesario (por ejemplo, 300-500 ms)
                delay(300L)
                // Vuelve a verificar la condición después del delay,
                // ya que el estado podría haber cambiado.
                if (mainViewModel.isSyncing.value && mainViewModel.isLibraryEmpty.value) {
                    canShowLoadingIndicator = true
                    loadingShownTimestamp = System.currentTimeMillis()
                }
            } else {
                // Ensure minimum display time before hiding
                if (canShowLoadingIndicator && loadingShownTimestamp > 0) {
                    val elapsed = System.currentTimeMillis() - loadingShownTimestamp
                    val remaining = minimumDisplayDuration - elapsed
                    if (remaining > 0) {
                        delay(remaining)
                    }
                }
                canShowLoadingIndicator = false
                loadingShownTimestamp = 0L
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MainUI(playerViewModel, navController)

            // Muestra el LoadingOverlay solo si las condiciones se cumplen Y el delay ha pasado
            if (canShowLoadingIndicator) {
                LoadingOverlay(syncProgress)
            }
        }
        Trace.endSection() // End MainActivity.MainAppContent
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun MainUI(playerViewModel: PlayerViewModel, navController: NavHostController) {
        Trace.beginSection("MainActivity.MainUI")

        val commonNavItems = remember {
            persistentListOf(
                BottomNavItem("Home", R.drawable.rounded_home_24, R.drawable.home_24_rounded_filled, Screen.Home),
                BottomNavItem("Explore", R.drawable.rounded_album_24, R.drawable.rounded_album_24, Screen.Explore),
                BottomNavItem("Library", R.drawable.rounded_library_music_24, R.drawable.round_library_music_24, Screen.Library)
            )
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        var isSearchBarActive by remember { mutableStateOf(false) }

        val mainRoutesWithNavigationBar = remember {
            setOf(
                Screen.Home.route,
                Screen.Explore.route,
                Screen.Search.route,
                Screen.Library.route
            )
        }
        val shouldHideNavigationBar by remember(currentRoute, isSearchBarActive) {
            derivedStateOf {
                if (currentRoute == null) {
                    false
                } else if (currentRoute == Screen.Search.route && isSearchBarActive) {
                    true
                } else {
                    currentRoute !in mainRoutesWithNavigationBar
                }
            }
        }

        val navBarStyle by playerViewModel.navBarStyle.collectAsStateWithLifecycle()
        val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
        val performanceModeEnabled by playerViewModel.userPreferencesRepository.performanceModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
        val liquidGlassEnabled by playerViewModel.userPreferencesRepository.liquidGlassEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassNavBarEnabled by playerViewModel.userPreferencesRepository.liquidGlassNavBarEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassMiniPlayerEnabled by playerViewModel.userPreferencesRepository.liquidGlassMiniPlayerEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassDismissPromptEnabled by playerViewModel.userPreferencesRepository.liquidGlassDismissPromptEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassBlurIntensity by playerViewModel.userPreferencesRepository.liquidGlassBlurIntensityFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val liquidGlassRefractionIntensity by playerViewModel.userPreferencesRepository.liquidGlassRefractionIntensityFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val liquidGlassVibrancyIntensity by playerViewModel.userPreferencesRepository.liquidGlassVibrancyIntensityFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val liquidGlassRefractionHeight by playerViewModel.userPreferencesRepository.liquidGlassRefractionHeightFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val liquidGlassRefractionAmount by playerViewModel.userPreferencesRepository.liquidGlassRefractionAmountFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val liquidGlassChromaticAberration by playerViewModel.userPreferencesRepository.liquidGlassChromaticAberrationFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassDepthEffect by playerViewModel.userPreferencesRepository.liquidGlassDepthEffectFlow.collectAsStateWithLifecycle(initialValue = true)
        val liquidGlassSurfaceTint by playerViewModel.userPreferencesRepository.liquidGlassSurfaceTintFlow.collectAsStateWithLifecycle(initialValue = "SYSTEM")
        val liquidGlassSurfaceOpacity by playerViewModel.userPreferencesRepository.liquidGlassSurfaceOpacityFlow.collectAsStateWithLifecycle(initialValue = 0.25f)
        val liquidGlassStyle by playerViewModel.userPreferencesRepository.liquidGlassStyleFlow.collectAsStateWithLifecycle(initialValue = "LIQUID")
        val liquidGlassTabSelectionOpacity by playerViewModel.userPreferencesRepository.liquidGlassTabSelectionOpacityFlow.collectAsStateWithLifecycle(initialValue = 0.35f)
        val liquidGlassTabSelectionColorMode by playerViewModel.userPreferencesRepository.liquidGlassTabSelectionColorModeFlow.collectAsStateWithLifecycle(initialValue = "CLEAR")
        val liquidGlassHighlightOpacity by playerViewModel.userPreferencesRepository.liquidGlassHighlightOpacityFlow.collectAsStateWithLifecycle(initialValue = 0.20f)
        val liquidGlassTextColor by playerViewModel.userPreferencesRepository.liquidGlassTextColorFlow.collectAsStateWithLifecycle(initialValue = "ADAPTIVE")
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val effectiveLiquidGlassEnabled = liquidGlassEnabled && !performanceModeEnabled && AndroidVersionCompat.supportsLiquidGlass
        val isLiquidGlassOnNavBar = effectiveLiquidGlassEnabled && liquidGlassNavBarEnabled
        val isLiquidGlassOnMiniPlayer = effectiveLiquidGlassEnabled && liquidGlassMiniPlayerEnabled
        val isLiquidGlassOnDismissPrompt = effectiveLiquidGlassEnabled && liquidGlassDismissPromptEnabled
        val navBackdrop = rememberLayerBackdrop {
            drawRect(if (isDarkTheme) Color.Black else Color.White)
            drawContent()
        }
        val navBarCornerRadiusRaw by playerViewModel.navBarCornerRadius.collectAsStateWithLifecycle()
        val navBarCornerRadius = sanitizeNavBarCornerRadius(navBarCornerRadiusRaw)
        val isMiniPlayerDismissing by playerViewModel.isMiniPlayerDismissing.collectAsStateWithLifecycle()
        val hapticsEnabled by playerViewModel.hapticsEnabled.collectAsStateWithLifecycle()
        val rootView = LocalView.current
        val platformHapticFeedback = LocalHapticFeedback.current
        val appHapticsConfig = remember(hapticsEnabled) {
            AppHapticsConfig(enabled = hapticsEnabled)
        }
        val motionBlurIntensity by userPreferencesRepository.uiMotionBlurIntensityFlow.collectAsStateWithLifecycle(initialValue = 1f)
        val motionBlurEnabled by userPreferencesRepository.uiMotionBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
        val scopedHapticFeedback = remember(platformHapticFeedback, appHapticsConfig.enabled) {
            if (appHapticsConfig.enabled) platformHapticFeedback else NoOpHapticFeedback
        }

        val systemNavBarInset = sanitizeNavigationBarBottomInset(
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )

        LaunchedEffect(hapticsEnabled, rootView) {
            rootView.isHapticFeedbackEnabled = hapticsEnabled
            rootView.rootView?.isHapticFeedbackEnabled = hapticsEnabled
        }

        val horizontalPadding = if (navBarStyle == NavBarStyle.DEFAULT) {
            if (systemNavBarInset > 30.dp) 14.dp else systemNavBarInset
        } else {
            0.dp
        }
        val animatedBottomBarPadding by animateDpAsState(
            targetValue = if (navBarStyle == NavBarStyle.FULL_WIDTH) 0.dp else systemNavBarInset,
            animationSpec = tween(400),
            label = "BottomBarPadding"
        )
        val bottomBarPadding = animatedBottomBarPadding
        val navBarHeight = resolveNavBarSurfaceHeight(navBarStyle, systemNavBarInset, navBarCompactMode)
        val navBarOccupiedHeight by remember(systemNavBarInset, navBarCompactMode) {
            derivedStateOf { resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode) }
        }
        
        // 1. Force the bar to start hidden on cold boot, then reveal it to trigger the spring!
        var isMounted by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(100) // Give the UI a split second to draw before springing up
            isMounted = true
        }

        // 2. Dynamically switch between fading out and springing up
        val targetNavVisibility = if (!isMounted || shouldHideNavigationBar) 0f else 1f
        val navBarVisibilityProgress by animateFloatAsState(
            targetValue = targetNavVisibility,
            animationSpec = if (targetNavVisibility == 0f) {
                // Going to Settings: Fast fade/collapse so it doesn't feel like a sluggish drop
                tween(durationMillis = 150)
            } else {
                // Returning / App Opening: Smooth slide-up with ZERO bounce
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "NavBarVisibilityProgress"
        )
        val visibleNavBarOccupiedHeight by remember(navBarOccupiedHeight, navBarVisibilityProgress) {
            derivedStateOf { navBarOccupiedHeight * navBarVisibilityProgress }
        }
        val miniPlayerBottomMargin by remember(systemNavBarInset, visibleNavBarOccupiedHeight) {
            derivedStateOf {
                if (visibleNavBarOccupiedHeight > systemNavBarInset) {
                    visibleNavBarOccupiedHeight
                } else {
                    systemNavBarInset
                }
            }
        }
        val shouldRenderNavigationBar by remember(shouldHideNavigationBar, navBarVisibilityProgress) {
            derivedStateOf {
                !shouldHideNavigationBar || navBarVisibilityProgress > 0.01f
            }
        }
        val isNavBarEffectivelyHidden by remember(shouldHideNavigationBar, navBarVisibilityProgress) {
            derivedStateOf {
                shouldHideNavigationBar && navBarVisibilityProgress <= 0.01f
            }
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val announcementService = remember { GitHubAnnouncementPropertiesService() }
        val context = LocalContext.current
        var playStoreAnnouncement by remember {
            mutableStateOf(PlayStoreAnnouncementDefaults.localizedTemplate(context))
        }
        var showPlayStoreAnnouncement by remember { mutableStateOf(false) }
        var showRecognitionDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (PlayStoreAnnouncementDefaults.LOCAL_PREVIEW_ENABLED) {
                playStoreAnnouncement = PlayStoreAnnouncementDefaults.hardcodedPreview(this@MainActivity)
                showPlayStoreAnnouncement = true
                return@LaunchedEffect
            }

            announcementService.fetchPlayStoreAnnouncement()
                .onSuccess { remoteConfig ->
                    val resolvedAnnouncement = remoteConfig.toUiModel(this@MainActivity)
                    playStoreAnnouncement = resolvedAnnouncement
                    showPlayStoreAnnouncement = resolvedAnnouncement.enabled
                }
                .onFailure { throwable ->
                    LogUtils.w(
                        this@MainActivity,
                        "Remote announcement unavailable. Keeping popup disabled. ${throwable.message ?: ""}",
                    )
                }
        }

        LaunchedEffect(userPreferencesRepository) {
            userPreferencesRepository.clearDeprecatedPlayerSheetPreference()
        }

        CompositionLocalProvider(
            LocalAppHapticsConfig provides appHapticsConfig,
            LocalHapticFeedback provides scopedHapticFeedback,
            LocalMotionBlurIntensity provides motionBlurIntensity,
            LocalMotionBlurEnabled provides motionBlurEnabled,
            LocalLiquidGlassBlurIntensity provides liquidGlassBlurIntensity,
            LocalLiquidGlassRefractionIntensity provides liquidGlassRefractionIntensity,
            LocalLiquidGlassEnabled provides effectiveLiquidGlassEnabled,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassVibrancyIntensity provides liquidGlassVibrancyIntensity,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionHeight provides liquidGlassRefractionHeight,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassRefractionAmount provides liquidGlassRefractionAmount,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassChromaticAberration provides liquidGlassChromaticAberration,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassDepthEffect provides liquidGlassDepthEffect,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceTint provides liquidGlassSurfaceTint,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassSurfaceOpacity provides liquidGlassSurfaceOpacity,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassStyle provides liquidGlassStyle,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTabSelectionOpacity provides liquidGlassTabSelectionOpacity,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTabSelectionColorMode provides liquidGlassTabSelectionColorMode,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassHighlightOpacity provides liquidGlassHighlightOpacity,
            com.saurav.pixelmusic.ui.theme.LocalLiquidGlassTextColor provides liquidGlassTextColor
        ) {
            AppSidebarDrawer(
                drawerState = drawerState,
                selectedRoute = currentRoute ?: Screen.Home.route,
                onDestinationSelected = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        DrawerDestination.Home -> navController.navigateSafely(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                        DrawerDestination.Settings -> navController.navigateSafely(Screen.Settings.route)
                        else -> { }
                    }
                }
            ) {
                val backgroundStyle by playerViewModel.userPreferencesRepository.appBackgroundStyleFlow.collectAsStateWithLifecycle(initialValue = com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.DEFAULT)
                val backgroundCustomUri by playerViewModel.userPreferencesRepository.appBackgroundCustomUriFlow.collectAsStateWithLifecycle(initialValue = "")
                val backgroundOpacity by playerViewModel.userPreferencesRepository.appBackgroundOpacityFlow.collectAsStateWithLifecycle(initialValue = 0.5f)
                val backgroundBlur by playerViewModel.userPreferencesRepository.appBackgroundBlurFlow.collectAsStateWithLifecycle(initialValue = 0f)
                
                val currentSong by remember { playerViewModel.stablePlayerState.map { it.currentSong }.distinctUntilChanged() }.collectAsStateWithLifecycle(initialValue = null)

                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. The Global Wallpaper (Drawn only once!)
                    // We bundle the triggers into a single state so it only fades when these specific values change
                    val backgroundTarget = remember(backgroundStyle, currentSong?.albumArtUriString, backgroundCustomUri) {
                        listOf(backgroundStyle, currentSong?.albumArtUriString, backgroundCustomUri)
                    }

                    Crossfade(
                        targetState = backgroundTarget,
                        animationSpec = tween(durationMillis = 800), // Nice, slow 800ms fade
                        label = "BackgroundCrossfade"
                    ) { state ->
                        val currentStyle = state[0] as com.saurav.pixelmusic.data.preferences.AppBackgroundStyle
                        val currentArt = state[1] as String?
                        val currentUri = state[2] as String?

                        if (currentStyle != com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.DEFAULT) {
                            // Reading these inside the lambda means sliders update instantly without triggering a crossfade!
                            val alpha = backgroundOpacity 
                            val blurMod = if (backgroundBlur > 0f) Modifier.blur(backgroundBlur.dp) else Modifier
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { this.alpha = alpha }
                                    .then(blurMod)
                            ) {
                                when (currentStyle) {
                                    com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.MUSIC_NOTES -> {
                                        Image(
                                            painter = painterResource(id = R.drawable.bg_music_notes_tintable),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.LIVE_BLUR -> {
                                        if (currentArt != null) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                com.saurav.pixelmusic.presentation.components.SmartImage(
                                                    model = currentArt,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                )
                                            }
                                        }
                                    }
                                    com.saurav.pixelmusic.data.preferences.AppBackgroundStyle.CUSTOM -> {
                                        if (!currentUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. The Main Content Scaffold (Transparent so background shows through)
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                    if (shouldRenderNavigationBar) {
                        val currentSongId by remember {
                            playerViewModel.stablePlayerState
                                .map { it.currentSong?.id }
                                .distinctUntilChanged()
                        }.collectAsStateWithLifecycle(initialValue = null)
                        val showPlayerContentArea = currentSongId != null
                        val navBarElevation = 3.dp

                        val animatedNavBarCornerRadius by animateDpAsState(
                            targetValue = navBarCornerRadius.dp,
                            animationSpec = tween(400),
                            label = "NavBarCornerRadius"
                        )

                        val animatedDefaultTopCornerRadius by animateDpAsState(
                            targetValue = if (showPlayerContentArea && !isMiniPlayerDismissing) 10.dp else navBarCornerRadius.dp,
                            animationSpec = tween(400),
                            label = "NavBarDefaultTopCornerRadius"
                        )

                        val actualShape = remember(
                            navBarStyle,
                            showPlayerContentArea,
                            isMiniPlayerDismissing,
                            navBarCornerRadius,
                            animatedNavBarCornerRadius,
                            animatedDefaultTopCornerRadius
                        ) {
                            DynamicSmoothCornerShape(
                                topRadiusProvider = {
                                    val fraction = playerViewModel.playerContentExpansionFraction.value
                                    if (navBarStyle == NavBarStyle.DEFAULT) {
                                        animatedDefaultTopCornerRadius
                                    } else if (navBarStyle == NavBarStyle.FULL_WIDTH) {
                                        lerp(navBarCornerRadius.dp, 26.dp, fraction)
                                    } else if (showPlayerContentArea) {
                                        if (fraction < 0.2f) {
                                            lerp(navBarCornerRadius.dp, 26.dp, (fraction / 0.2f).coerceIn(0f, 1f))
                                        } else {
                                            26.dp
                                        }
                                    } else {
                                        navBarCornerRadius.dp
                                    }
                                },
                                bottomRadiusProvider = {
                                    if (navBarStyle == NavBarStyle.FULL_WIDTH) 0.dp else animatedNavBarCornerRadius
                                }
                            )
                        }

                        var componentHeightPx by remember { mutableStateOf(0) }
                        val density = LocalDensity.current
                        val shadowOverflowPx = remember(navBarElevation, density) {
                            with(density) { (navBarElevation * 8).toPx() }
                        }
                        val bottomBarPaddingPx = remember(bottomBarPadding, density) {
                            with(density) { bottomBarPadding.toPx() }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(visibleNavBarOccupiedHeight)
                                // Removed .clipToBounds() so the bar slides cleanly without being cropped
                        ) {
                            val onSearchIconDoubleTap = remember(playerViewModel) {
                                { playerViewModel.onSearchNavIconDoubleTapped() }
                            }
                            val isNavBarCoveredByPlayer by remember(showPlayerContentArea) {
                                derivedStateOf {
                                    showPlayerContentArea && playerViewModel.playerContentExpansionFraction.value >= 0.45f
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(bottom = bottomBarPadding)
                                    .onSizeChanged { componentHeightPx = it.height }
                                    .graphicsLayer {
                                        val hideFraction = if (showPlayerContentArea) {
                                            playerViewModel.playerContentExpansionFraction.value.coerceIn(0f, 1f)
                                        } else {
                                            0f
                                        }
                                        val totalBarHeight = componentHeightPx + shadowOverflowPx + bottomBarPaddingPx
                                        val playerExpansionSlide = totalBarHeight * hideFraction
                                        val navigationVisibilitySlide = (1f - navBarVisibilityProgress) * totalBarHeight
                                        val expansionAlpha = (1f - hideFraction * 2.2f).coerceIn(0f, 1f)

                                        translationY = playerExpansionSlide + navigationVisibilitySlide
                                        alpha = (navBarVisibilityProgress * expansionAlpha).coerceIn(0f, 1f)
                                    }
                                    .height(navBarHeight)
                                    .padding(horizontal = horizontalPadding),
                            ) {
                                PlayerInternalNavigationBar(
                                    navController = navController,
                                    navItems = commonNavItems,
                                    currentRoute = currentRoute,
                                    navBarStyle = navBarStyle,
                                    compactMode = navBarCompactMode,
                                    bottomBarPadding = bottomBarPadding,
                                    onSearchIconDoubleTap = onSearchIconDoubleTap,
                                    isLiquidGlass = isLiquidGlassOnNavBar,
                                    backdrop = if (isNavBarCoveredByPlayer) null else navBackdrop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val density = LocalDensity.current
                        val containerHeight = this.maxHeight
                        val screenHeightPx = remember(containerHeight, density) {
                            with(density) { containerHeight.toPx() }
                        }

                        val showPlayerContentInitially by remember {
                            playerViewModel.stablePlayerState
                                .map { it.currentSong?.id != null }
                                .distinctUntilChanged()
                        }.collectAsStateWithLifecycle(initialValue = false)
                        val routesWithHiddenMiniPlayer = remember { setOf(Screen.NavBarCrRad.route) }
                        val shouldHideMiniPlayer by remember(currentRoute) {
                            derivedStateOf { currentRoute in routesWithHiddenMiniPlayer }
                        }

                        val miniPlayerH = with(density) { MiniPlayerHeight.toPx() }
                        val totalSheetHeightWhenContentCollapsedPx = if (showPlayerContentInitially && !shouldHideMiniPlayer) miniPlayerH else 0f

                        val bottomMargin = miniPlayerBottomMargin

                        val spacerPx = with(density) { MiniPlayerBottomSpacer.toPx() }
                        val bottomMarginPx = with(density) { bottomMargin.toPx() }
                        val sheetCollapsedTargetY = calculatePlayerSheetCollapsedTargetY(
                            containerHeightPx = screenHeightPx,
                            collapsedContentHeightPx = totalSheetHeightWhenContentCollapsedPx,
                            bottomMarginPx = bottomMarginPx,
                            bottomSpacerPx = spacerPx
                        )

                        val isPlayerFullyExpanded by remember(showPlayerContentInitially) {
                            derivedStateOf {
                                showPlayerContentInitially && playerViewModel.playerContentExpansionFraction.value >= 0.98f
                            }
                        }
                        val isBottomChromeCovered by remember(showPlayerContentInitially) {
                            derivedStateOf {
                                showPlayerContentInitially && playerViewModel.playerContentExpansionFraction.value >= 0.45f
                            }
                        }
                        val shouldCaptureNavBackdrop = (isLiquidGlassOnNavBar || isLiquidGlassOnMiniPlayer || isLiquidGlassOnDismissPrompt) && !isPlayerFullyExpanded

                        // ==========================================
                        // BASE CONTENT LAYER (SAMPLED FOR GLASS BACKDROP)
                        // Contains Scrims & AppNavigation ONLY (no floating glass surfaces)
                        // ==========================================
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (shouldCaptureNavBackdrop) Modifier.layerBackdrop(navBackdrop) else Modifier)
                        ) {
                            // ==========================================
                            // GLOBAL PERSISTENT SCRIMS (BACKGROUND LAYER)
                            // Placed before AppNavigation so they sit BEHIND the headers and lists
                            // ==========================================
                            val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
                            
                            val scrimTopColor = if (isLightTheme) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.background)
                            } else {
                                MaterialTheme.colorScheme.background
                            }
                            
                            val scrimBottomColor = if (isLightTheme) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).compositeOver(MaterialTheme.colorScheme.background)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.background)
                            }
                            
                            val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            
                            // Global Top Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .height(statusBarTopPadding + 64.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.00f to scrimTopColor.copy(alpha = 0.95f),
                                                0.18f to scrimTopColor.copy(alpha = 0.86f),
                                                0.36f to scrimTopColor.copy(alpha = 0.68f),
                                                0.54f to scrimTopColor.copy(alpha = 0.48f),
                                                0.72f to scrimTopColor.copy(alpha = 0.28f),
                                                0.88f to scrimTopColor.copy(alpha = 0.11f),
                                                1.00f to Color.Transparent
                                            )
                                        )
                                    )
                            )
                            
                            // Global Bottom Scrim (Subtle when Liquid Glass is on so glass samples real backdrop content)
                            val currentSongIdForScrim by remember { playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged() }.collectAsStateWithLifecycle(initialValue = null)
                            val bottomPaddingForScrim = innerPadding.calculateBottomPadding() + (if(currentSongIdForScrim != null) MiniPlayerHeight else 0.dp)
                            val bottomScrimAlphaScale = if (isLiquidGlassOnNavBar) 0.15f else 1.0f
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .height(bottomPaddingForScrim + 140.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.00f to Color.Transparent,
                                                0.12f to scrimBottomColor.copy(alpha = 0.11f * bottomScrimAlphaScale),
                                                0.28f to scrimBottomColor.copy(alpha = 0.28f * bottomScrimAlphaScale),
                                                0.46f to scrimBottomColor.copy(alpha = 0.48f * bottomScrimAlphaScale),
                                                0.64f to scrimBottomColor.copy(alpha = 0.68f * bottomScrimAlphaScale),
                                                0.82f to scrimBottomColor.copy(alpha = 0.86f * bottomScrimAlphaScale),
                                                1.00f to scrimBottomColor.copy(alpha = 0.95f * bottomScrimAlphaScale)
                                            )
                                        )
                                    )
                            )
                            
                            AppNavigation(
                                playerViewModel = playerViewModel,
                                navController = navController,
                                paddingValues = innerPadding,
                                userPreferencesRepository = userPreferencesRepository,
                                onSearchBarActiveChange = { isSearchBarActive = it },
                                onOpenSidebar = { scope.launch { drawerState.open() } }
                            )
                        }

                        val isHomeOrExploreRoute = currentRoute == Screen.Home.route || currentRoute == Screen.Explore.route
                        val isFabVisible by remember(currentRoute, isSearchBarActive) {
                            derivedStateOf { isHomeOrExploreRoute && !isSearchBarActive }
                        }
                        AnimatedVisibility(
                            visible = isFabVisible,
                            enter = fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.85f),
                            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .graphicsLayer {
                                    alpha = (1f - playerViewModel.playerContentExpansionFraction.value * 3f).coerceIn(0f, 1f)
                                }
                        ) {
                            val isExplore = currentRoute == Screen.Explore.route
                            val currentSong by remember {
                                playerViewModel.stablePlayerState
                                    .map { it.currentSong }
                                    .distinctUntilChanged()
                            }.collectAsStateWithLifecycle(initialValue = null)

                            val ltUiState by playerViewModel.listenTogetherUiState.collectAsStateWithLifecycle()
                            val isLtActive = ltUiState is com.saurav.pixelmusic.data.session.ListenTogetherUiState.Hosting ||
                                ltUiState is com.saurav.pixelmusic.data.session.ListenTogetherUiState.Guest

                            HomeShuffleFab(
                                isShuffleEnabled = false,
                                isPlayerActive = currentSong != null,
                                baseBottomOffset = innerPadding.calculateBottomPadding(),
                                isExploreMode = isExplore,
                                isSessionActive = isLtActive,
                                onClick = {
                                    if (isExplore) {
                                        navController.navigateSafely(Screen.SmartMix.route)
                                    } else {
                                        val yourMix = playerViewModel.yourMixSongs.value
                                        if (yourMix.isNotEmpty()) {
                                            playerViewModel.playSongsShuffled(yourMix, "Your Mix")
                                        } else {
                                            playerViewModel.playRandomSong()
                                        }
                                    }
                                },
                                onSwipeUp = { showRecognitionDialog = true },
                                onListenTogetherClick = { playerViewModel.openListenTogetherSheet() },
                                isLiquidGlass = isLiquidGlassOnNavBar,
                                backdrop = if (isBottomChromeCovered) null else navBackdrop
                            )
                        }
                            
                        val isExpandedOrExpanding by remember {
                            derivedStateOf {
                                playerViewModel.playerContentExpansionFraction.value > 0.01f
                            }
                        }
                        if (isExpandedOrExpanding) {
                            val scrimColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = (playerViewModel.playerContentExpansionFraction.value * 0.6f).coerceIn(0f, 0.6f)
                                    }
                                    .background(scrimColor)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            playerViewModel.collapsePlayerSheet()
                                        }
                                    }
                            )
                        }

                        UnifiedPlayerSheetV2(
                            playerViewModel = playerViewModel,
                            sheetCollapsedTargetY = sheetCollapsedTargetY,
                            collapsedStateHorizontalPadding = horizontalPadding + 16.dp,
                            hideMiniPlayer = shouldHideMiniPlayer,
                            containerHeight = containerHeight,
                            navController = navController,
                            isNavBarHidden = isNavBarEffectivelyHidden,
                            isLiquidGlass = isLiquidGlassOnMiniPlayer,
                            backdrop = navBackdrop
                        )

                        val dismissUndoBarSlice by remember {
                            playerViewModel.playerUiState
                                .map { state ->
                                    DismissUndoBarSlice(
                                        isVisible = state.showDismissUndoBar,
                                        durationMillis = state.undoBarVisibleDuration
                                    )
                                }
                                .distinctUntilChanged()
                        }.collectAsStateWithLifecycle(initialValue = DismissUndoBarSlice())
                        val onUndoDismissPlaylist = remember(playerViewModel) {
                            { playerViewModel.undoDismissPlaylist() }
                        }
                        val onCloseDismissUndoBar = remember(playerViewModel) {
                            { playerViewModel.hideDismissUndoBar() }
                        }

                       LaunchedEffect(dismissUndoBarSlice.isVisible) {
                      if (!dismissUndoBarSlice.isVisible) {
                       playerViewModel.reportBottomChromeTop("undo_bar", null)
                       }
                       }
                        AnimatedVisibility(
                            visible = dismissUndoBarSlice.isVisible,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = innerPadding.calculateBottomPadding() + MiniPlayerBottomSpacer)
                                .padding(horizontal = horizontalPadding)
                                .onGloballyPositioned { coordinates ->                       // ← ADD THIS LINE
                                 playerViewModel.reportBottomChromeTop("undo_bar", coordinates.positionInRoot().y)  // ← AND THIS
                                }
                        ) {
                            DismissUndoBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(MiniPlayerHeight)
                                    .padding(horizontal = 14.dp),
                                onUndo = onUndoDismissPlaylist,
                                onClose = onCloseDismissUndoBar,
                                durationMillis = dismissUndoBarSlice.durationMillis,
                                isLiquidGlass = isLiquidGlassOnDismissPrompt,
                                backdrop = navBackdrop
                            )
                        }

                        if (showPlayStoreAnnouncement) {
                            PlayStoreAnnouncementDialog(
                                announcement = playStoreAnnouncement,
                                onDismiss = { showPlayStoreAnnouncement = false },
                                onOpenPlayStore = { url ->
                                    showPlayStoreAnnouncement = false
                                    openExternalUrl(url)
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
                                onPlayMusic = { recognizedResult ->
                                    showRecognitionDialog = false
                                    scope.launch {
                                        val songToPlay = withContext(Dispatchers.IO) {
                                            val query = "${recognizedResult.title} ${recognizedResult.artist}"
                                            val searchResult = YouTube.search(
                                                query,
                                                YouTube.SearchFilter.FILTER_SONG
                                            ).getOrNull()

                                            val topResult = searchResult?.items
                                                ?.firstOrNull { it is SongItem } as? SongItem

                                            val nativeSong = topResult?.toNativeSong()
                                            nativeSong?.copy(
                                                albumArtUriString = recognizedResult.coverArtHqUrl
                                                    ?: recognizedResult.coverArtUrl
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
                    }
                }
            }
        }

        Trace.endSection()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun LoadingOverlay(syncProgress: SyncProgress) {
        // Animate progress smoothly instead of jumping in steps
        val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = syncProgress.progress,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ),
            label = "SyncProgressAnimation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                CircularWavyProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preparing your library...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (syncProgress.hasProgress) {
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scanned ${syncProgress.currentCount} of ${syncProgress.totalCount} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }


    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        LogUtils.d(this, "onStart")
        playerViewModel.onMainActivityStart()

        if (intent.getBooleanExtra("is_benchmark", false)) {
            // Benchmark mode no longer loads dummy data - uses real library data instead
        }

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        LogUtils.d(this, "onStop")
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

private class DynamicSmoothCornerShape(
    private val topRadiusProvider: () -> androidx.compose.ui.unit.Dp,
    private val bottomRadiusProvider: () -> androidx.compose.ui.unit.Dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val topRadius = topRadiusProvider()
        val bottomRadius = bottomRadiusProvider()
        val delegate = AbsoluteSmoothCornerShape(
            cornerRadiusTL = topRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTR = topRadius,
            smoothnessAsPercentTR = 60,
            cornerRadiusBL = bottomRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBR = bottomRadius,
            smoothnessAsPercentBR = 60
        )
        return delegate.createOutline(size, layoutDirection, density)
    }
}
