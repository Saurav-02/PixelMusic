package com.saurav.pixelmusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.database.youtube.AppDatabase
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.StreamingAudioQuality
import com.saurav.pixelmusic.data.remote.jiosaavn.JioSaavnHelper
import com.saurav.pixelmusic.data.remote.youtube.ExoCache
import com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.IOS
import saurav.shru.pixelmusic.innertube.utils.StreamClientUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object ShareVideoGenerator {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ShareVideoEntryPoint {
        fun exoCache(): ExoCache
        fun musicDao(): MusicDao
    }

    private data class StreamCandidate(
        val label: String,
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val preferM4a: Boolean = false
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun resolveAudioFile(
        context: Context,
        song: Song,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        PixelLogger.d(
            PixelLogger.Category.PLAYER,
            "ShareVideo",
            "Resolving audio for '${song.title}', id='${song.id}', ytId='${song.youtubeId}', path='${song.path}', uri='${song.contentUriString}'"
        )

        val entryPoint = runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ShareVideoEntryPoint::class.java
            )
        }.getOrNull()

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
                        val finalFile = ensureAccurateAudioExtension(tempAudioFile)
                        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Extracted content URI to cache file: ${finalFile.length()} bytes")
                        onProgress(0.35f, "Local audio track ready…")
                        return@withContext finalFile
                    }
                }
            } catch (e: Exception) {
                PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Error resolving content URI: $contentUriStr", e)
            }
        }

        // 3. Resolve canonical YouTube videoId (including synthetic Room DB -15... IDs)
        var videoId = extractCleanVideoId(song)
        var resolvedTitle = song.title
        var resolvedArtist = song.displayArtist.ifBlank { song.artist }

        val candidateLongId = song.id.toLongOrNull()
            ?: song.contentUriString.removePrefix("youtube://").toLongOrNull()
        if ((videoId.isNullOrBlank() || videoId.startsWith("-15")) && candidateLongId != null && entryPoint != null) {
            runCatching {
                val dbSong = entryPoint.musicDao().getSongByIdOnce(candidateLongId)
                if (dbSong != null) {
                    if (dbSong.filePath.isNotBlank()) {
                        val localDbFile = File(dbSong.filePath)
                        if (localDbFile.exists() && localDbFile.length() > 0) {
                            onProgress(0.35f, "Found local audio track…")
                            return@withContext localDbFile
                        }
                    }
                    if (dbSong.contentUriString.startsWith("youtube://")) {
                        val cleanDbId = dbSong.contentUriString.removePrefix("youtube://").removePrefix("youtube_")
                        if (cleanDbId.isNotBlank() && !cleanDbId.startsWith("-15")) {
                            videoId = cleanDbId
                        }
                    }
                    if (resolvedTitle.isBlank()) resolvedTitle = dbSong.title
                    if (resolvedArtist.isBlank()) resolvedArtist = dbSong.artistName
                }
            }
        }

        if (!videoId.isNullOrBlank()) {
            val cleanVideoId = videoId
            PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Checking offline & cached sources for videoId=$cleanVideoId")

            // 3A. Check YouTube downloaded audio file in local database
            try {
                val db = AppDatabase.getInstance(context)
                val ytSong = db.songRepository().getSong(cleanVideoId)
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

            // 3B. Check Music/PixelMusic downloaded file on external storage
            runCatching {
                val cleanTitle = resolvedTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val cleanArtist = resolvedArtist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "PixelMusic")
                val downloadedFile = File(musicDir, "$cleanTitle - $cleanArtist.m4a")
                if (downloadedFile.exists() && downloadedFile.length() > 30_000) {
                    onProgress(0.35f, "Found downloaded audio track…")
                    return@withContext downloadedFile
                }
            }

            // 3C. Check if audio file for this video was already cached from a previous share attempt
            val shareCacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
            val existingCached = listOf("m4a", "webm", "ogg", "mp3")
                .map { ext -> File(shareCacheDir, "yt_audio_${cleanVideoId}.$ext") }
                .firstOrNull { it.exists() && it.length() > 35_000 }
            if (existingCached != null) {
                val verifiedFile = ensureAccurateAudioExtension(existingCached)
                PixelLogger.i(PixelLogger.Category.PLAYER, "ShareVideo", "Reusing cached share audio: ${verifiedFile.absolutePath} (${verifiedFile.length()} bytes)")
                onProgress(0.35f, "Audio track ready…")
                return@withContext verifiedFile
            }

            // 3D. Check if ExoCache (SimpleCache) already has the complete stream buffered from active playback
            if (entryPoint != null) {
                val exoExtracted = extractFromExoCacheIfAvailable(
                    exoCache = entryPoint.exoCache(),
                    videoId = cleanVideoId,
                    shareCacheDir = shareCacheDir
                )
                if (exoExtracted != null) {
                    PixelLogger.i(PixelLogger.Category.PLAYER, "ShareVideo", "Extracted complete audio from ExoCache: ${exoExtracted.absolutePath} (${exoExtracted.length()} bytes)")
                    onProgress(0.35f, "Loaded buffered audio track…")
                    return@withContext exoExtracted
                }
            }

            // 4. Multi-candidate online stream resolution + download
            onProgress(0.05f, "Connecting to audio stream…")
            val durationFormatted = formatDurationMillis(song.duration)
            val ytModelSong = com.saurav.pixelmusic.data.model.youtube.Song(
                youtubeId = cleanVideoId,
                title = resolvedTitle,
                artist = resolvedArtist,
                duration = durationFormatted
            )

            val downloadedFile = resolveAndDownloadOnlineAudio(
                context = context,
                videoId = cleanVideoId,
                song = song,
                ytModelSong = ytModelSong,
                shareCacheDir = shareCacheDir,
                onProgress = onProgress
            )
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 16_000) {
                onProgress(0.35f, "Audio track ready…")
                return@withContext downloadedFile
            }
        }

        // 5. Final fallback: if contentUriString itself is an HTTP stream or JioSaavn search by title/artist
        if (contentUriStr.startsWith("http://") || contentUriStr.startsWith("https://")) {
            val shareCacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
            val fallbackDest = File(shareCacheDir, "yt_audio_direct_${System.currentTimeMillis()}.m4a")
            val ok = runCatching {
                downloadStreamToFile(
                    url = contentUriStr,
                    customHeaders = emptyMap(),
                    destination = fallbackDest,
                    onProgress = onProgress
                )
            }.getOrDefault(false)
            if (ok && fallbackDest.exists() && fallbackDest.length() > 16_000) {
                return@withContext ensureAccurateAudioExtension(fallbackDest)
            }
        }

        if (resolvedTitle.isNotBlank()) {
            val jioUrl = runCatching {
                JioSaavnHelper.getFallbackStreamUrl(
                    youtubeId = videoId ?: song.id,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    durationMs = song.duration,
                    preferKbps = 160
                )
            }.getOrNull()
            if (!jioUrl.isNullOrBlank()) {
                val shareCacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
                val jioDest = File(shareCacheDir, "yt_audio_${videoId ?: song.id.hashCode()}.m4a")
                val ok = runCatching {
                    downloadStreamToFile(
                        url = jioUrl,
                        customHeaders = emptyMap(),
                        destination = jioDest,
                        onProgress = onProgress
                    )
                }.getOrDefault(false)
                if (ok && jioDest.exists() && jioDest.length() > 16_000) {
                    onProgress(0.35f, "Audio track ready…")
                    return@withContext ensureAccurateAudioExtension(jioDest)
                }
            }
        }

        // 6. Fallback via MediaItemBuilder playback URI
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
                if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                    return@withContext ensureAccurateAudioExtension(tempAudioFile)
                }
            }
        } catch (e: Exception) {
            PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Fallback playback URI extraction failed", e)
        }

        PixelLogger.e(PixelLogger.Category.PLAYER, "ShareVideo", "No audio source found for song: ${song.title}")
        return@withContext null
    }

    private fun extractCleanVideoId(song: Song): String? {
        val candidates = listOfNotNull(
            song.youtubeId,
            song.id.takeIf { it.startsWith("youtube_") || it.startsWith("youtube://") },
            song.contentUriString.takeIf { it.startsWith("youtube://") },
            song.id.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
        )
        for (raw in candidates) {
            val cleaned = raw.removePrefix("youtube://").removePrefix("youtube_").trim()
            if (cleaned.isNotBlank() && !cleaned.startsWith("-15")) {
                return cleaned
            }
        }
        return null
    }

    private fun extractFromExoCacheIfAvailable(
        exoCache: ExoCache,
        videoId: String,
        shareCacheDir: File
    ): File? {
        return try {
            val simpleCache = exoCache.cache
            val contentLength = ContentMetadata.getContentLength(simpleCache.getContentMetadata(videoId))
            val spans = simpleCache.getCachedSpans(videoId)
                .filter { it.isCached && it.file?.exists() == true && it.length > 0 }
                .sortedBy { it.position }

            if (spans.isEmpty()) return null

            var contiguousBytes = 0L
            for (span in spans) {
                if (span.position > contiguousBytes) break
                val spanEnd = span.position + span.length
                if (spanEnd > contiguousBytes) {
                    contiguousBytes = spanEnd
                }
            }

            // Require the entire track to be cached in ExoCache (or at least 3MB if content length is unset)
            val isFullyCached = (contentLength > 35_000L && contiguousBytes >= contentLength) ||
                (contentLength <= 0L && contiguousBytes >= 3_000_000L)

            if (!isFullyCached) return null

            val tempOut = File(shareCacheDir, "yt_audio_${videoId}.tmp")
            FileOutputStream(tempOut).use { output ->
                var expectedOffset = 0L
                val buffer = ByteArray(64 * 1024)
                for (span in spans) {
                    if (span.position > expectedOffset) break
                    val skipBytes = (expectedOffset - span.position).coerceAtLeast(0L)
                    val spanFile = span.file ?: continue
                    FileInputStream(spanFile).use { input ->
                        if (skipBytes > 0) input.skip(skipBytes)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            expectedOffset += read
                        }
                    }
                }
                output.flush()
            }

            if (tempOut.exists() && tempOut.length() > 35_000L) {
                ensureAccurateAudioExtension(tempOut, baseNameWithoutExt = "yt_audio_${videoId}")
            } else {
                tempOut.delete()
                null
            }
        } catch (e: Exception) {
            PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "ExoCache extraction skipped: ${e.message}")
            null
        }
    }

    private suspend fun resolveAndDownloadOnlineAudio(
        context: Context,
        videoId: String,
        song: Song,
        ytModelSong: com.saurav.pixelmusic.data.model.youtube.Song,
        shareCacheDir: File,
        onProgress: (Float, String) -> Unit
    ): File? {
        val triedUrls = mutableSetOf<String>()

        suspend fun tryCandidate(candidate: StreamCandidate): File? {
            val rawUrl = candidate.url.trim()
            if (rawUrl.isBlank()) return null

            if (rawUrl.startsWith("/")) {
                val localFile = File(rawUrl)
                if (localFile.exists() && localFile.length() > 0) return localFile
                return null
            }

            if (!rawUrl.startsWith("http") || !triedUrls.add(rawUrl)) return null

            PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Trying stream candidate [${candidate.label}]: ${rawUrl.take(75)}…")
            val tempDest = File(shareCacheDir, "yt_audio_${videoId}_dl.tmp")
            val success = try {
                downloadStreamToFile(
                    url = rawUrl,
                    customHeaders = candidate.headers,
                    destination = tempDest,
                    onProgress = onProgress
                )
            } catch (e: Exception) {
                PixelLogger.w(PixelLogger.Category.PLAYER, "ShareVideo", "Candidate [${candidate.label}] failed: ${e.message}")
                false
            }

            if (success && tempDest.exists() && tempDest.length() > 16_000L) {
                return ensureAccurateAudioExtension(tempDest, baseNameWithoutExt = "yt_audio_${videoId}")
            }
            tempDest.delete()
            return null
        }

        // Candidate 1: Direct InnerTube M4A (itag 140) from native clients (ANDROID_VR_NO_AUTH / IOS)
        // Native AAC-in-MP4 streams download without PoToken/Range issues and mux directly in Media3 Transformer
        val directClients = listOf(ANDROID_VR_NO_AUTH, IOS, ANDROID_VR_1_61_48, ANDROID_MUSIC)
        for (client in directClients) {
            val candidate = runCatching {
                val response = YouTube.player(videoId = videoId, client = client, setLogin = false).getOrNull()
                val formats = response?.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio && !it.url.isNullOrBlank() }
                    .orEmpty()
                val m4aFormat = formats
                    .filter { it.itag == 140 || it.mimeType.contains("mp4", ignoreCase = true) }
                    .maxByOrNull { it.bitrate }
                    ?: formats.maxByOrNull { it.bitrate }
                m4aFormat?.url?.let { url ->
                    StreamCandidate(
                        label = "YouTube.player(${client.clientName})",
                        url = url,
                        headers = mapOf("User-Agent" to client.userAgent),
                        preferM4a = true
                    )
                }
            }.getOrNull()

            if (candidate != null) {
                tryCandidate(candidate)?.let { return it }
            }
        }

        // Candidate 2: InnerTubeXPlayer with PoToken & Zemer Cipher (including its exact streamHeaders)
        for (allowBounded in listOf(false, true)) {
            val candidate = runCatching {
                InnerTubeXPlayer.initialize(context)
                val playbackData = InnerTubeXPlayer.playerResponseForPlayback(
                    videoId = videoId,
                    audioQuality = StreamingAudioQuality.HIGH,
                    allowBoundedRange = allowBounded
                ).getOrNull()
                playbackData?.let {
                    StreamCandidate(
                        label = "InnerTubeX(${it.streamClient}, bounded=$allowBounded)",
                        url = it.streamUrl,
                        headers = it.streamHeaders
                    )
                }
            }.getOrNull()

            if (candidate != null) {
                tryCandidate(candidate)?.let { return it }
            }
        }

        // Candidate 3: YoutubeHelper cached/active player URL
        val playerUrl = runCatching {
            YoutubeHelper.getSongPlayerUrl(context, ytModelSong, allowLocal = true)
        }.getOrNull()
        if (!playerUrl.isNullOrBlank()) {
            tryCandidate(StreamCandidate("YoutubeHelper.getSongPlayerUrl", playerUrl))?.let { return it }
        }

        // Candidate 4: YoutubeHelper download URL (after invalidating any stale cache)
        runCatching { YoutubeHelper.invalidateStreamCache(videoId) }
        val downloadUrl = runCatching {
            YoutubeHelper.getDownloadUrl(context, ytModelSong)
        }.getOrNull()
        if (!downloadUrl.isNullOrBlank()) {
            tryCandidate(StreamCandidate("YoutubeHelper.getDownloadUrl", downloadUrl))?.let { return it }
        }

        // Candidate 5: JioSaavn direct M4A stream fallback
        val jioUrl = runCatching {
            JioSaavnHelper.getFallbackStreamUrl(
                youtubeId = videoId,
                title = song.title,
                artist = song.displayArtist.ifBlank { song.artist },
                durationMs = song.duration,
                preferKbps = 160
            )
        }.getOrNull()
        if (!jioUrl.isNullOrBlank()) {
            tryCandidate(StreamCandidate("JioSaavnFallback", jioUrl))?.let { return it }
        }

        return null
    }

    private fun buildRequestHeaders(url: String, customHeaders: Map<String, String>): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        val isYoutubeCdn = url.contains("googlevideo.com", ignoreCase = true) ||
            url.contains("youtube.com", ignoreCase = true)

        if (isYoutubeCdn) {
            val profile = StreamClientUtils.resolveRequestProfile(url)
            headers["User-Agent"] = profile.userAgent
            if (!profile.origin.isNullOrBlank()) {
                headers["Origin"] = profile.origin
            }
            if (!profile.referer.isNullOrBlank()) {
                headers["Referer"] = profile.referer
            }
        } else {
            headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        for ((k, v) in customHeaders) {
            if (v.isNotBlank() && !k.equals("Range", ignoreCase = true)) {
                headers[k] = v
            }
        }
        return headers
    }

    private fun downloadStreamToFile(
        url: String,
        customHeaders: Map<String, String>,
        destination: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        destination.parentFile?.mkdirs()
        val tempPartFile = File(destination.parentFile, "${destination.name}.part")
        if (tempPartFile.exists()) tempPartFile.delete()

        val headers = buildRequestHeaders(url, customHeaders)
        val hasQueryRange = url.contains("&range=") || url.contains("?range=")

        // Try chunked range download first (unless the URL already embeds a query range parameter)
        val chunkedSucceeded = if (!hasQueryRange) {
            runCatching {
                downloadChunkedWithOkHttp(url, headers, tempPartFile, onProgress)
            }.getOrDefault(false)
        } else {
            false
        }

        if (!chunkedSucceeded || !tempPartFile.exists() || tempPartFile.length() < 16_000L) {
            if (tempPartFile.exists()) tempPartFile.delete()
            val singleSucceeded = runCatching {
                downloadSingleStreamWithOkHttp(url, headers, tempPartFile, onProgress)
            }.getOrElse {
                runCatching {
                    downloadSingleStreamWithUrlConnection(url, headers, tempPartFile, onProgress)
                }.getOrDefault(false)
            }
            if (!singleSucceeded || !tempPartFile.exists() || tempPartFile.length() < 16_000L) {
                tempPartFile.delete()
                return false
            }
        }

        if (destination.exists()) destination.delete()
        return tempPartFile.renameTo(destination)
    }

    private fun downloadChunkedWithOkHttp(
        url: String,
        headers: Map<String, String>,
        tempPartFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        var totalBytes = parseTotalBytesFromUrl(url)
        val chunkSize = 2 * 1024 * 1024L // 2MB chunks bypass YouTube CDN throttling
        var startByte = 0L
        var isFinished = false

        FileOutputStream(tempPartFile, false).use { output ->
            while (!isFinished) {
                val endByte = if (totalBytes > 0) minOf(startByte + chunkSize - 1, totalBytes - 1) else startByte + chunkSize - 1
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .header("Range", "bytes=$startByte-$endByte")

                headers.forEach { (k, v) -> requestBuilder.header(k, v) }

                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                        throw IOException("Chunk download failed with HTTP $code")
                    }

                    if (totalBytes <= 0) {
                        val contentRange = response.header("Content-Range")
                        if (contentRange != null && contentRange.contains("/")) {
                            totalBytes = contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                        }
                        if (totalBytes <= 0 && code == HttpURLConnection.HTTP_OK) {
                            totalBytes = response.body.contentLength()
                        }
                    }

                    val inputStream = response.body.byteStream()
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var chunkReadTotal = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        chunkReadTotal += bytesRead
                        startByte += bytesRead

                        if (totalBytes > 0) {
                            val fraction = (startByte.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val pct = (fraction * 100).toInt()
                            onProgress(0.05f + fraction * 0.30f, "Downloading audio track • $pct%")
                        }
                    }

                    if (chunkReadTotal < chunkSize || (totalBytes > 0 && startByte >= totalBytes) || code == HttpURLConnection.HTTP_OK) {
                        isFinished = true
                    }
                }
            }
            output.flush()
        }
        return tempPartFile.exists() && tempPartFile.length() > 16_000L
    }

    private fun downloadSingleStreamWithOkHttp(
        url: String,
        headers: Map<String, String>,
        tempPartFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        val cleanUrl = url.replace(Regex("([?&])range=[^&]+&?"), "$1").trimEnd('?', '&')
        val requestBuilder = Request.Builder().url(cleanUrl).get()
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Single stream download failed with HTTP ${response.code}")
            }
            val totalBytes = response.body.contentLength().takeIf { it > 0 } ?: parseTotalBytesFromUrl(cleanUrl)
            var downloaded = 0L
            val buffer = ByteArray(64 * 1024)

            response.body.byteStream().use { input ->
                FileOutputStream(tempPartFile, false).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val fraction = (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val pct = (fraction * 100).toInt()
                            onProgress(0.05f + fraction * 0.30f, "Downloading audio track • $pct%")
                        }
                    }
                    output.flush()
                }
            }
        }
        return tempPartFile.exists() && tempPartFile.length() > 16_000L
    }

    private fun downloadSingleStreamWithUrlConnection(
        url: String,
        headers: Map<String, String>,
        tempPartFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        val cleanUrl = url.replace(Regex("([?&])range=[^&]+&?"), "$1").trimEnd('?', '&')
        val connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("URLConnection failed with HTTP $code")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: parseTotalBytesFromUrl(cleanUrl)
            var downloaded = 0L
            val buffer = ByteArray(64 * 1024)
            connection.inputStream.use { input ->
                FileOutputStream(tempPartFile, false).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val fraction = (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val pct = (fraction * 100).toInt()
                            onProgress(0.05f + fraction * 0.30f, "Downloading audio track • $pct%")
                        }
                    }
                    output.flush()
                }
            }
            return tempPartFile.exists() && tempPartFile.length() > 16_000L
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureAccurateAudioExtension(
        file: File,
        baseNameWithoutExt: String = file.nameWithoutExtension
    ): File {
        val ext = detectAudioExtension(file)
        val targetFile = File(file.parentFile, "$baseNameWithoutExt.$ext")
        if (file.absolutePath == targetFile.absolutePath) return file
        if (targetFile.exists()) targetFile.delete()
        return if (file.renameTo(targetFile)) targetFile else file
    }

    private fun detectAudioExtension(file: File): String {
        return try {
            val header = ByteArray(12)
            val read = FileInputStream(file).use { it.read(header) }
            if (read >= 4) {
                // EBML magic bytes (0x1A 0x45 0xDF 0xA3) -> WebM / Matroska
                if (header[0] == 0x1A.toByte() &&
                    header[1] == 0x45.toByte() &&
                    header[2] == 0xDF.toByte() &&
                    header[3] == 0xA3.toByte()
                ) {
                    return "webm"
                }
                // OggS magic bytes -> Ogg
                if (header[0] == 'O'.code.toByte() &&
                    header[1] == 'g'.code.toByte() &&
                    header[2] == 'g'.code.toByte() &&
                    header[3] == 'S'.code.toByte()
                ) {
                    return "ogg"
                }
                // ID3 or MPEG sync -> MP3
                if ((header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) ||
                    (header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0)
                ) {
                    return "mp3"
                }
            }
            if (read >= 8) {
                // ISO Base Media File Format ('ftyp' at bytes 4..7) -> M4A / MP4
                if (header[4] == 'f'.code.toByte() &&
                    header[5] == 't'.code.toByte() &&
                    header[6] == 'y'.code.toByte() &&
                    header[7] == 'p'.code.toByte()
                ) {
                    return "m4a"
                }
            }
            "m4a"
        } catch (_: Exception) {
            "m4a"
        }
    }

    private fun formatDurationMillis(durationMs: Long): String {
        if (durationMs <= 0L) return ""
        val totalSeconds = (durationMs / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun parseTotalBytesFromUrl(url: String): Long {
        return try {
            val clen = url.substringAfter("clen=", "").substringBefore("&")
            if (clen.isNotEmpty()) clen.toLongOrNull() ?: -1L else -1L
        } catch (_: Exception) {
            -1L
        }
    }

    suspend fun prepareVideoCardFrame(context: Context, cardBitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val frameFile = File(cacheDir, "temp_video_frame_${System.currentTimeMillis()}.png")

        // Standard 720x1280 resolution (9:16 aspect ratio, balanced for small video file size)
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

        val actualAudioDurationMs = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrDefault(0L)

        val effectiveSongDurationMs = when {
            actualAudioDurationMs > 1_000L && song.duration > 1_000L -> minOf(actualAudioDurationMs, song.duration)
            actualAudioDurationMs > 1_000L -> actualAudioDurationMs
            else -> song.duration
        }

        val desiredClipDurationMs = (desiredDurationSec * 1000L).coerceAtLeast(5000L)
        val clipDurationMs = if (effectiveSongDurationMs > 0) {
            minOf(desiredClipDurationMs, effectiveSongDurationMs)
        } else {
            desiredClipDurationMs
        }

        val maxStart = if (effectiveSongDurationMs > clipDurationMs) effectiveSongDurationMs - clipDurationMs else 0L
        val startPositionMs = if (currentPositionMs > 0L) {
            currentPositionMs.coerceIn(0L, maxStart)
        } else {
            if (effectiveSongDurationMs > clipDurationMs * 2) {
                (effectiveSongDurationMs * 0.25f).toLong().coerceIn(0L, maxStart)
            } else {
                0L
            }
        }
        val endPositionMs = startPositionMs + clipDurationMs
        PixelLogger.d(PixelLogger.Category.PLAYER, "ShareVideo", "Clip bounds: start=${startPositionMs}ms, end=${endPositionMs}ms, duration=${clipDurationMs}ms")

        val cacheDir = File(context.cacheDir, "share_cards").also { it.mkdirs() }
        val outputFile = File(cacheDir, "pixelmusic_video_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        onProgress(0.42f, "Encoding 30s video…")

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
                    .setFrameRate(15)
                    .setRemoveAudio(true)
                    .build()

                val audioMimeType = when (audioFile.extension.lowercase()) {
                    "webm" -> MimeTypes.AUDIO_WEBM
                    "ogg" -> MimeTypes.AUDIO_OGG
                    "mp3" -> MimeTypes.AUDIO_MPEG
                    else -> null
                }

                val audioMediaItemBuilder = MediaItem.Builder()
                    .setUri(Uri.fromFile(audioFile))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(startPositionMs)
                            .setEndPositionMs(endPositionMs)
                            .build()
                    )
                if (audioMimeType != null) {
                    audioMediaItemBuilder.setMimeType(audioMimeType)
                }

                val audioMediaItem = EditedMediaItem.Builder(audioMediaItemBuilder.build())
                    .setRemoveVideo(true)
                    .build()

                @Suppress("DEPRECATION")
                val videoSequence = EditedMediaItemSequence.Builder(listOf(imageMediaItem)).build()
                @Suppress("DEPRECATION")
                val audioSequence = EditedMediaItemSequence.Builder(listOf(audioMediaItem)).build()
                val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()

                val videoEncoderSettings = VideoEncoderSettings.Builder()
                    .setBitrate(750_000) // 750 kbps video bitrate = ~2.8 - 3.2 MB total for 30s
                    .build()

                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(videoEncoderSettings)
                    .setEnableFallback(true)
                    .build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .build()

                val progressHolder = ProgressHolder()
                val progressJob = launch {
                    while (isActive) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            val percent = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                            onProgress(0.42f + percent * 0.56f, "Encoding 30s video • ${(percent * 100).toInt()}%")
                        }
                        delay(180)
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
