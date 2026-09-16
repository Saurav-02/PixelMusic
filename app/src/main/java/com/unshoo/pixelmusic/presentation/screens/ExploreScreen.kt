@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.unshoo.pixelmusic.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.unshoo.pixelmusic.presentation.components.QuickPicksSection
import com.unshoo.pixelmusic.presentation.viewmodel.QuickPicksViewModel
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.remote.youtube.toNativeSong
import com.unshoo.pixelmusic.presentation.components.MiniPlayerHeight
import com.unshoo.pixelmusic.presentation.components.MusicRecognitionOverlay
import com.unshoo.pixelmusic.presentation.components.SmartImage
import com.unshoo.pixelmusic.presentation.components.subcomps.EnhancedSongListItem
import com.unshoo.pixelmusic.presentation.navigation.Screen
import com.unshoo.pixelmusic.presentation.navigation.navigateSafely
import com.unshoo.pixelmusic.presentation.navigation.navigateSafelyReplacing
import com.unshoo.pixelmusic.presentation.viewmodel.ExploreUiState
import com.unshoo.pixelmusic.presentation.viewmodel.ExploreViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.unshoo.pixelmusic.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.unshoo.pixelmusic.data.model.Playlist
import com.unshoo.pixelmusic.presentation.components.PlaylistCover
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import unshoo.ianshulyadav.pixelmusic.innertube.models.AlbumItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.ArtistItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.PlaylistItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.YTItem
import unshoo.ianshulyadav.pixelmusic.innertube.pages.HomePage
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.unshoo.pixelmusic.ui.modifiers.scrollMotionBlur
import androidx.compose.material3.TextButton
import com.unshoo.pixelmusic.presentation.components.HomeShuffleFab
import com.unshoo.pixelmusic.data.preferences.AppBackgroundStyle


private val MOOD_CHIPS = listOf(
    "Podcasts", "Feel good", "Relax", "Romance", "Energy",
    "Commute", "Workout", "Party", "Sad", "Focus", "Sleep"
)

data class DynamicAnimState(
    val modifier: Modifier,
    val cornerRadius: Dp,
    val factor: Float
)

@Composable
fun rememberDynamicEffect(
    baseCornerRadius: Dp = 20.dp,
    squishCornerRadius: Dp = 54.dp,
    minScale: Float = 0.92f,
    minAlpha: Float = 0.65f
): DynamicAnimState {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var visibilityFactor by remember { mutableFloatStateOf(1f) }

    val animModifier = Modifier
        .onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val cardCenterX = bounds.center.x
            val cardCenterY = bounds.center.y

            val hMargin = screenWidthPx * 0.18f
            val hFactor = when {
                cardCenterX < hMargin -> (cardCenterX / hMargin).coerceIn(0f, 1f)
                cardCenterX > (screenWidthPx - hMargin) -> ((screenWidthPx - cardCenterX) / hMargin).coerceIn(0f, 1f)
                else -> 1f
            }

            val vMargin = screenHeightPx * 0.15f
            val vFactor = when {
                cardCenterY < vMargin -> (cardCenterY / vMargin).coerceIn(0f, 1f)
                cardCenterY > (screenHeightPx - vMargin) -> ((screenHeightPx - cardCenterY) / vMargin).coerceIn(0f, 1f)
                else -> 1f
            }

            visibilityFactor = (hFactor * vFactor).coerceIn(0f, 1f)
        }
        .graphicsLayer {
            val scale = minScale + ((1f - minScale) * visibilityFactor)
            scaleX = scale
            scaleY = scale
            alpha = minAlpha + ((1f - minAlpha) * visibilityFactor)
        }

    val dynamicCorner = lerp(baseCornerRadius, squishCornerRadius, 1f - visibilityFactor)
    return DynamicAnimState(animModifier, dynamicCorner, visibilityFactor)
}

@UnstableApi
@Composable
fun ExploreScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    paddingValuesParent: PaddingValues,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    quickPicksViewModel: QuickPicksViewModel = hiltViewModel()
) {
    val uiState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val quickPicks by quickPicksViewModel.quickPicks.collectAsStateWithLifecycle()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val isMotionBlurEnabled by playerViewModel.userPreferencesRepository.uiMotionBlurEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val isPlaying = stablePlayerState.isPlaying
    val currentSongId = stablePlayerState.currentSong?.id
    val quickPicksDisplayMode by playerViewModel.quickPicksDisplayMode.collectAsStateWithLifecycle()
    val backgroundStyle by playerViewModel.userPreferencesRepository.appBackgroundStyleFlow.collectAsStateWithLifecycle(initialValue = AppBackgroundStyle.DEFAULT)
    val isCustomBackground = backgroundStyle != AppBackgroundStyle.DEFAULT
    val pullRefreshState = rememberPullToRefreshState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()

    var showRecognitionDialog by remember { mutableStateOf(false) }

    var isExploreFabActive by remember { mutableStateOf(false) }
    LaunchedEffect(currentSongId) {
        if (currentSongId != null) {
            delay(30)
            isExploreFabActive = true
        } else {
            isExploreFabActive = false
        }
    }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val exploreTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "exploreTitleAlpha"
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 5
        }.collect { nearEnd ->
            if (nearEnd && uiState.selectedMood == null && uiState.selectedFilter != "Charts") {
                exploreViewModel.loadMore()
            }
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isCustomBackground) Color.Transparent else MaterialTheme.colorScheme.background
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = {
                    exploreViewModel.loadData(forceRefresh = true)
                    quickPicksViewModel.refresh()
                },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCustomBackground) Modifier else Modifier.background(backgroundBrush))
                ) {
                    if (uiState.isLoading && uiState.homePageSections.isEmpty() && uiState.newReleaseAlbums.isEmpty() && uiState.chartsPage == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (uiState.error != null && uiState.homePageSections.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = { exploreViewModel.loadData() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    } else {
                        val homeSectionsFiltered = remember(uiState.homePageSections) {
                            uiState.homePageSections.filter {
                                !it.title.contains("quick picks", ignoreCase = true) &&
                                    !it.title.contains("quick", ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .scrollMotionBlur(listState, enabled = isMotionBlurEnabled),
                            contentPadding = PaddingValues(
                                top = statusBarHeight + innerPadding.calculateTopPadding() + 76.dp,
                                bottom = paddingValuesParent.calculateBottomPadding() + 160.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item(key = "explore_filters") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val categories = listOf("All", "For You", "New Releases", "Charts")
                                        categories.forEach { category ->
                                            FilterChip(
                                                selected = uiState.selectedFilter == category,
                                                onClick = { exploreViewModel.setSelectedFilter(category) },
                                                label = { Text(category) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    labelColor = MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = RoundedCornerShape(16.dp),
                                                border = null
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MOOD_CHIPS.forEach { mood ->
                                            val isSelected = uiState.selectedMood == mood
                                            Surface(
                                                onClick = { exploreViewModel.setSelectedMood(mood) },
                                                shape = RoundedCornerShape(50),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Text(
                                                    text = mood,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(
                                                        horizontal = 18.dp,
                                                        vertical = 10.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── CHARTS ─────────────────────────────────────────────────
                            if (uiState.selectedFilter == "Charts") {
                                val chartsPage = uiState.chartsPage
                                if (chartsPage != null && chartsPage.sections.isNotEmpty()) {
                                    chartsPage.sections.forEachIndexed { index, chartSection ->
                                        item(key = "chart_${chartSection.title}_${index}_header") {
                                            SectionHeader(title = chartSection.title)
                                        }

                                        val songItems = chartSection.items.filterIsInstance<SongItem>()
                                        if (songItems.isNotEmpty()) {
                                            val songListNative = songItems.map { it.toNativeSong() }
                                            items(songItems.size) { idx ->
                                                val songNative = songListNative[idx]
                                                EnhancedSongListItem(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    song = songNative,
                                                    isPlaying = isPlaying && currentSongId == songNative.id,
                                                    isCurrentSong = currentSongId == songNative.id,
                                                    onClick = {
                                                        playerViewModel.showAndPlaySong(
                                                            songNative,
                                                            songListNative,
                                                            chartSection.title
                                                        )
                                                    },
                                                    onMoreOptionsClick = {
                                                        playerViewModel.selectSongForInfo(songNative)
                                                    }
                                                )
                                            }
                                        } else {
                                            item(key = "chart_${chartSection.title}_${index}_list") {
                                                LazyRow(
                                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    items(chartSection.items) { item ->
                                                        when (item) {
                                                            is AlbumItem -> AlbumCarouselItem(album = item, onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId)) })
                                                            is ArtistItem -> ArtistCardItem(artist = item, onClick = { navController.navigateSafely(Screen.ArtistDetail.createRoute(item.id)) })
                                                            is PlaylistItem -> PlaylistCardItem(playlist = item, onClick = { navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id)) })
                                                            else -> {}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (uiState.isChartsLoading) {
                                    item(key = "charts_loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    item(key = "charts_empty") {
                                        ExploreEmptyState(
                                            message = "Charts unavailable",
                                            subMessage = "Tap Retry to fetch the latest charts."
                                        )
                                    }
                                    item(key = "charts_retry") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(
                                                onClick = { exploreViewModel.retryCharts() },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                            }

                            // ── MOOD VIEW ───────────────────────────────────────────────
                            if (uiState.selectedMood != null && uiState.selectedFilter != "Charts") {
                                if (uiState.isMoodLoading) {
                                    item(key = "mood_loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else if (uiState.moodSections.isEmpty()) {
                                    item(key = "mood_empty") {
                                        ExploreEmptyState(
                                            message = "No content for \"${uiState.selectedMood}\"",
                                            subMessage = "Try a different mood."
                                        )
                                    }
                                } else {
                                    uiState.moodSections.forEachIndexed { index, section ->
                                        val sectionKey = "mood_section_${index}_${section.title.hashCode()}"
                                        item(key = "${sectionKey}_header") {
                                            SectionHeader(title = section.title)
                                        }
                                        item(key = sectionKey) {
                                            val songItems = remember(section.items) { section.items.filterIsInstance<SongItem>() }
                                            val isAllSongs = songItems.size == section.items.size && songItems.isNotEmpty()
                                            when {
                                                isAllSongs -> {
                                                    SongPillsCarousel(
                                                        songs = songItems,
                                                        playerViewModel = playerViewModel,
                                                        sectionTitle = section.title
                                                    )
                                                }
                                                else -> {
                                                    YTItemCarousel(
                                                        items = section.items,
                                                        navController = navController,
                                                        playerViewModel = playerViewModel,
                                                        sectionTitle = section.title
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── DEFAULT VIEW ────────────────────────────────────────────
                            if (uiState.selectedMood == null && uiState.selectedFilter != "Charts") {
                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "New Releases") &&
                                    uiState.newReleaseAlbums.isNotEmpty()
                                ) {
                                    item(key = "new_releases_header") {
                                        SectionHeader(title = "New Releases")
                                    }
                                    item(key = "new_releases_carousel") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(uiState.newReleaseAlbums) { album ->
                                                AlbumCarouselItem(
                                                    album = album,
                                                    onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId)) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "For You") &&
                                    quickPicks.isNotEmpty()
                                ) {
                                    item(key = "quick_picks_section") {
                                        QuickPicksSection(
                                            songs = quickPicks,
                                            onSongClick = { song -> playerViewModel.showAndPlaySong(song, quickPicks, "Quick Picks") },
                                            onSeeAllClick = { navController.navigateSafely(Screen.QuickPicksAll.route) },
                                            currentSongId = currentSongId,
                                            displayMode = quickPicksDisplayMode
                                        )
                                    }
                                }

                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "For You") &&
                                    uiState.recentMixes.isNotEmpty()
                                ) {
                                    item(key = "recent_mixes_header") {
                                        SectionHeader(title = "Recent Mixes (last.fm)")
                                    }
                                    item(key = "recent_mixes_carousel") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(uiState.recentMixes) { playlist ->
                                                RecentMixCardItem(
                                                    playlist = playlist,
                                                    playerViewModel = playerViewModel,
                                                    onClick = { navController.navigateSafely(Screen.PlaylistDetail.createRoute(playlist.id)) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.selectedFilter == "All" || uiState.selectedFilter == "For You") {
                                    homeSectionsFiltered.forEachIndexed { index, section ->
                                        val sectionKey = "home_section_${index}_${section.title.hashCode()}"

                                        item(key = "${sectionKey}_header") {
                                            SectionHeader(title = section.title)
                                        }

                                        item(key = sectionKey) {
                                            val titleLower = section.title.lowercase()
                                            val songItems = remember(section.items) { section.items.filterIsInstance<SongItem>() }
                                            val isAllSongs = songItems.size == section.items.size && songItems.isNotEmpty()

                                            when {
                                                titleLower.startsWith("similar to") || titleLower.contains("fans also like") -> {
                                                    SimilarArtistsCarousel(
                                                        artists = section.items.filterIsInstance<ArtistItem>(),
                                                        navController = navController
                                                    )
                                                }

                                                isAllSongs && (titleLower.contains("trending") || titleLower.contains("covers") || titleLower.contains("remix") || titleLower.contains("hits")) -> {
                                                    SongPillsCarousel(
                                                        songs = songItems,
                                                        playerViewModel = playerViewModel,
                                                        sectionTitle = section.title
                                                    )
                                                }

                                                isAllSongs && (titleLower.contains("video") || titleLower.contains("long listen") || titleLower.contains("for you") || titleLower.contains("commented") || songItems.size >= 6) -> {
                                                    SongBigBoxCarousel(
                                                        songs = songItems,
                                                        playerViewModel = playerViewModel,
                                                        sectionTitle = section.title
                                                    )
                                                }

                                                titleLower.contains("mixed for you") || titleLower.contains("daily discover") -> {
                                                    MixedStationCarousel(
                                                        items = section.items,
                                                        navController = navController,
                                                        playerViewModel = playerViewModel
                                                    )
                                                }

                                                else -> {
                                                    YTItemCarousel(
                                                        items = section.items,
                                                        navController = navController,
                                                        playerViewModel = playerViewModel,
                                                        sectionTitle = section.title
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (uiState.isContinuationLoading) {
                                        item(key = "pagination_loading") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
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

        val scrimTopColor = if (isLightTheme) {
            primaryColor.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.background)
        } else {
            surfaceColor
        }

        Text(
            text = "Explore",
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 40.sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = statusBarHeight + 12.dp)
                .graphicsLayer { alpha = exploreTitleAlpha }
        )

        HomeShuffleFab(
            isShuffleEnabled = false,
            isPlayerActive = isExploreFabActive,
            baseBottomOffset = paddingValuesParent.calculateBottomPadding(), // <-- Add this line
            onClick = { navController.navigateSafely(Screen.SmartMix.route) },
            isExploreMode = true,
            onLongClick = { showRecognitionDialog = true },
            onSwipeUp = { showRecognitionDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd)
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

@Composable
private fun ExploreEmptyState(
    message: String,
    subMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Carousels — unchanged from the previous version
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SongPillsCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    val rows = remember(nativeSongs) {
        val row1 = mutableListOf<Song>()
        val row2 = mutableListOf<Song>()
        nativeSongs.forEachIndexed { i, s ->
            if (i % 2 == 0) row1.add(s) else row2.add(s)
        }
        listOf(row1, row2)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowSongs ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowSongs.forEach { song ->
                    val anim = rememberDynamicEffect(
                        baseCornerRadius = 28.dp,
                        squishCornerRadius = 14.dp,
                        minScale = 0.93f,
                        minAlpha = 0.65f
                    )
                    Surface(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        shape = RoundedCornerShape(anim.cornerRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .width(220.dp)
                            .height(56.dp)
                            .then(anim.modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmartImage(
                                model = song.albumArtUriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun SongBigBoxCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSongId = stablePlayerState.currentSong?.id
    val isPlayingNow = stablePlayerState.isPlaying

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(nativeSongs.size) { index ->
            val song = nativeSongs[index]
            val isThisSong = song.id == currentSongId
            val isThisSongPlaying = isThisSong && isPlayingNow

            val anim = rememberDynamicEffect(
                baseCornerRadius = 22.dp,
                squishCornerRadius = 54.dp,
                minScale = 0.94f,
                minAlpha = 0.7f
            )

            SongBigBoxItem(
                song = song,
                isThisSongPlaying = isThisSongPlaying,
                onClick = {
                    playerViewModel.showAndPlaySong(
                        song = song,
                        contextSongs = nativeSongs,
                        queueName = sectionTitle
                    )
                },
                onPlayPauseClick = {
                    if (isThisSong) {
                        playerViewModel.playPause()
                    } else {
                        playerViewModel.showAndPlaySong(
                            song = song,
                            contextSongs = nativeSongs,
                            queueName = sectionTitle
                        )
                    }
                },
                modifier = Modifier.then(anim.modifier)
            )
        }
    }
}

@Composable
private fun SongBigBoxItem(
    song: Song,
    isThisSongPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .width(280.dp)
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(64.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isThisSongPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isThisSongPlaying) "Pause" else "Play",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MixedStationCarousel(
    items: List<YTItem>,
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            val anim = rememberDynamicEffect(
                baseCornerRadius = 24.dp,
                squishCornerRadius = 56.dp,
                minScale = 0.92f,
                minAlpha = 0.65f
            )
            val dynamicImageCorner = lerp(16.dp, 48.dp, 1f - anim.factor)

            Card(
                onClick = {
                    when (item) {
                        is PlaylistItem -> navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id))
                        is AlbumItem -> navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId))
                        is SongItem -> playerViewModel.showAndPlaySong(item.toNativeSong())
                        else -> {}
                    }
                },
                shape = RoundedCornerShape(anim.cornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .width(190.dp)
                    .wrapContentHeight()
                    .then(anim.modifier)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(modifier = Modifier.size(170.dp)) {
                        SmartImage(
                            model = when (item) {
                                is PlaylistItem -> item.thumbnail
                                is AlbumItem -> item.thumbnail
                                is SongItem -> item.thumbnail
                                else -> null
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(dynamicImageCorner)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (item) {
                            is PlaylistItem -> item.title
                            is AlbumItem -> item.title
                            is SongItem -> item.title
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (item) {
                            is PlaylistItem -> item.author?.name ?: "Mix Station"
                            is AlbumItem -> item.artists?.firstOrNull()?.name ?: "Album"
                            is SongItem -> item.artists?.firstOrNull()?.name ?: "Song"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun YTItemCarousel(
    items: List<YTItem>,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    sectionTitle: String
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            when (item) {
                is SongItem -> {
                    val songNative = item.toNativeSong()
                    SongCardItem(
                        song = songNative,
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = songNative,
                                contextSongs = items.filterIsInstance<SongItem>().map { it.toNativeSong() },
                                queueName = sectionTitle
                            )
                        }
                    )
                }
                is AlbumItem -> {
                    AlbumCarouselItem(
                        album = item,
                        onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId)) }
                    )
                }
                is PlaylistItem -> {
                    PlaylistCardItem(
                        playlist = item,
                        onClick = { navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id)) }
                    )
                }
                is ArtistItem -> {
                    ArtistCardItem(
                        artist = item,
                        onClick = { navController.navigateSafely(Screen.ArtistDetail.createRoute(item.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongCardItem(
    song: Song,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(baseCornerRadius = 20.dp, squishCornerRadius = 50.dp)

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier)
    ) {
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = song.title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(anim.cornerRadius)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun AnimatedSparklesIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_rotation"
    )

    val colors = MaterialTheme.colorScheme
    val gradientBrush = remember(colors) {
        Brush.linearGradient(
            colors = listOf(colors.primary, colors.tertiary)
        )
    }

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .clip(CircleShape)
                .background(gradientBrush)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Smart Mix",
                modifier = Modifier.size(20.dp),
                tint = colors.onPrimary
            )
        }
    }
}

@Composable
fun RecentMixCardItem(
    playlist: Playlist,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(baseCornerRadius = 20.dp, squishCornerRadius = 50.dp)
    val previewSongIds = remember(playlist.songIds) { playlist.songIds.take(4) }
    var playlistSongs by remember(previewSongIds) {
        mutableStateOf<List<Song>?>(if (previewSongIds.isEmpty()) emptyList() else null)
    }
    LaunchedEffect(previewSongIds) {
        if (previewSongIds.isNotEmpty()) {
            playlistSongs = playerViewModel.getSongs(previewSongIds)
        }
    }

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier)
    ) {
        PlaylistCover(
            playlist = playlist,
            playlistSongs = playlistSongs ?: emptyList(),
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(anim.cornerRadius)),
            size = 140.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Smart Mix",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ExploreTopBar(
    onSettingsClick: () -> Unit,
    onCreateClick: () -> Unit,
    isScrolled: Boolean = false
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0.4f,
        animationSpec = tween(durationMillis = 300),
        label = "topbar_alpha_transition"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = animatedAlpha))
            .statusBarsPadding()
            .padding(start = 24.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Explore",
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 40.sp,
            letterSpacing = 1.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedSparklesIconButton(onClick = onCreateClick)

            FilledIconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.settings_top_bar_title),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = GoogleSansRounded
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (onActionClick != null && actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun AlbumCarouselItem(
    album: AlbumItem,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(baseCornerRadius = 24.dp, squishCornerRadius = 54.dp)

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier)
    ) {
        SmartImage(
            model = album.thumbnail,
            contentDescription = album.title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(anim.cornerRadius)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = album.artists?.joinToString { it.name } ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ArtistCardItem(
    artist: ArtistItem,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(minScale = 0.91f, minAlpha = 0.65f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier)
    ) {
        SmartImage(
            model = artist.thumbnail,
            contentDescription = artist.title,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansRounded
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Artist",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun PlaylistCardItem(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(baseCornerRadius = 18.dp, squishCornerRadius = 48.dp)

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier)
    ) {
        SmartImage(
            model = playlist.thumbnail,
            contentDescription = playlist.title,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(anim.cornerRadius)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = playlist.author?.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun SimilarArtistsCarousel(
    artists: List<ArtistItem>,
    navController: NavController
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(artists) { artist ->
            SimilarArtistCardItem(
                artist = artist,
                onClick = { navController.navigateSafely(Screen.ArtistDetail.createRoute(artist.id)) }
            )
        }
    }
}

@Composable
fun SimilarArtistCardItem(
    artist: ArtistItem,
    onClick: () -> Unit
) {
    val anim = rememberDynamicEffect(baseCornerRadius = 22.dp, squishCornerRadius = 54.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(anim.modifier),
        shape = RoundedCornerShape(anim.cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        color = primaryColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    model = artist.thumbnail,
                    contentDescription = artist.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = artist.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansRounded
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Similar Artist",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = primaryColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
