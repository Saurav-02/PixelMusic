@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem

data class MoodSectionData(
    val title: String,
    val items: List<YTItem>
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDetailScreen(
    title: String,
    browseId: String = "FEmusic_moods_and_genre_category",
    params: String? = null,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var sections by remember { mutableStateOf<List<MoodSectionData>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    fun loadMoodData(refresh: Boolean = false) {
        scope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            error = null
            try {
                val data = withContext(Dispatchers.IO) {
                    val browseResult = if (browseId.isNotBlank()) {
                        YouTube.browse(browseId = browseId, params = params).getOrNull()
                    } else null

                    if (browseResult != null && browseResult.items.isNotEmpty()) {
                        browseResult.items.map { shelf ->
                            MoodSectionData(
                                title = shelf.title ?: title,
                                items = shelf.items
                            )
                        }
                    } else {
                        // Fallback: search ONLY for curated featured playlists
                        val searchPlaylists = YouTube.search(
                            "$title playlist",
                            YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST
                        ).getOrNull()?.items?.filterIsInstance<PlaylistItem>() ?: emptyList()

                        if (searchPlaylists.isNotEmpty()) {
                            listOf(MoodSectionData("$title Playlists", searchPlaylists))
                        } else emptyList()
                    }
                }
                sections = data
                if (data.isEmpty()) {
                    error = "No content available for $title"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to load $title"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(title, browseId, params) {
        loadMoodData(refresh = false)
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadMoodData(refresh = true) },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (isLoading && sections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (error != null && sections.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadMoodData(refresh = false) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollMotionBlur(listState, enabled = true),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = navBarBottom + MiniPlayerHeight + 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Header Bar (matches screenshot 249613.jpg: "< Romance")
                    item(key = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Render Sections (matches screenshot 249613.jpg)
                    sections.forEachIndexed { index, section ->
                        item(key = "mood_section_${index}_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = section.title,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item(key = "mood_section_${index}_carousel") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(section.items, key = { "mood_item_${it.id}_$index" }) { item ->
                                    when (item) {
                                        is PlaylistItem -> {
                                            Column(
                                                modifier = Modifier
                                                    .width(148.dp)
                                                    .clickable {
                                                        navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id))
                                                    }
                                            ) {
                                                SmartImage(
                                                    model = item.thumbnail,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.size(148.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.author?.name ?: "Playlist • YouTube Music",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        is AlbumItem -> {
                                            Column(
                                                modifier = Modifier
                                                    .width(148.dp)
                                                    .clickable {
                                                        navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId))
                                                    }
                                            ) {
                                                SmartImage(
                                                    model = item.thumbnail,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.size(148.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Album • ${item.artists?.firstOrNull()?.name ?: ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        is SongItem -> {
                                            val songNative = item.toNativeSong()
                                            Column(
                                                modifier = Modifier
                                                    .width(148.dp)
                                                    .clickable {
                                                        playerViewModel.showAndPlaySong(
                                                            song = songNative,
                                                            contextSongs = section.items.filterIsInstance<SongItem>().map { it.toNativeSong() },
                                                            queueName = section.title
                                                        )
                                                    }
                                            ) {
                                                SmartImage(
                                                    model = item.thumbnail,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.size(148.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.artists.firstOrNull()?.name ?: "Song",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        else -> {}
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
