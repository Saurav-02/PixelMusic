package com.unshoo.pixelmusic.data.remote.youtube

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.unshoo.pixelmusic.data.model.youtube.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

object DownloadHelper {
    private val client = YoutubeHelper.client

    suspend fun downloadImage(context: Context, imageUrl: String, id: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val imageDir = UmihiHelper.getDownloadDirectory(context, Constants.Downloads.THUMBNAILS_FOLDER)
                val imageFile = File(imageDir, "$id.jpg")

                if (imageFile.exists()) return@withContext imageFile

                URL(imageUrl).openStream().use { input ->
                    imageFile.outputStream().use { output -> input.copyTo(output) }
                }
                imageFile
            } catch (e: Exception) {
                UmihiHelper.printe("Error Downloading Thumbnail", exception = e)
                null
            }
        }
    }

    suspend fun downloadAudio(
        context: Context,
        song: Song,
        connections: Int = 8
    ): String? = withContext(Dispatchers.IO) {
        
        val cacheDir = File(context.cacheDir, "PixelMusic_Temp")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        
        // Changed to .m4a
        val tempOutputFile = File(cacheDir, "${song.youtubeId}_temp.m4a")

        val url = YoutubeHelper.getDownloadUrl(context, song)
        val total = try {
            val headReq = Request.Builder().url(url).header("Range", "bytes=0-0").build()
            client.newCall(headReq).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                res.headers["Content-Range"]?.substringAfter("/")?.toLongOrNull() ?: return@withContext null
            }
        } catch (e: Exception) {
            UmihiHelper.printe("Failed to get content length: ${e.message}")
            return@withContext null
        }

        val chunkSize = total / connections
        val tempFiles = mutableListOf<File>()

        try {
            (0 until connections).map { i ->
                async {
                    val start = i * chunkSize
                    val end = if (i == connections - 1) total - 1 else (start + chunkSize - 1)
                    val chunkFile = File(cacheDir, "${song.youtubeId}.part$i")

                    val req = Request.Builder()
                        .url(url)
                        .header("Range", "bytes=$start-$end")
                        .header("User-Agent", Constants.YoutubeApi.USER_AGENT)
                        .build()

                    client.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Failed to download chunk $i")
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(chunkFile).use { output -> input.copyTo(output) }
                        }
                    }
                    chunkFile
                }
            }.awaitAll().also { tempFiles.addAll(it) }

            FileOutputStream(tempOutputFile).use { out ->
                tempFiles.sortedBy { it.name }.forEach { part ->
                    part.inputStream().use { it.copyTo(out) }
                    part.delete()
                }
            }
        } catch (e: Exception) {
            tempFiles.forEach { it.delete() }
            tempOutputFile.delete()
            UmihiHelper.printe("Download failed: ${e.message}")
            return@withContext null
        }

        val safeTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val safeArtist = song.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        // Changed to .m4a
        val finalFileName = "${safeTitle}_${safeArtist}_${System.currentTimeMillis()}.m4a"

        val publicUriString = writeToPublicMediaStore(context, tempOutputFile, finalFileName)
        
        tempOutputFile.delete()

        return@withContext publicUriString
    }
    
    private fun writeToPublicMediaStore(context: Context, tempFile: File, fileName: String): String? {
        val resolver = context.contentResolver
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            // THE FIX: Changed to audio/mp4 to bypass MediaStore strict checks
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/PixelMusic")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "PixelMusic")
                if (!musicDir.exists()) musicDir.mkdirs()
                put(MediaStore.Audio.Media.DATA, File(musicDir, fileName).absolutePath)
            }
        }

        val newUri = resolver.insert(audioCollection, contentValues) ?: return null

        return try {
            resolver.openOutputStream(newUri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)
            } else {
                val path = contentValues.getAsString(MediaStore.Audio.Media.DATA)
                if (path != null) {
                    // Changed scanner mime type to audio/mp4
                    MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("audio/mp4"), null)
                }
            }
            
            newUri.toString()
            
        } catch (e: Exception) {
            resolver.delete(newUri, null, null)
            e.printStackTrace()
            null
        }
    }

    fun copyToPublicDownload(context: Context, sourceFilePath: String, songTitle: String, artistName: String): File? {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return null

            val safeTitle = songTitle.replace(Regex("[\\\\/:*?\"\\<>|]"), "_")
            val safeArtist = artistName.replace(Regex("[\\\\/:*?\"\\<>|]"), "_")
            // Changed to .m4a
            val fileName = "$safeTitle - $safeArtist.m4a"

            val publicDownloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "PixelMusic"
            )
            if (!publicDownloadDir.exists()) {
                publicDownloadDir.mkdirs()
            }
            val destinationFile = File(publicDownloadDir, fileName)

            sourceFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Changed scanner mime type to audio/mp4
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destinationFile.absolutePath),
                arrayOf("audio/mp4"),
                null
            )

            return destinationFile
        } catch (e: Exception) {
            UmihiHelper.printe("Failed to copy to public downloads: ${e.message}", exception = e)
            return null
        }
    }
}
