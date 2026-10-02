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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.QuickPicksDisplayMode

private val QuickPicksPillHeight = 56.dp
private val QuickPicksPillSpacing = 8.dp
private const val QuickPicksPillsPerColumn = 3
private const val QuickPicksLimit = 48
private val QuickPicksPillArtSize = 36.dp
private val QuickPicksWidthSteps = listOf(148.dp, 166.dp, 184.dp, 202.dp, 220.dp)

private data class QuickPicksPillCell(val song: Song, val width: Dp)
private data class QuickPicksPillRow(val pills: List<QuickPicksPillCell>, val contentWidth: Dp)

@Composable
fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    currentSongId: String? = null,
    displayMode: QuickPicksDisplayMode = QuickPicksDisplayMode.LIST,
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
                FilledIconButton(
                    modifier = Modifier
                        .height(40.dp)
                        .width(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    onClick = onSeeAllClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "See all quick picks",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        if (displayMode == QuickPicksDisplayMode.CARD) {
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
                            onClick = { onSongClick(song) }
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
                                onClick = { onSongClick(song) }
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
                                onClick = { onSongClick(cell.song) }
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
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var visibilityFactor by remember { mutableFloatStateOf(1f) }

    val targetBg = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "QuickPickBg"
    )

    val cardCornerRadius = lerp(16.dp, 60.dp, 1f - visibilityFactor)
    val imageCornerRadius = lerp(12.dp, 56.dp, 1f - visibilityFactor)
    val contentScaleFactor = 0.90f + (0.10f * visibilityFactor)

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .padding(bottom = 8.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val cardCenterX = bounds.center.x
                val cardCenterY = bounds.center.y

                val hMargin = screenWidthPx * 0.20f
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
                scaleX = contentScaleFactor
                scaleY = contentScaleFactor
                alpha = 0.5f + (0.5f * visibilityFactor)
            },
        shape = RoundedCornerShape(cardCornerRadius),
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
                shape = RoundedCornerShape(imageCornerRadius),
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
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var visibilityFactor by remember { mutableFloatStateOf(1f) }

    val targetBg = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "QuickPickBg"
    )

    val cardCornerRadius = lerp(QuickPicksPillHeight / 2, 60.dp, 1f - visibilityFactor)
    val contentScaleFactor = 0.90f + (0.10f * visibilityFactor)

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(QuickPicksPillHeight)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val cardCenterX = bounds.center.x
                val cardCenterY = bounds.center.y

                val hMargin = screenWidthPx * 0.15f
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
                scaleX = contentScaleFactor
                scaleY = contentScaleFactor
                alpha = 0.5f + (0.5f * visibilityFactor)
            },
        shape = RoundedCornerShape(cardCornerRadius),
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
