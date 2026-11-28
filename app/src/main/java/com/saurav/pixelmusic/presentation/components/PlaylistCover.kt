package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.saurav.pixelmusic.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.model.PlaylistShapeType
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.utils.resolvePlaylistCoverContentColor
import com.saurav.pixelmusic.utils.shapes.RoundedStarShape
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun PlaylistCover(
    playlist: Playlist,
    playlistSongs: List<Song>,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val shape = remember(playlist.coverShapeType, playlist.coverShapeDetail1, playlist.coverShapeDetail2, playlist.coverShapeDetail3, size) {
        when (playlist.coverShapeType) {
            PlaylistShapeType.Circle.name -> CircleShape
            PlaylistShapeType.SmoothRect.name -> {
                val referenceSize = 200f
                val currentSize = size.value
                val scale = currentSize / referenceSize
                val r = ((playlist.coverShapeDetail1 ?: 20f) * scale).dp
                val s = (playlist.coverShapeDetail2 ?: 60f).toInt()
                AbsoluteSmoothCornerShape(r, s, r, s, r, s, r, s)
            }
            PlaylistShapeType.RotatedPill.name -> {
                androidx.compose.foundation.shape.GenericShape { size, _ ->
                    val w = size.width
                    val h = size.height
                    val pillW = w * 0.75f
                    val offset = (w - pillW) / 2
                    addRoundRect(RoundRect(offset, 0f, offset + pillW, h, CornerRadius(pillW/2, pillW/2)))
                }
            }
            PlaylistShapeType.Star.name -> RoundedStarShape(
                sides = (playlist.coverShapeDetail4 ?: 5f).toInt(),
                curve = (playlist.coverShapeDetail1 ?: 0.15f).toDouble(),
                rotation = playlist.coverShapeDetail2 ?: 0f
            )
            else -> RoundedCornerShape(8.dp)
        }
    }

    val shapeMod = if (playlist.coverShapeType == PlaylistShapeType.RotatedPill.name) Modifier.graphicsLayer(rotationZ = 45f) else Modifier
    val iconMod = if (playlist.coverShapeType == PlaylistShapeType.RotatedPill.name) Modifier.graphicsLayer(rotationZ = -45f) else Modifier
    val scaleMod = if (playlist.coverShapeType == PlaylistShapeType.Star.name) {
        val s = playlist.coverShapeDetail3 ?: 1f
        Modifier.graphicsLayer(scaleX = s, scaleY = s)
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(scaleMod)
            .then(shapeMod)
            .clip(shape)
    ) {
        val bwColorFilter: androidx.compose.ui.graphics.ColorFilter? = null

        if (!playlist.coverImageUri.isNullOrBlank()) {
            AsyncImage(
                model = if (playlist.coverImageUri.startsWith("/")) java.io.File(playlist.coverImageUri) else playlist.coverImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = bwColorFilter
            )
        } else if (playlist.coverColorArgb != null) {
            val baseColor = Color(playlist.coverColorArgb)
            val effectiveColor = baseColor
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(effectiveColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(getIconResByName(playlist.coverIconName)),
                    contentDescription = null,
                    tint = resolvePlaylistCoverContentColor(
                        playlist.coverColorArgb,
                        MaterialTheme.colorScheme
                    ),
                    modifier = Modifier.size(size / 2).then(iconMod)
                )
            }
        } else {
            PlaylistArtCollage(
                songs = playlistSongs,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun getIconResByName(name: String?): Int {
    return when (name) {
        "MusicNote" -> R.drawable.rounded_music_note_24
        "Headphones" -> R.drawable.rounded_headphones_24
        "Album" -> R.drawable.rounded_album_24
        "Mic" -> R.drawable.rounded_mic_external_on_24
        "Speaker" -> R.drawable.rounded_speaker_24
        "Favorite" -> R.drawable.rounded_favorite_24
        "Piano" -> R.drawable.rounded_piano_24
        "Queue" -> R.drawable.rounded_queue_music_24
        else -> R.drawable.rounded_music_note_24
    }
}
