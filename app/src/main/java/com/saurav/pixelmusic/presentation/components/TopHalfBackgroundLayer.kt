package com.saurav.pixelmusic.presentation.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.preferences.AppBackgroundStyle
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository

/**
 * Renders the user-selected ambient background wallpaper strictly across the top half
 * of Home and Explore screens, fading smoothly into [MaterialTheme.colorScheme.background]
 * toward the middle of the viewport (similar to Gemini / YouTube Music).
 */
@Composable
fun TopHalfBackgroundLayer(
    userPreferencesRepository: UserPreferencesRepository,
    currentArtUri: String?,
    modifier: Modifier = Modifier,
) {
    val backgroundStyle by userPreferencesRepository.appBackgroundStyleFlow
        .collectAsStateWithLifecycle(initialValue = AppBackgroundStyle.DEFAULT)
    if (backgroundStyle == AppBackgroundStyle.DEFAULT) return

    val backgroundCustomUri by userPreferencesRepository.appBackgroundCustomUriFlow
        .collectAsStateWithLifecycle(initialValue = "")
    val backgroundOpacity by userPreferencesRepository.appBackgroundOpacityFlow
        .collectAsStateWithLifecycle(initialValue = 0.5f)
    val backgroundBlur by userPreferencesRepository.appBackgroundBlurFlow
        .collectAsStateWithLifecycle(initialValue = 0f)

    val colorScheme = MaterialTheme.colorScheme
    val baseBackground = colorScheme.background
    val isLightTheme = baseBackground.luminance() > 0.5f

    val targetKey = remember(backgroundStyle, currentArtUri, backgroundCustomUri) {
        Triple(backgroundStyle, currentArtUri, backgroundCustomUri)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.54f)
    ) {
        Crossfade(
            targetState = targetKey,
            animationSpec = tween(durationMillis = 700),
            label = "TopHalfBackgroundCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { (style, artUri, customUri) ->
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backgroundBlur > 0f) {
                Modifier.blur(backgroundBlur.dp)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backgroundOpacity.coerceIn(0.1f, 1f) }
                    .then(blurModifier)
            ) {
                when (style) {
                    AppBackgroundStyle.DEFAULT -> Unit

                    AppBackgroundStyle.GRADIENT_BLUR -> {
                        val primary = colorScheme.primary
                        val tertiary = colorScheme.tertiary
                        val secondary = colorScheme.secondary
                        val primaryContainer = colorScheme.primaryContainer
                        val tertiaryContainer = colorScheme.tertiaryContainer
                        val intensity = if (isLightTheme) 0.55f else 0.72f
                        Spacer(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val w = size.width
                                    val h = size.height
                                    val orb1 = Brush.radialGradient(
                                        colors = listOf(
                                            primary.copy(alpha = intensity),
                                            primaryContainer.copy(alpha = intensity * 0.55f),
                                            Color.Transparent
                                        ),
                                        center = Offset(w * 0.18f, h * 0.16f),
                                        radius = w * 0.88f
                                    )
                                    val orb2 = Brush.radialGradient(
                                        colors = listOf(
                                            tertiary.copy(alpha = intensity * 0.9f),
                                            tertiaryContainer.copy(alpha = intensity * 0.45f),
                                            Color.Transparent
                                        ),
                                        center = Offset(w * 0.86f, h * 0.22f),
                                        radius = w * 0.82f
                                    )
                                    val orb3 = Brush.radialGradient(
                                        colors = listOf(
                                            secondary.copy(alpha = intensity * 0.55f),
                                            Color.Transparent
                                        ),
                                        center = Offset(w * 0.50f, h * 0.42f),
                                        radius = w * 0.75f
                                    )
                                    onDrawBehind {
                                        drawRect(brush = orb1)
                                        drawRect(brush = orb2)
                                        drawRect(brush = orb3)
                                    }
                                }
                        )
                    }

                    AppBackgroundStyle.MUSIC_NOTES -> {
                        Image(
                            painter = painterResource(id = R.drawable.bg_music_notes_tintable),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.tint(colorScheme.primary),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppBackgroundStyle.LIVE_BLUR -> {
                        if (!artUri.isNullOrBlank()) {
                            val liveArtBlur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur((backgroundBlur.coerceAtLeast(18f)).dp)
                            } else {
                                Modifier
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                SmartImage(
                                    model = artUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(liveArtBlur)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(baseBackground.copy(alpha = 0.52f))
                                        .background(colorScheme.primary.copy(alpha = 0.08f))
                                )
                            }
                        }
                    }

                    AppBackgroundStyle.CUSTOM -> {
                        if (!customUri.isNullOrBlank()) {
                            AsyncImage(
                                model = customUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Smooth fade-out scrim into the solid app background at the bottom of the top half
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.36f to baseBackground.copy(alpha = 0.14f),
                            0.64f to baseBackground.copy(alpha = 0.58f),
                            0.84f to baseBackground.copy(alpha = 0.90f),
                            0.93f to baseBackground,
                            1.00f to baseBackground
                        )
                    )
                )
        )
    }
}
