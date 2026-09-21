package com.saurav.pixelmusic.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.saurav.pixelmusic.utils.AlbumArtUtils
import com.saurav.pixelmusic.utils.ArtworkTransportSanitizer
import com.saurav.pixelmusic.utils.LocalArtworkUri

private val SUPPORTED_WIDGET_LOCAL_ARTWORK_SCHEMES = setOf(
    "content",
    "file",
    "android.resource",
    LocalArtworkUri.SCHEME,
)

internal fun decodeWidgetAlbumArtBitmap(
    context: Context,
    rawUri: String,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Bitmap? {
    val isLocalArtworkUri = LocalArtworkUri.isLocalArtworkUri(rawUri)
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    val isSupportedLocalUri = isLocalArtworkUri ||
        scheme in SUPPORTED_WIDGET_LOCAL_ARTWORK_SCHEMES ||
        (scheme.isNullOrBlank() && rawUri.startsWith("/"))
    if (!isSupportedLocalUri) {
        return null
    }

    val appContext = context.applicationContext
    val encodedBytes = AlbumArtUtils.openArtworkInputStream(appContext, uri)?.use { input ->
        readBytesCapped(input, ArtworkTransportSanitizer.WIDGET_CONFIG.sourceBytesLimit)
    } ?: return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(encodedBytes, 0, encodedBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val reqWidth = if (targetWidthPx > 0) targetWidthPx else 128
    val reqHeight = if (targetHeightPx > 0) targetHeightPx else 128

    var inSampleSize = 1
    if (bounds.outHeight > reqHeight || bounds.outWidth > reqWidth) {
        val halfHeight = bounds.outHeight / 2
        val halfWidth = bounds.outWidth / 2
        while (
            halfHeight / inSampleSize >= reqHeight &&
            halfWidth / inSampleSize >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }

    return BitmapFactory.decodeByteArray(
        encodedBytes,
        0,
        encodedBytes.size,
        BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
    )
}

private fun readBytesCapped(input: java.io.InputStream, maxBytes: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalRead = 0
    while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        totalRead += read
        if (totalRead > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray().takeIf { it.isNotEmpty() }
}
