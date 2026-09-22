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
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun resolveAudioFile(context: Context, song: Song): File? = withContext(Dispatchers.IO) {
        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Resolving audio for '${song.title}', id='${song.id}', path='${song.path}', uri='${song.contentUriString}'")

        // 1. Direct local file path
        if (song.path.isNotBlank()) {
            val file = File(song.path)
            if (file.exists() && file.length() > 0 && file.canRead()) {
                PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Local file exists: ${file.absolutePath} (${file.length()} bytes)")
                return@withContext file
            }
        }

        // 2. Local content URI or file URI: copy to a temporary audio file in cache so Media3 has 100% direct file access
        val contentUriStr = song.contentUriString
        if (contentUriStr.isNotBlank() && (contentUriStr.startsWith("content://") || contentUriStr.startsWith("file://") || contentUriStr.startsWith("/"))) {
            try {
                val uri = if (contentUriStr.startsWith("/")) Uri.fromFile(File(contentUriStr)) else Uri.parse(contentUriStr)
                if (uri.scheme == "file") {
                    val f = File(uri.path ?: contentUriStr)
                    if (f.exists() && f.length() > 0) {
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "File URI valid: ${f.absolutePath}")
                        return@withContext f
                    }
                } else if (uri.scheme == "content") {
                    val tempAudioFile = File(context.cacheDir, "share_cards/temp_content_${System.currentTimeMillis()}.m4a")
                    tempAudioFile.parentFile?.mkdirs()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempAudioFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Extracted content URI to cache file: ${tempAudioFile.length()} bytes")
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
                        return@withContext f
                    }
                }
            } catch (e: Exception) {
                PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Error querying YouTube DB", e)
            }

            // 4. Online stream URL: download audio stream to temporary cache file
            try {
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
                PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Resolved stream URL: ${streamUrl.take(70)}...")
                if (streamUrl.isNotBlank()) {
                    if (streamUrl.startsWith("/")) {
                        val f = File(streamUrl)
                        if (f.exists() && f.length() > 0) return@withContext f
                    } else if (streamUrl.startsWith("http")) {
                        val tempAudioFile = File(context.cacheDir, "share_cards/temp_yt_${videoId}_${System.currentTimeMillis()}.m4a")
                        tempAudioFile.parentFile?.mkdirs()
                        downloadStreamToFile(streamUrl, tempAudioFile)
                        if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                            PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Downloaded stream to file: ${tempAudioFile.length()} bytes")
                            return@withContext tempAudioFile
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

    private fun downloadStreamToFile(url: String, destination: File) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download stream failed HTTP code: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw IOException("Response body is null")
            destination.outputStream().use { out ->
                body.byteStream().copyTo(out)
            }
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
        onProgress: (Float) -> Unit = {}
    ): File {
        PixelLogger.i(PixelLogger.Category.UI, "ShareVideo", "Starting video generation for '${song.title}', duration=${desiredDurationSec}s, currentPositionMs=$currentPositionMs")

        val audioFile = resolveAudioFile(context, song)
            ?: throw IllegalStateException("Audio source could not be resolved or downloaded for this track.")

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

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val imageMediaItem = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(frameFile))
                        .setMimeType(MimeTypes.IMAGE_PNG) // CRITICAL: Explicitly specify IMAGE_PNG
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
                            onProgress(progressHolder.progress / 100f)
                        }
                        delay(250)
                    }
                }

                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        progressJob.cancel()
                        frameFile.delete()
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
                        PixelLogger.e(PixelLogger.Category.UI, "ShareVideo", "Transformer export failed with exception: ${exportException.errorCodeName}", exportException)
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
                    PixelLogger.w(PixelLogger.Category.UI, "ShareVideo", "Video generation cancelled by user")
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
