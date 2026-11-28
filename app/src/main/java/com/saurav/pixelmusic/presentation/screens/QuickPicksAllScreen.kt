@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saurav.pixelmusic.presentation.screens

import com.saurav.pixelmusic.presentation.components.expressiveTapColorWave

import com.saurav.pixelmusic.ui.blur.ProgressiveStatusBarHeaderBackdrop
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.blur.rememberGlassState

import android.content.res.Configuration
import com.saurav.pixelmusic.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.presentation.components.ExpressiveHeroCollageHeader
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.ExpressiveGroupedCardSpacing
import com.saurav.pixelmusic.presentation.components.expressiveGroupedCardShape
import com.saurav.pixelmusic.presentation.components.expressiveListItemScrollAnimation
import com.saurav.pixelmusic.presentation.components.subcomps.EnhancedSongListItem
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.presentation.viewmodel.QuickPicksViewModel
import com.saurav.pixelmusic.presentation.viewmodel.PlaylistViewModel
import com.saurav.pixelmusic.presentation.components.SongInfoBottomSheet
import com.saurav.pixelmusic.presentation.components.PlaylistBottomSheet
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun QuickPicksAllScreen(
    playerViewModel: PlayerViewModel,
    navController: NavController,
    quickPicksViewModel: QuickPicksViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val songs by quickPicksViewModel.quickPicks.collectAsStateWithLifecycle()
    val isLoading by quickPicksViewModel.isLoading.collectAsStateWithLifecycle()
    val selectedCategory by quickPicksViewModel.selectedCategory.collectAsStateWithLifecycle()
    val categories by quickPicksViewModel.categories.collectAsStateWithLifecycle()
    val isMotionBlurEnabled by playerViewModel.userPreferencesRepository.uiMotionBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val listState = rememberLazyListState()

    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var playlistSheetSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val bottomBarHeightDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val bgColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
        MaterialTheme.colorScheme.surface
    )
    val backgroundBrush = androidx.compose.runtime.remember { Brush.verticalGradient(colors = bgColors, endY = 900f) }
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val subtitleText = when {
        isLoading -> "Fetching from YouTube..."
        songs.isNotEmpty() -> "${songs.size} songs from YouTube"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val quickPicksGlassState = rememberGlassState()
        val qpHeaderContent: @Composable (Dp, Float, Float) -> Unit = { headerH, pOffset, cAlpha ->
            ExpressiveHeroCollageHeader(
                title = "Quick Picks",
                subtitle = subtitleText,
                songs = songs,
                headerHeight = headerH,
                parallaxOffset = pOffset,
                collageAlpha = cAlpha,
                trailingAction = {
                    IconButton(
                        onClick = { quickPicksViewModel.refresh() },
                        modifier = Modifier.size(34.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                ExpressiveHeroCollageHeader(
                    title = "Quick Picks",
                    subtitle = subtitleText,
                    songs = songs,
                    isLandscapeFullHeight = true,
                    bottomContentPadding = MiniPlayerHeight + navBarPadding + 20.dp,
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    trailingAction = {
                        IconButton(
                            onClick = { quickPicksViewModel.refresh() },
                            modifier = Modifier.size(34.dp),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(if (isLandscape) 0.55f else 1f)
                    .fillMaxHeight()
                    .glassRoot(quickPicksGlassState)
                    .scrollMotionBlur(listState, enabled = isMotionBlurEnabled),
                contentPadding = PaddingValues(
                    top = if (isLandscape) statusBarTopPadding + 12.dp else 0.dp,
                    bottom = MiniPlayerHeight + navBarPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(com.saurav.pixelmusic.presentation.components.ExpressiveGroupedCardSpacing)
            ) {
                if (!isLandscape) {
                    // Expressive Hero Collage Header
                    item(key = "qp_header") {
                        val parallaxOffset by remember {
                            derivedStateOf {
                                if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset * 0.45f else 0f
                            }
                        }
                        val collageAlpha by remember {
                            derivedStateOf {
                                (1f - (listState.firstVisibleItemScrollOffset / 520f)).coerceIn(0f, 1f)
                            }
                        }
                        qpHeaderContent(330.dp, parallaxOffset, collageAlpha)
                    }
                }

                // Master action pill (at top of right-side song list in landscape, below hero in portrait)
                if (songs.isNotEmpty()) {
                    item(key = "qp_actions") {
                        QuickPicksActions(
                            onPlay = {
                                val first = songs.firstOrNull() ?: return@QuickPicksActions
                                playerViewModel.playSongs(songs, first, "Quick Picks")
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

            // Category chips
            item(key = "qp_chips") {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides 0.dp
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(categories, key = { it }) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { quickPicksViewModel.setCategory(category) },
                                label = {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = CircleShape,
                                elevation = FilterChipDefaults.filterChipElevation(elevation = 0.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = Color.Transparent,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                item(key = "qp_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Songs
            if (!isLoading) {
                items(count = songs.size, key = { songs[it].id }) { index ->
                    val song = songs[index]
                    EnhancedSongListItem(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp)
                            .expressiveListItemScrollAnimation(listState),
                        song = song,
                        isCurrentSong = currentSongId == song.id,
                        isPlaying = currentSongId == song.id && isPlaying,
                        customShape = com.saurav.pixelmusic.presentation.components.expressiveGroupedCardShape(index, songs.size),
                        onClick = {
                            playerViewModel.showAndPlaySong(song, songs, "Quick Picks")
                        },
                        onMoreOptionsClick = { clickedSong ->
                            playerViewModel.selectSongForInfo(clickedSong)
                            showSongInfoBottomSheet = true
                        }
                    )
                }
            }
            }
        }

        ProgressiveStatusBarHeaderBackdrop(
            extraHeight = 64.dp,
            scrimColor = MaterialTheme.colorScheme.surface,
            glassState = quickPicksGlassState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Floating Back button over Hero Header
        FilledTonalIconButton(
            onClick = { navController.popBackStack() },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 14.dp, top = 8.dp)
                .size(42.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
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
                    if (songs.isNotEmpty()) {
                        playerViewModel.playSongs(songs, song, "Quick Picks")
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
            PlaylistBottomSheet(
                playlistUiState = playlistUiState,
                songs = playlistSheetSongs,
                onDismiss = { showPlaylistBottomSheet = false },
                bottomBarHeight = bottomBarHeightDp,
                playerViewModel = playerViewModel,
            )
        }
    }
}

@Composable
private fun QuickPicksAllHeader(songCount: Int, isLoading: Boolean, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Picks",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (!isLoading && songCount > 0) {
                Text(
                    text = "$songCount songs from YouTube",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isLoading) {
                Text(
                    text = "Fetching from YouTube...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickPicksActions(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playShape = CircleShape

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPlay,
            modifier = Modifier
                .fillMaxSize()
                .expressiveTapColorWave(
                    waveColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
                    shape = playShape
                ),
            shape = playShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation()
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_play_arrow_filled_24),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Play All")
        }
    }
}
