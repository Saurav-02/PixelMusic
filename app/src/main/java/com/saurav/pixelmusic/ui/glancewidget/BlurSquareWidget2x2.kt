package com.saurav.pixelmusic.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.saurav.pixelmusic.MainActivity
import com.saurav.pixelmusic.data.model.PlayerInfo
import timber.log.Timber

/**
 * 1:1 iOS-inspired square widget with a full-bleed blurred album art background, smooth rounded
 * corners, compact header + focal track display, and Material You Expressive shaped controls.
 */
class BlurSquareWidget2x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            GlanceTheme {
                BlurSquareWidgetContent(playerInfo = playerInfo, context = context)
            }
        }
    }

    @Composable
    private fun BlurSquareWidgetContent(
        playerInfo: PlayerInfo,
        context: Context
    ) {
        val title = playerInfo.songTitle.ifEmpty { "PixelMusic" }
        val artist = playerInfo.artistName.ifEmpty { "Tap to open" }
        val isPlaying = playerInfo.isPlaying
        val theme = playerInfo.themeColors

        // Dynamically size padding & controls from the actual rendered widget bounds so buttons
        // never clip even on narrow 2x2 launcher cells.
        val widgetSize = LocalSize.current
        val widgetWidth = widgetSize.width.takeIf { it.value > 0f } ?: 150.dp
        val isCompactWidth = widgetWidth < 155.dp
        val horizontalPadding = if (isCompactWidth) 10.dp else 14.dp
        val verticalPadding = if (isCompactWidth) 12.dp else 14.dp
        val buttonSpacing = if (isCompactWidth) 5.dp else 8.dp
        val availableRowWidth = (widgetWidth - horizontalPadding * 2 - buttonSpacing * 2)
            .coerceAtLeast(88.dp)
        val skipWidth = (availableRowWidth * 0.30f).coerceIn(28.dp, 44.dp)
        val playWidth = (availableRowWidth * 0.40f).coerceIn(36.dp, 54.dp)
        val skipHeight = skipWidth.coerceIn(32.dp, 38.dp)
        val playHeight = (skipHeight + 2.dp).coerceAtMost(42.dp)
        val skipIconSize = if (skipWidth < 36.dp) 16.dp else 19.dp
        val playIconSize = if (playWidth < 44.dp) 19.dp else 22.dp

        val widgetCornerRadius = 28.dp
        // Material You Expressive morphing shapes:
        // Play/Pause morphs between expressive squircle (playing) and full pill (paused).
        val playPauseCornerRadius = if (isPlaying) 14.dp else 22.dp
        val skipButtonCornerRadius = if (skipWidth < 36.dp) 12.dp else 16.dp

        // Light, airy Material You tonal palette
        val lightSurfaceTintArgb = theme?.let {
            if (it.lightSurfaceContainerHigh != 0) it.lightSurfaceContainerHigh else it.lightSurfaceContainer
        } ?: 0xFFF4EFF4.toInt()

        val fallbackSurfaceColor: GlanceColorProvider = ColorProvider(
            day = Color(lightSurfaceTintArgb),
            night = Color(lightSurfaceTintArgb)
        )
        val headerAccentColor: GlanceColorProvider = theme?.let {
            val primaryInk = Color(it.lightPlayPauseBackground)
            ColorProvider(day = primaryInk, night = primaryInk)
        } ?: ColorProvider(day = Color(0xFF6750A4), night = Color(0xFF6750A4))

        val focalTextColor: GlanceColorProvider = theme?.let {
            val ink = Color(it.lightTitle)
            ColorProvider(day = ink, night = ink)
        } ?: ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFF1C1B1F))

        val subtitleTextColor: GlanceColorProvider = theme?.let {
            val sub = Color(it.lightArtist)
            ColorProvider(day = sub, night = sub)
        } ?: ColorProvider(day = Color(0xFF49454F), night = Color(0xFF49454F))

        val playBgColor: GlanceColorProvider = theme?.let {
            val bg = Color(it.lightPlayPauseBackground)
            ColorProvider(day = bg, night = bg)
        } ?: ColorProvider(day = Color(0xFF6750A4), night = Color(0xFF6750A4))

        val playIconColor: GlanceColorProvider = theme?.let {
            val fg = Color(it.lightPlayPauseIcon)
            ColorProvider(day = fg, night = fg)
        } ?: ColorProvider(day = Color.White, night = Color.White)

        val skipBgColor: GlanceColorProvider = theme?.let {
            val softContainer = lerp(Color(it.lightPrevNextBackground), Color.White, 0.25f)
            ColorProvider(day = softContainer, night = softContainer)
        } ?: ColorProvider(day = Color(0xEBF7F2FA), night = Color(0xEBF7F2FA))

        val skipIconColor: GlanceColorProvider = theme?.let {
            val fg = Color(it.lightPrevNextIcon)
            ColorProvider(day = fg, night = fg)
        } ?: ColorProvider(day = Color(0xFF6750A4), night = Color(0xFF6750A4))

        val headerText = if (playerInfo.songTitle.isNotBlank() && playerInfo.artistName.isNotBlank()) {
            "${playerInfo.songTitle.uppercase()} · ${playerInfo.artistName}"
        } else {
            title
        }

        val blurredBgProvider = resolveBlurredBackgroundProvider(
            context = context,
            bitmapData = playerInfo.albumArtBitmapData,
            albumArtUri = playerInfo.albumArtUri,
            cornerRadiusDp = widgetCornerRadius.value,
            lightSurfaceTintArgb = lightSurfaceTintArgb
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(widgetCornerRadius)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (blurredBgProvider != null) {
                Image(
                    provider = blurredBgProvider,
                    contentDescription = null,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(widgetCornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                WidgetRoundedBackgroundLayer(
                    color = fallbackSurfaceColor,
                    cornerRadius = widgetCornerRadius
                )
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // Top compact header (e.g. "MASAKALI · Mohit Chauhan")
                Text(
                    text = headerText,
                    style = TextStyle(
                        fontSize = if (isCompactWidth) 11.sp else 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = headerAccentColor
                    ),
                    maxLines = 1
                )

                // Middle focal section: musical note + track title
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(
                        text = "♪",
                        style = TextStyle(
                            fontSize = if (isCompactWidth) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerAccentColor
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = if (isCompactWidth) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = focalTextColor
                        ),
                        maxLines = 2
                    )
                    if (playerInfo.songTitle.isBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = artist,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = subtitleTextColor
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Bottom Material You Expressive shaped controls (responsively sized to never clip)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    PreviousButton(
                        modifier = GlanceModifier.size(width = skipWidth, height = skipHeight),
                        backgroundColor = skipBgColor,
                        iconColor = skipIconColor,
                        cornerRadius = skipButtonCornerRadius,
                        iconSize = skipIconSize
                    )

                    Spacer(GlanceModifier.width(buttonSpacing))

                    PlayPauseButton(
                        modifier = GlanceModifier.size(width = playWidth, height = playHeight),
                        isPlaying = isPlaying,
                        backgroundColor = playBgColor,
                        iconColor = playIconColor,
                        cornerRadius = playPauseCornerRadius,
                        iconSize = playIconSize
                    )

                    Spacer(GlanceModifier.width(buttonSpacing))

                    NextButton(
                        modifier = GlanceModifier.size(width = skipWidth, height = skipHeight),
                        backgroundColor = skipBgColor,
                        iconColor = skipIconColor,
                        cornerRadius = skipButtonCornerRadius,
                        iconSize = skipIconSize
                    )
                }
            }
        }
    }

    private fun resolveBlurredBackgroundProvider(
        context: Context,
        bitmapData: ByteArray?,
        albumArtUri: String?,
        cornerRadiusDp: Float,
        lightSurfaceTintArgb: Int
    ): ImageProvider? {
        val density = context.resources.displayMetrics.density
        val targetPx = (180f * density).toInt().coerceIn(220, 360)
        val cornerRadiusPx = cornerRadiusDp * density
        val tintKey = (lightSurfaceTintArgb and 0x00FFFFFF).toString(16)

        val bitmap: Bitmap? = bitmapData?.let { data ->
            val cacheKey = "${AlbumArtBitmapCache.getKey(data)}_m3lightblur_${targetPx}_r${cornerRadiusDp.toInt()}_$tintKey"
            AlbumArtBitmapCache.getBitmap(cacheKey) ?: runCatching {
                val decoded = BitmapFactory.decodeByteArray(data, 0, data.size)
                decoded?.let { src ->
                    createBlurredRoundedAlbumArtBitmap(
                        source = src,
                        targetWidthPx = targetPx,
                        targetHeightPx = targetPx,
                        cornerRadiusPx = cornerRadiusPx,
                        lightSurfaceTintArgb = lightSurfaceTintArgb
                    ).also {
                        AlbumArtBitmapCache.putBitmap(cacheKey, it)
                        if (it !== src) src.recycle()
                    }
                }
            }.onFailure { e ->
                Timber.tag("BlurSquareWidget").w(e, "Failed to blur widget bitmap bytes")
            }.getOrNull()
        } ?: albumArtUri?.takeIf { it.isNotBlank() }?.let { rawUri ->
            val cacheKey = "uri:${rawUri}_m3lightblur_${targetPx}_r${cornerRadiusDp.toInt()}_$tintKey"
            AlbumArtBitmapCache.getBitmap(cacheKey) ?: runCatching {
                val decoded = decodeWidgetAlbumArtBitmap(
                    context = context,
                    rawUri = rawUri,
                    targetWidthPx = 160,
                    targetHeightPx = 160
                )
                decoded?.let { src ->
                    createBlurredRoundedAlbumArtBitmap(
                        source = src,
                        targetWidthPx = targetPx,
                        targetHeightPx = targetPx,
                        cornerRadiusPx = cornerRadiusPx,
                        lightSurfaceTintArgb = lightSurfaceTintArgb
                    ).also {
                        AlbumArtBitmapCache.putBitmap(cacheKey, it)
                        if (it !== src) src.recycle()
                    }
                }
            }.onFailure { e ->
                Timber.tag("BlurSquareWidget").w(e, "Failed to blur widget art from URI")
            }.getOrNull()
        }

        return bitmap?.let { ImageProvider(it) }
    }
}
