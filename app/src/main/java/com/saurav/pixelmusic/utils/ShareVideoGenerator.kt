package com.saurav.pixelmusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.saurav.pixelmusic.data.database.youtube.AppDatabase
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object ShareVideoGenerator {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun resolveAudioFile(
        context: Context,
        song: Song,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Resolving audio for '${song.title}', id='${song.id}', path='${song.path}', uri='${song.contentUriString}'")

        // 1. Direct local file path
        if (song.path.isNotBlank()) {
            val file = File(song.path)
            if (file.exists() && file.length() > 0 && file.canRead()) {
                PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Local file exists: ${file.absolutePath} (${file.length()} bytes)")
                onProgress(0.35f, "Found local audio track…")
                return@withContext file
            }
        }

        // 2. Local content URI or file URI: copy to a temporary audio file in cache
        val contentUriStr = song.contentUriString
        if (contentUriStr.isNotBlank() && (contentUriStr.startsWith("content://") || contentUriStr.startsWith("file://") || contentUriStr.startsWith("/"))) {
            try {
                val uri = if (contentUriStr.startsWith("/")) Uri.fromFile(File(contentUriStr)) else Uri.parse(contentUriStr)
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: contentUriStr)
                    if (f.exists() && f.length() > 0) {
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "File URI valid: ${f.absolutePath}")
                        onProgress(0.35f, "Found local audio track…")
                        return@withContext f
                    }
                } else if (uri.scheme == "content") {
                    val tempAudioFile = File(context.cacheDir, "share_cards/temp_content_${System.currentTimeMillis()}.m4a")
                    tempAudioFile.parentFile?.mkdirs()
                    onProgress(0.15f, "Extracting audio from storage…")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempAudioFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Extracted content URI to cache file: ${tempAudioFile.length()} bytes")
                        onProgress(0.35f, "Local audio track ready…")
                        return@withContext tempAudioFile
                    }
                }
            } catch (e: Exception) {
                PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Error resolving content URI: $contentUriStr", e)
            }
        }

        // 3. YouTube downloaded audio file in local database
        val videoId = song.youtubeId
            ?: if (song.id.startsWith("youtube_")) song.id.substringAfter("youtube_")
            else if (song.contentUriString.startsWith("youtube://")) song.contentUriString.substringAfter("youtube://")
            else null

        if (!videoId.isNullOrBlank()) {
            PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Checking YouTube database for videoId=$videoId")
            try {
                val db = AppDatabase.getInstance(context)
                val ytSong = db.songRepository().getSong(videoId)
                if (ytSong?.audioFilePath != null) {
                    val f = File(ytSong.audioFilePath)
                    if (f.exists() && f.length() > 0) {
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Found downloaded YouTube audio file: ${f.absolutePath}")
                        onProgress(0.35f, "Found offline audio track…")
                        return@withContext f
                    }
                }
            } catch (e: Exception) {
                PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Error querying YouTube DB", e)
            }

            // Check if audio file for this video was already cached from a previous attempt
            val cachedShareAudio = File(context.cacheDir, "share_cards/yt_audio_${videoId}.m4a")
            if (cachedShareAudio.exists() && cachedShareAudio.length() > 50_000) {
                PixelLogger.i(PixelLogger.Category.PLAYER, "ShareVideo", "Reusing cached audio file: ${cachedShareAudio.absolutePath} (${cachedShareAudio.length()} bytes)")
                onProgress(0.35f, "Audio track ready…")
                return@withContext cachedShareAudio
            }

            // 4. Online stream URL: download audio stream to temporary cache file
            try {
                onProgress(0.05f, "Connecting to audio stream…")
                PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Fetching stream URL for YouTube track: $videoId")
                val ytModelSong = com.saurav.pixelmusic.data.model.youtube.Song(
                    youtubeId = videoId,
                    title = song.title,
                    artist = song.artist,
                    duration = song.duration.toString()
                )
                val streamUrl = YoutubeHelper.getDownloadUrl(context, ytModelSong).ifBlank {
                    YoutubeHelper.getSongPlayerUrl(context, ytModelSong, allowLocal = true)
                }
                PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Resolved stream URL: ${streamUrl.take(70)}…")
                if (streamUrl.isNotBlank()) {
                    if (streamUrl.startsWith("/")) {
                        val f = File(streamUrl)
                        if (f.exists() && f.length() > 0) return@withContext f
                    } else if (streamUrl.startsWith("http")) {
                        downloadStreamToFile(streamUrl, cachedShareAudio, onProgress)
                        if (cachedShareAudio.exists() && cachedShareAudio.length() > 0) {
                            PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Downloaded stream to file: ${cachedShareAudio.length()} bytes")
                            onProgress(0.35f, "Audio track downloaded…")
                            return@withContext cachedShareAudio
                        }
                    }
                }
            } catch (e: Exception) {
                PixelLogger.e(PixelLogger.Category.PLAYER, "ShareVideo", "Failed to fetch and download online stream for $videoId", e)
            }
        }

        // 5. Fallback via MediaItemBuilder playback URI
        try {
            val fallbackUri = MediaItemBuilder.playbackUri(song)
            if (fallbackUri.scheme == "file") {
                val f = File(fallbackUri.path ?: "")
                if (f.exists() && f.length() > 0) return@withContext f
            } else if (fallbackUri.scheme == "content") {
                val tempAudioFile = File(context.cacheDir, "share_cards/temp_fallback_${System.currentTimeMillis()}.m4a")
                tempAudioFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(fallbackUri)?.use { input ->
                    FileOutputStream(tempAudioFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempAudioFile.exists() && tempAudioFile.length() > 0) return@withContext tempAudioFile
            }
        } catch (e: Exception) {
            PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Fallback playback URI extraction failed", e)
        }

        PixelLogger.e(PixelLogger.Category.PLAYER, "ShareVideo", "No audio source found for song: ${song.title}")
        return@withContext null
    }

    private fun downloadStreamToFile(
        url: String,
        destination: File,
        onProgress: (Float, String) -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        destination.parentFile?.mkdirs()
        val tempPartFile = File(destination.parentFile, "${destination.name}.part")
        if (tempPartFile.exists()) tempPartFile.delete()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download stream failed HTTP code: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw IOException("Response body is null")
            val totalBytes = body.contentLength()
            var bytesCopied = 0L
            val buffer = ByteArray(16384)

            val inStream = body.byteStream()
            val outStream = FileOutputStream(tempPartFile)
            try {
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                    bytesCopied += read
                    if (totalBytes > 0) {
                        val fraction = (bytesCopied.toFloat() / totalBytes).coerceIn(0f, 1f)
                        onProgress(0.05f + fraction * 0.30f, "Downloading audio: ${(fraction * 100).toInt()}%")
                    }
                }
                outStream.flush()
            } finally {
                outStream.close()
                inStream.close()
            }
            if (destination.exists()) destination.delete()
            tempPartFile.renameTo(destination)
        }
    }

    suspend fun prepareVideoCardFrame(context: Context, cardBitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val frameFile = File(cacheDir, "temp_video_frame_${System.currentTimeMillis()}.png")

        // Standard 720x1280 resolution (9:16 aspect ratio, even dimensions for H.264)
        val targetWidth = 720
        val targetHeight = 1280
        val scaledBitmap = Bitmap.createScaledBitmap(cardBitmap, targetWidth, targetHeight, true)

        FileOutputStream(frameFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaledBitmap != cardBitmap) {
            scaledBitmap.recycle()
        }
        PixelLogger.d(PixelLogger.Category.UI, "ShareVideo", "Prepared card frame PNG: ${frameFile.absolutePath} (${frameFile.length()} bytes)")
        frameFile
    }

    suspend fun generateVideo(
        context: Context,
        cardBitmap: Bitmap,
        song: Song,
        currentPositionMs: Long = 0L,
        desiredDurationSec: Int = 30,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File {
        PixelLogger.i(PixelLogger.Category.UI, "ShareVideo", "Starting video generation for '${song.title}', duration=${desiredDurationSec}s, currentPositionMs=$currentPositionMs")
        onProgress(0.02f, "Preparing audio track…")

        val audioFile = resolveAudioFile(context, song, onProgress)
            ?: throw IllegalStateException("Audio source could not be resolved or downloaded for this track.")

        onProgress(0.38f, "Rendering card visuals…")
        val frameFile = prepareVideoCardFrame(context, cardBitmap)

        val desiredClipDurationMs = (desiredDurationSec * 1000L).coerceAtLeast(5000L)
        val clipDurationMs = if (song.duration > 0) {
            minOf(desiredClipDurationMs, song.duration)
        } else {
            desiredClipDurationMs
        }

        val maxStart = if (song.duration > clipDurationMs) song.duration - clipDurationMs else 0L
        val startPositionMs = if (currentPositionMs > 0L) {
            currentPositionMs.coerceIn(0L, maxStart)
        } else {
            if (song.duration > clipDurationMs * 2) {
                (song.duration * 0.25f).toLong().coerceIn(0L, maxStart)
            } else {
                0L
            }
        }
        val endPositionMs = startPositionMs + clipDurationMs
        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Clip bounds: start=${startPositionMs}ms, end=${endPositionMs}ms, duration=${clipDurationMs}ms")

        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val outputFile = File(cacheDir, "pixelmusic_video_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        onProgress(0.40f, "Encoding 30s video…")

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val imageMediaItem = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(frameFile))
                        .setMimeType(MimeTypes.IMAGE_PNG)
                        .setImageDurationMs(clipDurationMs)
                        .build()
                )
                    .setDurationUs(clipDurationMs * 1000L)
                    .setRemoveAudio(true)
                    .build()

                val audioMediaItem = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(audioFile))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startPositionMs)
                                .setEndPositionMs(endPositionMs)
                                .build()
                        )
                        .build()
                )
                    .setRemoveVideo(true)
                    .build()

                val videoSequence = EditedMediaItemSequence.Builder(imageMediaItem).build()
                val audioSequence = EditedMediaItemSequence.Builder(audioMediaItem).build()
                val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .build()

                val progressHolder = ProgressHolder()
                val progressJob = launch {
                    while (isActive) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            val percent = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                            onProgress(0.40f + percent * 0.58f, "Encoding 30s video: ${(percent * 100).toInt()}%")
                        }
                        delay(200)
                    }
                }

                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        progressJob.cancel()
                        frameFile.delete()
                        onProgress(1f, "Video ready!")
                        PixelLogger.i(PixelLogger.Category.UI, "ShareVideo", "Transformer completed successfully! Output: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                        if (continuation.isActive) {
                            continuation.resume(outputFile)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        progressJob.cancel()
                        frameFile.delete()
                        outputFile.delete()
                        PixelLogger.e(PixelLogger.Category.UI, "ShareVideo", "Transformer export failed: ${exportException.errorCodeName}", exportException)
                        if (continuation.isActive) {
                            continuation.resumeWithException(exportException)
                        }
                    }
                })

                continuation.invokeOnCancellation {
                    progressJob.cancel()
                    transformer.cancel()
                    frameFile.delete()
                    outputFile.delete()
                    PixelLogger.w(PixelLogger.Category.UI, "ShareVideo", "Video generation cancelled")
                }

                try {
                    PixelLogger.d(PixelLogger.Category.UI, "ShareVideo", "Starting Transformer export to ${outputFile.absolutePath}")
                    transformer.start(composition, outputFile.absolutePath)
                } catch (e: Exception) {
                    progressJob.cancel()
                    frameFile.delete()
                    outputFile.delete()
                    PixelLogger.e(PixelLogger.Category.UI, "ShareVideo", "Failed to start Transformer", e)
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }
}
