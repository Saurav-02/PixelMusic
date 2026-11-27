package com.saurav.pixelmusic.data.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ProfilePhotoHelper {
    private const val PROFILE_DIR = "profile"
    private const val AVATAR_FILE_NAME = "avatar.jpg"
    private const val LOCAL_SIZE_PX = 512
    private const val WIRE_SIZE_PX = 80

    suspend fun savePickedPhoto(context: Context, uri: Uri): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val decoded = decodeSampledBitmap(context, uri, LOCAL_SIZE_PX)
                    ?: error("Could not decode selected image")
                val oriented = applyExifOrientation(context, uri, decoded)
                val cropped = centerCropSquare(oriented)
                val localBitmap = if (cropped.width != LOCAL_SIZE_PX || cropped.height != LOCAL_SIZE_PX) {
                    Bitmap.createScaledBitmap(cropped, LOCAL_SIZE_PX, LOCAL_SIZE_PX, true)
                } else {
                    cropped
                }

                val dir = File(context.filesDir, PROFILE_DIR).apply { mkdirs() }
                val dest = File(dir, AVATAR_FILE_NAME)
                FileOutputStream(dest).use { out ->
                    localBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }

                val wireBitmap = Bitmap.createScaledBitmap(localBitmap, WIRE_SIZE_PX, WIRE_SIZE_PX, true)
                val wireBytes = ByteArrayOutputStream().use { baos ->
                    wireBitmap.compress(Bitmap.CompressFormat.JPEG, 65, baos)
                    baos.toByteArray()
                }
                val wireDataUri = "data:image/jpeg;base64," + Base64.encodeToString(wireBytes, Base64.NO_WRAP)

                if (wireBitmap !== localBitmap) wireBitmap.recycle()
                if (localBitmap !== cropped) localBitmap.recycle()
                if (cropped !== oriented) cropped.recycle()
                if (oriented !== decoded) oriented.recycle()
                decoded.recycle()

                dest.absolutePath to wireDataUri
            }
        }

    fun deleteLocalPhoto(context: Context) {
        runCatching {
            val file = File(File(context.filesDir, PROFILE_DIR), AVATAR_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sample = 1
        while (srcW / (sample * 2) >= targetSize && srcH / (sample * 2) >= targetSize) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}
