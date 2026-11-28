package com.saurav.pixelmusic.ui.glancewidget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.color.ColorProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.PlayerInfo

object AlbumArtBitmapCache {
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024 // 4 MiB
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getBitmap(key: String): Bitmap? = lruCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            lruCache.put(key, bitmap)
        }
    }

    fun getKey(byteArray: ByteArray): String {
        return byteArray.contentHashCode().toString()
    }
}

@DrawableRes
fun widgetShapeResForRadius(cornerRadius: Dp): Int {
    return when {
        cornerRadius >= 36.dp -> R.drawable.widget_bg_pill
        cornerRadius >= 22.dp -> R.drawable.widget_bg_card
        cornerRadius >= 14.dp -> R.drawable.widget_bg_squircle
        else -> R.drawable.widget_bg_rounded_12
    }
}

@Composable
fun WidgetRoundedBackgroundLayer(
    color: ColorProvider,
    cornerRadius: Dp
) {
    Image(
        provider = ImageProvider(widgetShapeResForRadius(cornerRadius)),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(color)
    )
}

fun centerCropAndRoundBitmap(
    source: Bitmap,
    targetWidthPx: Int,
    targetHeightPx: Int,
    cornerRadiusPx: Float
): Bitmap {
    if (source.width <= 0 || source.height <= 0) return source
    val outW = targetWidthPx.coerceAtLeast(1)
    val outH = targetHeightPx.coerceAtLeast(1)
    val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val scale = maxOf(
        outW.toFloat() / source.width.toFloat(),
        outH.toFloat() / source.height.toFloat()
    )
    val dx = (outW - source.width * scale) * 0.5f
    val dy = (outH - source.height * scale) * 0.5f
    val matrix = android.graphics.Matrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    }
    val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(matrix)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        this.shader = shader
    }
    val rect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
    val clampedRadius = cornerRadiusPx.coerceIn(0f, minOf(outW, outH) / 2f)
    canvas.drawRoundRect(rect, clampedRadius, clampedRadius, paint)
    return output
}

/**
 * Produces a smooth iOS-style full-bleed blurred album art bitmap with a subtle dark readability
 * scrim, delicate frosted glass edge highlight, and rounded corners for 1:1 Glance widgets.
 */
fun createBlurredRoundedAlbumArtBitmap(
    source: Bitmap,
    targetWidthPx: Int,
    targetHeightPx: Int,
    cornerRadiusPx: Float,
    blurRadius: Int = 14,
    lightSurfaceTintArgb: Int = 0xFFF4EFF4.toInt()
): Bitmap {
    if (source.width <= 0 || source.height <= 0) return source
    val outW = targetWidthPx.coerceAtLeast(64)
    val outH = targetHeightPx.coerceAtLeast(64)

    // 1. Center-crop into a compact working buffer so multi-pass box blur is fast (<3ms) and ultra-smooth.
    val workSize = 96
    val cropped = Bitmap.createBitmap(workSize, workSize, Bitmap.Config.ARGB_8888)
    val cropCanvas = Canvas(cropped)
    val cropScale = maxOf(
        workSize.toFloat() / source.width.toFloat(),
        workSize.toFloat() / source.height.toFloat()
    )
    val cropDx = (workSize - source.width * cropScale) * 0.5f
    val cropDy = (workSize - source.height * cropScale) * 0.5f
    val cropMatrix = android.graphics.Matrix().apply {
        setScale(cropScale, cropScale)
        postTranslate(cropDx, cropDy)
    }
    cropCanvas.drawBitmap(source, cropMatrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

    // 2. Three-pass separable box blur (closely approximates a creamy Gaussian blur on all API levels).
    val pixels = IntArray(workSize * workSize)
    val temp = IntArray(workSize * workSize)
    cropped.getPixels(pixels, 0, workSize, 0, 0, workSize, workSize)
    val radius = blurRadius.coerceIn(4, 24)
    repeat(3) {
        boxBlurHorizontal(pixels, temp, workSize, workSize, radius)
        boxBlurVertical(temp, pixels, workSize, workSize, radius)
    }
    cropped.setPixels(pixels, 0, workSize, 0, 0, workSize, workSize)

    // 3. Upscale smoothly, clip to rounded corners, and layer readability scrim + subtle glass rim.
    val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val rect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
    val clampedRadius = cornerRadiusPx.coerceIn(0f, minOf(outW, outH) / 2f)

    val upMatrix = android.graphics.Matrix().apply {
        setScale(outW.toFloat() / workSize.toFloat(), outH.toFloat() / workSize.toFloat())
    }
    val shader = BitmapShader(cropped, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(upMatrix)
    }
    val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        this.shader = shader
    }
    canvas.drawRoundRect(rect, clampedRadius, clampedRadius, artPaint)

    // Luminous Material You light frosted wash so blurred album hues glow through softly
    // without turning darkish or muddy.
    val tintRgb = lightSurfaceTintArgb and 0x00FFFFFF
    val topVeil = (0x8C shl 24) or tintRgb       // ~55% Material You surface tint at top
    val midVeil = (0x7A shl 24) or tintRgb       // ~48% in middle to let blurred colors bloom
    val bottomVeil = (0xA6 shl 24) or tintRgb    // ~65% near bottom for crisp expressive controls
    val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = LinearGradient(
            0f,
            0f,
            0f,
            outH.toFloat(),
            intArrayOf(topVeil, midVeil, bottomVeil),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRoundRect(rect, clampedRadius, clampedRadius, scrimPaint)

    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (outW / 170f).coerceIn(1.2f, 2.2f)
        color = 0x73FFFFFF
    }
    val inset = rimPaint.strokeWidth * 0.5f
    canvas.drawRoundRect(
        RectF(inset, inset, outW - inset, outH - inset),
        (clampedRadius - inset).coerceAtLeast(0f),
        (clampedRadius - inset).coerceAtLeast(0f),
        rimPaint
    )

    cropped.recycle()
    return output
}

private fun boxBlurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val windowSize = radius * 2 + 1
    for (y in 0 until height) {
        val rowOffset = y * width
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val px = src[rowOffset + i.coerceIn(0, width - 1)]
            sumR += (px ushr 16) and 0xFF
            sumG += (px ushr 8) and 0xFF
            sumB += px and 0xFF
        }
        for (x in 0 until width) {
            dst[rowOffset + x] = (0xFF shl 24) or
                ((sumR / windowSize) shl 16) or
                ((sumG / windowSize) shl 8) or
                (sumB / windowSize)
            val removeIdx = (x - radius).coerceIn(0, width - 1)
            val addIdx = (x + radius + 1).coerceIn(0, width - 1)
            val pRemove = src[rowOffset + removeIdx]
            val pAdd = src[rowOffset + addIdx]
            sumR += ((pAdd ushr 16) and 0xFF) - ((pRemove ushr 16) and 0xFF)
            sumG += ((pAdd ushr 8) and 0xFF) - ((pRemove ushr 8) and 0xFF)
            sumB += (pAdd and 0xFF) - (pRemove and 0xFF)
        }
    }
}

private fun boxBlurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val windowSize = radius * 2 + 1
    for (x in 0 until width) {
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (i in -radius..radius) {
            val px = src[i.coerceIn(0, height - 1) * width + x]
            sumR += (px ushr 16) and 0xFF
            sumG += (px ushr 8) and 0xFF
            sumB += px and 0xFF
        }
        for (y in 0 until height) {
            dst[y * width + x] = (0xFF shl 24) or
                ((sumR / windowSize) shl 16) or
                ((sumG / windowSize) shl 8) or
                (sumB / windowSize)
            val removeIdx = (y - radius).coerceIn(0, height - 1)
            val addIdx = (y + radius + 1).coerceIn(0, height - 1)
            val pRemove = src[removeIdx * width + x]
            val pAdd = src[addIdx * width + x]
            sumR += ((pAdd ushr 16) and 0xFF) - ((pRemove ushr 16) and 0xFF)
            sumG += ((pAdd ushr 8) and 0xFF) - ((pRemove ushr 8) and 0xFF)
            sumB += (pAdd and 0xFF) - (pRemove and 0xFF)
        }
    }
}

data class WidgetColors(
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val artist: ColorProvider,
    val playPauseBackground: ColorProvider,
    val playPauseIcon: ColorProvider,
    val prevNextBackground: ColorProvider,
    val prevNextIcon: ColorProvider
)

@Composable
fun PlayerInfo.getWidgetColors(): WidgetColors {
    val theme = this.themeColors
    
    return if (theme != null) {
        WidgetColors(
            surface = ColorProvider(
                day = Color(theme.lightSurfaceContainer),
                night = Color(theme.darkSurfaceContainer)
            ),
            onSurface = ColorProvider(
                day = Color(theme.lightTitle),
                night = Color(theme.darkTitle)
            ),
            artist = ColorProvider(
                day = Color(theme.lightArtist),
                night = Color(theme.darkArtist)
            ),
            playPauseBackground = ColorProvider(
                day = Color(theme.lightPlayPauseBackground),
                night = Color(theme.darkPlayPauseBackground)
            ),
            playPauseIcon = ColorProvider(
                day = Color(theme.lightPlayPauseIcon),
                night = Color(theme.darkPlayPauseIcon)
            ),
            prevNextBackground = ColorProvider(
                day = Color(theme.lightPrevNextBackground),
                night = Color(theme.darkPrevNextBackground)
            ),
            prevNextIcon = ColorProvider(
                day = Color(theme.lightPrevNextIcon),
                night = Color(theme.darkPrevNextIcon)
            )
        )
    } else {
        WidgetColors(
            surface = GlanceTheme.colors.surface,
            onSurface = GlanceTheme.colors.onSurface,
            artist = GlanceTheme.colors.onSurface,
            playPauseBackground = GlanceTheme.colors.primaryContainer,
            playPauseIcon = GlanceTheme.colors.onPrimaryContainer,
            prevNextBackground = GlanceTheme.colors.secondaryContainer,
            prevNextIcon = GlanceTheme.colors.onSecondaryContainer
        )
    }
}
