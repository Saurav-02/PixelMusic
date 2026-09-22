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
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object ShareVideoGenerator {

    suspend fun resolveAudioUri(context: Context, song: Song): Uri? = withContext(Dispatchers.IO) {
        // 1. Direct local file path
        if (song.path.isNotBlank()) {
            val file = File(song.path)
            if (file.exists() && file.length() > 0) {
                return@withContext Uri.fromFile(file)
            }
        }

        // 2. Local content URI
        val contentUri = song.contentUriString
        if (contentUri.isNotBlank()) {
            if (contentUri.startsWith("content://") || contentUri.startsWith("file://")) {
                val uri = Uri.parse(contentUri)
                try {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                        return@withContext uri
                    }
                } catch (_: Exception) {}
            } else if (contentUri.startsWith("/")) {
                val file = File(contentUri)
                if (file.exists() && file.length() > 0) {
                    return@withContext Uri.fromFile(file)
                }
            }
        }

        // 3. YouTube downloaded audio file in local database
        val videoId = song.youtubeId ?: if (song.id.startsWith("youtube_")) song.id.substringAfter("youtube_") else null
        if (!videoId.isNullOrBlank()) {
            try {
                val db = AppDatabase.getInstance(context)
                val ytSong = db.songRepository().getSong(videoId)
                if (ytSong?.audioFilePath != null) {
                    val f = File(ytSong.audioFilePath)
                    if (f.exists() && f.length() > 0) {
                        return@withContext Uri.fromFile(f)
                    }
                }
            } catch (_: Exception) {}

            // 4. Online stream URL if available
            try {
                val ytModelSong = com.saurav.pixelmusic.data.model.youtube.Song(
                    youtubeId = videoId,
                    title = song.title,
                    artist = song.artist,
                    duration = song.duration.toString()
                )
                val streamUrl = YoutubeHelper.getSongPlayerUrl(context, ytModelSong, allowLocal = true)
                if (streamUrl.isNotBlank()) {
                    return@withContext if (streamUrl.startsWith("/")) Uri.fromFile(File(streamUrl)) else Uri.parse(streamUrl)
                }
            } catch (_: Exception) {}
        }

        // 5. Fallback via MediaItemBuilder
        return@withContext runCatching { MediaItemBuilder.playbackUri(song) }.getOrNull()
    }

    suspend fun prepareVideoCardFrame(context: Context, cardBitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val frameFile = File(cacheDir, "temp_video_frame_${System.currentTimeMillis()}.png")

        // Target standard 720x1280 resolution (9:16 aspect ratio, even dimensions for H.264)
        val targetWidth = 720
        val targetHeight = 1280
        val scaledBitmap = Bitmap.createScaledBitmap(cardBitmap, targetWidth, targetHeight, true)

        FileOutputStream(frameFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaledBitmap != cardBitmap) {
            scaledBitmap.recycle()
        }
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
        val audioUri = resolveAudioUri(context, song)
            ?: throw IllegalStateException("Audio source could not be resolved for this track.")

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

        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val outputFile = File(cacheDir, "pixelmusic_video_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val imageMediaItem = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(frameFile))
                        .setImageDurationMs(clipDurationMs)
                        .build()
                )
                    .setDurationUs(clipDurationMs * 1000L)
                    .setRemoveAudio(true)
                    .build()

                val audioMediaItem = EditedMediaItem.Builder(
                    MediaItem.Builder()
                        .setUri(audioUri)
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
                }

                try {
                    transformer.start(composition, outputFile.absolutePath)
                } catch (e: Exception) {
                    progressJob.cancel()
                    frameFile.delete()
                    outputFile.delete()
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }
}
