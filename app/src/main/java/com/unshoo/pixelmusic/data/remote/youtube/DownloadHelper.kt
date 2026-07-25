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
                val imageDir =
                    UmihiHelper.getDownloadDirectory(context, Constants.Downloads.THUMBNAILS_FOLDER)
                val imageFile = File(imageDir, "$id.jpg")

                if (imageFile.exists()) {
                    return@withContext imageFile
                }

                URL(imageUrl).openStream().use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                imageFile

            } catch (e: Exception) {
                UmihiHelper.printe(
                    tag = "PlaylistDownloadWorker",
                    message = "Error Downloading Thumbnail",
                    exception = e
                )
                null
            }
        }
    }

    suspend fun downloadAudio(
        context: Context,
        song: Song,
        connections: Int = 8
    ): String? = withContext(Dispatchers.IO) {

        val safeTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val safeArtist = song.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val finalFileName = "$safeTitle - $safeArtist.webm"

        // 1. Check the PUBLIC folder first
        val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "PixelMusic")
        val publicFile = File(publicMusicDir, finalFileName)
        
        if (publicFile.exists() && publicFile.length() > 0) {
            return@withContext publicFile.absolutePath
        }

        // 2. Setup internal hidden cache for chunk downloading
        val audioDir = UmihiHelper.getDownloadDirectory(context, Constants.Downloads.AUDIO_FILES_FOLDER)
        val outputFile = File(audioDir, "${song.youtubeId}.webm")

        // 3. Download the file if it isn't even in the hidden cache
        if (!outputFile.exists() || outputFile.length() == 0L) {
            val url = YoutubeHelper.getSongPlayerUrl(context, song)
            val total = try {
                val headReq = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-0")
                    .build()

                client.newCall(headReq).execute().use { headRes ->
                    if (!headRes.isSuccessful) return@withContext null
                    headRes.headers["Content-Range"]?.substringAfter("/")?.toLongOrNull() ?: return@withContext null
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
                        val temp = File(audioDir, "${song.youtubeId}.part$i")

                        try {
                            val req = Request.Builder()
                                .url(url)
                                .header("Range", "bytes=$start-$end")
                                .header("User-Agent", Constants.YoutubeApi.USER_AGENT)
                                .build()

                            client.newCall(req).execute().use { response ->
                                if (!response.isSuccessful) throw IOException("Failed to download chunk $i: ${response.code}")
                                response.body?.byteStream()?.use { input ->
                                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                                } ?: throw IOException("Empty response body for chunk $i")
                            }
                            temp
                        } catch (e: Exception) {
                            temp.delete()
                            throw e
                        }
                    }
                }.awaitAll().also { tempFiles.addAll(it) }

                FileOutputStream(outputFile).use { out ->
                    tempFiles.sortedBy { it.name }.forEach { part ->
                        part.inputStream().use { it.copyTo(out) }
                        part.delete()
                    }
                }
            } catch (e: Exception) {
                UmihiHelper.printe("Download failed for ${song.youtubeId}: ${e.message}")
                tempFiles.forEach { it.delete() }
                outputFile.delete()
                return@withContext null
            }
        }

        // 4. Move the completed file from the hidden cache to the public directory
        val finalPublicPath = moveToPublicMusicDirectory(context, outputFile, finalFileName)
        
        if (finalPublicPath != null) {
            outputFile.delete() // Wipe the hidden copy to save space
            return@withContext finalPublicPath
        }

        // Fallback just in case the system completely blocks public access
        return@withContext outputFile.absolutePath
    }
    
    private fun moveToPublicMusicDirectory(context: Context, tempFile: File, fileName: String): String? {
        val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "PixelMusic")
        if (!publicMusicDir.exists()) {
            publicMusicDir.mkdirs()
        }
        val finalFile = File(publicMusicDir, fileName)

        // Strategy A: Direct Stream Copy (Works natively on Android 11+ and Android 9-)
        try {
            tempFile.inputStream().use { input ->
                finalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(finalFile.absolutePath), arrayOf("audio/webm"), null)
            return finalFile.absolutePath
        } catch (e: Exception) {
            // Strategy B: MediaStore API (Fallback specifically for Android 10 Scoped Storage)
            try {
                val resolver = context.contentResolver
                val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/webm")
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/PixelMusic")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    } else {
                        put(MediaStore.Audio.Media.DATA, finalFile.absolutePath)
                    }
                }

                val newUri = resolver.insert(audioCollection, contentValues) ?: return null

                resolver.openOutputStream(newUri)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(newUri, contentValues, null, null)
                }
                
                // Return the actual file path so ExoPlayer and the database don't crash
                return finalFile.absolutePath
                
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                return null
            }
        }
    }

    fun copyToPublicDownload(context: Context, sourceFilePath: String, songTitle: String, artistName: String): File? {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) return null

            val safeTitle = songTitle.replace(Regex("[\\\\/:*?\"\\<>|]"), "_")
            val safeArtist = artistName.replace(Regex("[\\\\/:*?\"\\<>|]"), "_")
            val fileName = "$safeTitle - $safeArtist.webm"

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

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destinationFile.absolutePath),
                arrayOf("audio/webm"),
                null
            )

            return destinationFile
        } catch (e: Exception) {
            UmihiHelper.printe("Failed to copy to public downloads: ${e.message}", exception = e)
            return null
        }
    }
}
