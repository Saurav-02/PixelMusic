package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.CollagePattern
import com.saurav.pixelmusic.utils.shapes.FluidBlobShape
import com.saurav.pixelmusic.utils.shapes.FluidBlobType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AlbumArtCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
    height: Dp = 440.dp,
    padding: Dp = 0.dp,
    @Suppress("UNUSED_PARAMETER") pattern: CollagePattern = CollagePattern.FLUID_BLOB,
    onSongClick: (Song) -> Unit,
) {
    FluidBlobCollage(
        songs = songs,
        modifier = modifier.padding(padding),
        height = height,
        onSongClick = onSongClick
    )
}

/**
 * Fluid Blob-Grid Overlay using the exact traced shapes & proportions from user specification:
 * - 832 x 1044 proportional coordinate space
 * - Shape 1 (Top-Left Blob): 405 x 421 at (55.5, 75.5)
 * - Shape 2 (Top-Right Blob): 308 x 343 at (486.5, 50.5)
 * - Shape 3 (Center Blob): 381 x 379 at (239.0, 304.5)
 * - Shape 4 (Right Circle): 180 x 179 at (611.5, 464.5)
 * - Shape 5 (Bottom-Left Blob): 384 x 349 at (72.5, 609.5)
 * - Shape 6 (Material You Cookie 7-sided): 237 x 235 at (512.0, 694.6)
 */
@Composable
fun FluidBlobCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
    height: Dp = 440.dp,
    onSongClick: (Song) -> Unit
) {
    val songsToShow = remember(songs) {
        if (songs.isEmpty()) {
            emptyList<Song>()
        } else {
            (0 until 6).map { songs[it % songs.size] }
        }
    }

    if (songsToShow.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_music_note_24),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalHeight = maxHeight
        val isWide = maxWidth > totalHeight * 1.05f
        // Keep each individual blob's natural ~0.90 aspect ratio while spreading positions across the wider landscape canvas
        val canvasWidth = if (isWide) minOf(maxWidth, totalHeight * 1.32f) else maxWidth
        val blobRefWidth = if (isWide) totalHeight * 0.90f else canvasWidth
        val largeHalo = (totalHeight * 0.018f).coerceIn(5.dp, 8.dp)
        val mediumHalo = (totalHeight * 0.015f).coerceIn(4.dp, 7.dp)
        val smallHalo = (totalHeight * 0.013f).coerceIn(4.dp, 6.dp)

        Box(modifier = Modifier.size(canvasWidth, totalHeight)) {

        // 1. Top-Left Blob (Shape 1)
        val tlWidth = blobRefWidth * if (isWide) 0.430f else 0.440f
        val tlHeight = totalHeight * if (isWide) 0.340f else 0.350f
        FluidBlobItem(
            song = songsToShow.getOrNull(0),
            shape = remember { FluidBlobShape(FluidBlobType.BLOB_TOP_LEFT) },
            width = tlWidth,
            height = tlHeight,
            haloPadding = largeHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.020f else 0.015f,
                    y = totalHeight * 0.020f
                ),
            onClick = { songsToShow.getOrNull(0)?.let(onSongClick) }
        )

        // 2. Top-Right Blob (Shape 2)
        val trWidth = blobRefWidth * if (isWide) 0.370f else 0.365f
        val trHeight = totalHeight * if (isWide) 0.310f else 0.318f
        FluidBlobItem(
            song = songsToShow.getOrNull(1),
            shape = remember { FluidBlobShape(FluidBlobType.BLOB_TOP_RIGHT) },
            width = trWidth,
            height = trHeight,
            haloPadding = mediumHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.670f else 0.575f,
                    y = totalHeight * if (isWide) 0.010f else 0.020f
                ),
            onClick = { songsToShow.getOrNull(1)?.let(onSongClick) }
        )

        // 3. Center Blob (Shape 3)
        val centerWidth = blobRefWidth * if (isWide) 0.370f else 0.365f
        val centerHeight = totalHeight * if (isWide) 0.320f else 0.318f
        FluidBlobItem(
            song = songsToShow.getOrNull(2),
            shape = remember { FluidBlobShape(FluidBlobType.BLOB_CENTER) },
            width = centerWidth,
            height = centerHeight,
            haloPadding = mediumHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.380f else 0.295f,
                    y = totalHeight * if (isWide) 0.260f else 0.335f
                ),
            onClick = { songsToShow.getOrNull(2)?.let(onSongClick) }
        )

        // 4. Right Circle (Shape 4)
        val circleWidth = blobRefWidth * if (isWide) 0.220f else 0.215f
        val circleHeight = totalHeight * if (isWide) 0.200f else 0.192f
        FluidBlobItem(
            song = songsToShow.getOrNull(3),
            shape = remember { FluidBlobShape(FluidBlobType.BLOB_RIGHT_CIRCLE) },
            width = circleWidth,
            height = circleHeight,
            haloPadding = smallHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.770f else 0.745f,
                    y = totalHeight * if (isWide) 0.400f else 0.425f
                ),
            onClick = { songsToShow.getOrNull(3)?.let(onSongClick) }
        )

        // 5. Bottom-Left Blob (Shape 5)
        val blWidth = blobRefWidth * if (isWide) 0.470f else 0.485f
        val blHeight = totalHeight * if (isWide) 0.330f else 0.335f
        FluidBlobItem(
            song = songsToShow.getOrNull(4),
            shape = remember { FluidBlobShape(FluidBlobType.BLOB_BOTTOM_LEFT) },
            width = blWidth,
            height = blHeight,
            haloPadding = largeHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.020f else 0.015f,
                    y = totalHeight * if (isWide) 0.600f else 0.615f
                ),
            onClick = { songsToShow.getOrNull(4)?.let(onSongClick) }
        )

        // 6. Bottom-Right Cookie (Shape 6)
        val cookieWidth = blobRefWidth * if (isWide) 0.310f else 0.305f
        val cookieHeight = totalHeight * if (isWide) 0.270f else 0.270f
        FluidBlobItem(
            song = songsToShow.getOrNull(5),
            shape = remember { FluidBlobShape(FluidBlobType.COOKIE_BOTTOM_RIGHT) },
            width = cookieWidth,
            height = cookieHeight,
            haloPadding = smallHalo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.560f else 0.635f,
                    y = totalHeight * if (isWide) 0.660f else 0.685f
                ),
            onClick = { songsToShow.getOrNull(5)?.let(onSongClick) }
        )

        // Floating Dreamy Accent Orbs
        FloatingOrb(
            size = if (isWide) 22.dp else 28.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.94f else 0.92f,
                    y = totalHeight * 0.04f
                )
        )
        FloatingOrb(
            size = if (isWide) 20.dp else 26.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.24f else 0.22f,
                    y = totalHeight * 0.45f
                )
        )
        FloatingOrb(
            size = if (isWide) 18.dp else 24.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = canvasWidth * if (isWide) 0.46f else 0.57f,
                    y = totalHeight * if (isWide) 0.88f else 0.93f
                )
        )
        }
    }
}

@Composable
private fun FluidBlobItem(
    song: Song?,
    shape: Shape,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    haloPadding: Dp = 8.dp,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val haloColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    } else {
        Color(0xFFE5D4C6).copy(alpha = 0.88f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        // Organic Halo Layer
        Box(
            modifier = Modifier
                .size(width + haloPadding * 2, height + haloPadding * 2)
                .graphicsLayer {
                    clip = true
                    this.shape = shape
                }
                .background(haloColor)
        )

        // Inner Artwork
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer {
                    clip = true
                    this.shape = shape
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
                    onClick = onClick
                )
        ) {
            if (song != null) {
                SmartImage(
                    model = song.albumArtUriString,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    crossfadeDurationMillis = 0,
                    isThumbnail = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FloatingOrb(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val orbBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                Color.Transparent
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFD6C6E8).copy(alpha = 0.9f),
                Color(0xFFF8D2BA).copy(alpha = 0.85f),
                Color(0xFFF3B89E).copy(alpha = 0.4f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(orbBrush)
    )
}
