package com.unshoo.pixelmusic.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale

object SongDownloader {

    private const val CHANNEL_ID = "pixelmusic_song_download_channel"
    private const val CHUNK_SIZE = 5 * 1024 * 1024L // 5MB chunks

    // Global state controls for Pause/Cancel from notifications
    @Volatile var isPaused = false
    @Volatile var isCancelled = false
    private var isReceiverRegistered = false

    private const val ACTION_PAUSE = "com.unshoo.pixelmusic.DOWNLOAD_PAUSE"
    private const val ACTION_RESUME = "com.unshoo.pixelmusic.DOWNLOAD_RESUME"
    private const val ACTION_CANCEL = "com.unshoo.pixelmusic.DOWNLOAD_CANCEL"

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> isPaused = true
                ACTION_RESUME -> isPaused = false
                ACTION_CANCEL -> {
                    isCancelled = true
                    isPaused = false // unblock loop if paused
                }
            }
        }
    }

    private fun ensureReceiverRegistered(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PAUSE)
                addAction(ACTION_RESUME)
                addAction(ACTION_CANCEL)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.applicationContext.registerReceiver(controlReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    suspend fun downloadAndTagSong(
        context: Context,
        song: Song,
        lyricsText: String? = null,
        playlistProgress: String? = null // New parameter to show "Playlist: 5/200"
    ): Boolean = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
        
        ensureReceiverRegistered(context)
        isCancelled = false

        var tempAudioFile: File? = null
        var tempRemuxedFile: File? = null
        var tempImageFile: File? = null

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (song.youtubeId ?: song.id).hashCode()

        createNotificationChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading: ${song.title}")
            .setContentText(playlistProgress ?: "Connecting...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)

        try {
            updateLiveProgress(context, notificationManager, notificationId, notificationBuilder, 0L, 0L, playlistProgress)

            val ytSong = com.unshoo.pixelmusic.data.model.youtube.Song(
                youtubeId = song.youtubeId ?: song.id.removePrefix("youtube_"),
                title = song.title,
                artist = song.displayArtist,
                duration = "",
                thumbnailHref = song.albumArtUriString ?: ""
            )
            val streamUrl = YoutubeHelper.getDownloadUrl(context, ytSong)
            if (streamUrl.isBlank()) throw Exception("Could not resolve stream URL")

            val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val cleanArtist = song.displayArtist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$cleanTitle - $cleanArtist.m4a"

            tempAudioFile = File(context.cacheDir, "raw_$fileName")
            tempRemuxedFile = File(context.cacheDir, "clean_$fileName")
            tempImageFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")

            val imageDownloadJob = async(Dispatchers.IO) {
                if (!song.albumArtUriString.isNullOrBlank()) {
                    try {
                        val url = URL(song.albumArtUriString!!)
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 15000
                            instanceFollowRedirects = true
                        }
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.use { input ->
                                FileOutputStream(tempImageFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        connection.disconnect()
                    } catch (_: Exception) {}
                }
            }

            // Support exact byte resumption
            var startByte = if (tempAudioFile!!.exists()) tempAudioFile!!.length() else 0L
            var totalBytes = parseTotalBytesFromUrl(streamUrl)
            var totalDownloaded = startByte
            var isFinished = false
            var lastNotificationUpdateTime = 0L

            // Use 'true' to append to file in case we paused/resumed
            FileOutputStream(tempAudioFile, true).use { output ->
                while (!isFinished) {
                    if (isCancelled) throw Exception("Cancelled by user")

                    // Suspend the loop gracefully if paused
                    while (isPaused) {
                        if (isCancelled) throw Exception("Cancelled by user")
                        notificationBuilder.setContentText("${playlistProgress?.let { "$it - " } ?: ""}Paused")
                        updateLiveProgress(context, notificationManager, notificationId, notificationBuilder, totalDownloaded, totalBytes, playlistProgress)
                        delay(1000)
                    }

                    val endByte = startByte + CHUNK_SIZE - 1
                    val connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 60000
                        requestMethod = "GET"
                        setRequestProperty("Range", "bytes=$startByte-$endByte")
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        setRequestProperty("Origin", "https://music.youtube.com")
                        setRequestProperty("Referer", "https://music.youtube.com/")
                        instanceFollowRedirects = true
                    }

                    try {
                        connection.connect()
                        val responseCode = connection.responseCode
                        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                            throw Exception("Chunk download failed. HTTP Code: $responseCode")
                        }

                        if (totalBytes <= 0) {
                            val contentRange = connection.getHeaderField("Content-Range")
                            if (contentRange != null && contentRange.contains("/")) {
                                totalBytes = contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                            }
                            if (totalBytes <= 0 && responseCode == HttpURLConnection.HTTP_OK) {
                                totalBytes = connection.contentLengthLong
                            }
                        }

                        val inputStream = connection.inputStream
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var chunkReadTotal = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (isCancelled || isPaused) {
                                break // Drop connection instantly to pause or cancel
                            }

                            output.write(buffer, 0, bytesRead)
                            chunkReadTotal += bytesRead
                            totalDownloaded += bytesRead
                            startByte += bytesRead // Precisely track bytes for seamless resume

                            val now = System.currentTimeMillis()
                            if (now - lastNotificationUpdateTime > 500L) {
                                lastNotificationUpdateTime = now
                                updateLiveProgress(
                                    context,
                                    notificationManager,
                                    notificationId,
                                    notificationBuilder,
                                    totalDownloaded,
                                    totalBytes,
                                    playlistProgress
                                )
                            }
                        }

                        if (!isCancelled && !isPaused && chunkReadTotal < CHUNK_SIZE) {
                            isFinished = true
                        }
                        inputStream.close()
                    } finally {
                        connection.disconnect()
                    }
                }
                output.flush()
            }

            if (isCancelled) throw Exception("Cancelled by user")

            notificationBuilder
                .setContentText("Processing audio file...")
                .setProgress(100, 100, true)
                .clearActions()
            updateNotification(notificationManager, notificationId, notificationBuilder)

            val extractor = MediaExtractor().apply {
                setDataSource(tempAudioFile!!.absolutePath)
            }

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
                val format = extractor.getTrackFormat(audioTrackIndex)

                val muxer = MediaMuxer(tempRemuxedFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocateDirect(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()
            } else {
                extractor.release()
                throw Exception("No audio track found in downloaded file")
            }

            imageDownloadJob.await()

            notificationBuilder.setContentText("Writing metadata...")
            updateNotification(notificationManager, notificationId, notificationBuilder)

try {
    android.os.ParcelFileDescriptor.open(tempRemuxedFile!!, android.os.ParcelFileDescriptor.MODE_READ_WRITE).use { fd ->
        val metadataFd = fd.dup()
        val existingMetadata = com.kyant.taglib.TagLib.getMetadata(metadataFd.detachFd())
        val propertyMap = java.util.HashMap(existingMetadata?.propertyMap ?: emptyMap())

        propertyMap["TITLE"] = arrayOf(song.title)
        propertyMap["ARTIST"] = arrayOf(song.displayArtist)
        if (!song.album.isNullOrBlank()) {
            propertyMap["ALBUM"] = arrayOf(song.album)
        }
        if (!lyricsText.isNullOrBlank()) {
            propertyMap["LYRICS"] = arrayOf(lyricsText)
        }

        com.kyant.taglib.TagLib.savePropertyMap(fd.dup().detachFd(), propertyMap)
        
        if (tempImageFile!!.exists() && tempImageFile!!.length() > 0) {
            val picture = com.kyant.taglib.Picture(
                data = tempImageFile!!.readBytes(),
                description = "Front Cover",
                pictureType = "Front Cover",
                mimeType = "image/jpeg"
            )
            com.kyant.taglib.TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))
        }
    }
} catch (e: Exception) {
    e.printStackTrace()
    // If TagLib fails, the download won't crash. 
    // MediaStore will still receive the metadata below this block.
}
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.TITLE, song.title)
                put(MediaStore.Audio.Media.ARTIST, song.displayArtist)
                if (!song.album.isNullOrBlank()) {
                    put(MediaStore.Audio.Media.ALBUM, song.album)
                }
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/PixelMusic")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempRemuxedFile!!.inputStream().use { inputStream ->
                    val copyBuffer = ByteArray(64 * 1024)
                    var readCount: Int
                    while (inputStream.read(copyBuffer).also { readCount = it } != -1) {
                        outputStream.write(copyBuffer, 0, readCount)
                    }
                    outputStream.flush()
                }
            }

            val playIntent = Intent(context, Class.forName("com.unshoo.pixelmusic.MainActivity")).apply {
                action = "PLAY_DOWNLOADED_SONG"
                putExtra("song_id", song.id)
                putExtra("ACTION_SHOW_PLAYER", true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 
                notificationId, 
                playIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .setContentTitle("Downloaded: ${song.title}")
                .setContentText("Tap to play offline (${formatMb(totalDownloaded)})")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .clearActions()
                .setContentIntent(pendingIntent)
            updateNotification(notificationManager, notificationId, notificationBuilder)

            return@withContext true

        } catch (e: Exception) {
            if (e.message == "Cancelled by user") {
                notificationManager.cancel(notificationId)
            } else {
                notificationBuilder
                    .setContentTitle("Download failed")
                    .setContentText(e.message ?: "Unknown error")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .clearActions()
                updateNotification(notificationManager, notificationId, notificationBuilder)
            }
            return@withContext false
        } finally {
            tempAudioFile?.takeIf { it.exists() }?.delete()
            tempRemuxedFile?.takeIf { it.exists() }?.delete()
            tempImageFile?.takeIf { it.exists() }?.delete()
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Song Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress and controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getActionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(action).apply { setPackage(context.packageName) }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateLiveProgress(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        currentBytes: Long,
        totalBytes: Long,
        playlistProgress: String?
    ) {
        builder.clearActions()
        
        if (isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", getActionPendingIntent(context, ACTION_RESUME))
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", getActionPendingIntent(context, ACTION_PAUSE))
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", getActionPendingIntent(context, ACTION_CANCEL))

        val prefix = playlistProgress?.let { "$it - " } ?: ""

        if (totalBytes > 0) {
            val progressPercent = ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            val currentMb = formatMb(currentBytes)
            val totalMb = formatMb(totalBytes)
            val statusText = if (isPaused) "Paused" else "$currentMb / $totalMb ($progressPercent%)"
            
            builder
                .setContentText(prefix + statusText)
                .setProgress(100, progressPercent, false)
        } else {
            val statusText = if (isPaused) "Paused" else "${formatMb(currentBytes)} downloaded"
            builder
                .setContentText(prefix + statusText)
                .setProgress(0, 0, true)
        }
        updateNotification(notificationManager, notificationId, builder)
    }

    private fun updateNotification(
        notificationManager: NotificationManager,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun parseTotalBytesFromUrl(url: String): Long {
        return try {
            val clen = url.substringAfter("clen=", "").substringBefore("&")
            if (clen.isNotEmpty()) clen.toLongOrNull() ?: -1L else -1L
        } catch (_: Exception) {
            -1L
        }
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
