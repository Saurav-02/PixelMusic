package com.unshoo.pixelmusic.data.remote.youtube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import kotlin.time.measureTimedValue
import com.unshoo.pixelmusic.utils.PixelLogger



object UmihiHelper {
    const val TAG = "UmihiPrint"
    const val WEAROS_MAX_IMAGE_SIZE = 720

    inline fun <T> benchmark(
        label: String,
        block: () -> T
    ): T {
        val result = measureTimedValue(block)
        printd(tag = "UmihiBench", message = "$label: ${result.duration.inWholeMilliseconds} ms")
        return result.value
    }

    fun printd(message: String, tag: String = TAG) {
    Log.d(tag, message)
    PixelLogger.d(PixelLogger.Category.MISC, tag, message)
}

fun printe(message: String, tag: String = TAG, exception: java.lang.Exception? = null) {
    Log.e(TAG, message, exception)
    PixelLogger.e(PixelLogger.Category.MISC, tag, message, exception)
}

    fun getDownloadDirectory(context: Context, directory: String? = null): File {
        val dir = File(
            context.filesDir,
            if (directory == null)
                Constants.Downloads.DIRECTORY
            else
                Paths.get(Constants.Downloads.DIRECTORY, directory)
                    .toString()
        )
        dir.mkdirs()
        return dir
    }

    suspend fun fetchArtworkBytes(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val client = YoutubeHelper.client

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val bytes = response.body?.bytes() ?: return@withContext null

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.toByteArray().cappedTo()
            } catch (e: Exception) {
                Log.e("Artwork", "Failed to fetch artwork: ${e.message}")
                null
            }
        }
    }
}

fun ByteArray.cappedTo(maxSize: Int = UmihiHelper.WEAROS_MAX_IMAGE_SIZE): ByteArray? {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size) ?: return null
    val capped = if (bitmap.width <= maxSize && bitmap.height <= maxSize) bitmap else {
        val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
    }
    val stream = ByteArrayOutputStream()
    capped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    return stream.toByteArray()
}
