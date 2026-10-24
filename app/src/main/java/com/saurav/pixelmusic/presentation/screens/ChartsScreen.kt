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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.presentation.components.liquidGlassInteractiveSurface
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import com.saurav.pixelmusic.presentation.components.LiquidGlassDropdownMenu
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.pages.ChartsPage
import com.saurav.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality

private val CHART_COUNTRIES = listOf(
    "India" to "IN",
    "Global" to null,
    "United States" to "US",
    "United Kingdom" to "GB",
    "Japan" to "JP",
    "South Korea" to "KR",
    "Germany" to "DE",
    "France" to "FR",
    "Brazil" to "BR",
    "Canada" to "CA",
    "Australia" to "AU"
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var chartsPage by remember { mutableStateOf<ChartsPage?>(null) }
    var selectedCountryName by remember { mutableStateOf("India") }
    var selectedCountryCode by remember { mutableStateOf<String?>("IN") }
    var showCountryMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying = stablePlayerState?.isPlaying == true
    val currentSongId = stablePlayerState?.currentSong?.id
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    fun loadCharts(countryCode: String?, refresh: Boolean = false) {
        scope.launch {
            if (refresh) isRefreshing = true else isLoading = true
            error = null
            val result = withContext(Dispatchers.IO) {
                YouTube.getChartsPage(countryCode).getOrNull()
            }
            if (result != null && result.sections.isNotEmpty()) {
                chartsPage = result
            } else if (result != null && result.sections.isEmpty()) {
                chartsPage = result
                error = "No charts available for this region"
            } else {
                error = "Failed to load charts"
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedCountryCode) {
        loadCharts(selectedCountryCode, refresh = false)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadCharts(selectedCountryCode, refresh = true) },
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
            if (isLoading && chartsPage == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (error != null && chartsPage == null) {
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
                    Button(onClick = { loadCharts(selectedCountryCode, refresh = false) }) {
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
                                text = "Charts",
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Country Selector Pill (matches screenshot 249611.jpg)
                    item(key = "country_selector") {
                        val isLiquidGlass = LocalLiquidGlassEnabled.current
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val countryPillInteractionSource = remember { MutableInteractionSource() }
                        val isCountryPillPressed by countryPillInteractionSource.collectIsPressedAsState()
                        val countryPillPress = remember { Animatable(0f) }
                        val countryPillJiggle = remember { Animatable(0f) }

                        LaunchedEffect(isCountryPillPressed) {
                            if (isCountryPillPressed) {
                                countryPillJiggle.snapTo(1f)
                                launch { countryPillJiggle.animateTo(0f, spring(0.32f, 280f)) }
                                countryPillPress.animateTo(1f, spring(0.5f, 450f))
                            } else {
                                countryPillPress.animateTo(0f, spring(0.5f, 450f))
                            }
                        }

                        val countryBorder = remember(isLiquidGlass, isDark) {
                            if (!isLiquidGlass) null
                            else BorderStroke(
                                0.75.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.12f else 0.22f),
                                        Color.White.copy(alpha = if (isDark) 0.03f else 0.06f)
                                    )
                                )
                            )
                        }

                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides 0.dp
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Surface(
                                    onClick = { showCountryMenu = true },
                                    interactionSource = countryPillInteractionSource,
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isLiquidGlass) {
                                        if (isDark) Color(0x24FFFFFF) else Color(0x55FFFFFF)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .liquidGlassInteractiveSurface(shape = RoundedCornerShape(18.dp))
                                        .clip(RoundedCornerShape(18.dp))
                                ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedCountryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Select country",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            LiquidGlassDropdownMenu(
                                expanded = showCountryMenu,
                                onDismissRequest = { showCountryMenu = false },
                                offset = DpOffset(0.dp, 4.dp)
                            ) {
                                CHART_COUNTRIES.forEach { (name, code) ->
                                    val isSelected = selectedCountryCode == code
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectedCountryName = name
                                            selectedCountryCode = code
                                            showCountryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                    // Render Sections from ChartsPage (matches screenshot 249611.jpg)
                    chartsPage?.sections?.forEachIndexed { index, section ->
                        item(key = "chart_section_${index}_header") {
                            Text(
                                text = section.title,
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        val allSongs = section.items.filterIsInstance<SongItem>()
                        if (allSongs.isNotEmpty() && allSongs.size == section.items.size) {
                            val songListNative = allSongs.map { it.toNativeSong() }
                            val columns = songListNative.chunked(4)
                            item(key = "chart_song_deck_$index") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(columns.size, key = { "chart_col_${index}_$it" }) { colIdx ->
                                        val colSongs = columns[colIdx]
                                        val isLiquidGlassSong = LocalLiquidGlassEnabled.current
                                        val isDarkSong = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                                        val songCardBorder = remember(isLiquidGlassSong, isDarkSong) {
                                            if (!isLiquidGlassSong) null
                                            else BorderStroke(
                                                0.75.dp,
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = if (isDarkSong) 0.08f else 0.16f),
                                                        Color.White.copy(alpha = if (isDarkSong) 0.02f else 0.04f)
                                                    )
                                                )
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.width(300.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            colSongs.forEachIndexed { rowIdx, nativeSong ->
                                                val rank = colIdx * 4 + rowIdx + 1
                                                Surface(
                                                    onClick = {
                                                        playerViewModel.showAndPlaySong(
                                                            song = nativeSong,
                                                            contextSongs = songListNative,
                                                            queueName = section.title
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isLiquidGlassSong) {
                                                        if (isDarkSong) Color(0x1EFFFFFF) else Color(0x45FFFFFF)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceContainerLow
                                                    },
                                                    border = songCardBorder
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = rank.toString(),
                                                            fontFamily = GoogleSansRounded,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.width(28.dp)
                                                        )
                                                        SmartImage(crossfadeDurationMillis = 0, 
                                                            model = nativeSong.albumArtUriString,
                                                            contentDescription = nativeSong.title,
                                                            contentScale = ContentScale.Crop,
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.size(48.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = nativeSong.title,
                                                                fontFamily = GoogleSansRounded,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = nativeSong.artist,
                                                                fontFamily = GoogleSansRounded,
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
                        } else {
                            item(key = "chart_section_${index}_items") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(section.items, key = { "chart_item_${it.id}_$index" }) { item ->
                                        when (item) {
                                            is SongItem -> {
                                                val nativeSong = item.toNativeSong()
                                                val isPlayingThis = currentSongId == nativeSong.id && isPlaying
                                                ChartCardItem(
                                                    title = item.title,
                                                    subtitle = item.artists.firstOrNull()?.name ?: "Song",
                                                    thumbnail = upgradeThumbnailUrlToHighQuality(item.thumbnail) ?: item.thumbnail,
                                                    isPlayingThis = isPlayingThis,
                                                    onClick = {
                                                        playerViewModel.showAndPlaySong(
                                                            song = nativeSong,
                                                            contextSongs = section.items.filterIsInstance<SongItem>().map { it.toNativeSong() },
                                                            queueName = section.title
                                                        )
                                                    },
                                                    onPlayPauseClick = {
                                                        if (isPlayingThis) {
                                                            playerViewModel.playPause()
                                                        } else {
                                                            playerViewModel.showAndPlaySong(
                                                                song = nativeSong,
                                                                contextSongs = section.items.filterIsInstance<SongItem>().map { it.toNativeSong() },
                                                                queueName = section.title
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                            is PlaylistItem -> {
                                                ChartCardItem(
                                                    title = item.title,
                                                    subtitle = item.author?.name ?: "Chart • YouTube Music",
                                                    thumbnail = upgradeThumbnailUrlToHighQuality(item.thumbnail) ?: item.thumbnail.orEmpty(),
                                                    onClick = {
                                                        navController.navigateSafely(Screen.PlaylistDetail.createRoute(item.id))
                                                    }
                                                )
                                            }
                                            is AlbumItem -> {
                                                ChartCardItem(
                                                    title = item.title,
                                                    subtitle = "Album • ${item.artists?.firstOrNull()?.name ?: ""}",
                                                    thumbnail = upgradeThumbnailUrlToHighQuality(item.thumbnail) ?: item.thumbnail,
                                                    onClick = {
                                                        navController.navigateSafely(Screen.AlbumDetail.createRoute(item.browseId))
                                                    }
                                                )
                                            }
                                            is ArtistItem -> {
                                                ArtistCardItem(
                                                    artist = item,
                                                    onClick = {
                                                        navController.navigateSafely(Screen.ArtistDetail.createRoute(item.id))
                                                    }
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
}

@Composable
private fun ChartCardItem(
    title: String,
    subtitle: String,
    thumbnail: String,
    isPlayingThis: Boolean = false,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit = onClick
) {
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.size(156.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SmartImage(crossfadeDurationMillis = 0, 
                    model = thumbnail,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Interactive play/pause button with liquid glass effect & spring physics
                Surface(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    color = if (isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else colorScheme.primary,
                    tonalElevation = 0.dp,
                    shadowElevation = if (isLiquidGlass) 0.dp else 2.dp,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
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
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
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
