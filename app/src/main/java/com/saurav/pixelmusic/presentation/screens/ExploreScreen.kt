@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.AppBackgroundStyle
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality
import com.saurav.pixelmusic.presentation.components.PlaylistCover
import com.saurav.pixelmusic.presentation.components.QuickPicksSection
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.PlaylistBottomSheet
import com.saurav.pixelmusic.presentation.components.SongInfoBottomSheet
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.ui.graphics.compositeOver
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.saurav.pixelmusic.presentation.components.LiquidGlassHeaderBar
import com.saurav.pixelmusic.utils.AndroidVersionCompat
import com.saurav.pixelmusic.presentation.components.subcomps.PlayingEqIcon
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.viewmodel.ExploreViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.QuickPicksViewModel
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import com.saurav.pixelmusic.presentation.components.liquidGlassInteractiveSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val MOOD_CHIPS = listOf(
    "Podcasts", "Feel good", "Relax", "Romance", "Energy",
    "Commute", "Workout", "Party", "Sad", "Focus", "Sleep"
)

private val EXPLORE_CHART_REGIONS = listOf(
    "India" to "IN",
    "Global" to null,
    "US" to "US",
    "UK" to "GB",
    "Korea" to "KR",
    "Japan" to "JP"
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
    return remember(baseCornerRadius) {
        DynamicAnimState(
            modifier = Modifier,
            cornerRadius = baseCornerRadius,
            factor = 1f
        )
    }
}

private class ExpressiveHarmonicShape(
    private val lobes1: Int = 8,
    private val amp1: Float = 0.065f,
    private val lobes2: Int = 4,
    private val amp2: Float = 0.02f,
    private val rotationDeg: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val baseR = min(size.width, size.height) * 0.5f / (1f + amp1 + amp2)
        val rotRad = rotationDeg * (PI.toFloat() / 180f)
        val steps = 120
        val twoPi = (2.0 * PI).toFloat()
        for (i in 0..steps) {
            val theta = (i.toFloat() / steps.toFloat()) * twoPi
            val factor = 1f + amp1 * cos(lobes1 * theta) + amp2 * cos(lobes2 * theta)
            val r = baseR * factor
            val ang = theta + rotRad
            val x = cx + r * cos(ang)
            val y = cy + r * sin(ang)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun ExploreScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    paddingValuesParent: PaddingValues,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    quickPicksViewModel: QuickPicksViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by exploreViewModel.uiState.collectAsStateWithLifecycle()
    val quickPicks by quickPicksViewModel.quickPicks.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val interestedSongIds by playerViewModel.interestedSongIds.collectAsStateWithLifecycle()
    val notInterestedSongIds by playerViewModel.notInterestedSongIds.collectAsStateWithLifecycle()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var playlistSheetSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val isPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isMotionBlurEnabled by playerViewModel.userPreferencesRepository.uiMotionBlurEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val quickPicksDisplayMode by playerViewModel.quickPicksDisplayMode.collectAsStateWithLifecycle()
    val backgroundStyle by playerViewModel.userPreferencesRepository.appBackgroundStyleFlow
        .collectAsStateWithLifecycle(initialValue = AppBackgroundStyle.DEFAULT)
    val isCustomBackground = backgroundStyle != AppBackgroundStyle.DEFAULT
    val pullRefreshState = rememberPullToRefreshState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    val performanceModeEnabled by playerViewModel.userPreferencesRepository.performanceModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val liquidGlassEnabled by playerViewModel.userPreferencesRepository.liquidGlassEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val liquidGlassHeaderEnabled by playerViewModel.userPreferencesRepository.liquidGlassHeaderEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val isLiquidGlassOnHeader = liquidGlassEnabled && liquidGlassHeaderEnabled && !performanceModeEnabled && AndroidVersionCompat.supportsLiquidGlass

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
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }.collect { nearEnd ->
            if (nearEnd && uiState.selectedMood == null && uiState.selectedFilter != "Charts") {
                exploreViewModel.loadMore()
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val surfaceColor = colorScheme.surface
    val primaryColor = colorScheme.primary
    val isLightTheme = colorScheme.background.luminance() > 0.5f
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

    val homeSectionsFiltered = remember(uiState.homePageSections, notInterestedSongIds, interestedSongIds) {
        uiState.homePageSections.filter {
            !it.title.contains("quick picks", ignoreCase = true) &&
                    !it.title.equals("quick", ignoreCase = true) &&
                    it.items.isNotEmpty()
        }.map { section ->
            val filtered = section.items.filter { item ->
                when (item) {
                    is SongItem -> item.id !in notInterestedSongIds
                    else -> true
                }
            }.sortedByDescending { item ->
                when (item) {
                    is SongItem -> item.id in interestedSongIds
                    else -> false
                }
            }
            section.copy(items = filtered)
        }.filter { it.items.isNotEmpty() }
    }

    val filteredQuickPicks = remember(quickPicks, notInterestedSongIds, interestedSongIds) {
        quickPicks.filter { it.id !in notInterestedSongIds }
            .sortedByDescending { it.id in interestedSongIds }
    }

    val inlineChartSongs = remember(uiState.chartsPage) {
        uiState.chartsPage?.sections
            ?.firstOrNull { it.items.any { item -> item is SongItem } }
            ?.items?.filterIsInstance<SongItem>()
            .orEmpty()
    }

    val shelvesToDisplay = remember(homeSectionsFiltered, uiState.selectedFilter) {
        if (uiState.selectedFilter == "New Releases") {
            homeSectionsFiltered.filter {
                val t = it.title.lowercase()
                t.contains("new") || t.contains("release") || t.contains("album") || t.contains("fresh") || it.items.any { item -> item is AlbumItem }
            }
        } else {
            homeSectionsFiltered
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isCustomBackground) Color.Transparent else colorScheme.background
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusBarHeight + 56.dp)
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
                            ContainedLoadingIndicator(modifier = Modifier.size(52.dp))
                        }
                    } else if (uiState.error != null && uiState.homePageSections.isEmpty() && uiState.newReleaseAlbums.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.error!!,
                                fontFamily = GoogleSansRounded,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = { exploreViewModel.loadData(forceRefresh = true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .scrollMotionBlur(listState, enabled = isMotionBlurEnabled),
                            contentPadding = PaddingValues(
                                top = statusBarHeight + 76.dp,
                                bottom = paddingValuesParent.calculateBottomPadding() + 160.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            // ── 1. CLEAN FILTER & MOOD CHIPS ───────────────────────────────
                            item(key = "explore_filters") {
                                ExploreFilterAndMoodDock(
                                    selectedFilter = uiState.selectedFilter,
                                    selectedMood = uiState.selectedMood,
                                    onFilterClick = { exploreViewModel.setSelectedFilter(it) },
                                    onMoodClick = { exploreViewModel.setSelectedMood(it) },
                                    isLiquidGlass = isLiquidGlassOnHeader
                                )
                            }

                            // ── 2. MOOD VIEW ───────────────────────────────────────────────
                            if (uiState.selectedMood != null && uiState.selectedFilter != "Charts") {
                                if (uiState.isMoodLoading) {
                                    item(key = "mood_loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ContainedLoadingIndicator(modifier = Modifier.size(44.dp))
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
                                        val sectionKey = "mood_sec_${uiState.selectedMood}_${index}_${section.title.hashCode()}"
                                        item(key = "${sectionKey}_header") {
                                            SectionHeader(title = section.title)
                                        }
                                        item(key = sectionKey) {
                                            RenderExpressiveExploreShelf(
                                                section = section,
                                                shelfIndex = index,
                                                navController = navController,
                                                playerViewModel = playerViewModel,
                                                onSongMoreOptions = { song ->
                                                    playerViewModel.selectSongForInfo(song)
                                                    showSongInfoBottomSheet = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // ── 3. CHARTS VIEW ─────────────────────────────────────────────
                            else if (uiState.selectedFilter == "Charts") {
                                item(key = "charts_region_selector") {
                                    ChartRegionSelectorRow(
                                        selectedCountryName = uiState.selectedChartCountryName,
                                        onSelectRegion = { name, code ->
                                            exploreViewModel.setChartCountry(name, code)
                                        },
                                        onOpenFullCharts = {
                                            navController.navigateSafely(Screen.Charts.route)
                                        }
                                    )
                                }

                                val chartsPage = uiState.chartsPage
                                if (uiState.isChartsLoading) {
                                    item(key = "charts_loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ContainedLoadingIndicator(modifier = Modifier.size(44.dp))
                                        }
                                    }
                                } else if (chartsPage != null && chartsPage.sections.isNotEmpty()) {
                                    chartsPage.sections.forEachIndexed { cIndex, chartSection ->
                                        val chartKey = "chart_sec_${uiState.selectedChartCountryName}_${cIndex}_${chartSection.title.hashCode()}"
                                        item(key = "${chartKey}_header") {
                                            SectionHeader(title = chartSection.title)
                                        }

                                        val songItems = chartSection.items.filterIsInstance<SongItem>()
                                        if (songItems.isNotEmpty()) {
                                            item(key = "${chartKey}_podium_and_deck") {
                                                ChartsPodiumAndRankedDeck(
                                                    songs = songItems,
                                                    sectionTitle = chartSection.title,
                                                    currentSongId = currentSongId,
                                                    isPlaying = isPlaying,
                                                    playerViewModel = playerViewModel,
                                                    onMoreOptionsClick = { song ->
                                                        playerViewModel.selectSongForInfo(song)
                                                        showSongInfoBottomSheet = true
                                                    }
                                                )
                                            }
                                        } else {
                                            item(key = "${chartKey}_carousel") {
                                                YTItemCarousel(
                                                    items = chartSection.items,
                                                    navController = navController,
                                                    playerViewModel = playerViewModel,
                                                    sectionTitle = chartSection.title
                                                )
                                            }
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
                                            FilledTonalButton(
                                                onClick = { exploreViewModel.retryCharts() },
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Retry", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── 4. MOODS & GENRES VIEW ─────────────────────────────────────
                            else if (uiState.selectedFilter == "Moods & Genres") {
                                item(key = "moods_genres_grid_section") {
                                    ExploreMoodsAndGenresSection(
                                        remoteSections = uiState.moodsAndGenresSections,
                                        navController = navController,
                                        onSelectQuickMood = { exploreViewModel.setSelectedMood(it) }
                                    )
                                }
                            }

                            // ── 5. DEFAULT VIEW ("All", "For You", "New Releases") ─────────
                            else {
                                // A. Featured Spotlight Pager
                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "New Releases") &&
                                    uiState.newReleaseAlbums.isNotEmpty()
                                ) {
                                    item(key = "explore_hero_spotlight_pager") {
                                        ExploreHeroSpotlightPager(
                                            albums = uiState.newReleaseAlbums.take(6),
                                            onAlbumClick = { album ->
                                                navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId))
                                            },
                                            onSeeAllNewReleases = {
                                                navController.navigateSafely(Screen.NewReleases.route)
                                            }
                                        )
                                    }
                                }

                                // B. Quick Picks
                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "For You") &&
                                    filteredQuickPicks.isNotEmpty()
                                ) {
                                    item(key = "quick_picks_section") {
                                        QuickPicksSection(
                                            songs = filteredQuickPicks,
                                            onSongClick = { song ->
                                                playerViewModel.showAndPlaySong(song, filteredQuickPicks, "Quick Picks")
                                            },
                                            onSeeAllClick = { navController.navigateSafely(Screen.QuickPicksAll.route) },
                                            onMoreOptionsClick = { song ->
                                                playerViewModel.selectSongForInfo(song)
                                                showSongInfoBottomSheet = true
                                            },
                                            currentSongId = currentSongId,
                                            displayMode = quickPicksDisplayMode
                                        )
                                    }
                                }

                                // C. New Releases Vinyl Sleeve Carousel
                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "New Releases") &&
                                    uiState.newReleaseAlbums.isNotEmpty()
                                ) {
                                    item(key = "new_releases_header") {
                                        SectionHeader(
                                            title = "New Releases",
                                            actionLabel = "See All",
                                            onActionClick = { navController.navigateSafely(Screen.NewReleases.route) }
                                        )
                                    }
                                    item(key = "new_releases_carousel") {
                                        VinylAlbumShowcaseCarousel(
                                            albums = if (uiState.selectedFilter == "New Releases") uiState.newReleaseAlbums else uiState.newReleaseAlbums.drop(1),
                                            onAlbumClick = { album ->
                                                navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId))
                                            }
                                        )
                                    }
                                }

                                // D. Inline Top Charts Podium & Ranked Deck
                                if (uiState.selectedFilter == "All" && inlineChartSongs.isNotEmpty()) {
                                    item(key = "inline_charts_header") {
                                        SectionHeader(
                                            title = "Top Charts • ${uiState.selectedChartCountryName}",
                                            actionLabel = "See All",
                                            onActionClick = { navController.navigateSafely(Screen.Charts.route) }
                                        )
                                    }
                                    item(key = "inline_charts_region_bar") {
                                        ChartRegionSelectorRow(
                                            selectedCountryName = uiState.selectedChartCountryName,
                                            onSelectRegion = { name, code ->
                                                exploreViewModel.setChartCountry(name, code)
                                            },
                                            onOpenFullCharts = {
                                                navController.navigateSafely(Screen.Charts.route)
                                            }
                                        )
                                    }
                                    item(key = "inline_charts_podium_and_deck") {
                                        ChartsPodiumAndRankedDeck(
                                            songs = inlineChartSongs.take(12),
                                            sectionTitle = "Top Charts (${uiState.selectedChartCountryName})",
                                            currentSongId = currentSongId,
                                            isPlaying = isPlaying,
                                            playerViewModel = playerViewModel,
                                            onMoreOptionsClick = { song ->
                                                playerViewModel.selectSongForInfo(song)
                                                showSongInfoBottomSheet = true
                                            }
                                        )
                                    }
                                }

                                // E. Music Videos Carousel
                                if ((uiState.selectedFilter == "All" || uiState.selectedFilter == "For You") &&
                                    uiState.videoSpotlightItems.isNotEmpty()
                                ) {
                                    item(key = "video_spotlight_header") {
                                        SectionHeader(title = "Music Videos & Live")
                                    }
                                    item(key = "video_spotlight_carousel") {
                                        VideoShowcaseCarousel(
                                            videos = uiState.videoSpotlightItems,
                                            playerViewModel = playerViewModel,
                                            sectionTitle = "Music Videos & Live"
                                        )
                                    }
                                }

                                // F. Recent Mixes (Last.fm)
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
                                            items(
                                                items = uiState.recentMixes,
                                                key = { "recent_mix_${it.id}" }
                                            ) { playlist ->
                                                RecentMixCardItem(
                                                    playlist = playlist,
                                                    playerViewModel = playerViewModel,
                                                    onClick = { navController.navigateSafely(Screen.PlaylistDetail.createRoute(playlist.id)) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // G. Dynamic Home & Discovery Shelves
                                if (uiState.selectedFilter == "All" || uiState.selectedFilter == "For You" || uiState.selectedFilter == "New Releases") {
                                    shelvesToDisplay.forEachIndexed { index, section ->
                                        val sectionKey = "home_section_${index}_${section.title.hashCode()}"
                                        item(key = "${sectionKey}_header") {
                                            SectionHeader(title = section.title)
                                        }
                                        item(key = sectionKey) {
                                            RenderExpressiveExploreShelf(
                                                section = section,
                                                shelfIndex = index,
                                                navController = navController,
                                                playerViewModel = playerViewModel,
                                                onSongMoreOptions = { song ->
                                                    playerViewModel.selectSongForInfo(song)
                                                    showSongInfoBottomSheet = true
                                                }
                                            )
                                        }
                                    }
                                }

                                // H. Podcasts Shelf
                                if (uiState.selectedFilter == "All" && uiState.podcastPlaylists.isNotEmpty()) {
                                    item(key = "podcast_playlists_header") {
                                        SectionHeader(
                                            title = "Podcasts & Shows",
                                            actionLabel = "See All",
                                            onActionClick = { navController.navigateSafely(Screen.Podcasts.route) }
                                        )
                                    }
                                    item(key = "podcast_playlists_carousel") {
                                        MixedStationCarousel(
                                            items = uiState.podcastPlaylists,
                                            navController = navController,
                                            playerViewModel = playerViewModel
                                        )
                                    }
                                }

                                // I. Moods & Genres Preview at Bottom of "All"
                                if (uiState.selectedFilter == "All") {
                                    item(key = "inline_moods_genres_preview") {
                                        ExploreMoodsAndGenresSection(
                                            remoteSections = uiState.moodsAndGenresSections.take(1),
                                            navController = navController,
                                            onSelectQuickMood = { exploreViewModel.setSelectedMood(it) },
                                            isCompactPreview = true
                                        )
                                    }
                                }

                                // J. Pagination Loading
                                if (uiState.isContinuationLoading) {
                                    item(key = "pagination_loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LoadingIndicator(modifier = Modifier.size(32.dp))
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
            colorScheme.primary.copy(alpha = 0.15f).compositeOver(colorScheme.background)
        } else {
            colorScheme.background
        }

        // Top status bar protection scrim matching Home screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(statusBarHeight + 64.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.95f)),
                            0.18f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.86f)),
                            0.36f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.68f)),
                            0.54f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.48f)),
                            0.72f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.28f)),
                            0.88f to (if (isCustomBackground || isLiquidGlassOnHeader) Color.Transparent else scrimTopColor.copy(alpha = 0.11f)),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // Top bar with Explore text pill on the left and Settings button on the right
        // Both vanish when scrolling (exploreTitleAlpha fades out)
        LiquidGlassHeaderBar(
            pillText = "Explore",
            pillIcon = null,
            pillTextSize = 28.sp,
            isLiquidGlass = isLiquidGlassOnHeader,
            backdrop = null,
            alpha = exploreTitleAlpha,
            onPillClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onSettingsClick = {
                if (exploreTitleAlpha > 0.05f) {
                    navController.navigateSafely(Screen.Settings.route)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = statusBarHeight + 14.dp)
        )

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
                    playerViewModel.showAndPlaySong(song)
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
                playerViewModel = playerViewModel
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. FILTER & MOOD CHIPS (LIQUID GLASS SURFACES WITH DYNAMIC SPRING SPACING)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiquidGlassFilterChip(
    text: String,
    isSelected: Boolean,
    isLiquidGlass: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colorScheme = MaterialTheme.colorScheme

    val animatedHorizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 22.dp else 16.dp,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 400f),
        label = "chip_pad_$text"
    )

    val chipShape = CircleShape

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primary
            isLiquidGlass -> if (isDark) Color(0x20FFFFFF) else Color(0x50FFFFFF)
            else -> colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "chip_bg_$text"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.onPrimary
            else -> colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "chip_content_$text"
    )

    val borderStroke = remember(isLiquidGlass, isSelected, isDark, colorScheme.primary) {
        if (!isLiquidGlass) null
        else if (isSelected) {
            BorderStroke(0.75.dp, colorScheme.primary.copy(alpha = 0.35f))
        } else {
            BorderStroke(
                0.75.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.16f),
                        Color.White.copy(alpha = if (isDark) 0.02f else 0.04f)
                    )
                )
            )
        }
    }

    Surface(
        onClick = onClick,
        shape = chipShape,
        color = containerColor,
        border = borderStroke,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.height(38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = animatedHorizontalPadding)
        ) {
            Text(
                text = text,
                fontFamily = GoogleSansRounded,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExploreFilterAndMoodDock(
    selectedFilter: String,
    selectedMood: String?,
    onFilterClick: (String) -> Unit,
    onMoodClick: (String) -> Unit,
    isLiquidGlass: Boolean = true
) {
    val categories = remember {
        listOf("All", "For You", "New Releases", "Charts", "Moods & Genres")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .animateContentSize(spring(dampingRatio = 0.72f, stiffness = 380f)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedFilter == category && selectedMood == null
                LiquidGlassFilterChip(
                    text = category,
                    isSelected = isSelected,
                    isLiquidGlass = isLiquidGlass,
                    onClick = { onFilterClick(category) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .animateContentSize(spring(dampingRatio = 0.72f, stiffness = 380f)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MOOD_CHIPS.forEach { mood ->
                val isSelected = selectedMood == mood
                LiquidGlassFilterChip(
                    text = mood,
                    isSelected = isSelected,
                    isLiquidGlass = isLiquidGlass,
                    onClick = { onMoodClick(mood) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. HERO SPOTLIGHT PAGER (CLEAN BORDERLESS SURFACE + PEEKING VINYL)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExploreHeroSpotlightPager(
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit,
    onSeeAllNewReleases: () -> Unit
) {
    if (albums.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { albums.size })
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(pagerState.currentPage, albums.size) {
        if (albums.size > 1) {
            delay(5500L)
            val next = (pagerState.currentPage + 1) % albums.size
            pagerState.animateScrollToPage(
                page = next,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val album = albums[page]
            val hqThumb = remember(album.thumbnail) { upgradeThumbnailUrlToHighQuality(album.thumbnail) }

            Card(
                onClick = { onAlbumClick(album) },
                shape = AbsoluteSmoothCornerShape(26.dp, 60),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Featured Release",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = album.title,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 28.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = album.artists?.joinToString { it.name }.orEmpty().ifBlank { "New Release" } +
                                    (album.year?.let { " • $it" } ?: ""),
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            onClick = { onAlbumClick(album) },
                            shape = RoundedCornerShape(50),
                            color = colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Listen Now",
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Album Sleeve + Peeking Vinyl Disc
                    Box(
                        modifier = Modifier
                            .width(152.dp)
                            .height(136.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val primaryCol = colorScheme.primary
                        val surfaceCol = colorScheme.surfaceContainerHigh
                        Canvas(
                            modifier = Modifier
                                .size(122.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            val r = size.minDimension * 0.5f
                            drawCircle(color = Color(0xFF16161A), radius = r)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.10f),
                                radius = r * 0.82f,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.07f),
                                radius = r * 0.65f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(color = primaryCol, radius = r * 0.32f)
                            drawCircle(color = surfaceCol, radius = r * 0.09f)
                        }

                        SmartImage(
                            crossfadeDurationMillis = 0,
                            model = hqThumb,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(AbsoluteSmoothCornerShape(20.dp, 60))
                        )
                    }
                }
            }
        }

        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(albums.size) { idx ->
                    val isCurrent = pagerState.currentPage == idx
                    val width by animateDpAsState(
                        targetValue = if (isCurrent) 24.dp else 7.dp,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                        label = "PagerDotWidth_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(width)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCurrent) colorScheme.primary
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Text(
                text = "All New Releases",
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAllNewReleases)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. CHARTS PODIUM & RANKED TRACK DECK + REGION SELECTOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChartRegionSelectorRow(
    selectedCountryName: String,
    onSelectRegion: (String, String?) -> Unit,
    onOpenFullCharts: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EXPLORE_CHART_REGIONS.forEach { (name, code) ->
            val selected = selectedCountryName == name
            Surface(
                onClick = { onSelectRegion(name, code) },
                shape = RoundedCornerShape(50),
                color = if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = name,
                    fontFamily = GoogleSansRounded,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartsPodiumAndRankedDeck(
    songs: List<SongItem>,
    sectionTitle: String,
    currentSongId: String?,
    isPlaying: Boolean,
    playerViewModel: PlayerViewModel,
    onMoreOptionsClick: (Song) -> Unit = {}
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return

    val podiumSongs = remember(nativeSongs) { nativeSongs.take(3) }
    val deckColumns = remember(nativeSongs) {
        nativeSongs.drop(3).chunked(3)
    }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top 3 Podium Row (Clean, borderless surfaces)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = podiumSongs,
                key = { idx, s -> "podium_${idx}_${s.id}" }
            ) { index, song ->
                val rank = index + 1
                val isCurrent = currentSongId == song.id

                Card(
                    modifier = Modifier
                        .width(168.dp)
                        .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                        .combinedClickable(
                            onClick = {
                                playerViewModel.showAndPlaySong(song, nativeSongs, sectionTitle)
                            },
                            onLongClick = { onMoreOptionsClick(song) }
                        ),
                    shape = AbsoluteSmoothCornerShape(22.dp, 60),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) colorScheme.primaryContainer
                        else colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                        ) {
                            SmartImage(
                                crossfadeDurationMillis = 0,
                                model = song.albumArtUriString,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(AbsoluteSmoothCornerShape(16.dp, 60))
                            )
                            Surface(
                                shape = ExpressiveHarmonicShape(lobes1 = 8, amp1 = 0.08f),
                                color = colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(34.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#$rank",
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onPrimary
                                    )
                                }
                            }

                            if (isCurrent && isPlaying) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surface.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PlayingEqIcon(
                                            modifier = Modifier.size(15.dp),
                                            color = colorScheme.primary,
                                            isPlaying = true
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = song.title,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = song.artist,
                            fontFamily = GoogleSansRounded,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        // Ranked 3-Per-Column Horizontal Track Deck (#4+)
        if (deckColumns.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = deckColumns,
                    key = { colIdx, _ -> "chart_col_$colIdx" }
                ) { colIdx, columnSongs ->
                    Column(
                        modifier = Modifier.width(296.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columnSongs.forEachIndexed { rowIdx, song ->
                            val rankNum = 4 + colIdx * 3 + rowIdx
                            val isCurrent = currentSongId == song.id
                            Surface(
                                onClick = {
                                    playerViewModel.showAndPlaySong(song, nativeSongs, sectionTitle)
                                },
                                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                                color = if (isCurrent) colorScheme.primaryContainer
                                else colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = rankNum.toString(),
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isCurrent) colorScheme.onPrimaryContainer
                                        else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(24.dp)
                                    )

                                    SmartImage(
                                        crossfadeDurationMillis = 0,
                                        model = song.albumArtUriString,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(46.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCurrent) colorScheme.onPrimaryContainer
                                            else colorScheme.onSurface
                                        )
                                        Text(
                                            text = song.artist,
                                            fontFamily = GoogleSansRounded,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isCurrent && isPlaying) {
                                        PlayingEqIcon(
                                            modifier = Modifier.size(16.dp),
                                            color = colorScheme.onPrimaryContainer,
                                            isPlaying = true
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { playerViewModel.selectSongForInfo(song) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "More options",
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
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
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. VINYL SLEEVE ALBUM SHOWCASE CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VinylAlbumShowcaseCarousel(
    albums: List<AlbumItem>,
    onAlbumClick: (AlbumItem) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = albums,
            key = { idx, album -> "vinyl_album_${idx}_${album.browseId}" }
        ) { _, album ->
            val hqThumb = remember(album.thumbnail) { upgradeThumbnailUrlToHighQuality(album.thumbnail) }

            Column(
                modifier = Modifier
                    .width(162.dp)
                    .clip(AbsoluteSmoothCornerShape(18.dp, 60))
                    .clickable { onAlbumClick(album) }
            ) {
                Box(
                    modifier = Modifier
                        .width(162.dp)
                        .height(142.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val primaryCol = colorScheme.primary
                    val surfaceCol = colorScheme.surface
                    Canvas(
                        modifier = Modifier
                            .size(122.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        val r = size.minDimension * 0.5f
                        drawCircle(color = Color(0xFF16161B), radius = r)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.10f),
                            radius = r * 0.82f,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = r * 0.64f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(color = primaryCol.copy(alpha = 0.85f), radius = r * 0.32f)
                        drawCircle(color = surfaceCol, radius = r * 0.09f)
                    }

                    SmartImage(
                        crossfadeDurationMillis = 0,
                        model = hqThumb,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(AbsoluteSmoothCornerShape(22.dp, 60))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = album.title,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = album.artists?.joinToString { it.name }.orEmpty().ifBlank { "Album" },
                    fontFamily = GoogleSansRounded,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. WIDESCREEN 16:9 VIDEO CAROUSEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoShowcaseCarousel(
    videos: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String
) {
    val nativeVideos = remember(videos) { videos.map { it.toNativeSong() } }
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = videos,
            key = { idx, v -> "video_${idx}_${v.id}" }
        ) { idx, videoItem ->
            val nativeSong = nativeVideos[idx]
            val hqThumb = remember(videoItem.thumbnail) { upgradeThumbnailUrlToHighQuality(videoItem.thumbnail) }

            Card(
                onClick = {
                    playerViewModel.showAndPlaySong(nativeSong, nativeVideos, sectionTitle)
                },
                shape = AbsoluteSmoothCornerShape(22.dp, 60),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .width(260.dp)
                    .height(148.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartImage(
                        crossfadeDurationMillis = 0,
                        model = hqThumb,
                        contentDescription = videoItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = videoItem.title,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = videoItem.artists.joinToString { it.name },
                                fontFamily = GoogleSansRounded,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. CLEAN MOODS & GENRES SECTION (NO COLORED BORDERS OR PASTEL FILLS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExploreMoodsAndGenresSection(
    remoteSections: List<YouTube.MoodsAndGenresSection>,
    navController: NavController,
    onSelectQuickMood: (String) -> Unit,
    isCompactPreview: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val stripeColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Moods & Genres",
            actionLabel = if (isCompactPreview) "See All" else null,
            onActionClick = if (isCompactPreview) {
                { navController.navigateSafely(Screen.MoodsAndGenres.route) }
            } else null
        )

        if (remoteSections.isNotEmpty()) {
            remoteSections.forEachIndexed { sIdx, section ->
                if (!isCompactPreview) {
                    Text(
                        text = section.title,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                val displayItems = if (isCompactPreview) section.items.take(8) else section.items
                val pairs = remember(displayItems) { displayItems.chunked(2) }
                val isLiquidGlassMood = LocalLiquidGlassEnabled.current
                val isDarkMood = colorScheme.surface.luminance() < 0.5f
                val moodCardBorder = remember(isLiquidGlassMood, isDarkMood) {
                    if (!isLiquidGlassMood) null
                    else BorderStroke(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDarkMood) 0.08f else 0.16f),
                                Color.White.copy(alpha = if (isDarkMood) 0.02f else 0.04f)
                            )
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pairs.forEachIndexed { pIdx, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { cIdx, item ->
                                val stripe = stripeColors[(sIdx + pIdx + cIdx) % stripeColors.size]

                                Surface(
                                    onClick = {
                                        navController.navigateSafely(
                                            Screen.MoodDetail.createRoute(
                                                title = item.title,
                                                browseId = item.browseId,
                                                params = item.params
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isLiquidGlassMood) {
                                        if (isDarkMood) Color(0x20FFFFFF) else Color(0x50FFFFFF)
                                    } else {
                                        colorScheme.surfaceContainer
                                    },
                                    border = moodCardBorder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(stripe)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.title,
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            val pairs = remember { MOOD_CHIPS.chunked(2) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pairs.forEachIndexed { pIdx, rowMoods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMoods.forEachIndexed { cIdx, mood ->
                            val stripe = stripeColors[(pIdx + cIdx) % stripeColors.size]
                            Surface(
                                onClick = { onSelectQuickMood(mood) },
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(stripe)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = mood,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (rowMoods.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. DYNAMIC SHELF ROUTER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RenderExpressiveExploreShelf(
    section: saurav.shru.pixelmusic.innertube.pages.HomePage.Section,
    shelfIndex: Int,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onSongMoreOptions: (Song) -> Unit = {}
) {
    val titleLower = section.title.lowercase()
    val songItems = remember(section.items) { section.items.filterIsInstance<SongItem>() }
    val albumItems = remember(section.items) { section.items.filterIsInstance<AlbumItem>() }
    val artistItems = remember(section.items) { section.items.filterIsInstance<ArtistItem>() }
    val playlistItems = remember(section.items) { section.items.filterIsInstance<PlaylistItem>() }

    val isAllSongs = songItems.size == section.items.size && songItems.isNotEmpty()
    val isAllAlbums = albumItems.size == section.items.size && albumItems.isNotEmpty()
    val isAllArtists = artistItems.size == section.items.size && artistItems.isNotEmpty()
    val isAllPlaylists = playlistItems.size == section.items.size && playlistItems.isNotEmpty()

    when {
        isAllArtists || titleLower.startsWith("similar to") || titleLower.contains("fans also like") || titleLower.contains("favorite artists") -> {
            SimilarArtistsCarousel(
                artists = artistItems,
                navController = navController
            )
        }

        isAllAlbums || titleLower.contains("liked albums") -> {
            VinylAlbumShowcaseCarousel(
                albums = albumItems,
                onAlbumClick = { album ->
                    navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId))
                }
            )
        }

        isAllSongs && (titleLower.contains("video") || titleLower.contains("live") || titleLower.contains("performance")) -> {
            VideoShowcaseCarousel(
                videos = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title
            )
        }

        isAllSongs && (titleLower.contains("trending") || titleLower.contains("hits") || titleLower.contains("top songs")) -> {
            ExploreTrendingCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllSongs && (titleLower.contains("discover") || titleLower.contains("daily")) -> {
            ExploreDiscoverCardsCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllSongs && titleLower.contains("shorts") -> {
            ExploreShortsVerticalCardsCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllSongs && (titleLower.contains("forgotten") || titleLower.contains("favourite") || titleLower.contains("favorite") || titleLower.contains("listen again")) -> {
            ExploreNostalgiaVinylCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllSongs && (titleLower.contains("cover") || titleLower.contains("remix")) -> {
            ExploreCoversAndRemixesCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllSongs -> {
            ExploreGeneralSongsCarousel(
                songs = songItems,
                playerViewModel = playerViewModel,
                sectionTitle = section.title,
                onMoreOptionsClick = onSongMoreOptions
            )
        }

        isAllPlaylists || titleLower.contains("mixed for you") || titleLower.contains("daily discover") || titleLower.contains("community") || titleLower.contains("your playlists") -> {
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
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreTrendingCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val carouselState = rememberCarouselState { nativeSongs.size }
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val colorScheme = MaterialTheme.colorScheme

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 260.dp,
        itemSpacing = 14.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) { index ->
        val song = nativeSongs[index]
        val isCurrent = currentSongId == song.id
        val isCurrentPlaying = isCurrent && isPlayingNow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .background(colorScheme.surfaceContainerLow)
                .combinedClickable(
                    onClick = {
                        playerViewModel.showAndPlaySong(
                            song = song,
                            contextSongs = nativeSongs,
                            queueName = sectionTitle
                        )
                    },
                    onLongClick = { onMoreOptionsClick(song) }
                )
        ) {
            SmartImage(
                crossfadeDurationMillis = 0,
                model = song.albumArtUriString,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                shape = CircleShape,
                color = if (index < 3) colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                contentColor = if (index < 3) colorScheme.onPrimary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "#${index + 1}",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { onMoreOptionsClick(song) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(34.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontFamily = GoogleSansRounded,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isCurrentPlaying) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PlayingEqIcon(
                                    modifier = Modifier.size(15.dp),
                                    color = colorScheme.onPrimary,
                                    isPlaying = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreDiscoverCardsCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = nativeSongs,
            key = { idx, s -> "discover_${idx}_${s.id}" }
        ) { _, song ->
            val isCurrent = currentSongId == song.id
            val isCurrentPlaying = isCurrent && isPlayingNow

            Card(
                modifier = Modifier
                    .width(172.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        onLongClick = { onMoreOptionsClick(song) }
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(152.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        SmartImage(
                            crossfadeDurationMillis = 0,
                            model = song.albumArtUriString,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            shape = CircleShape,
                            color = colorScheme.secondaryContainer.copy(alpha = 0.90f),
                            contentColor = colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { onMoreOptionsClick(song) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isCurrent) colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                            contentColor = if (isCurrent) colorScheme.onPrimary else Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCurrentPlaying) {
                                    PlayingEqIcon(
                                        modifier = Modifier.size(16.dp),
                                        color = if (isCurrent) colorScheme.onPrimary else Color.White,
                                        isPlaying = true
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = song.title,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )
                    Text(
                        text = song.artist,
                        fontFamily = GoogleSansRounded,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreShortsVerticalCardsCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = nativeSongs,
            key = { idx, s -> "shorts_${idx}_${s.id}" }
        ) { _, song ->
            val isCurrent = currentSongId == song.id
            val isCurrentPlaying = isCurrent && isPlayingNow

            Card(
                modifier = Modifier
                    .width(148.dp)
                    .height(224.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .combinedClickable(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        onLongClick = { onMoreOptionsClick(song) }
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartImage(
                        crossfadeDurationMillis = 0,
                        model = song.albumArtUriString,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    FilledTonalIconButton(
                        onClick = { onMoreOptionsClick(song) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrentPlaying) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF0033),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PlayingEqIcon(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.White,
                                            isPlaying = true
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreNostalgiaVinylCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = nativeSongs,
            key = { idx, s -> "vinyl_${idx}_${s.id}" }
        ) { _, song ->
            val isCurrent = currentSongId == song.id
            val isCurrentPlaying = isCurrent && isPlayingNow

            Card(
                modifier = Modifier
                    .width(184.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        onLongClick = { onMoreOptionsClick(song) }
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1E1E),
                            border = BorderStroke(1.dp, Color(0xFF333333)),
                            modifier = Modifier
                                .size(136.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF111111),
                                    border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
                                    modifier = Modifier.size(92.dp)
                                ) {}
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black,
                                            modifier = Modifier.size(10.dp)
                                        ) {}
                                    }
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .size(132.dp)
                                .align(Alignment.CenterStart),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                SmartImage(
                                    crossfadeDurationMillis = 0,
                                    model = song.albumArtUriString,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrent) colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                                    contentColor = if (isCurrent) colorScheme.onPrimary else Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCurrentPlaying) {
                                            PlayingEqIcon(
                                                modifier = Modifier.size(14.dp),
                                                color = if (isCurrent) colorScheme.onPrimary else Color.White,
                                                isPlaying = true
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isCurrentPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = "Play",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { onMoreOptionsClick(song) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                                contentColor = colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = song.title,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )
                    Text(
                        text = song.artist,
                        fontFamily = GoogleSansRounded,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreCoversAndRemixesCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val pairs = remember(nativeSongs) { nativeSongs.chunked(2) }
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = pairs,
            key = { idx, pair -> "covers_col_${idx}_${pair.firstOrNull()?.id}" }
        ) { _, pair ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { song ->
                    val isCurrent = currentSongId == song.id
                    val isCurrentPlaying = isCurrent && isPlayingNow

                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .combinedClickable(
                                onClick = {
                                    playerViewModel.showAndPlaySong(
                                        song = song,
                                        contextSongs = nativeSongs,
                                        queueName = sectionTitle
                                    )
                                },
                                onLongClick = { onMoreOptionsClick(song) }
                            ),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                SmartImage(
                                    crossfadeDurationMillis = 0,
                                    model = song.albumArtUriString,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isCurrentPlaying) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            PlayingEqIcon(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                isPlaying = true
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colorScheme.tertiaryContainer,
                                        contentColor = colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "REMIX",
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = song.title,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = song.artist,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                    else colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onMoreOptionsClick(song) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    contentColor = colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreGeneralSongsCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String,
    onMoreOptionsClick: (Song) -> Unit
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    if (nativeSongs.isEmpty()) return
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = nativeSongs,
            key = { idx, s -> "gen_song_${idx}_${s.id}" }
        ) { _, song ->
            val isCurrent = currentSongId == song.id
            val isCurrentPlaying = isCurrent && isPlayingNow

            Card(
                modifier = Modifier
                    .width(200.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        onLongClick = { onMoreOptionsClick(song) }
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SmartImage(
                        crossfadeDurationMillis = 0,
                        model = song.albumArtUriString,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    FilledTonalIconButton(
                        onClick = { onMoreOptionsClick(song) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.50f),
                                        Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrentPlaying) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        PlayingEqIcon(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.Black,
                                            isPlaying = true
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

// ─────────────────────────────────────────────────────────────────────────────
// 8. CAROUSELS & PUBLIC COMPONENTS (CLEAN, BORDERLESS SURFACES)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SongPillsCarousel(
    songs: List<SongItem>,
    playerViewModel: PlayerViewModel,
    sectionTitle: String
) {
    val nativeSongs = remember(songs) { songs.map { it.toNativeSong() } }
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val rows = remember(nativeSongs) {
        val row1 = mutableListOf<Song>()
        val row2 = mutableListOf<Song>()
        nativeSongs.forEachIndexed { i, s ->
            if (i % 2 == 0) row1.add(s) else row2.add(s)
        }
        listOf(row1, row2).filter { it.isNotEmpty() }
    }
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

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
                    val isCurrent = currentSongId == song.id
                    Surface(
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = song,
                                contextSongs = nativeSongs,
                                queueName = sectionTitle
                            )
                        },
                        shape = RoundedCornerShape(28.dp),
                        color = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .width(228.dp)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SmartImage(
                                crossfadeDurationMillis = 0,
                                model = song.albumArtUriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                                Text(
                                    text = song.artist,
                                    fontFamily = GoogleSansRounded,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isCurrent) colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                    else colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCurrent && isPlayingNow) {
                                PlayingEqIcon(
                                    modifier = Modifier.size(16.dp),
                                    color = colorScheme.onPrimaryContainer,
                                    isPlaying = true
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
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = nativeSongs,
            key = { idx, song -> "bigbox_${idx}_${song.id}" }
        ) { _, song ->
            val isThisSong = song.id == currentSongId
            val isThisSongPlaying = isThisSong && isPlayingNow

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
                }
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
    val colorScheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
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
                crossfadeDurationMillis = 0,
                model = song.albumArtUriString,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(64.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = GoogleSansRounded,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontFamily = GoogleSansRounded,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurfaceVariant
                )
            }
            val isLiquidGlass = LocalLiquidGlassEnabled.current
            FilledIconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .liquidGlassInteractiveSurface(isCircle = true),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else colorScheme.primary,
                    contentColor = if (isLiquidGlass) colorScheme.primary else colorScheme.onPrimary
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
    val colorScheme = MaterialTheme.colorScheme
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle(initialValue = null)
    val isPlayingNow = stablePlayerState?.isPlaying == true
    val currentSongId = stablePlayerState?.currentSong?.id

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { idx, item -> "mix_station_${idx}_${item.id}" }
        ) { _, item ->
            val rawThumb = when (item) {
                is PlaylistItem -> item.thumbnail
                is AlbumItem -> item.thumbnail
                is SongItem -> item.thumbnail
                else -> null
            }
            val hqThumb = remember(rawThumb) { 
                // Mix stations have complex thumbnails that get broken by quality upgrades, so use raw
                if (item is PlaylistItem && item.title.contains("Mix", ignoreCase = true) || item is PlaylistItem && item.title.contains("for you", ignoreCase = true)) {
                    rawThumb
                } else {
                    upgradeThumbnailUrlToHighQuality(rawThumb) 
                }
            }
            val title = when (item) {
                is PlaylistItem -> item.title
                is AlbumItem -> item.title
                is SongItem -> item.title
                else -> ""
            }
            val subtitle = when (item) {
                is PlaylistItem -> item.author?.name ?: "Mix Station"
                is AlbumItem -> item.artists?.firstOrNull()?.name ?: "Album"
                is SongItem -> item.artists.firstOrNull()?.name ?: "Song"
                else -> ""
            }

            Card(
                onClick = {
                    when (item) {
                        is PlaylistItem -> navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id))
                        is AlbumItem -> navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId))
                        is SongItem -> playerViewModel.showAndPlaySong(item.toNativeSong())
                        else -> {}
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .width(190.dp)
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(modifier = Modifier.size(170.dp)) {
                        SmartImage(
                            crossfadeDurationMillis = 0,
                            model = hqThumb,
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        val isLiquidGlass = LocalLiquidGlassEnabled.current
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val isPlayingThis = when (item) {
                            is SongItem -> item.id == currentSongId && isPlayingNow
                            else -> false
                        }
                        Surface(
                            onClick = {
                                if (isPlayingThis) {
                                    playerViewModel.playPause()
                                } else {
                                    when (item) {
                                        is PlaylistItem -> navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id))
                                        is AlbumItem -> navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId))
                                        is SongItem -> playerViewModel.showAndPlaySong(item.toNativeSong())
                                        else -> {}
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else colorScheme.primary,
                            tonalElevation = 0.dp,
                            shadowElevation = if (isLiquidGlass) 0.dp else 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .liquidGlassInteractiveSurface(isCircle = true)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlayingThis) "Pause" else "Play",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        fontFamily = GoogleSansRounded,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansRounded,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurfaceVariant
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
    val contextSongs = remember(items) {
        items.filterIsInstance<SongItem>().map { it.toNativeSong() }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { idx, item -> "yt_item_${idx}_${item.id}" }
        ) { _, item ->
            when (item) {
                is SongItem -> {
                    val songNative = item.toNativeSong()
                    SongCardItem(
                        song = songNative,
                        onClick = {
                            playerViewModel.showAndPlaySong(
                                song = songNative,
                                contextSongs = contextSongs,
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
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(AbsoluteSmoothCornerShape(20.dp, 60))
            .clickable(onClick = onClick)
    ) {
        SmartImage(
            crossfadeDurationMillis = 0,
            model = song.albumArtUriString,
            contentDescription = song.title,
            modifier = Modifier
                .size(148.dp)
                .clip(AbsoluteSmoothCornerShape(22.dp, 60)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = song.artist,
            fontFamily = GoogleSansRounded,
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

    val isLiquidGlass = LocalLiquidGlassEnabled.current
    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .liquidGlassInteractiveSurface(isCircle = true)
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

@Composable
fun RecentMixCardItem(
    playlist: Playlist,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit
) {
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
            .width(148.dp)
            .clip(AbsoluteSmoothCornerShape(22.dp, 60))
            .clickable(onClick = onClick)
    ) {
        PlaylistCover(
            playlist = playlist,
            playlistSongs = playlistSongs ?: emptyList(),
            modifier = Modifier
                .size(148.dp)
                .clip(AbsoluteSmoothCornerShape(22.dp, 60)),
            size = 148.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Smart Mix",
            fontFamily = GoogleSansRounded,
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
    subtitle: String? = null,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (onActionClick != null && actionLabel != null) {
            Text(
                text = actionLabel,
                fontFamily = GoogleSansRounded,
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
    val hqThumb = remember(album.thumbnail) { upgradeThumbnailUrlToHighQuality(album.thumbnail) }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(AbsoluteSmoothCornerShape(22.dp, 60))
            .clickable(onClick = onClick)
    ) {
        SmartImage(
            crossfadeDurationMillis = 0,
            model = hqThumb,
            contentDescription = album.title,
            modifier = Modifier
                .size(148.dp)
                .clip(AbsoluteSmoothCornerShape(22.dp, 60)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = album.artists?.joinToString { it.name } ?: "",
            fontFamily = GoogleSansRounded,
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
    val hqThumb = remember(artist.thumbnail) { upgradeThumbnailUrlToHighQuality(artist.thumbnail) }
    val scallopShape = remember { ExpressiveHarmonicShape(lobes1 = 8, amp1 = 0.065f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        SmartImage(
            crossfadeDurationMillis = 0,
            model = hqThumb,
            contentDescription = artist.title,
            modifier = Modifier
                .size(104.dp)
                .clip(scallopShape),
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
            fontFamily = GoogleSansRounded,
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
    val hqThumb = remember(playlist.thumbnail) {
        if (playlist.title.contains("Mix", ignoreCase = true) || playlist.title.contains("for you", ignoreCase = true) || playlist.id.startsWith("RD")) {
            playlist.thumbnail
        } else {
            upgradeThumbnailUrlToHighQuality(playlist.thumbnail)
        }
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(AbsoluteSmoothCornerShape(20.dp, 60))
            .clickable(onClick = onClick)
    ) {
        SmartImage(
            crossfadeDurationMillis = 0,
            model = hqThumb,
            contentDescription = playlist.title,
            modifier = Modifier
                .size(148.dp)
                .clip(AbsoluteSmoothCornerShape(20.dp, 60)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            fontFamily = GoogleSansRounded,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = playlist.author?.name ?: "",
            fontFamily = GoogleSansRounded,
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
        itemsIndexed(
            items = artists,
            key = { idx, artist -> "sim_artist_${idx}_${artist.id}" }
        ) { _, artist ->
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
    val colorScheme = MaterialTheme.colorScheme
    val hqThumb = remember(artist.thumbnail) { upgradeThumbnailUrlToHighQuality(artist.thumbnail) }
    val scallopShape = remember { ExpressiveHarmonicShape(lobes1 = 8, amp1 = 0.065f) }

    Card(
        onClick = onClick,
        modifier = Modifier.width(144.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
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
                        color = colorScheme.primary.copy(alpha = 0.12f),
                        shape = scallopShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    crossfadeDurationMillis = 0,
                    model = hqThumb,
                    contentDescription = artist.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(scallopShape),
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
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Similar Artist",
                fontFamily = GoogleSansRounded,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.primary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
