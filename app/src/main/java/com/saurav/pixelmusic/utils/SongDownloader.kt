package com.saurav.pixelmusic.utils

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
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.repository.LyricsRepository
import kotlinx.coroutines.flow.first

object SongDownloader {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloaderEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
        fun lyricsRepository(): LyricsRepository
    }

    private const val CHANNEL_ID = "pixelmusic_song_download_channel"
    private const val CHUNK_SIZE = 2 * 1024 * 1024L // 2MB chunks for faster throughput and smooth progress

    private enum class NetworkProvider {
        OKHTTP_PRIMARY,
        HTTP_URL_CONNECTION,
        OKHTTP_SECONDARY
    }

    private data class DownloadChunk(
        val index: Int,
        val startByte: Long,
        val endByte: Long
    )

    private val downloadOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS) // No call timeout for file downloads
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build()
    }

    private val secondaryOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build()
    }

    // Global state controls for Pause/Cancel from notifications
    @Volatile var isPaused = false
    @Volatile var isCancelled = false
    private var isReceiverRegistered = false
    @Volatile private var lastNotificationUpdateTime = 0L

    private const val ACTION_PAUSE = "com.saurav.pixelmusic.DOWNLOAD_PAUSE"
    private const val ACTION_RESUME = "com.saurav.pixelmusic.DOWNLOAD_RESUME"
    private const val ACTION_CANCEL = "com.saurav.pixelmusic.DOWNLOAD_CANCEL"

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

            val ytSong = com.saurav.pixelmusic.data.model.youtube.Song(
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

            val entryPoint = try {
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    DownloaderEntryPoint::class.java
                )
            } catch (_: Exception) { null }
            val isEmbedFullMetadata = entryPoint?.userPreferencesRepository()?.isEmbedFullMetadataOnDownloadFlow?.first() ?: true

            var resolvedLyrics = lyricsText
            if (resolvedLyrics.isNullOrBlank() && isEmbedFullMetadata) {
                try {
                    val lyricsRepo = entryPoint?.lyricsRepository()
                    val lyricsObj = lyricsRepo?.getLyrics(song)
                    if (lyricsObj != null) {
                        resolvedLyrics = if (!lyricsObj.synced.isNullOrEmpty()) {
                            lyricsObj.synced.joinToString("\n") { line ->
                                val minutes = line.time / 60000
                                val seconds = (line.time % 60000) / 1000
                                val hundredths = (line.time % 1000) / 10
                                String.format(Locale.US, "[%02d:%02d.%02d]%s", minutes, seconds, hundredths, line.line)
                            }
                        } else {
                            lyricsObj.plain?.joinToString("\n")
                        }
                    }
                } catch (_: Exception) {}
            }

            val rawArtworkUrl = song.albumArtUriString
            val artworkUrl = if (!rawArtworkUrl.isNullOrBlank()) {
                com.saurav.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(rawArtworkUrl)
                    ?: rawArtworkUrl
            } else null

            val imageDownloadJob = async(Dispatchers.IO) {
                if (!artworkUrl.isNullOrBlank()) {
                    val providers = listOf("OKHTTP", "HTTP_URL_CONNECTION", "SECONDARY_OKHTTP")
                    for (prov in providers) {
                        try {
                            when (prov) {
                                "OKHTTP" -> {
                                    val req = okhttp3.Request.Builder()
                                        .url(artworkUrl)
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                        .build()
                                    downloadOkHttpClient.newCall(req).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            resp.body.byteStream().use { input ->
                                                FileOutputStream(tempImageFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        }
                                    }
                                }
                                "HTTP_URL_CONNECTION" -> {
                                    val conn = URL(artworkUrl).openConnection() as HttpURLConnection
                                    conn.connectTimeout = 15_000
                                    conn.readTimeout = 15_000
                                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    try {
                                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                            conn.inputStream.use { input ->
                                                FileOutputStream(tempImageFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        }
                                    } finally {
                                        conn.disconnect()
                                    }
                                }
                                "SECONDARY_OKHTTP" -> {
                                    val req = okhttp3.Request.Builder()
                                        .url(artworkUrl)
                                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                        .build()
                                    secondaryOkHttpClient.newCall(req).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            resp.body.byteStream().use { input ->
                                                FileOutputStream(tempImageFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (tempImageFile.exists() && tempImageFile.length() > 0) {
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            val totalBytes = probeTotalBytes(streamUrl)
            val activeStreamUrl = AtomicReference(streamUrl)
            val totalDownloaded = AtomicLong(0L)

            if (totalBytes > 0L) {
                val chunks = mutableListOf<DownloadChunk>()
                var byteOffset = 0L
                var chunkIdx = 0
                while (byteOffset < totalBytes) {
                    val end = minOf(byteOffset + CHUNK_SIZE - 1, totalBytes - 1)
                    chunks.add(DownloadChunk(chunkIdx++, byteOffset, end))
                    byteOffset = end + 1
                }

                val chunkQueue = ConcurrentLinkedQueue(chunks)
                val raf = RandomAccessFile(tempAudioFile, "rw")
                val rafLock = Any()

                try {
                    raf.setLength(totalBytes)

                    coroutineScope {
                        val workerPrimary = launch(Dispatchers.IO) {
                            val buffer = ByteArray(64 * 1024)
                            while (isActive && !isCancelled) {
                                while (isPaused) {
                                    if (isCancelled) throw Exception("Cancelled by user")
                                    notificationBuilder.setContentText("${playlistProgress?.let { "$it - " } ?: ""}Paused")
                                    updateLiveProgress(context, notificationManager, notificationId, notificationBuilder, totalDownloaded.get(), totalBytes, playlistProgress)
                                    delay(1000)
                                }
                                val chunk = chunkQueue.poll() ?: break
                                downloadChunkWithRetry(
                                    chunk = chunk,
                                    preferredProvider = NetworkProvider.OKHTTP_PRIMARY,
                                    activeUrlRef = activeStreamUrl,
                                    ytSong = ytSong,
                                    context = context,
                                    raf = raf,
                                    rafLock = rafLock,
                                    buffer = buffer,
                                    totalDownloaded = totalDownloaded,
                                    totalBytes = totalBytes,
                                    notificationManager = notificationManager,
                                    notificationId = notificationId,
                                    notificationBuilder = notificationBuilder,
                                    playlistProgress = playlistProgress
                                )
                            }
                        }

                        val workerSecondary = launch(Dispatchers.IO) {
                            val buffer = ByteArray(64 * 1024)
                            while (isActive && !isCancelled) {
                                while (isPaused) {
                                    if (isCancelled) throw Exception("Cancelled by user")
                                    delay(1000)
                                }
                                val chunk = chunkQueue.poll() ?: break
                                downloadChunkWithRetry(
                                    chunk = chunk,
                                    preferredProvider = NetworkProvider.HTTP_URL_CONNECTION,
                                    activeUrlRef = activeStreamUrl,
                                    ytSong = ytSong,
                                    context = context,
                                    raf = raf,
                                    rafLock = rafLock,
                                    buffer = buffer,
                                    totalDownloaded = totalDownloaded,
                                    totalBytes = totalBytes,
                                    notificationManager = notificationManager,
                                    notificationId = notificationId,
                                    notificationBuilder = notificationBuilder,
                                    playlistProgress = playlistProgress
                                )
                            }
                        }

                        workerPrimary.join()
                        workerSecondary.join()
                    }
                } finally {
                    try { raf.close() } catch (_: Exception) {}
                }
            } else {
                // Fallback: sequential chunk streaming with alternating providers
                var startByte = 0L
                var isFinished = false
                var chunkIdx = 0
                val buffer = ByteArray(64 * 1024)

                FileOutputStream(tempAudioFile, true).use { output ->
                    while (!isFinished) {
                        if (isCancelled) throw Exception("Cancelled by user")
                        while (isPaused) {
                            if (isCancelled) throw Exception("Cancelled by user")
                            notificationBuilder.setContentText("${playlistProgress?.let { "$it - " } ?: ""}Paused")
                            updateLiveProgress(context, notificationManager, notificationId, notificationBuilder, totalDownloaded.get(), totalBytes, playlistProgress)
                            delay(1000)
                        }

                        val endByte = startByte + CHUNK_SIZE - 1
                        val provider = when (chunkIdx % 3) {
                            0 -> NetworkProvider.OKHTTP_PRIMARY
                            1 -> NetworkProvider.HTTP_URL_CONNECTION
                            else -> NetworkProvider.OKHTTP_SECONDARY
                        }

                        var chunkReadTotal = 0L
                        val providersToTry = when (provider) {
                            NetworkProvider.OKHTTP_PRIMARY -> listOf(NetworkProvider.OKHTTP_PRIMARY, NetworkProvider.HTTP_URL_CONNECTION, NetworkProvider.OKHTTP_SECONDARY)
                            NetworkProvider.HTTP_URL_CONNECTION -> listOf(NetworkProvider.HTTP_URL_CONNECTION, NetworkProvider.OKHTTP_PRIMARY, NetworkProvider.OKHTTP_SECONDARY)
                            NetworkProvider.OKHTTP_SECONDARY -> listOf(NetworkProvider.OKHTTP_SECONDARY, NetworkProvider.HTTP_URL_CONNECTION, NetworkProvider.OKHTTP_PRIMARY)
                        }

                        var downloadedThisChunk = false
                        for (p in providersToTry) {
                            try {
                                when (p) {
                                    NetworkProvider.OKHTTP_PRIMARY -> {
                                        downloadRangeOkHttp(downloadOkHttpClient, activeStreamUrl.get(), startByte, endByte, buffer) { data, len ->
                                            output.write(data, 0, len)
                                            chunkReadTotal += len
                                            val d = totalDownloaded.addAndGet(len.toLong())
                                            startByte += len
                                            maybeUpdateNotification(context, notificationManager, notificationId, notificationBuilder, d, totalBytes, playlistProgress)
                                        }
                                    }
                                    NetworkProvider.HTTP_URL_CONNECTION -> {
                                        downloadRangeHttpUrlConnection(activeStreamUrl.get(), startByte, endByte, buffer) { data, len ->
                                            output.write(data, 0, len)
                                            chunkReadTotal += len
                                            val d = totalDownloaded.addAndGet(len.toLong())
                                            startByte += len
                                            maybeUpdateNotification(context, notificationManager, notificationId, notificationBuilder, d, totalBytes, playlistProgress)
                                        }
                                    }
                                    NetworkProvider.OKHTTP_SECONDARY -> {
                                        downloadRangeOkHttp(secondaryOkHttpClient, activeStreamUrl.get(), startByte, endByte, buffer) { data, len ->
                                            output.write(data, 0, len)
                                            chunkReadTotal += len
                                            val d = totalDownloaded.addAndGet(len.toLong())
                                            startByte += len
                                            maybeUpdateNotification(context, notificationManager, notificationId, notificationBuilder, d, totalBytes, playlistProgress)
                                        }
                                    }
                                }
                                downloadedThisChunk = true
                                break
                            } catch (_: Exception) {}
                        }

                        if (!downloadedThisChunk) {
                            throw java.io.IOException("Failed to download chunk at offset $startByte across all providers")
                        }

                        if (!isCancelled && !isPaused && chunkReadTotal < CHUNK_SIZE) {
                            isFinished = true
                        }
                        chunkIdx++
                    }
                    output.flush()
                }
            }

            if (isCancelled) throw Exception("Cancelled by user")

            val finalDownloadedBytes = totalDownloaded.get()
            notificationBuilder
                .setContentText("Processing audio file...")
                .setProgress(100, 100, true)
                .clearActions()
            updateNotification(notificationManager, notificationId, notificationBuilder)

            val extractor = MediaExtractor().apply {
                setDataSource(tempAudioFile.absolutePath)
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

                val muxer = MediaMuxer(tempRemuxedFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocateDirect(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize <= 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    if (!extractor.advance()) break
                }

                muxer.stop()
                muxer.release()
                extractor.release()
            } else {
                extractor.release()
                throw Exception("No audio track found in downloaded file")
            }

            try {
                kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    imageDownloadJob.await()
                }
            } catch (_: Exception) {
            }

            notificationBuilder.setContentText("Writing metadata...")
            updateNotification(notificationManager, notificationId, notificationBuilder)

try {
    android.os.ParcelFileDescriptor.open(tempRemuxedFile, android.os.ParcelFileDescriptor.MODE_READ_WRITE).use { fd ->
        val metadataFd = fd.dup()
        val existingMetadata = com.kyant.taglib.TagLib.getMetadata(metadataFd.detachFd())
        val propertyMap = java.util.HashMap(existingMetadata?.propertyMap ?: emptyMap())

        propertyMap["TITLE"] = arrayOf(song.title)
        propertyMap["ARTIST"] = arrayOf(song.displayArtist)
        if (!song.album.isNullOrBlank()) {
            propertyMap["ALBUM"] = arrayOf(song.album)
        }
        if (isEmbedFullMetadata) {
            if (song.displayArtist.isNotBlank()) {
                propertyMap["ALBUMARTIST"] = arrayOf(song.displayArtist)
            }
            if (song.year > 0) {
                propertyMap["DATE"] = arrayOf(song.year.toString())
                propertyMap["YEAR"] = arrayOf(song.year.toString())
            }
            if (song.trackNumber > 0) {
                propertyMap["TRACKNUMBER"] = arrayOf(song.trackNumber.toString())
            }
            if (!song.genre.isNullOrBlank()) {
                propertyMap["GENRE"] = arrayOf(song.genre)
            }
            if (!song.albumArtist.isNullOrBlank()) {
                propertyMap["ALBUMARTIST"] = arrayOf(song.albumArtist)
            }
            if (!resolvedLyrics.isNullOrBlank()) {
                propertyMap["LYRICS"] = arrayOf(resolvedLyrics)
            }
            propertyMap["COMMENT"] = arrayOf("Downloaded via PixelMusic")
        } else {
            if (!lyricsText.isNullOrBlank()) {
                propertyMap["LYRICS"] = arrayOf(lyricsText)
            }
        }

        com.kyant.taglib.TagLib.savePropertyMap(fd.dup().detachFd(), propertyMap)
        
        if (tempImageFile.exists() && tempImageFile.length() > 0) {
            val picture = com.kyant.taglib.Picture(
                data = tempImageFile.readBytes(),
                description = "Front Cover",
                pictureType = "Front Cover",
                mimeType = "image/jpeg"
            )
            com.kyant.taglib.TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))
        }
    }
} catch (e: Exception) {
    e.printStackTrace()
    // Secondary tagger fallback: JAudioTagger
    try {
        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(tempRemuxedFile)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, song.title)
        tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, song.displayArtist)
        if (!song.album.isNullOrBlank()) {
            tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, song.album)
        }
        if (isEmbedFullMetadata) {
            if (song.year > 0) tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, song.year.toString())
            if (!song.genre.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.GENRE, song.genre)
            if (!resolvedLyrics.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, resolvedLyrics)
        }
        if (tempImageFile.exists() && tempImageFile.length() > 0) {
            val artwork = org.jaudiotagger.tag.images.StandardArtwork.createArtworkFromFile(tempImageFile)
            tag.setField(artwork)
        }
        audioFile.commit()
    } catch (_: Exception) {}
}
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.TITLE, song.title)
                put(MediaStore.Audio.Media.ARTIST, song.displayArtist)
                if (!song.album.isNullOrBlank()) {
                    put(MediaStore.Audio.Media.ALBUM, song.album)
                }
                if (song.year > 0) {
                    put(MediaStore.Audio.Media.YEAR, song.year)
                }
                if (song.trackNumber > 0) {
                    put(MediaStore.Audio.Media.TRACK, song.trackNumber)
                }
                if (!song.genre.isNullOrBlank()) {
                    put(MediaStore.Audio.Media.GENRE, song.genre)
                }
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/PixelMusic")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                tempRemuxedFile.inputStream().use { inputStream ->
                    val copyBuffer = ByteArray(64 * 1024)
                    var readCount: Int
                    while (inputStream.read(copyBuffer).also { readCount = it } != -1) {
                        outputStream.write(copyBuffer, 0, readCount)
                    }
                    outputStream.flush()
                }
            }

            val playIntent = Intent(context, Class.forName("com.saurav.pixelmusic.MainActivity")).apply {
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
                .setContentText("Tap to play offline (${formatMb(finalDownloadedBytes)})")
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

    private fun probeTotalBytes(url: String): Long {
        var total = parseTotalBytesFromUrl(url)
        if (total > 0L) return total

        try {
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            downloadOkHttpClient.newCall(req).execute().use { resp ->
                val range = resp.header("Content-Range")
                if (range != null && range.contains("/")) {
                    total = range.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                }
                if (total <= 0L && resp.isSuccessful) {
                    total = resp.body.contentLength()
                }
            }
        } catch (_: Exception) {}

        if (total > 0L) return total

        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-0")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }
            val range = conn.getHeaderField("Content-Range")
            if (range != null && range.contains("/")) {
                total = range.substringAfterLast("/").trim().toLongOrNull() ?: -1L
            }
            conn.disconnect()
        } catch (_: Exception) {}

        return total
    }

    private fun downloadRangeOkHttp(
        client: OkHttpClient,
        url: String,
        startByte: Long,
        endByte: Long,
        buffer: ByteArray,
        onBytesRead: (ByteArray, Int) -> Unit
    ): Long {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Range", "bytes=$startByte-$endByte")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            if (!response.isSuccessful && code != 206) {
                throw java.io.IOException("OkHttp chunk download failed. HTTP Code: $code")
            }
            val body = response.body
            val inputStream = body.byteStream()
            var totalRead = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled || isPaused) break
                onBytesRead(buffer, bytesRead)
                totalRead += bytesRead
            }
            return totalRead
        }
    }

    private fun downloadRangeHttpUrlConnection(
        url: String,
        startByte: Long,
        endByte: Long,
        buffer: ByteArray,
        onBytesRead: (ByteArray, Int) -> Unit
    ): Long {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("Range", "bytes=$startByte-$endByte")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            instanceFollowRedirects = true
        }

        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw java.io.IOException("HttpURLConnection chunk failed. HTTP Code: $code")
            }
            val inputStream = connection.inputStream
            var totalRead = 0L
            var bytesRead: Int
            inputStream.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled || isPaused) break
                    onBytesRead(buffer, bytesRead)
                    totalRead += bytesRead
                }
            }
            return totalRead
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadChunkWithRetry(
        chunk: DownloadChunk,
        preferredProvider: NetworkProvider,
        activeUrlRef: AtomicReference<String>,
        ytSong: com.saurav.pixelmusic.data.model.youtube.Song,
        context: Context,
        raf: RandomAccessFile,
        rafLock: Any,
        buffer: ByteArray,
        totalDownloaded: AtomicLong,
        totalBytes: Long,
        notificationManager: NotificationManager,
        notificationId: Int,
        notificationBuilder: NotificationCompat.Builder,
        playlistProgress: String?
    ) {
        val maxRetries = 3
        var attempt = 0
        var success = false
        var lastErr: Exception? = null

        val providersToTry = when (preferredProvider) {
            NetworkProvider.OKHTTP_PRIMARY -> listOf(
                NetworkProvider.OKHTTP_PRIMARY,
                NetworkProvider.HTTP_URL_CONNECTION,
                NetworkProvider.OKHTTP_SECONDARY
            )
            NetworkProvider.HTTP_URL_CONNECTION -> listOf(
                NetworkProvider.HTTP_URL_CONNECTION,
                NetworkProvider.OKHTTP_PRIMARY,
                NetworkProvider.OKHTTP_SECONDARY
            )
            NetworkProvider.OKHTTP_SECONDARY -> listOf(
                NetworkProvider.OKHTTP_SECONDARY,
                NetworkProvider.HTTP_URL_CONNECTION,
                NetworkProvider.OKHTTP_PRIMARY
            )
        }

        while (attempt < maxRetries && !success) {
            if (isCancelled) throw Exception("Cancelled by user")
            val provider = providersToTry[attempt % providersToTry.size]
            val currentUrl = activeUrlRef.get()
            var chunkBytesWritten = 0L

            try {
                when (provider) {
                    NetworkProvider.OKHTTP_PRIMARY -> {
                        downloadRangeOkHttp(
                            client = downloadOkHttpClient,
                            url = currentUrl,
                            startByte = chunk.startByte,
                            endByte = chunk.endByte,
                            buffer = buffer
                        ) { data, len ->
                            synchronized(rafLock) {
                                raf.seek(chunk.startByte + chunkBytesWritten)
                                raf.write(data, 0, len)
                            }
                            chunkBytesWritten += len
                            val downloadedSoFar = totalDownloaded.addAndGet(len.toLong())
                            maybeUpdateNotification(
                                context, notificationManager, notificationId,
                                notificationBuilder, downloadedSoFar, totalBytes, playlistProgress
                            )
                        }
                    }
                    NetworkProvider.HTTP_URL_CONNECTION -> {
                        downloadRangeHttpUrlConnection(
                            url = currentUrl,
                            startByte = chunk.startByte,
                            endByte = chunk.endByte,
                            buffer = buffer
                        ) { data, len ->
                            synchronized(rafLock) {
                                raf.seek(chunk.startByte + chunkBytesWritten)
                                raf.write(data, 0, len)
                            }
                            chunkBytesWritten += len
                            val downloadedSoFar = totalDownloaded.addAndGet(len.toLong())
                            maybeUpdateNotification(
                                context, notificationManager, notificationId,
                                notificationBuilder, downloadedSoFar, totalBytes, playlistProgress
                            )
                        }
                    }
                    NetworkProvider.OKHTTP_SECONDARY -> {
                        downloadRangeOkHttp(
                            client = secondaryOkHttpClient,
                            url = currentUrl,
                            startByte = chunk.startByte,
                            endByte = chunk.endByte,
                            buffer = buffer
                        ) { data, len ->
                            synchronized(rafLock) {
                                raf.seek(chunk.startByte + chunkBytesWritten)
                                raf.write(data, 0, len)
                            }
                            chunkBytesWritten += len
                            val downloadedSoFar = totalDownloaded.addAndGet(len.toLong())
                            maybeUpdateNotification(
                                context, notificationManager, notificationId,
                                notificationBuilder, downloadedSoFar, totalBytes, playlistProgress
                            )
                        }
                    }
                }
                success = true
            } catch (e: Exception) {
                if (isCancelled) throw e
                lastErr = e
                if (chunkBytesWritten > 0L) {
                    totalDownloaded.addAndGet(-chunkBytesWritten)
                }
                if (e.message?.contains("403") == true || e.message?.contains("410") == true) {
                    try {
                        YoutubeHelper.invalidateStreamCache(ytSong.youtubeId)
                        val freshUrl = YoutubeHelper.getDownloadUrl(context, ytSong)
                        if (freshUrl.isNotBlank()) {
                            activeUrlRef.set(freshUrl)
                        }
                    } catch (_: Exception) {}
                }
                attempt++
                delay(300L * attempt)
            }
        }

        if (!success) {
            throw lastErr ?: java.io.IOException("Failed to download chunk ${chunk.index} after $maxRetries attempts across providers")
        }
    }

    private fun maybeUpdateNotification(
        context: Context,
        notificationManager: NotificationManager,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        currentBytes: Long,
        totalBytes: Long,
        playlistProgress: String?
    ) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime > 400L) {
            lastNotificationUpdateTime = now
            updateLiveProgress(context, notificationManager, notificationId, builder, currentBytes, totalBytes, playlistProgress)
        }
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
