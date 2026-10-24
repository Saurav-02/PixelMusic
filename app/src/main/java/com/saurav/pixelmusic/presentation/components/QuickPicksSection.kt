package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState as rememberM3CarouselState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import coil.size.Size
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.QuickPicksDisplayMode
import com.saurav.pixelmusic.data.preferences.QuickPicksSectionStyle
import com.saurav.pixelmusic.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.ui.graphics.luminance

private val QuickPicksPillHeight = 56.dp
private val QuickPicksPillSpacing = 8.dp
private const val QuickPicksPillsPerColumn = 3
private const val QuickPicksLimit = 15
private val QuickPicksPillArtSize = 36.dp
private val QuickPicksWidthSteps = listOf(148.dp, 166.dp, 184.dp, 202.dp, 220.dp)

private data class QuickPicksPillCell(val song: Song, val width: Dp)
private data class QuickPicksPillRow(val pills: List<QuickPicksPillCell>, val contentWidth: Dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    currentSongId: String? = null,
    displayMode: QuickPicksDisplayMode = QuickPicksDisplayMode.LIST,
    sectionStyle: QuickPicksSectionStyle = QuickPicksSectionStyle.CURRENT,
    onMoreOptionsClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val visible = remember(songs) {
        val count = (songs.size / 3) * 3
        songs.take(count.coerceAtMost(QuickPicksLimit))
    }
    val rows = remember(visible) { buildQuickPickRows(visible) }
    val scrollState = rememberScrollState()
    val actualRowsCount = rows.size
    val sectionHeight = if (actualRowsCount > 0) {
        QuickPicksPillHeight * actualRowsCount + QuickPicksPillSpacing * (actualRowsCount - 1)
    } else 0.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Picks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
            if (onSeeAllClick != null) {
                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlassInteractiveSurface(isCircle = true),
                    onClick = onSeeAllClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "See all quick picks",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        if (sectionStyle == QuickPicksSectionStyle.MATERIAL_YOU_EXPRESSIVE) {
            MaterialExpressiveQuickPicksCarousel(
                songs = songs,
                onSongClick = onSongClick,
                onMoreOptionsClick = onMoreOptionsClick,
                currentSongId = currentSongId
            )
        } else if (displayMode == QuickPicksDisplayMode.CARD) {
            val topScrollState = rememberScrollState()
            val bottomScrollState = rememberScrollState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(topScrollState)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    songs.take(10).forEach { song ->
                        QuickPickCard(
                            song = song,
                            isPlaying = song.id == currentSongId,
                            onClick = { onSongClick(song) },
                            onMoreOptionsClick = onMoreOptionsClick
                        )
                    }
                }

                val bottomRowSongs = songs.drop(10).take(10)
                if (bottomRowSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(bottomScrollState)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        bottomRowSongs.forEach { song ->
                            QuickPickCard(
                                song = song,
                                isPlaying = song.id == currentSongId,
                                onClick = { onSongClick(song) },
                                onMoreOptionsClick = onMoreOptionsClick
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sectionHeight)
                    .horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(QuickPicksPillSpacing)
            ) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(QuickPicksPillSpacing),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        row.pills.forEach { cell ->
                            QuickPickPill(
                                song = cell.song,
                                width = cell.width,
                                isPlaying = cell.song.id == currentSongId,
                                onClick = { onSongClick(cell.song) },
                                onMoreOptionsClick = onMoreOptionsClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialExpressiveQuickPicksCarousel(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onMoreOptionsClick: ((Song) -> Unit)? = null,
    currentSongId: String? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val carouselSongs = remember(songs) { songs.take(20) }
    val carouselState = rememberM3CarouselState { carouselSongs.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 260.dp,
        itemSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) { index ->
        val song = carouselSongs[index]
        val isPlaying = song.id == currentSongId

        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .combinedClickable(
                    onClick = { onSongClick(song) },
                    onLongClick = { onMoreOptionsClick?.invoke(song) }
                )
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                shape = RectangleShape,
                targetSize = Size(512, 512),
                isThumbnail = false,
                placeHolderBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxSize()
            )

            FilledTonalIconButton(
                onClick = { onMoreOptionsClick?.invoke(song) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(38.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.presentation_batch_g_list_cd_more_for_title, song.title),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
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
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isPlaying) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Now playing",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreOptionsClick: ((Song) -> Unit)? = null
) {
    val targetBg = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "QuickPickBg"
    )

    val cardShape = remember { RoundedCornerShape(16.dp) }
    val imageShape = remember { RoundedCornerShape(12.dp) }

    Card(
        modifier = Modifier
            .width(140.dp)
            .padding(bottom = 8.dp)
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onMoreOptionsClick?.invoke(song) }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val artUri = song.albumArtUriString
            SmartImage(
                model = artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = imageShape,
                targetSize = SmartImageListTargetSize,
                isThumbnail = true,
                modifier = Modifier
                    .size(124.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickPickPill(
    song: Song,
    width: Dp,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreOptionsClick: ((Song) -> Unit)? = null
) {
    val targetBg = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "QuickPickBg"
    )

    val pillShape = remember { RoundedCornerShape(QuickPicksPillHeight / 2) }

    Card(
        modifier = Modifier
            .width(width)
            .height(QuickPicksPillHeight)
            .clip(pillShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onMoreOptionsClick?.invoke(song) }
            ),
        shape = pillShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val artUri = song.albumArtUriString
            SmartImage(
                model = artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = CircleShape,
                targetSize = SmartImageCompactListTargetSize,
                isThumbnail = true,
                modifier = Modifier.size(QuickPicksPillArtSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun buildQuickPickRows(songs: List<Song>): List<QuickPicksPillRow> {
    val groups = songs.chunked(QuickPicksPillsPerColumn).take(QuickPicksLimit / QuickPicksPillsPerColumn)
    val columns = groups.mapIndexed { colIndex, group ->
        val widthStep = QuickPicksWidthSteps[colIndex % QuickPicksWidthSteps.size]
        group.map { QuickPicksPillCell(it, widthStep) }
    }
    val rows = mutableListOf<QuickPicksPillRow>()
    for (rowIdx in 0 until QuickPicksPillsPerColumn) {
        val pills = columns.mapNotNull { col -> col.getOrNull(rowIdx) }
        if (pills.isEmpty()) continue
        val totalWidth = pills.sumOf { it.width.value.toDouble() }.dp +
                QuickPicksPillSpacing * (pills.size - 1)
        rows.add(QuickPicksPillRow(pills, totalWidth))
    }
    return rows
}
