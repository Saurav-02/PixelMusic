package com.saurav.pixelmusic.presentation.navigation

import DelimiterConfigScreen
import com.saurav.pixelmusic.presentation.screens.WordDelimiterConfigScreen
import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.saurav.pixelmusic.R
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.saurav.pixelmusic.data.preferences.LaunchTab
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.presentation.screens.AlbumDetailScreen
import com.saurav.pixelmusic.presentation.screens.AccountsScreen
import com.saurav.pixelmusic.presentation.screens.ArtistDetailScreen
import com.saurav.pixelmusic.presentation.screens.ArtistSettingsScreen
import com.saurav.pixelmusic.presentation.screens.DailyMixScreen
import com.saurav.pixelmusic.presentation.screens.EditTransitionScreen
import com.saurav.pixelmusic.presentation.screens.ChartsScreen
import com.saurav.pixelmusic.presentation.screens.EasterEggScreen
import com.saurav.pixelmusic.presentation.screens.MoodDetailScreen
import com.saurav.pixelmusic.presentation.screens.MoodsAndGenresScreen
import com.saurav.pixelmusic.presentation.screens.NewReleasesScreen
import com.saurav.pixelmusic.presentation.screens.PodcastsScreen
import com.saurav.pixelmusic.presentation.screens.ExperimentalSettingsScreen
import com.saurav.pixelmusic.presentation.screens.MusicLiveWallpaperScreen
import com.saurav.pixelmusic.presentation.screens.HomeScreen
import com.saurav.pixelmusic.presentation.screens.ExploreScreen
import com.saurav.pixelmusic.presentation.screens.LibraryScreen
import com.saurav.pixelmusic.presentation.screens.MashupScreen
import com.saurav.pixelmusic.presentation.screens.NavBarCornerRadiusScreen
import com.saurav.pixelmusic.presentation.screens.PaletteStyleSettingsScreen
import com.saurav.pixelmusic.presentation.screens.PlaylistDetailScreen
import com.saurav.pixelmusic.presentation.screens.RecentlyPlayedScreen
import com.saurav.pixelmusic.presentation.screens.QuickPicksAllScreen
import com.saurav.pixelmusic.presentation.screens.SmartMixScreen

import com.saurav.pixelmusic.presentation.screens.AboutScreen
import com.saurav.pixelmusic.presentation.screens.ArtistAlbumsAllScreen
import com.saurav.pixelmusic.presentation.screens.ArtistSongsAllScreen
import com.saurav.pixelmusic.presentation.screens.SearchScreen
import com.saurav.pixelmusic.presentation.screens.SettingsSearchScreen
import com.saurav.pixelmusic.presentation.screens.StatsScreen
import com.saurav.pixelmusic.presentation.screens.SettingsScreen
import com.saurav.pixelmusic.presentation.screens.PixelJamSettingsScreen
import com.saurav.pixelmusic.presentation.screens.ProfileScreen
import com.saurav.pixelmusic.presentation.screens.SettingsCategoryScreen
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import kotlinx.coroutines.flow.first
import com.saurav.pixelmusic.presentation.components.AiPlaylistGeneratingFullScreenPage
import com.saurav.pixelmusic.presentation.components.ScreenWrapper

@OptIn(UnstableApi::class)
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigation(
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues,
    userPreferencesRepository: UserPreferencesRepository,
    onSearchBarActiveChange: (Boolean) -> Unit,
    onOpenSidebar: () -> Unit
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = userPreferencesRepository.launchTabFlow
            .first()
            .toRoute()
    }

    val isGeneratingAiPlaylist by playerViewModel.isGeneratingAiPlaylist.collectAsStateWithLifecycle()
    val aiSuccess by playerViewModel.aiSuccess.collectAsStateWithLifecycle()
    val aiStatus by playerViewModel.aiStatus.collectAsStateWithLifecycle()
    val aiError by playerViewModel.aiError.collectAsStateWithLifecycle()
    val lastCreatedAiPlaylistId by playerViewModel.lastCreatedAiPlaylistId.collectAsStateWithLifecycle()
    val showAiGeneratingOverlay by playerViewModel.showAiGeneratingOverlay.collectAsStateWithLifecycle()

    LaunchedEffect(lastCreatedAiPlaylistId, aiSuccess) {
        val playlistId = lastCreatedAiPlaylistId
        if (playlistId != null && aiSuccess && showAiGeneratingOverlay) {
            // Immediately navigate to the created playlist in the background at t = 0ms
            // while the 3-into-1 morph and 1-component zoom-in animation plays on top!
            navController.navigateSafely(Screen.PlaylistDetail.createRoute(playlistId))
        }
    }

    startDestination?.let { initialRoute ->
        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = initialRoute
        ) {
            composable(
                Screen.Home.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    if (playerViewModel.aiSuccess.value) {
                        ExitTransition.None
                    } else {
                        mainRootExitTransition(
                            fromRoute = initialState.destination.route,
                            toRoute = targetState.destination.route,
                            fallback = exitTransition()
                        )
                    }
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    HomeScreen(
                        navController = navController,
                        paddingValuesParent = paddingValues,
                        playerViewModel = playerViewModel,
                        onOpenSidebar = onOpenSidebar
                    )
                }
            }
            composable(
                Screen.Explore.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ExploreScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        paddingValuesParent = paddingValues
                    )
                }
            }
            composable(
                Screen.Search.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    SearchScreen(
                        paddingValues = paddingValues,
                        playerViewModel = playerViewModel,
                        navController = navController,
                        onSearchBarActiveChange = onSearchBarActiveChange
                    )
                }
            }
            composable(
                Screen.Library.route,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    if (playerViewModel.aiSuccess.value) {
                        ExitTransition.None
                    } else {
                        mainRootExitTransition(
                            fromRoute = initialState.destination.route,
                            toRoute = targetState.destination.route,
                            fallback = exitTransition()
                        )
                    }
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    LibraryScreen(navController = navController, playerViewModel = playerViewModel)
                }
            }
            composable(
                Screen.Settings.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    SettingsScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onNavigationIconClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(
                route = Screen.Equalizer.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightTitle = backStackEntry.arguments?.getString("highlight")?.let { android.net.Uri.decode(it) }
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.saurav.pixelmusic.presentation.screens.LocalSettingsHighlightTitle provides highlightTitle
                    ) {
                        com.saurav.pixelmusic.presentation.screens.audiolab.AudioLabScreen(
                            navController = navController,
                        )
                    }
                }
            }
            composable(
                route = Screen.AudioLabEq.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightTitle = backStackEntry.arguments?.getString("highlight")?.let { android.net.Uri.decode(it) }
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.saurav.pixelmusic.presentation.screens.LocalSettingsHighlightTitle provides highlightTitle
                    ) {
                        com.saurav.pixelmusic.presentation.screens.audiolab.equalizer.AudioLabEqualizerScreen(
                            navController = navController,
                        )
                    }
                }
            }
            composable(
                route = Screen.AudioLabFx.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightTitle = backStackEntry.arguments?.getString("highlight")?.let { android.net.Uri.decode(it) }
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.saurav.pixelmusic.presentation.screens.LocalSettingsHighlightTitle provides highlightTitle
                    ) {
                        com.saurav.pixelmusic.presentation.screens.audiolab.AudioFxScreen(
                            navController = navController,
                        )
                    }
                }
            }
            composable(
                route = Screen.AudioLabStage.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val highlightTitle = backStackEntry.arguments?.getString("highlight")?.let { android.net.Uri.decode(it) }
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.saurav.pixelmusic.presentation.screens.LocalSettingsHighlightTitle provides highlightTitle
                    ) {
                        com.saurav.pixelmusic.presentation.screens.audiolab.stage.StageScreen(
                            navController = navController,
                        )
                    }
                }
            }
            composable(
                Screen.PixelJamSettings.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    PixelJamSettingsScreen(navController = navController)
                }
            }
            composable(
                Screen.Profile.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ProfileScreen(navController = navController)
                }
            }
            composable(
                Screen.SettingsSearch.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    SettingsSearchScreen(
                        navController = navController,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Accounts.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    AccountsScreen(
                        onBackClick = { navController.popBackStack() },
                        onOpenYoutubeAuth = {
                            navController.navigateSafely(Screen.YoutubeAuth.createRoute())
                        },
                        onOpenLastfmSettings = {
                            navController.navigateSafely(Screen.SettingsCategory.createRoute("lastfm"))
                        }
                    )
                }
            }
            composable(
                route = Screen.SettingsCategory.route,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    val categoryId = backStackEntry.arguments?.getString("categoryId")
                    val highlight = backStackEntry.arguments?.getString("highlight")
                    if (categoryId != null) {
                        SettingsCategoryScreen(
                            categoryId = categoryId,
                            navController = navController,
                            playerViewModel = playerViewModel,
                            highlightTitle = highlight,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
            composable(
                Screen.PaletteStyle.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    PaletteStyleSettingsScreen(
                        playerViewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Experimental.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ExperimentalSettingsScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onNavigationIconClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.MusicLiveWallpaper.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    MusicLiveWallpaperScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onNavigationIconClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.DailyMixScreen.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    DailyMixScreen(
                        playerViewModel = playerViewModel,
                        navController = navController
                    )
                }
            }
            composable(
                Screen.SmartMix.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    SmartMixScreen(
                        navController = navController,
                        playerViewModel = playerViewModel
                    )
                }
            }
            composable(
                Screen.RecentlyPlayed.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    RecentlyPlayedScreen(
                        playerViewModel = playerViewModel,
                        navController = navController
                    )
                }
            }
            composable(
                Screen.QuickPicksAll.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    QuickPicksAllScreen(
                        playerViewModel = playerViewModel,
                        navController = navController
                    )
                }
            }
            composable(
                Screen.Stats.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    StatsScreen(
                        navController = navController
                    )
                }
            }
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                enterTransition = {
                    if (playerViewModel.aiSuccess.value) EnterTransition.None else enterTransition()
                },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")
                val playlistViewModel: PlaylistViewModel = hiltViewModel()
                if (playlistId != null) {
                    ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            playerViewModel = playerViewModel,
                            playlistViewModel = playlistViewModel,
                            onBackClick = { navController.popBackStack() },
                            onDeletePlayListClick = { navController.popBackStack() },
                            navController = navController
                        )
                    }
                }
            }

            composable(
                Screen.DJSpace.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    MashupScreen()
                }
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")
                if (albumId != null) {
                    ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                        AlbumDetailScreen(
                            albumId = albumId,
                            navController = navController,
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId")
                if (artistId != null) {
                    ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                        ArtistDetailScreen(
                            artistId = artistId,
                            navController = navController,
                            playerViewModel = playerViewModel
                        )
                    }
                }
            }
            composable(
                route = Screen.ArtistAlbumsAll.route,
                arguments = listOf(
                    navArgument("artistId") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                val albumType = backStackEntry.arguments?.getString("type") ?: "albums"
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ArtistAlbumsAllScreen(
                        artistId = artistId,
                        type = albumType,
                        navController = navController
                    )
                }
            }
            composable(
                route = Screen.ArtistSongsAll.route,
                arguments = listOf(
                    navArgument("artistId") { type = NavType.StringType }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ArtistSongsAllScreen(
                        artistId = artistId,
                        navController = navController,
                        playerViewModel = playerViewModel
                    )
                }
            }
            composable(
                "nav_bar_corner_radius",
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    NavBarCornerRadiusScreen(navController)
                }
            }
            composable(
                route = Screen.EditTransition.route,
                arguments = listOf(navArgument("playlistId") {
                    type = NavType.StringType
                    nullable = true
                }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    EditTransitionScreen(navController = navController)
                }
            }
            composable(
                Screen.About.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    AboutScreen(
                        navController = navController,
                        viewModel = playerViewModel,
                        onNavigationIconClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.EasterEgg.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    EasterEggScreen(
                        viewModel = playerViewModel,
                        onNavigationIconClick = { navController.popBackStack() },
                    )
                }
            }
            composable(
                Screen.NewReleases.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    NewReleasesScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Charts.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ChartsScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.MoodsAndGenres.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    MoodsAndGenresScreen(
                        navController = navController,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = Screen.MoodDetail.route,
                arguments = listOf(
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("browseId") {
                        type = NavType.StringType
                        defaultValue = "FEmusic_moods_and_genre_category"
                    },
                    navArgument("params") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                ),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                val title = runCatching { java.net.URLDecoder.decode(rawTitle, "UTF-8") }.getOrDefault(rawTitle)
                val rawBrowseId = backStackEntry.arguments?.getString("browseId") ?: "FEmusic_moods_and_genre_category"
                val browseId = runCatching { java.net.URLDecoder.decode(rawBrowseId, "UTF-8") }.getOrDefault(rawBrowseId)
                val rawParams = backStackEntry.arguments?.getString("params")
                val params = if (rawParams.isNullOrEmpty()) null else runCatching { java.net.URLDecoder.decode(rawParams, "UTF-8") }.getOrNull()

                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    MoodDetailScreen(
                        title = title,
                        browseId = browseId,
                        params = params,
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.Podcasts.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    PodcastsScreen(
                        navController = navController,
                        playerViewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
            composable(
                Screen.ArtistSettings.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    ArtistSettingsScreen(navController = navController)
                }
            }
            composable(
                Screen.DelimiterConfig.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    DelimiterConfigScreen(navController = navController)
                }
            }
            composable(
                Screen.WordDelimiterConfig.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    WordDelimiterConfigScreen(navController = navController)
                }
            }
            composable(
                Screen.DeviceCapabilities.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    com.saurav.pixelmusic.presentation.screens.DeviceCapabilitiesScreen(
                        navController = navController,
                        playerViewModel = playerViewModel
                    )
                }
            }

            composable(
                Screen.FeatureCompatibility.route,
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) {
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    com.saurav.pixelmusic.presentation.screens.FeatureCompatibilityScreen(
                        navController = navController
                    )
                }
            }

            composable(
                route = Screen.YoutubeAuth.route,
                arguments = listOf(navArgument("addAccount") {
                    type = NavType.BoolType
                    defaultValue = false
                }),
                enterTransition = { enterTransition() },
                exitTransition = { exitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() },
            ) { backStackEntry ->
                ScreenWrapper(navController = navController, playerViewModel = playerViewModel) {
                    com.saurav.pixelmusic.presentation.screens.youtube.AuthScreen(
                        onBack = { navController.popBackStack() },
                        addAccount = backStackEntry.arguments?.getBoolean("addAccount") == true
                    )
                }
            }

        }

        if (showAiGeneratingOverlay && (isGeneratingAiPlaylist || aiSuccess || !aiError.isNullOrBlank())) {
            AiPlaylistGeneratingFullScreenPage(
                isSuccess = aiSuccess,
                status = aiStatus,
                error = aiError,
                onDismiss = {
                    playerViewModel.dismissAiGeneratingOverlay()
                }
            )
        }
        }
    }
}

private fun String.toRoute(): String = when (this) {
    LaunchTab.EXPLORE -> Screen.Explore.route
    LaunchTab.SEARCH -> Screen.Search.route
    LaunchTab.LIBRARY -> Screen.Library.route
    else -> Screen.Home.route
}

private enum class MainRootDirection {
    FORWARD,
    BACKWARD
}

private const val BOTTOM_NAV_ENTER_DURATION = 280
private const val BOTTOM_NAV_EXIT_DURATION = 180

private val BottomNavEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val BottomNavExitEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

private val MAIN_ROOT_ENTER_SLIDE_SPEC =
    tween<IntOffset>(durationMillis = BOTTOM_NAV_ENTER_DURATION, easing = BottomNavEnterEasing)

private val MAIN_ROOT_EXIT_SLIDE_SPEC =
    tween<IntOffset>(durationMillis = BOTTOM_NAV_EXIT_DURATION, easing = BottomNavExitEasing)

private val MAIN_ROOT_ENTER_FADE_SPEC =
    tween<Float>(durationMillis = 220, delayMillis = 35, easing = BottomNavEnterEasing)

private val MAIN_ROOT_EXIT_FADE_SPEC =
    tween<Float>(durationMillis = 150, easing = BottomNavExitEasing)

private val MAIN_ROOT_ENTER_SCALE_SPEC =
    tween<Float>(durationMillis = BOTTOM_NAV_ENTER_DURATION, easing = BottomNavEnterEasing)

private val MAIN_ROOT_EXIT_SCALE_SPEC =
    tween<Float>(durationMillis = BOTTOM_NAV_EXIT_DURATION, easing = BottomNavExitEasing)

private fun mainRootDirection(
    fromRoute: String?,
    toRoute: String?
): MainRootDirection? {
    val fromIndex = mainRootRouteIndex(fromRoute) ?: return null
    val toIndex = mainRootRouteIndex(toRoute) ?: return null
    if (fromIndex == toIndex) return null
    return if (toIndex > fromIndex) MainRootDirection.FORWARD else MainRootDirection.BACKWARD
}

private fun mainRootEnterTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: EnterTransition
): EnterTransition = when (mainRootDirection(fromRoute, toRoute)) {
    MainRootDirection.FORWARD -> {
        slideInHorizontally(
            animationSpec = MAIN_ROOT_ENTER_SLIDE_SPEC,
            initialOffsetX = { (it * 0.06f).toInt() }
        ) + scaleIn(
            animationSpec = MAIN_ROOT_ENTER_SCALE_SPEC,
            initialScale = 0.965f,
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(animationSpec = MAIN_ROOT_ENTER_FADE_SPEC)
    }
    MainRootDirection.BACKWARD -> {
        slideInHorizontally(
            animationSpec = MAIN_ROOT_ENTER_SLIDE_SPEC,
            initialOffsetX = { -(it * 0.06f).toInt() }
        ) + scaleIn(
            animationSpec = MAIN_ROOT_ENTER_SCALE_SPEC,
            initialScale = 0.965f,
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(animationSpec = MAIN_ROOT_ENTER_FADE_SPEC)
    }
    null -> fallback
}

private fun mainRootExitTransition(
    fromRoute: String?,
    toRoute: String?,
    fallback: ExitTransition
): ExitTransition = when (mainRootDirection(fromRoute, toRoute)) {
    MainRootDirection.FORWARD -> {
        slideOutHorizontally(
            animationSpec = MAIN_ROOT_EXIT_SLIDE_SPEC,
            targetOffsetX = { -(it * 0.06f).toInt() }
        ) + scaleOut(
            animationSpec = MAIN_ROOT_EXIT_SCALE_SPEC,
            targetScale = 0.965f,
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(animationSpec = MAIN_ROOT_EXIT_FADE_SPEC)
    }
    MainRootDirection.BACKWARD -> {
        slideOutHorizontally(
            animationSpec = MAIN_ROOT_EXIT_SLIDE_SPEC,
            targetOffsetX = { (it * 0.06f).toInt() }
        ) + scaleOut(
            animationSpec = MAIN_ROOT_EXIT_SCALE_SPEC,
            targetScale = 0.965f,
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(animationSpec = MAIN_ROOT_EXIT_FADE_SPEC)
    }
    null -> fallback
}
