package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Album
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.model.SearchFilterType
import com.saurav.pixelmusic.data.model.SearchHistoryItem
import com.saurav.pixelmusic.data.model.SearchResultItem
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.AppBackgroundStyle
import com.saurav.pixelmusic.data.preferences.SearchSource
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.PlaylistBottomSheet
import com.saurav.pixelmusic.presentation.components.PlaylistCover
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.components.SmartImageListTargetSize
import com.saurav.pixelmusic.presentation.components.SongInfoBottomSheet
import com.saurav.pixelmusic.presentation.components.resolveNavBarOccupiedHeight
import com.saurav.pixelmusic.presentation.components.subcomps.EnhancedSongListItem
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.navigation.navigateSafelyReplacing
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.utils.formatSongCount
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import timber.log.Timber

private data class SearchUiSlice(
    val selectedSearchFilter: SearchFilterType = SearchFilterType.ALL,
    val searchResults: ImmutableList<SearchResultItem> = persistentListOf(),
    val searchHistory: ImmutableList<SearchHistoryItem> = persistentListOf()
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    navController: NavHostController,
    onSearchBarActiveChange: (Boolean) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf(playerViewModel.searchQuery) }
    val statusBarTopInset = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)

    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var playlistSheetSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val searchUiState by remember(playerViewModel) {
        playerViewModel.playerUiState
            .map { uiState ->
                SearchUiSlice(
                    selectedSearchFilter = uiState.selectedSearchFilter,
                    searchResults = uiState.searchResults,
                    searchHistory = uiState.searchHistory
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = SearchUiSlice())
    val currentFilter = searchUiState.selectedSearchFilter
    val isSearchLoading by playerViewModel.isSearchLoading.collectAsStateWithLifecycle()
    val searchSuggestions by playerViewModel.searchSuggestions.collectAsStateWithLifecycle()

    val currentPlayingSongId by remember {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val searchListIsPlaying by remember {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val searchSource by playerViewModel.searchSource.collectAsStateWithLifecycle()
    val isMotionBlurEnabled by playerViewModel.userPreferencesRepository.uiMotionBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchInputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        onSearchBarActiveChange(false)
        playerViewModel.loadSearchHistory()
        if (searchQuery.isBlank()) {
            playerViewModel.performSearch("")
        }
    }

    LaunchedEffect(playerViewModel, keyboardController) {
        playerViewModel.searchNavDoubleTapEvents.collect {
            delay(40L)
            searchInputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(searchQuery, currentFilter, searchSource) {
        playerViewModel.performSearch(searchQuery)
    }

    val searchResults = searchUiState.searchResults
    val handleSongMoreOptionsClick: (Song) -> Unit = { song ->
        playerViewModel.selectSongForInfo(song)
        showSongInfoBottomSheet = true
    }

    val colorScheme = MaterialTheme.colorScheme
    val backgroundStyle by playerViewModel.userPreferencesRepository.appBackgroundStyleFlow.collectAsStateWithLifecycle(initialValue = AppBackgroundStyle.DEFAULT)
    val isCustomBackground = backgroundStyle != AppBackgroundStyle.DEFAULT
    val isLightTheme = colorScheme.background.luminance() > 0.5f
    val surfaceColor = colorScheme.surface
    val primaryColor = colorScheme.primary

    val backgroundBrush = remember(surfaceColor, primaryColor, isLightTheme) {
        if (isLightTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f),
                    surfaceColor.copy(alpha = 0.65f),
                    surfaceColor
                ),
                endY = 950f
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.08f),
                    surfaceColor,
                    surfaceColor
                ),
                endY = 900f
            )
        }
    }

    val listBottomPadding = bottomBarHeightDp + (if (currentPlayingSongId != null) MiniPlayerHeight + 28.dp else 28.dp)

    DisposableEffect(Unit) {
        onDispose {
            onSearchBarActiveChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isCustomBackground) Modifier else Modifier.background(backgroundBrush))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top Search Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = statusBarTopInset + 10.dp,
                        end = 16.dp,
                        bottom = 6.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search_icon),
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                val placeholderText = if (searchSource == SearchSource.LOCAL) {
                                    "Search local library…"
                                } else {
                                    stringResource(R.string.search_placeholder)
                                }
                                Text(
                                    text = placeholderText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    playerViewModel.updateSearchQuery(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchInputFocusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            playerViewModel.onSearchQuerySubmitted(searchQuery)
                                        }
                                        keyboardController?.hide()
                                    }
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = searchQuery.isNotBlank(),
                            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                            exit = fadeOut(tween(150)) + scaleOut(tween(150))
                        ) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    playerViewModel.updateSearchQuery("")
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search_query),
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Source Pill Toggle (YouTube Music vs Local Library)
                        Surface(
                            onClick = { playerViewModel.toggleSearchSource() },
                            shape = CircleShape,
                            color = if (searchSource == SearchSource.LOCAL) {
                                colorScheme.secondaryContainer
                            } else {
                                colorScheme.primaryContainer.copy(alpha = 0.75f)
                            },
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (searchSource == SearchSource.LOCAL) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_library_music_24),
                                        contentDescription = "Local Library Search",
                                        tint = colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "Local",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_youtube),
                                        contentDescription = "YouTube Music Search",
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                FilledIconButton(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    ),
                    onClick = { navController.navigateSafely(Screen.Settings.route) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_settings_24),
                        contentDescription = stringResource(R.string.presentation_batch_d_open_settings_cd),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Body: Browse Mode vs Active Search Results ──────────────────────
            val showGenreBrowse by remember(searchQuery) { derivedStateOf { searchQuery.isBlank() } }
            AnimatedContent(
                targetState = showGenreBrowse,
                transitionSpec = {
                    val switchingToGenre = targetState
                    val enter = fadeIn(animationSpec = tween(durationMillis = 280)) +
                        slideInVertically(animationSpec = tween(durationMillis = 280)) { fullHeight ->
                            if (switchingToGenre) -fullHeight / 14 else fullHeight / 14
                        }
                    val exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                        slideOutVertically(animationSpec = tween(durationMillis = 200)) { fullHeight ->
                            if (switchingToGenre) fullHeight / 16 else -fullHeight / 16
                        }
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "search_mode_transition"
            ) { isGenreMode ->
                if (isGenreMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        SearchHistoryList(
                            historyItems = searchUiState.searchHistory,
                            bottomContentPadding = listBottomPadding,
                            onHistoryClick = { query ->
                                searchQuery = query
                                playerViewModel.updateSearchQuery(query)
                                playerViewModel.onSearchQuerySubmitted(query)
                                keyboardController?.hide()
                            },
                            onHistoryDelete = { query ->
                                playerViewModel.deleteSearchHistoryItem(query)
                            },
                            onClearAllHistory = {
                                playerViewModel.clearSearchHistory()
                            },
                            onNavigateToNewReleases = { navController.navigateSafely(Screen.NewReleases.route) },
                            onNavigateToCharts = { navController.navigateSafely(Screen.Charts.route) },
                            onNavigateToMoodsAndGenres = { navController.navigateSafely(Screen.MoodsAndGenres.route) },
                            onNavigateToPodcasts = { navController.navigateSafely(Screen.Podcasts.route) },
                            isMotionBlurEnabled = isMotionBlurEnabled
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Single-row horizontally scrollable filter chips
                        val filters = remember {
                            listOf(
                                SearchFilterType.ALL,
                                SearchFilterType.SONGS,
                                SearchFilterType.VIDEOS,
                                SearchFilterType.ALBUMS,
                                SearchFilterType.ARTISTS,
                                SearchFilterType.PLAYLISTS
                            )
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters, key = { it.name }) { filterType ->
                                SearchFilterChip(
                                    filterType = filterType,
                                    currentFilter = currentFilter,
                                    playerViewModel = playerViewModel
                                )
                            }
                        }

                        // Live autocomplete suggestions row (when online suggestions exist)
                        AnimatedVisibility(
                            visible = searchSource == SearchSource.ONLINE && searchSuggestions.isNotEmpty()
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 2.dp,
                                    bottom = 6.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchSuggestions, key = { "sug_$it" }) { suggestion ->
                                    Surface(
                                        onClick = {
                                            searchQuery = suggestion
                                            playerViewModel.updateSearchQuery(suggestion)
                                            playerViewModel.onSearchQuerySubmitted(suggestion)
                                            keyboardController?.hide()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = colorScheme.surfaceContainer,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null,
                                                tint = colorScheme.primary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Crossfade(
                                targetState = when {
                                    isSearchLoading && searchResults.isEmpty() -> "LOADING"
                                    searchResults.isEmpty() -> "EMPTY"
                                    else -> "RESULTS"
                                },
                                animationSpec = tween(durationMillis = 220),
                                label = "search_results_state"
                            ) { state ->
                                when (state) {
                                    "LOADING" -> {
                                        SearchLoadingPlaceholder(bottomPadding = listBottomPadding)
                                    }
                                    "EMPTY" -> {
                                        EmptySearchResults(
                                            searchQuery = searchQuery,
                                            colorScheme = colorScheme
                                        )
                                    }
                                    else -> {
                                        SearchResultsList(
                                            results = searchResults,
                                            searchQuery = searchQuery,
                                            playerViewModel = playerViewModel,
                                            currentFilter = currentFilter,
                                            bottomContentPadding = listBottomPadding,
                                            onItemSelected = {
                                                if (searchQuery.isNotBlank()) {
                                                    playerViewModel.onSearchQuerySubmitted(searchQuery)
                                                }
                                            },
                                            currentPlayingSongId = currentPlayingSongId,
                                            isPlaying = searchListIsPlaying,
                                            onSongMoreOptionsClick = handleSongMoreOptionsClick,
                                            navController = navController
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

    if (showSongInfoBottomSheet && selectedSongForInfo != null) {
        val currentSong = selectedSongForInfo
        val isFavorite = remember(currentSong?.id, favoriteSongIds) {
            derivedStateOf {
                currentSong?.let { favoriteSongIds.contains(it.id) }
            }
        }.value ?: false
        val removeFromListTrigger = remember(currentSong) {
            { searchQuery = "$searchQuery " }
        }

        if (currentSong != null) {
            SongInfoBottomSheet(
                song = currentSong,
                isFavorite = isFavorite,
                removeFromListTrigger = removeFromListTrigger,
                onToggleFavorite = {
                    playerViewModel.toggleFavoriteSpecificSong(currentSong)
                },
                onDismiss = { showSongInfoBottomSheet = false },
                onPlaySong = {
                    playerViewModel.showAndPlaySong(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToQueue = {
                    playerViewModel.addSongToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddNextToQueue = {
                    playerViewModel.addSongNextToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToPlayList = {
                    playlistSheetSongs = listOf(currentSong)
                    showSongInfoBottomSheet = false
                    showPlaylistBottomSheet = true
                },
                onDeleteFromDevice = playerViewModel::deleteFromDevice,
                onNavigateToAlbum = {
                    navController.navigateSafelyReplacing(
                        route = Screen.AlbumDetail.createRoute(currentSong.albumId),
                        patternToPop = Screen.AlbumDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtist = {
                    navController.navigateSafelyReplacing(
                        route = Screen.ArtistDetail.createRoute(currentSong.artistId),
                        patternToPop = Screen.ArtistDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtistById = { artistId ->
                    navController.navigateSafelyReplacing(
                        route = Screen.ArtistDetail.createRoute(artistId),
                        patternToPop = Screen.ArtistDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToGenre = {},
                onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate ->
                    playerViewModel.editSongMetadata(
                        currentSong, newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate
                    )
                },
                generateAiMetadata = { fields ->
                    playerViewModel.generateAiMetadata(currentSong, fields)
                },
            )
        }
    }

    if (showPlaylistBottomSheet) {
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = playlistSheetSongs,
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = bottomBarHeightDp,
            playerViewModel = playerViewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchLoadingPlaceholder(bottomPadding: Dp) {
    val dummySong = remember { Song.emptySong() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularWavyProgressIndicator(modifier = Modifier.size(22.dp))
            Text(
                text = "Searching…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        repeat(6) {
            EnhancedSongListItem(
                song = dummySong,
                isPlaying = false,
                isLoading = true,
                onMoreOptionsClick = {},
                onClick = {}
            )
        }
    }
}

@Composable
fun SearchResultSectionHeader(
    title: String,
    count: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (count != null && count > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun SearchHistoryList(
    historyItems: List<SearchHistoryItem>,
    bottomContentPadding: Dp = 160.dp,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onNavigateToNewReleases: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {},
    onNavigateToMoodsAndGenres: () -> Unit = {},
    onNavigateToPodcasts: () -> Unit = {},
    isMotionBlurEnabled: Boolean = true
) {
    val historyListState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        state = historyListState,
        modifier = Modifier
            .fillMaxSize()
            .scrollMotionBlur(historyListState, enabled = isMotionBlurEnabled),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = bottomContentPadding
        )
    ) {
        item(key = "explore_categories_header") {
            Text(
                text = "Browse & Discover",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        item(key = "explore_categories_grid") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExploreCategoryCard(
                        title = "New releases",
                        subtitle = "Fresh albums & singles",
                        icon = Icons.Rounded.Album,
                        accentColor = colorScheme.primaryContainer,
                        onAccentColor = colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToNewReleases
                    )
                    ExploreCategoryCard(
                        title = "Charts",
                        subtitle = "Top trending tracks",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = colorScheme.tertiaryContainer,
                        onAccentColor = colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCharts
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExploreCategoryCard(
                        title = "Moods & genres",
                        subtitle = "Vibes for every moment",
                        icon = Icons.Rounded.SentimentSatisfied,
                        accentColor = colorScheme.secondaryContainer,
                        onAccentColor = colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMoodsAndGenres
                    )
                    ExploreCategoryCard(
                        title = "Podcasts",
                        subtitle = "Shows & episodes",
                        icon = Icons.Rounded.Podcasts,
                        accentColor = colorScheme.surfaceContainerHighest,
                        onAccentColor = colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPodcasts
                    )
                }
            }
        }

        if (historyItems.isNotEmpty()) {
            item(key = "recent_searches_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    TextButton(onClick = onClearAllHistory) {
                        Text(
                            text = stringResource(R.string.clear_all),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            items(
                items = historyItems,
                key = { "history_${it.id ?: it.query}" },
                contentType = { "search_history" }
            ) { item ->
                SearchHistoryListItem(
                    item = item,
                    onHistoryClick = onHistoryClick,
                    onHistoryDelete = onHistoryDelete
                )
            }
        }
    }
}

@Composable
private fun ExploreCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 24.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTR = 24.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 24.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 24.dp, smoothnessAsPercentTL = 60
        )
    }
    Surface(
        onClick = onClick,
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier.height(104.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = onAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryListItem(
    item: SearchHistoryItem,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit
) {
    Surface(
        onClick = { onHistoryClick(item.query) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = stringResource(R.string.cd_search_history_icon),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.query,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onHistoryDelete(item.query) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = stringResource(R.string.cd_delete_search_history_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySearchResults(searchQuery: String, colorScheme: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_no_search_results),
                    modifier = Modifier.size(40.dp),
                    tint = colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotBlank()) {
                stringResource(R.string.search_no_results_for_query, searchQuery)
            } else {
                stringResource(R.string.search_nothing_found)
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.search_try_different_or_filters),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchResultsList(
    results: List<SearchResultItem>,
    searchQuery: String,
    playerViewModel: PlayerViewModel,
    currentFilter: SearchFilterType,
    bottomContentPadding: Dp = 160.dp,
    onItemSelected: () -> Unit,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onSongMoreOptionsClick: (Song) -> Unit,
    navController: NavHostController
) {
    val searchIsShuffleEnabled by remember {
        playerViewModel.stablePlayerState.map { it.isShuffleEnabled }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    if (results.isEmpty()) {
        EmptySearchResults(
            searchQuery = searchQuery,
            colorScheme = MaterialTheme.colorScheme
        )
        return
    }

    val groupedResults = remember(results, currentFilter) {
        results.groupBy { item ->
            when (item) {
                is SearchResultItem.SongItem -> if (currentFilter == SearchFilterType.VIDEOS) SearchFilterType.VIDEOS else SearchFilterType.SONGS
                is SearchResultItem.AlbumItem -> SearchFilterType.ALBUMS
                is SearchResultItem.ArtistItem -> SearchFilterType.ARTISTS
                is SearchResultItem.PlaylistItem -> SearchFilterType.PLAYLISTS
            }
        }
    }

    val allSongsInResults = remember(results) {
        results.filterIsInstance<SearchResultItem.SongItem>().map { it.song }
    }

    val searchQueueName = remember(searchQuery) {
        searchQuery.trim().takeIf { it.isNotEmpty() }?.let { "Search: $it" } ?: "Search Results"
    }
    val onSongResultClick = remember(playerViewModel, onItemSelected, searchQueueName, allSongsInResults) {
        { song: Song ->
            val queue = if (allSongsInResults.isNotEmpty()) allSongsInResults else listOf(song)
            playerViewModel.showAndPlaySong(song, queue, searchQueueName)
            onItemSelected()
        }
    }

    val sectionOrder = remember(currentFilter) {
        when (currentFilter) {
            SearchFilterType.ALL -> listOf(
                SearchFilterType.SONGS,
                SearchFilterType.ARTISTS,
                SearchFilterType.ALBUMS,
                SearchFilterType.PLAYLISTS
            )
            SearchFilterType.SONGS -> listOf(SearchFilterType.SONGS)
            SearchFilterType.VIDEOS -> listOf(SearchFilterType.VIDEOS)
            SearchFilterType.ALBUMS -> listOf(SearchFilterType.ALBUMS)
            SearchFilterType.ARTISTS -> listOf(SearchFilterType.ARTISTS)
            SearchFilterType.PLAYLISTS -> listOf(SearchFilterType.PLAYLISTS)
        }
    }

    val topSpotlightItem = remember(results, currentFilter) {
        if (currentFilter == SearchFilterType.ALL) results.firstOrNull() else null
    }

    val lazyListState = rememberLazyListState()
    val isMotionBlurEnabled by playerViewModel.userPreferencesRepository.uiMotionBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val localDensity = LocalDensity.current

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsNumber > 0 && lastVisibleItemIndex >= totalItemsNumber - 5
        }.distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) {
                    playerViewModel.loadMoreSearch()
                }
            }
    }

    val imePadding = WindowInsets.ime.getBottom(localDensity).dp

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            )
            .scrollMotionBlur(lazyListState, enabled = isMotionBlurEnabled),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = if (imePadding <= 8.dp) bottomContentPadding else imePadding + 24.dp
        )
    ) {
        if (topSpotlightItem != null) {
            item(key = "top_result_spotlight") {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    SearchResultSectionHeader(title = "Top Result")
                    SearchTopResultSpotlightCard(
                        item = topSpotlightItem,
                        onPlaySong = { song -> onSongResultClick(song) },
                        onSongMoreOptions = onSongMoreOptionsClick,
                        onOpenArtist = { artist ->
                            val targetId = artist.channelId ?: artist.id.toString()
                            navController.navigateSafelyReplacing(
                                route = Screen.ArtistDetail.createRoute(targetId),
                                patternToPop = Screen.ArtistDetail.route
                            )
                            onItemSelected()
                        },
                        onPlayArtist = { artist ->
                            playerViewModel.playArtist(artist)
                            onItemSelected()
                        },
                        onOpenAlbum = { album ->
                            navController.navigateSafelyReplacing(
                                route = Screen.AlbumDetail.createRoute(album.id),
                                patternToPop = Screen.AlbumDetail.route
                            )
                            onItemSelected()
                        },
                        onPlayAlbum = { album ->
                            playerViewModel.playAlbum(album)
                            onItemSelected()
                        },
                        onOpenPlaylist = { playlist ->
                            navController.navigateSafely(Screen.PlaylistDetail.createRoute(playlist.id))
                            onItemSelected()
                        }
                    )
                }
            }
        }

        sectionOrder.forEach { filterType ->
            val itemsForSection = groupedResults[filterType] ?: emptyList()

            if (itemsForSection.isNotEmpty()) {
                item(key = "header_${filterType.name}") {
                    SearchResultSectionHeader(
                        title = when (filterType) {
                            SearchFilterType.SONGS -> "Songs"
                            SearchFilterType.VIDEOS -> "Videos"
                            SearchFilterType.ALBUMS -> "Albums"
                            SearchFilterType.ARTISTS -> "Artists"
                            SearchFilterType.PLAYLISTS -> "Playlists"
                            else -> "Results"
                        },
                        count = itemsForSection.size
                    )
                }

                items(
                    count = itemsForSection.size,
                    key = { index ->
                        val item = itemsForSection[index]
                        when (item) {
                            is SearchResultItem.SongItem -> "song_${item.song.id}_$index"
                            is SearchResultItem.AlbumItem -> "album_${item.album.id}_$index"
                            is SearchResultItem.ArtistItem -> "artist_${item.artist.id}_$index"
                            is SearchResultItem.PlaylistItem -> "playlist_${item.playlist.id}_$index"
                        }
                    },
                    contentType = { index ->
                        when (itemsForSection[index]) {
                            is SearchResultItem.SongItem -> "search_song"
                            is SearchResultItem.AlbumItem -> "search_album"
                            is SearchResultItem.ArtistItem -> "search_artist"
                            is SearchResultItem.PlaylistItem -> "search_playlist"
                        }
                    }
                ) { index ->
                    val item = itemsForSection[index]
                    Box(modifier = Modifier.padding(bottom = 10.dp)) {
                        when (item) {
                            is SearchResultItem.SongItem -> {
                                SwipeActionWrapper(
                                    onSwipeRight = {
                                        playerViewModel.addSongNextToQueue(item.song)
                                        playerViewModel.sendToast("Added to Play Next")
                                    },
                                    onSwipeLeft = {
                                        playerViewModel.addSongToQueue(item.song)
                                        playerViewModel.sendToast("Added to Queue")
                                    }
                                ) {
                                    EnhancedSongListItem(
                                        song = item.song,
                                        isPlaying = isPlaying,
                                        isCurrentSong = currentPlayingSongId == item.song.id,
                                        onMoreOptionsClick = onSongMoreOptionsClick,
                                        onClick = { onSongResultClick(item.song) }
                                    )
                                }
                            }

                            is SearchResultItem.AlbumItem -> {
                                val onPlayClick = remember(item.album, playerViewModel, onItemSelected) {
                                    {
                                        Timber.tag("SearchScreen").d("Album clicked: ${item.album.title}")
                                        playerViewModel.playAlbum(item.album)
                                        onItemSelected()
                                    }
                                }
                                val onOpenClick = remember(item.album, playerViewModel, onItemSelected) {
                                    {
                                        navController.navigateSafelyReplacing(
                                            route = Screen.AlbumDetail.createRoute(item.album.id),
                                            patternToPop = Screen.AlbumDetail.route
                                        )
                                        onItemSelected()
                                    }
                                }
                                SearchResultAlbumItem(
                                    album = item.album,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
                                )
                            }

                            is SearchResultItem.ArtistItem -> {
                                val onPlayClick = remember(item.artist, playerViewModel, onItemSelected) {
                                    {
                                        Timber.tag("SearchScreen").d("Artist clicked: ${item.artist.name}")
                                        playerViewModel.playArtist(item.artist)
                                        onItemSelected()
                                    }
                                }
                                val onOpenClick = remember(item.artist, playerViewModel, onItemSelected) {
                                    {
                                        val targetId = item.artist.channelId ?: item.artist.id.toString()
                                        navController.navigateSafelyReplacing(
                                            route = Screen.ArtistDetail.createRoute(targetId),
                                            patternToPop = Screen.ArtistDetail.route
                                        )
                                        onItemSelected()
                                    }
                                }
                                SearchResultArtistItem(
                                    artist = item.artist,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
                                )
                            }

                            is SearchResultItem.PlaylistItem -> {
                                val playlistSongs by remember(item.playlist.songIds, playerViewModel) {
                                    playerViewModel.observeSongs(item.playlist.songIds)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())
                                val coroutineScope = rememberCoroutineScope()
                                val onPlayClick: () -> Unit = {
                                    val playlistId = item.playlist.id
                                    coroutineScope.launch {
                                        if (playlistId.startsWith("PL") || playlistId.startsWith("VL") || playlistId.toLongOrNull() == null) {
                                            val ytPlaylistResult = withContext(Dispatchers.IO) {
                                                saurav.shru.pixelmusic.innertube.YouTube.playlist(playlistId)
                                            }
                                            if (ytPlaylistResult.isSuccess) {
                                                val ytPlaylistPage = ytPlaylistResult.getOrThrow()
                                                val firstPageSongs = ytPlaylistPage.songs.map { it.toNativeSong() }

                                                if (firstPageSongs.isNotEmpty()) {
                                                    playerViewModel.insertYoutubeSongs(firstPageSongs)
                                                    playerViewModel.playSongs(
                                                        firstPageSongs,
                                                        firstPageSongs.first(),
                                                        item.playlist.name
                                                    )
                                                    if (searchIsShuffleEnabled) playerViewModel.toggleShuffle()
                                                    onItemSelected()

                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        var continuation = ytPlaylistPage.songsContinuation ?: ytPlaylistPage.continuation
                                                        var pages = 0
                                                        while (continuation != null && pages < 10) {
                                                            val contResult = saurav.shru.pixelmusic.innertube.YouTube.playlistContinuation(continuation)
                                                            if (contResult.isSuccess) {
                                                                val contPage = contResult.getOrThrow()
                                                                val contSongs = contPage.songs.map { it.toNativeSong() }
                                                                if (contSongs.isNotEmpty()) {
                                                                    playerViewModel.insertYoutubeSongs(contSongs)
                                                                    withContext(Dispatchers.Main) {
                                                                        contSongs.forEach { playerViewModel.addSongToQueue(it) }
                                                                    }
                                                                }
                                                                continuation = contPage.continuation
                                                                pages++
                                                            } else {
                                                                break
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    playerViewModel.sendToast("Empty playlist")
                                                    onItemSelected()
                                                }
                                            } else {
                                                playerViewModel.sendToast("Failed to fetch playlist")
                                                onItemSelected()
                                            }
                                        } else {
                                            val songs = playerViewModel.getSongs(item.playlist.songIds)
                                            if (songs.isNotEmpty()) {
                                                playerViewModel.playSongs(songs, songs.first(), item.playlist.name)
                                                if (searchIsShuffleEnabled) playerViewModel.toggleShuffle()
                                            } else {
                                                playerViewModel.sendToast("Empty playlist")
                                            }
                                            onItemSelected()
                                        }
                                    }
                                }
                                val onOpenClick = remember(item.playlist, playerViewModel, onItemSelected) {
                                    {
                                        navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.playlist.id))
                                        onItemSelected()
                                    }
                                }
                                SearchResultPlaylistItem(
                                    playlist = item.playlist,
                                    playlistSongs = playlistSongs,
                                    onPlayClick = onPlayClick,
                                    onOpenClick = onOpenClick
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
private fun SearchTopResultSpotlightCard(
    item: SearchResultItem,
    onPlaySong: (Song) -> Unit,
    onSongMoreOptions: (Song) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onPlayArtist: (Artist) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onPlayAlbum: (Album) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 28.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTR = 28.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 28.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 28.dp, smoothnessAsPercentTL = 60
        )
    }

    val (title, subtitle, badgeLabel, artUri, isCircleArt, onPrimaryClick, onPlayAction) = when (item) {
        is SearchResultItem.SongItem -> SpotlightData(
            title = item.song.title,
            subtitle = item.song.artist,
            badgeLabel = "Song",
            artUri = item.song.albumArtUriString,
            isCircleArt = false,
            onPrimaryClick = { onPlaySong(item.song) },
            onPlayAction = { onPlaySong(item.song) }
        )
        is SearchResultItem.ArtistItem -> SpotlightData(
            title = item.artist.name,
            subtitle = "Artist",
            badgeLabel = "Artist",
            artUri = item.artist.effectiveImageUrl,
            isCircleArt = true,
            onPrimaryClick = { onOpenArtist(item.artist) },
            onPlayAction = { onPlayArtist(item.artist) }
        )
        is SearchResultItem.AlbumItem -> SpotlightData(
            title = item.album.title,
            subtitle = item.album.artist,
            badgeLabel = "Album",
            artUri = item.album.albumArtUriString,
            isCircleArt = false,
            onPrimaryClick = { onOpenAlbum(item.album) },
            onPlayAction = { onPlayAlbum(item.album) }
        )
        is SearchResultItem.PlaylistItem -> SpotlightData(
            title = item.playlist.name,
            subtitle = "Playlist",
            badgeLabel = "Playlist",
            artUri = item.playlist.coverImageUri,
            isCircleArt = false,
            onPrimaryClick = { onOpenPlaylist(item.playlist) },
            onPlayAction = { onOpenPlaylist(item.playlist) }
        )
    }

    Surface(
        onClick = onPrimaryClick,
        shape = cardShape,
        color = colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.primaryContainer.copy(alpha = 0.38f),
                            colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartImage(
                model = artUri,
                contentDescription = title,
                targetSize = SmartImageListTargetSize,
                modifier = Modifier
                    .size(72.dp)
                    .clip(if (isCircleArt) CircleShape else RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FilledIconButton(
                onClick = onPlayAction,
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play $title",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

private data class SpotlightData(
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val artUri: String?,
    val isCircleArt: Boolean,
    val onPrimaryClick: () -> Unit,
    val onPlayAction: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultAlbumItem(
    album: Album,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp, smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick,
        shape = itemShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartImage(
                model = album.albumArtUriString,
                contentDescription = "Album Art: ${album.title}",
                targetSize = SmartImageListTargetSize,
                modifier = Modifier.size(56.dp).clip(itemShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.cd_play_album), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultArtistItem(
    artist: Artist,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp, smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick,
        shape = itemShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!artist.effectiveImageUrl.isNullOrBlank()) {
                SmartImage(
                    model = artist.effectiveImageUrl,
                    contentDescription = "Artist: ${artist.name}",
                    targetSize = SmartImageListTargetSize,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_artist_24),
                    contentDescription = "Artist",
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val artistSubtitle = if (artist.songCount > 0) {
                    formatSongCount(artist.songCount)
                } else {
                    "Artist"
                }
                Text(
                    text = artistSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Artist", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultPlaylistItem(
    playlist: Playlist,
    playlistSongs: List<Song>,
    onOpenClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val itemShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp, smoothnessAsPercentBR = 60,
            cornerRadiusBR = 26.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBL = 26.dp, smoothnessAsPercentTL = 60
        )
    }

    Card(
        onClick = onOpenClick,
        shape = itemShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCover(
                playlist = playlist,
                playlistSongs = playlistSongs,
                size = 56.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val subtitleText = if (playlist.source == "YOUTUBE" && playlist.songIds.isEmpty()) {
                    "YouTube Playlist"
                } else {
                    formatSongCount(playlist.songIds.size)
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Playlist", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchFilterChip(
    filterType: SearchFilterType,
    currentFilter: SearchFilterType,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val selected = filterType == currentFilter

    FilterChip(
        selected = selected,
        onClick = { playerViewModel.updateSearchFilter(filterType) },
        label = {
            Text(
                text = filterType.name.lowercase().replaceFirstChar { it.titlecase() },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(width = 0.dp, color = Color.Transparent),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.rounded_check_circle_24),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else null
    )
}

@Composable
fun SwipeActionWrapper(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 88.dp.toPx() } }
    val colorScheme = MaterialTheme.colorScheme
    val isDragging = abs(dragOffset.value) > 2f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                when {
                    dragOffset.value > 2f -> colorScheme.primaryContainer
                    dragOffset.value < -2f -> colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
    ) {
        if (isDragging && dragOffset.value > 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play Next",
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play Next",
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else if (isDragging && dragOffset.value < 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Add to Queue",
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = "Add to Queue",
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val finalOffset = dragOffset.value
                                if (finalOffset > thresholdPx) onSwipeRight()
                                else if (finalOffset < -thresholdPx) onSwipeLeft()
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffset.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val currentOffset = dragOffset.value
                                val newOffset = currentOffset + dragAmount
                                val dampenedOffset = if (newOffset > thresholdPx) {
                                    thresholdPx + (newOffset - thresholdPx) * 0.28f
                                } else if (newOffset < -thresholdPx) {
                                    -thresholdPx + (newOffset + thresholdPx) * 0.28f
                                } else {
                                    newOffset
                                }
                                dragOffset.snapTo(dampenedOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
