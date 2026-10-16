@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.SmartImage
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReleasesScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var albums by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    fun loadReleases(refresh: Boolean = false) {
        scope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            error = null
            val result = withContext(Dispatchers.IO) {
                YouTube.newReleaseAlbums().getOrNull()
            }
            if (result != null) {
                albums = result
            } else {
                error = "Failed to load new releases"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        loadReleases(refresh = false)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadReleases(refresh = true) },
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
            if (isLoading && albums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (error != null && albums.isEmpty()) {
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
                    Button(onClick = { loadReleases(refresh = false) }) {
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
                        top = statusBarTop + 8.dp,
                        bottom = navBarBottom + MiniPlayerHeight + 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Bar
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
                                text = "New releases",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Featured Highlights Shelf (matches screenshot 249610.jpg)
                    if (albums.size >= 2) {
                        item(key = "hero_shelf") {
                            val hero1 = albums[0]
                            val hero2 = albums[1]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Featured 1: RELEASED Banner
                                Card(
                                    onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(hero1.browseId)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(210.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SmartImage(
                                            model = hero1.thumbnail,
                                            contentDescription = hero1.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                    )
                                                )
                                        )
                                        // Vertical "RELEASED" text pill
                                        Surface(
                                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                            color = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = "RELEASED",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = Color.Black,
                                                letterSpacing = 2.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = hero1.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = hero1.artists?.firstOrNull()?.name ?: "New Release",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Featured 2: New Release Mix
                                Card(
                                    onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(hero2.browseId)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(210.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SmartImage(
                                            model = hero2.thumbnail,
                                            contentDescription = hero2.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                    )
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "New Release Mix",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Mix • YouTube Music",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Title: Albums & singles >
                    item(key = "albums_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Albums & singles",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Horizontal Carousel: Albums & singles
                    item(key = "albums_carousel") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(albums.drop(2).take(12), key = { "album_h_${it.browseId}" }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable { navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId)) }
                                ) {
                                    SmartImage(
                                        model = album.thumbnail,
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(140.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = album.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Album • ${album.artists?.firstOrNull()?.name ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Section Title: More new releases
                    if (albums.size > 14) {
                        item(key = "more_header") {
                            Text(
                                text = "More to explore",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        items(albums.drop(14), key = { "album_more_${it.browseId}" }) { album ->
                            Surface(
                                onClick = { navController.navigateSafely(Screen.AlbumDetail.createRoute(album.browseId)) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SmartImage(
                                        model = album.thumbnail,
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = album.artists?.firstOrNull()?.name ?: "Album",
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
                }
            }
        }
    }
}
