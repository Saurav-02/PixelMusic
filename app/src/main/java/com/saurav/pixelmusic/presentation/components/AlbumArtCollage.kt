package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.CollagePattern
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
data class Config(val size: Dp, val width: Dp, val height: Dp, val align: Alignment, val rot: Float, val shape: Shape, val offsetX: Dp, val offsetY: Dp)

/**
 * Muestra hasta 6 portadas en un layout de collage con formas simplificadas y redondeadas.
 * Las formas se dividen en dos grupos (superior e inferior) para evitar superposición.
 * Incluye una píldora central, círculo, squircle y estrella, con disposición ajustada.
 * Ajusta tamaños, rotaciones y posiciones para crear un look dinámico.
 */
@Composable
fun AlbumArtCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
    height: Dp = 400.dp,
    padding: Dp = 0.dp,
    pattern: CollagePattern = CollagePattern.default,
    onSongClick: (Song) -> Unit,
) {
    val songsToShow = remember(songs) {
        (songs.take(6) + List(6 - songs.size.coerceAtMost(6)) { null }).toImmutableList()
    }

    val contentHeight = remember(height, padding) { maxOf(0.dp, height - (padding * 2)) }
    val min = remember(height) { minOf(300.dp, height) }
    val shapeConfigs = remember(pattern, min, contentHeight) {
        buildCollageConfigs(pattern, min, contentHeight)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(padding)
    ) {
        if (shapeConfigs.isNotEmpty()) {
            val (topConfigs, bottomConfigs) = remember(shapeConfigs) {
                shapeConfigs.take(3) to shapeConfigs.drop(3)
            }

            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(contentHeight * 0.6f)) {
                    topConfigs.forEachIndexed { idx, cfg ->
                        songsToShow.getOrNull(idx)?.let { song ->
                            val interactionSource = remember(song.id) { MutableInteractionSource() }
                            SmartImage(
                                model = song.albumArtUriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                crossfadeDurationMillis = 0,
                                modifier = Modifier
                                    .size(cfg.width, cfg.height)
                                    .align(cfg.align)
                                    .offset(cfg.offsetX, cfg.offsetY)
                                    .graphicsLayer(
                                        rotationZ = cfg.rot,
                                        clip = true,
                                        shape = cfg.shape
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onSongClick(song) }
                                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(contentHeight * 0.4f)) {
                    bottomConfigs.forEachIndexed { j, cfg ->
                        val idx = j + 3
                        songsToShow.getOrNull(idx)?.let { song ->
                            val interactionSource = remember(song.id) { MutableInteractionSource() }
                            SmartImage(
                                model = song.albumArtUriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                crossfadeDurationMillis = 0,
                                modifier = Modifier
                                    .size(cfg.width, cfg.height)
                                    .align(cfg.align)
                                    .offset(cfg.offsetX, cfg.offsetY)
                                    .graphicsLayer(
                                        rotationZ = cfg.rot,
                                        clip = true,
                                        shape = cfg.shape
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onSongClick(song) }
                                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.rounded_music_note_24),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}
