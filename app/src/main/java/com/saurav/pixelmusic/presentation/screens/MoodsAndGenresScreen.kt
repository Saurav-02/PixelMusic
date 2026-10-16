@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
package com.saurav.pixelmusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saurav.shru.pixelmusic.innertube.YouTube
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.navigation.Screen
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

data class MoodCategoryItem(
    val name: String,
    val color: Color,
    val browseId: String = "FEmusic_moods_and_genre_category",
    val params: String? = null
)

private val FOR_YOU_MOODS = listOf(
    MoodCategoryItem("Romance", Color(0xFFE53935)),
    MoodCategoryItem("Feel good", Color(0xFF43A047)),
    MoodCategoryItem("Hindi", Color(0xFFFDD835)),
    MoodCategoryItem("Party", Color(0xFF8E24AA)),
    MoodCategoryItem("Desi hip-hop", Color(0xFFFB8C00)),
    MoodCategoryItem("Commute", Color(0xFFFDD835))
)

private val MOODS_AND_MOMENTS = listOf(
    MoodCategoryItem("Chill", Color(0xFF1E88E5)),
    MoodCategoryItem("Commute", Color(0xFFFBC02D)),
    MoodCategoryItem("Energize", Color(0xFFFDD835)),
    MoodCategoryItem("Feel good", Color(0xFF43A047)),
    MoodCategoryItem("Focus", Color(0xFF00897B)),
    MoodCategoryItem("Gaming", Color(0xFF3949AB)),
    MoodCategoryItem("Party", Color(0xFF8E24AA)),
    MoodCategoryItem("Romance", Color(0xFFE53935)),
    MoodCategoryItem("Sad", Color(0xFF5E35B1)),
    MoodCategoryItem("Sleep", Color(0xFF546E7A))
)

private val GENRES = listOf(
    MoodCategoryItem("Bollywood", Color(0xFFFBC02D)),
    MoodCategoryItem("Pop", Color(0xFFD81B60)),
    MoodCategoryItem("Hip-Hop", Color(0xFFF4511E)),
    MoodCategoryItem("Rock", Color(0xFFD32F2F)),
    MoodCategoryItem("Electronic", Color(0xFF00ACC1)),
    MoodCategoryItem("Classical", Color(0xFF6D4C41)),
    MoodCategoryItem("Jazz", Color(0xFFFFB300)),
    MoodCategoryItem("Folk", Color(0xFF7CB342)),
    MoodCategoryItem("Metal", Color(0xFF424242)),
    MoodCategoryItem("Punjabi", Color(0xFFFB8C00)),
    MoodCategoryItem("Tamil", Color(0xFF1E88E5)),
    MoodCategoryItem("Telugu", Color(0xFF00897B)),
    MoodCategoryItem("Bengali", Color(0xFF00897B)),
    MoodCategoryItem("Indie", Color(0xFF43A047))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodsAndGenresScreen(
    navController: NavController,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()
    var remoteSections by remember { mutableStateOf<List<YouTube.MoodsAndGenresSection>?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                YouTube.moodsAndGenres().getOrNull()
            }
            if (!res.isNullOrEmpty()) {
                remoteSections = res
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .scrollMotionBlur(listState, enabled = true),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = navBarBottom + MiniPlayerHeight + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
                        text = "Moods & genres",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val sections = remoteSections
            if (!sections.isNullOrEmpty()) {
                sections.forEachIndexed { sIndex, section ->
                    item(key = "remote_section_title_$sIndex") {
                        Text(
                            text = section.title,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    val mappedItems = section.items.map { item ->
                        val color = item.stripeColor?.let { Color(it.toInt()) }
                            ?: when (sIndex % 4) {
                                0 -> Color(0xFFE53935)
                                1 -> Color(0xFF43A047)
                                2 -> Color(0xFFFDD835)
                                else -> Color(0xFF8E24AA)
                            }
                        MoodCategoryItem(
                            name = item.title,
                            color = color,
                            browseId = item.browseId,
                            params = item.params
                        )
                    }

                    item(key = "remote_section_grid_$sIndex") {
                        MoodGridSection(
                            items = mappedItems,
                            onItemClick = { mood ->
                                navController.navigateSafely(
                                    Screen.MoodDetail.createRoute(
                                        title = mood.name,
                                        browseId = mood.browseId,
                                        params = mood.params
                                    )
                                )
                            }
                        )
                    }
                }
            } else {
                // Fallback to presets while loading or offline
                item(key = "for_you_title") {
                    Text(
                        text = "For you",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item(key = "for_you_grid") {
                    MoodGridSection(
                        items = FOR_YOU_MOODS,
                        onItemClick = { mood ->
                            navController.navigateSafely(
                                Screen.MoodDetail.createRoute(
                                    title = mood.name,
                                    browseId = mood.browseId,
                                    params = mood.params
                                )
                            )
                        }
                    )
                }

                item(key = "moods_moments_title") {
                    Text(
                        text = "Moods & moments",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item(key = "moods_moments_grid") {
                    MoodGridSection(
                        items = MOODS_AND_MOMENTS,
                        onItemClick = { mood ->
                            navController.navigateSafely(
                                Screen.MoodDetail.createRoute(
                                    title = mood.name,
                                    browseId = mood.browseId,
                                    params = mood.params
                                )
                            )
                        }
                    )
                }

                item(key = "genres_title") {
                    Text(
                        text = "Genres",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item(key = "genres_grid") {
                    MoodGridSection(
                        items = GENRES,
                        onItemClick = { mood ->
                            navController.navigateSafely(
                                Screen.MoodDetail.createRoute(
                                    title = mood.name,
                                    browseId = mood.browseId,
                                    params = mood.params
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodGridSection(
    items: List<MoodCategoryItem>,
    onItemClick: (MoodCategoryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pairs = items.chunked(2)
        pairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { mood ->
                    MoodPillCard(
                        mood = mood,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(mood) }
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodPillCard(
    mood: MoodCategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored indicator bar (matches screenshot 249612.jpg)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(mood.color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = mood.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
