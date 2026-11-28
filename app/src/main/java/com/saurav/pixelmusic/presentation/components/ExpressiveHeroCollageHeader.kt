package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

@Composable
fun ExpressiveHeroCollageHeader(
    title: String,
    subtitle: String,
    songs: List<Song>,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 340.dp,
    parallaxOffset: Float = 0f,
    collageAlpha: Float = 1f,
    showCollage: Boolean = true,
    isLandscapeFullHeight: Boolean = false,
    bottomContentPadding: Dp = 14.dp,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val topAlbumArt = remember(songs) {
        songs.firstOrNull { !it.albumArtUriString.isNullOrBlank() }?.albumArtUriString
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryGlow = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLandscapeFullHeight) Modifier.fillMaxHeight() else Modifier.height(headerHeight))
            .clipToBounds()
    ) {
        // Subtle fallback ambient gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.24f),
                            surfaceColor.copy(alpha = 0.5f),
                            surfaceColor
                        )
                    )
                )
        )

        // Full-bleed iOS / Apple Music-style single top-song artwork backdrop
        if (showCollage && topAlbumArt != null) {
            SmartImage(
                model = topAlbumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = parallaxOffset
                        alpha = collageAlpha * 0.85f
                    }
            )
        } else if (showCollage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = parallaxOffset
                        alpha = collageAlpha * 0.35f
                    },
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    model = R.drawable.rounded_album_24,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        // Smooth iOS-style legibility scrim (vertical fade in portrait; soft bottom scrim + right-edge melt in landscape)
        if (isLandscapeFullHeight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to surfaceColor.copy(alpha = 0.28f),
                                0.24f to Color.Transparent,
                                0.56f to surfaceColor.copy(alpha = 0.28f),
                                0.84f to surfaceColor.copy(alpha = 0.72f),
                                1.0f to surfaceColor.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.62f to Color.Transparent,
                                0.86f to surfaceColor.copy(alpha = 0.88f),
                                0.96f to surfaceColor,
                                1.0f to surfaceColor
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to surfaceColor.copy(alpha = 0.34f),
                                0.22f to Color.Transparent,
                                0.52f to surfaceColor.copy(alpha = 0.35f),
                                0.78f to surfaceColor.copy(alpha = 0.86f),
                                0.92f to surfaceColor,
                                1.0f to surfaceColor
                            )
                        )
                    )
            )
        }

        // Centered Title + Subtitle anchored cleanly at the bottom of the hero (above mini player in landscape)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 14.dp,
                    bottom = bottomContentPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trailingAction != null) {
                    Spacer(modifier = Modifier.size(6.dp))
                    trailingAction()
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = GoogleSansRounded
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
