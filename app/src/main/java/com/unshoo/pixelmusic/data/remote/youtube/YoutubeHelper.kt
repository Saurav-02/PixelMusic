package com.unshoo.pixelmusic.data.remote.youtube

import android.content.Context
import android.util.LruCache
import android.widget.Toast
import androidx.core.net.toUri
import com.unshoo.pixelmusic.data.database.youtube.AppDatabase
import com.unshoo.pixelmusic.data.model.youtube.PlaylistInfo
import com.unshoo.pixelmusic.data.model.youtube.Song
import com.unshoo.pixelmusic.data.model.youtube.UmihiSettings
import com.unshoo.pixelmusic.data.preferences.StreamingAudioQuality
import com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository
import com.unshoo.pixelmusic.presentation.viewmodel.ConnectivityStateHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.IOS
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.MOBILE
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.TVHTML5
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.WEB
import unshoo.ianshulyadav.pixelmusic.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import unshoo.ianshulyadav.pixelmusic.innertube.utils.StreamClientUtils
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.PlaybackAuthState
import unshoo.ianshulyadav.pixelmusic.innertube.models.response.PlayerResponse
import com.unshoo.pixelmusic.data.preferences.PlayerStreamClient
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withTimeoutOrNull
import com.unshoo.pixelmusic.data.remote.youtube.cipher.FaradayCipherEngine
import io.ktor.client.HttpClient
import io.ktor.http.parseQueryString
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import com.unshoo.pixelmusic.utils.PixelHttpLoggingInterceptor



object YoutubeHelper {
    private val jsThread = Executors.newSingleThreadExecutor { runnable ->
        Thread(null, runnable, "QuickJs", 32L * 1024L * 1024L)
    }.asCoroutineDispatcher()

    private val faradayEngine by lazy {
        FaradayCipherEngine(
            httpClient = HttpClient(),
            jsThread = jsThread
        )
    }
    val client = OkHttpClient.Builder()
    .connectionPool(okhttp3.ConnectionPool(15, 5, java.util.concurrent.TimeUnit.MINUTES))
    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
    .callTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
    .addInterceptor(PixelHttpLoggingInterceptor("stream") { url ->
        !url.contains("ytimg.com") && !url.contains("ggpht.com")
    })
    .build()

    val streamUrlLruCache = LruCache<String, String>(200)
    val streamMimeTypeLruCache = LruCache<String, String>(200)
    val streamBitrateLruCache = LruCache<String, Int>(200)
    private val localFilePathCache = LruCache<String, String>(200)
    private val failedStreamClientsUntil = ConcurrentHashMap<String, Long>()
    val playbackTrackingCache = ConcurrentHashMap<String, String>()
    private const val FAILED_CLIENT_BACKOFF_MS = 10 * 60 * 1000L
    @Volatile private var lastSuccessfulClientKey: String? = null

    suspend fun extractGenre(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonString = YoutubeRequestHelper.getPlayerInfo(videoId)
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val category = json["microformat"]
                ?.jsonObject?.get("microformatDataRenderer")
                ?.jsonObject?.get("category")
                ?.jsonPrimitive?.contentOrNull
            category?.takeIf { it.isNotBlank() && it != "Music" }
        } catch (e: Exception) {
            UmihiHelper.printe("Failed to extract genre: ${e.message}")
            null
        }
    }

    fun extractYouTubeVideoId(url: String): String? {
        val uri = url.toUri()
        return when {
            uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
            uri.host?.contains("youtube.com") == true || uri.host?.contains("music.youtube.com") == true -> uri.getQueryParameter("v")
            else -> null
        }
    }

    fun getBestThumbnailUrl(thumbnailElement: JsonElement): String {
        val url = thumbnailElement.jsonObject["musicThumbnailRenderer"]?.jsonObject?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray?.last()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
        return upgradeThumbnailUrlToHighQuality(url)
    }

    private fun upgradeThumbnailUrlToHighQuality(url: String, quality: Int = 400): String {
        if (url.isBlank()) return url
        val resizeRegex = Regex("=w\\d+-h\\d+.*")
        if (resizeRegex.containsMatchIn(url)) {
            return url.replace(resizeRegex, "=w$quality-h$quality")
        }
        val sRegex = Regex("=s\\d+.*")
        if (sRegex.containsMatchIn(url)) {
            return url.replace(sRegex, "=s$quality")
        }
        if (url.contains("googleusercontent.com")) {
            return if (url.contains("=")) {
                url.substringBeforeLast("=") + "=w$quality-h$quality"
            } else {
                "$url=w$quality-h$quality"
            }
        }
        return url
    }

    // Public method for Now Playing/Lockscreen/Notification to get the HQ art
    fun getHighResThumbnailUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        return upgradeThumbnailUrlToHighQuality(url, 1000)
    }

    fun getSongInfo(songMap: JsonElement, songInfoIndex: SongInfoType): String {
        return songMap.jsonObject["flexColumns"]
            ?.jsonArray?.getOrNull(songInfoIndex.index)
            ?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")
            ?.jsonObject?.get("text")
            ?.jsonObject?.get("runs")
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.contentOrNull ?: ""
    }

    fun extractPlaylists(jsonString: String, settings: UmihiSettings): List<PlaylistInfo> {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val playlistInfos = mutableListOf<PlaylistInfo>()
        val tabs = json["contents"]?.jsonObject?.get("singleColumnBrowseResultsRenderer")?.jsonObject?.get("tabs")?.jsonArray

        val selectedTab = tabs?.firstOrNull {
            it.jsonObject["tabRenderer"]?.jsonObject?.get("selected")?.jsonPrimitive?.booleanOrNull == true
        }?.jsonObject?.get("tabRenderer")?.jsonObject

        val sectionList = selectedTab?.get("content")?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray

        sectionList?.forEach { section ->
            val renderer = section.jsonObject["gridRenderer"]?.jsonObject ?: return@forEach
            renderer["items"]?.jsonArray?.forEach { item ->
                val playlistRenderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@forEach
                val title = playlistRenderer["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach
                val browseId = playlistRenderer["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull ?: return@forEach
                val thumbnailUrl = getBestThumbnailUrl(playlistRenderer["thumbnailRenderer"] ?: return@forEach)
                playlistInfos.add(PlaylistInfo(id = browseId, title = title, coverHref = thumbnailUrl))
            }

            val continuationToken = renderer["continuations"]?.jsonArray?.firstOrNull()?.jsonObject?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.contentOrNull
            if (continuationToken != null) {
                val continuationJson = YoutubeRequestHelper.requestContinuation(continuationToken = continuationToken, settings = settings)
                playlistInfos.addAll(extractPlaylists(continuationJson, settings))
            }
        }

        val continuationGridItems = json["continuationContents"]?.jsonObject?.get("gridContinuation")?.jsonObject?.get("items")?.jsonArray
        continuationGridItems?.forEach { item ->
            val playlistRenderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@forEach
            val title = playlistRenderer["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach
            val browseId = playlistRenderer["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull ?: return@forEach
            val thumbnailUrl = getBestThumbnailUrl(playlistRenderer["thumbnailRenderer"] ?: return@forEach)
            playlistInfos.add(PlaylistInfo(id = browseId, title = title, coverHref = thumbnailUrl))
        }

        val continuationToken = json["continuationContents"]?.jsonObject?.get("gridContinuation")?.jsonObject?.get("continuations")?.jsonArray?.firstOrNull()?.jsonObject?.get("nextContinuationData")?.jsonObject?.get("continuation")?.jsonPrimitive?.contentOrNull
        if (continuationToken != null) {
            val continuationJson = YoutubeRequestHelper.requestContinuation(continuationToken = continuationToken, settings = settings)
            playlistInfos.addAll(extractPlaylists(continuationJson, settings))
        }

        return playlistInfos
    }

    fun extractSearchResults(jsonString: String): List<Song> {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val tabs = json["contents"]?.jsonObject?.get("tabbedSearchResultsRenderer")?.jsonObject?.get("tabs")?.jsonArray ?: return emptyList()
        val selectedTab = tabs.firstOrNull {
            it.jsonObject["tabRenderer"]?.jsonObject?.get("selected")?.jsonPrimitive?.booleanOrNull == true
        }?.jsonObject?.get("tabRenderer")?.jsonObject ?: return emptyList()
        val contents = selectedTab["content"]?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray ?: return emptyList()
        val songRendererList = contents.jsonArray.firstNotNullOfOrNull {
            it.jsonObject["musicShelfRenderer"]?.jsonObject?.get("contents")?.jsonArray
        } ?: return emptyList()
        return songRendererList.mapNotNull { extractSong(it) }
    }

    fun extractRelatedSongs(jsonString: String): List<Song> {
        return try {
            val root = Json.parseToJsonElement(jsonString).jsonObject
            val autoplayItems = root["contents"]?.jsonObject?.get("singleColumnWatchNextResults")?.jsonObject?.get("playlist")?.jsonObject?.get("playlist")?.jsonObject?.get("contents")?.jsonArray

            if (autoplayItems != null && autoplayItems.size > 1) {
                return autoplayItems.drop(1).take(10).mapNotNull { item ->
                    val renderer = item.jsonObject["playlistPanelVideoRenderer"]?.jsonObject ?: return@mapNotNull null
                    val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                    val artist = renderer["longBylineText"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                    val thumbnail = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray?.last()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
                    Song(youtubeId = videoId, title = title, artist = artist, thumbnailHref = upgradeThumbnailUrlToHighQuality(thumbnail))
                }
            }

            val queueItems = root["contents"]?.jsonObject?.get("singleColumnWatchNextResults")?.jsonObject?.get("tabbedRenderer")?.jsonObject?.get("watchNextTabbedResultsRenderer")?.jsonObject?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject?.get("tabRenderer")?.jsonObject?.get("content")?.jsonObject?.get("musicQueueRenderer")?.jsonObject?.get("content")?.jsonObject?.get("playlistPanelRenderer")?.jsonObject?.get("contents")?.jsonArray

            queueItems?.drop(1)?.take(10)?.mapNotNull { item ->
                val renderer = item.jsonObject["playlistPanelVideoRenderer"]?.jsonObject ?: return@mapNotNull null
                val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                val artist = renderer["longBylineText"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                val thumbnail = renderer["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray?.last()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
                Song(youtubeId = videoId, title = title, artist = artist, thumbnailHref = upgradeThumbnailUrlToHighQuality(thumbnail))
            } ?: emptyList()
        } catch (e: Exception) {
            UmihiHelper.printe("extractRelatedSongs failed: ${e.message}")
            emptyList()
        }
    }

    fun extractSongInfo(jsonString: String): Song {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val details = json.jsonObject["videoDetails"]?.jsonObject
        val videoId = details?.get("videoId")?.jsonPrimitive?.contentOrNull ?: ""
        val title = details?.get("title")?.jsonPrimitive?.contentOrNull ?: ""
        val author = details?.get("author")?.jsonPrimitive?.contentOrNull ?: ""
        val lengthSeconds: Int = details?.get("lengthSeconds")?.jsonPrimitive?.contentOrNull?.toInt() ?: 0

        return Song(
            youtubeId = videoId,
            title = title,
            artist = author,
            duration = formatSecondsForYouTubeDisplay(lengthSeconds),
            thumbnailHref = extractHighQualityThumbnail(jsonString)
        )
    }

    fun extractSongList(jsonString: String, settings: UmihiSettings): List<Song> {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val contents = json["contents"]?.jsonObject?.get("twoColumnBrowseResultsRenderer")?.jsonObject?.get("secondaryContents")?.jsonObject?.get("sectionListRenderer")?.jsonObject?.get("contents")?.jsonArray?.getOrNull(0)?.jsonObject?.get("musicPlaylistShelfRenderer")?.jsonObject?.get("contents")?.jsonArray
        return parseSongsFromContents(contents, settings)
    }

    fun extractContinuationSongs(jsonString: String, settings: UmihiSettings): List<Song> {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val contents = json["onResponseReceivedActions"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("appendContinuationItemsAction")?.jsonObject?.get("continuationItems")?.jsonArray
        return parseSongsFromContents(contents, settings)
    }

    private fun formatSecondsForYouTubeDisplay(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    private fun extractHighQualityThumbnail(jsonString: String): String {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val url = json["videoDetails"]?.jsonObject?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray?.last()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        return upgradeThumbnailUrlToHighQuality(url ?: "")
    }

    private fun parseSongsFromContents(contents: JsonArray?, settings: UmihiSettings): List<Song> {
        val songs = mutableListOf<Song>()
        if (contents == null) return songs

        for (shelf in contents) {
            val continuationContent = shelf.jsonObject["continuationItemRenderer"]
            if (continuationContent != null) {
                val token = continuationContent.jsonObject["continuationEndpoint"]?.jsonObject?.get("continuationCommand")?.jsonObject?.get("token")?.jsonPrimitive?.contentOrNull ?: ""
                val otherSongs = extractContinuationSongs(YoutubeRequestHelper.requestContinuation(continuationToken = token, settings = settings), settings)
                songs.addAll(otherSongs)
                continue
            }
            val song = extractSong(shelf) ?: continue
            songs.add(song)
        }
        return songs
    }

    fun extractSong(json: JsonElement): Song? {
        val songContent = json.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: return null
        val thumbnailUrl = getBestThumbnailUrl(songContent["thumbnail"] ?: return null)
        val title = getSongInfo(songContent, SongInfoType.TITLE)
        val artist = getSongInfo(songContent, SongInfoType.ARTIST)
        val videoId = songContent["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull ?: return null
        val duration = extractDuration(songContent)

        return Song(
            youtubeId = videoId,
            title = title,
            artist = artist,
            duration = duration,
            thumbnailHref = thumbnailUrl
        )
    }

    private suspend fun getTargetBitrateCeiling(context: Context, forDownload: Boolean = false): Int {
        return try {
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication<YoutubeHelperEntryPoint>(
                context.applicationContext,
                YoutubeHelperEntryPoint::class.java
            )
            val userPreferencesRepository = entryPoint.userPreferencesRepository()

            if (forDownload) {
                val targetQuality = userPreferencesRepository.downloadAudioQualityFlow.first()
                if (targetQuality == StreamingAudioQuality.HIGH) 0 else targetQuality.maxBitrateKbps
            } else {
                val connectivityStateHolder = entryPoint.connectivityStateHolder()
                val isMetered = connectivityStateHolder.isMeteredNetwork.value
                val forceHigh = userPreferencesRepository.forceHighQualityOnMobileFlow.first()

                val targetQuality = if (isMetered && !forceHigh) {
                    userPreferencesRepository.streamingAudioQualityMobileFlow.first()
                } else {
                    userPreferencesRepository.streamingAudioQualityWifiFlow.first()
                }
                if (targetQuality == StreamingAudioQuality.HIGH) 0 else targetQuality.maxBitrateKbps
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getDownloadUrl(context: Context, song: Song): String {
        val videoId = song.youtubeId
        val maxBitrate = getTargetBitrateCeiling(context, forDownload = true)
        val cacheKey = if (maxBitrate > 0) "${videoId}_dl_q$maxBitrate" else "${videoId}_dl_high"

        val cachedQuality = streamUrlLruCache.get(cacheKey)
        if (cachedQuality != null && isYoutubeUrlValid(cachedQuality)) return cachedQuality

        val result = getSongUrlFromYoutube(context, song, lowQuality = false, maxBitrateKbps = maxBitrate, requireM4a = true)
        val newUri = result.first
        streamUrlLruCache.put(cacheKey, newUri)
        return newUri
    }

    suspend fun getSongPlayerUrl(context: Context, song: Song, allowLocal: Boolean = false): String {
        val videoId = song.youtubeId
        if (song.audioFilePath?.isNotBlank() == true && File(song.audioFilePath).exists()) return song.audioFilePath

        val cachedLocalPath = localFilePathCache.get(videoId)
        if (cachedLocalPath != null && File(cachedLocalPath).exists()) return cachedLocalPath

        val localSongRepository = AppDatabase.getInstance(context).songRepository()
        var savedSong: Song? = null
        try { savedSong = localSongRepository.getSong(videoId) } catch (ex: Exception) { UmihiHelper.printe(ex.toString()) }

        if (savedSong?.audioFilePath != null && File(savedSong.audioFilePath).exists()) {
            localFilePathCache.put(videoId, savedSong.audioFilePath)
            return savedSong.audioFilePath
        }

        val maxBitrate = getTargetBitrateCeiling(context)
        val cacheKey = if (maxBitrate > 0) "${videoId}_q$maxBitrate" else "${videoId}_high"

        val cachedQuality = streamUrlLruCache.get(cacheKey)
        if (cachedQuality != null && isYoutubeUrlValid(cachedQuality)) return cachedQuality

        if (maxBitrate == 0 || maxBitrate >= 256) {
            val cachedHigh = streamUrlLruCache.get("${videoId}_high")
            if (cachedHigh != null && isYoutubeUrlValid(cachedHigh)) return cachedHigh
        }

        val result = getSongUrlFromYoutube(context, song, lowQuality = false, maxBitrateKbps = maxBitrate)
        val newUri = result.first
        val mimeType = result.second
        val bitrate = result.third
        streamUrlLruCache.put(cacheKey, newUri)
        mimeType?.let { streamMimeTypeLruCache.put(cacheKey, it) }
        bitrate?.let { streamBitrateLruCache.put(cacheKey, it) }
        if (maxBitrate == 0 || maxBitrate >= 256) {
            streamUrlLruCache.put("${videoId}_high", newUri)
            mimeType?.let { streamMimeTypeLruCache.put("${videoId}_high", it) }
            bitrate?.let { streamBitrateLruCache.put("${videoId}_high", it) }
        }
        return newUri
    }

    suspend fun getLowestQualityStreamUrl(context: Context, song: Song): String {
        val videoId = song.youtubeId
        if (song.audioFilePath?.isNotBlank() == true && File(song.audioFilePath).exists()) return song.audioFilePath

        val cachedLocalPath = localFilePathCache.get(videoId)
        if (cachedLocalPath != null && File(cachedLocalPath).exists()) return cachedLocalPath
        
        val localSongRepository = AppDatabase.getInstance(context).songRepository()
        val savedSong = try { localSongRepository.getSong(videoId) } catch (_: Exception) { null }
        if (savedSong?.audioFilePath != null && File(savedSong.audioFilePath).exists()) {
            localFilePathCache.put(videoId, savedSong.audioFilePath)
            return savedSong.audioFilePath
        }

        streamUrlLruCache.get("${videoId}_low")?.let { if (isYoutubeUrlValid(it)) return it }
        streamUrlLruCache.get("${videoId}_high")?.let { if (isYoutubeUrlValid(it)) return it }

        val lowResult = getSongUrlFromYoutube(context, song, lowQuality = true)
        val lowUrl = lowResult.first
        val mimeType = lowResult.second
        val bitrate = lowResult.third
        streamUrlLruCache.put("${videoId}_low", lowUrl)
        mimeType?.let { streamMimeTypeLruCache.put("${videoId}_low", it) }
        bitrate?.let { streamBitrateLruCache.put("${videoId}_low", it) }
        return lowUrl
    }

    suspend fun getHighestQualityStreamUrl(context: Context, song: Song): String {
        val videoId = song.youtubeId
        if (song.audioFilePath?.isNotBlank() == true && File(song.audioFilePath).exists()) return song.audioFilePath

        val cachedLocalPath = localFilePathCache.get(videoId)
        if (cachedLocalPath != null && File(cachedLocalPath).exists()) return cachedLocalPath
        
        val localSongRepository = AppDatabase.getInstance(context).songRepository()
        val savedSong = try { localSongRepository.getSong(videoId) } catch (_: Exception) { null }
        if (savedSong?.audioFilePath != null && File(savedSong.audioFilePath).exists()) {
            localFilePathCache.put(videoId, savedSong.audioFilePath)
            return savedSong.audioFilePath
        }

        val maxBitrate = getTargetBitrateCeiling(context)
        val cacheKey = if (maxBitrate > 0) "${videoId}_q$maxBitrate" else "${videoId}_high"
        streamUrlLruCache.get(cacheKey)?.let { if (isYoutubeUrlValid(it)) return it }

        val highResult = getSongUrlFromYoutube(context, song, lowQuality = false, maxBitrateKbps = maxBitrate)
        val highUrl = highResult.first
        val mimeType = highResult.second
        val bitrate = highResult.third
        streamUrlLruCache.put(cacheKey, highUrl)
        mimeType?.let { streamMimeTypeLruCache.put(cacheKey, it) }
        bitrate?.let { streamBitrateLruCache.put(cacheKey, it) }
        if (maxBitrate == 0 || maxBitrate >= 256) {
            streamUrlLruCache.put("${videoId}_high", highUrl)
            mimeType?.let { streamMimeTypeLruCache.put("${videoId}_high", it) }
            bitrate?.let { streamBitrateLruCache.put("${videoId}_high", it) }
        }
        return highUrl
    }

    fun registerLocalFilePath(youtubeId: String, filePath: String) {
        if (filePath.isNotBlank() && File(filePath).exists()) {
            localFilePathCache.put(youtubeId, filePath)
        }
    }

    suspend fun getSongPlayerUrlWithQuality(context: Context, song: Song, maxBitrateKbps: Int = 0): String {
        val videoId = song.youtubeId
        if (song.audioFilePath?.isNotBlank() == true && File(song.audioFilePath).exists()) return song.audioFilePath

        val cachedLocalPath = localFilePathCache.get(videoId)
        if (cachedLocalPath != null && File(cachedLocalPath).exists()) return cachedLocalPath
        
        val localSongRepository = AppDatabase.getInstance(context).songRepository()
        val savedSong = try { localSongRepository.getSong(videoId) } catch (_: Exception) { null }
        if (savedSong?.audioFilePath != null && File(savedSong.audioFilePath).exists()) {
            localFilePathCache.put(videoId, savedSong.audioFilePath)
            return savedSong.audioFilePath
        }

        val cacheKey = if (maxBitrateKbps > 0) "${videoId}_q${maxBitrateKbps}" else "${videoId}_high"
        streamUrlLruCache.get(cacheKey)?.let { if (isYoutubeUrlValid(it)) return it }

        val urlResult = getSongUrlFromYoutube(context, song, lowQuality = false, maxBitrateKbps = maxBitrateKbps)
        val url = urlResult.first
        val mimeType = urlResult.second
        val bitrate = urlResult.third
        streamUrlLruCache.put(cacheKey, url)
        mimeType?.let { streamMimeTypeLruCache.put(cacheKey, it) }
        bitrate?.let { streamBitrateLruCache.put(cacheKey, it) }
        return url
    }

    fun invalidateStreamCache(youtubeId: String) {
        streamUrlLruCache.remove("${youtubeId}_low")
        streamUrlLruCache.remove("${youtubeId}_high")
        streamMimeTypeLruCache.remove("${youtubeId}_low")
        streamMimeTypeLruCache.remove("${youtubeId}_high")
        streamBitrateLruCache.remove("${youtubeId}_low")
        streamBitrateLruCache.remove("${youtubeId}_high")
    }

    private fun extractDuration(songContent: JsonObject): String {
        val durationRegex = Regex("""\d+:\d{2}(:\d{2})?""")
        val fixedDuration = songContent["fixedColumns"]?.jsonArray?.firstOrNull()?.jsonObject?.get("musicResponsiveListItemFixedColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
        if (fixedDuration != null) return fixedDuration

        val flexColumns = songContent["flexColumns"]?.jsonArray ?: return ""
        for (column in flexColumns) {
            val runs = column.jsonObject["musicResponsiveListItemFlexColumnRenderer"]?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray ?: continue
            for (run in runs) {
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: continue
                if (durationRegex.matches(text)) return text
            }
        }
        return ""
    }

    private fun isCipheredFormat(format: PlayerResponse.StreamingData.Format): Boolean {
        return format.url == null && (format.signatureCipher != null || format.cipher != null)
    }

    private fun shouldSkipCipheredWebCandidate(client: YouTubeClient, format: PlayerResponse.StreamingData.Format, authState: PlaybackAuthState): Boolean {
        val isWebClient = StreamClientUtils.isWebClient(client.clientName)
        val isCiphered = isCipheredFormat(format)
        val hasGvsPoToken = !authState.resolveGvsPoToken(client).isNullOrBlank()
        if (authState.webClientPoTokenEnabled && isWebClient && isCiphered && !hasGvsPoToken) return true
        return false
    }

    private fun isStreamClientTemporarilyBlocked(videoId: String, clientKey: String?, authFingerprint: String): Boolean {
        val normalizedClientKey = StreamClientUtils.normalizeClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return false
        val key = "$authFingerprint:$videoId:$normalizedClientKey"
        val until = failedStreamClientsUntil[key] ?: return false
        if (until <= System.currentTimeMillis()) {
            failedStreamClientsUntil.remove(key)
            return false
        }
        return true
    }

    private fun markStreamClientFailed(videoId: String, clientKey: String?, httpStatusCode: Int, authFingerprint: String) {
        if (httpStatusCode !in setOf(403, 404, 410, 416)) return
        val normalizedClientKey = StreamClientUtils.normalizeClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return
        val key = "$authFingerprint:$videoId:$normalizedClientKey"
        failedStreamClientsUntil[key] = System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
    }

    private fun validateStatus(url: String): Boolean {
        val expireParam = url.substringAfter("expire=", "").substringBefore("&")
        if (expireParam.isNotEmpty()) {
            val expireSecs = expireParam.toLongOrNull()
            if (expireSecs != null) {
                val currentSecs = System.currentTimeMillis() / 1000
                if (expireSecs > currentSecs + 60) return true
            }
        }
        try {
            val requestProfile = StreamClientUtils.resolveRequestProfile(url)
            val rangeRequest = StreamClientUtils.applyRequestProfile(okhttp3.Request.Builder().get().header("Range", "bytes=0-0").url(url), requestProfile).build()
            val streamProxy = unshoo.ianshulyadav.pixelmusic.innertube.YouTube.streamProxy
            val httpClient = if (streamProxy != null) {
                OkHttpClient.Builder().connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES)).proxy(streamProxy).build()
            } else { client }
            return httpClient.newCall(rangeRequest).execute().use { response ->
                val code = response.code
                if (code == 403) return@use false
                if (code !in 200..399 && code != 416) return@use false
                val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.US)
                if (contentType.startsWith("text/html") || contentType.startsWith("text/plain") || contentType.startsWith("application/json") || contentType.startsWith("application/xml") || contentType.startsWith("text/xml")) return@use false
                if (code == 416) return@use true
                response.body.source().request(1)
            }
        } catch (e: Exception) { UmihiHelper.printe("validateStatus: probe failed: ${e.message}") }
        return false
    }

    fun getMimeTypeForCachedUrl(cacheKey: String): String? = streamMimeTypeLruCache.get(cacheKey)
    fun getBitrateForCachedUrl(cacheKey: String): Int? = streamBitrateLruCache.get(cacheKey)

    private fun selectCandidates(playerResponse: PlayerResponse, lowQuality: Boolean, maxBitrateKbps: Int, requireM4a: Boolean = false): List<PlayerResponse.StreamingData.Format> {
        val formats = playerResponse.streamingData?.adaptiveFormats?.filter { 
                it.mimeType.contains("audio", ignoreCase = true) && it.bitrate > 0 && !it.mimeType.contains("mp3", ignoreCase = true) && !it.mimeType.contains("mpeg", ignoreCase = true) && !it.mimeType.contains("mpga", ignoreCase = true)
            }.orEmpty()
        if (formats.isEmpty()) return emptyList()

        val opusFormats = formats.filter { it.mimeType.contains("opus", ignoreCase = true) }
        val m4aFormats = formats.filter { (it.mimeType.contains("mp4", ignoreCase = true) || it.mimeType.contains("m4a", ignoreCase = true) || it.mimeType.contains("mp4a", ignoreCase = true)) && !it.mimeType.contains("opus", ignoreCase = true) }
        val webmFormats = formats.filter { it.mimeType.contains("webm", ignoreCase = true) && !it.mimeType.contains("opus", ignoreCase = true) }
        val otherFormats = formats.filter { !it.mimeType.contains("opus", ignoreCase = true) && !it.mimeType.contains("mp4", ignoreCase = true) && !it.mimeType.contains("m4a", ignoreCase = true) && !it.mimeType.contains("mp4a", ignoreCase = true) && !it.mimeType.contains("webm", ignoreCase = true) }

        fun sortGroup(group: List<PlayerResponse.StreamingData.Format>): List<PlayerResponse.StreamingData.Format> {
            if (group.isEmpty()) return emptyList()
            return when {
                lowQuality -> group.sortedBy { it.bitrate }
                maxBitrateKbps > 0 -> {
                    val bpsCeiling = maxBitrateKbps * 1000
                    val withinCeiling = group.filter { it.bitrate <= bpsCeiling }
                    if (withinCeiling.isNotEmpty()) withinCeiling.sortedByDescending { it.bitrate } else group.sortedBy { it.bitrate }
                }
                else -> group.sortedByDescending { it.bitrate }
            }
        }
        if (requireM4a) return sortGroup(m4aFormats) + sortGroup(otherFormats)
        return sortGroup(opusFormats) + sortGroup(m4aFormats) + sortGroup(webmFormats) + sortGroup(otherFormats)
    }

private suspend fun getSongUrlFromYoutube(
    context: Context,
    song: Song,
    retries: Int = 3,
    lowQuality: Boolean = false,
    maxBitrateKbps: Int = 0,
    requireM4a: Boolean = false
): Triple<String, String?, Int?> = withContext(Dispatchers.IO) {
    val videoId = song.youtubeId ?: throw Exception("Invalid video ID")
    val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
    
    val audioStreams = streamInfo.audioStreams
    if (audioStreams.isNullOrEmpty()) throw Exception("No audio streams found for $videoId")

    val filteredStreams = if (requireM4a) {
        audioStreams.filter { 
            val formatName = it.format?.name ?: ""
            formatName.contains("m4a", true) || formatName.contains("mp4", true) 
        }
    } else audioStreams
    
    val targetStreams = if (filteredStreams.isNotEmpty()) filteredStreams else audioStreams

    val candidate = when {
        lowQuality -> targetStreams.minByOrNull { it.averageBitrate }
        maxBitrateKbps > 0 -> {
            val bpsCeiling = maxBitrateKbps * 1000
            val withinCeiling = targetStreams.filter { it.averageBitrate <= bpsCeiling }
            if (withinCeiling.isNotEmpty()) withinCeiling.maxByOrNull { it.averageBitrate }
            else targetStreams.minByOrNull { it.averageBitrate }
        }
        else -> targetStreams.maxByOrNull { it.averageBitrate }
    } ?: targetStreams.first()

    val formatName = candidate.format?.name ?: "mp4"
    val mimeType = "audio/" + formatName.lowercase().replace("m4a", "mp4")
    Triple(candidate.content, mimeType, candidate.averageBitrate)
}
    private fun normalizeMimeType(rawMimeType: String): String {
        val lower = rawMimeType.lowercase(Locale.US)
        return when {
            lower.contains("opus") -> "audio/opus"
            lower.contains("mp4a") || lower.contains("mp4") || lower.contains("m4a") -> "audio/mp4"
            lower.contains("vorbis") -> "audio/ogg"
            lower.contains("webm") -> "audio/webm"
            else -> rawMimeType.substringBefore(";").trim()
        }
    }

    private suspend fun isYoutubeUrlValid(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val expireParam = url.substringAfter("expire=", "").substringBefore("&")
            if (expireParam.isNotEmpty()) {
                val expireTimeSecs = expireParam.toLongOrNull()
                if (expireTimeSecs != null) {
                    val currentTimeSecs = System.currentTimeMillis() / 1000
                    if (expireTimeSecs > currentTimeSecs + 60) return@withContext true
                }
            }

            val request = Request.Builder().url(url).head().build()
            val streamProxy = unshoo.ianshulyadav.pixelmusic.innertube.YouTube.streamProxy
            val httpClient = if (streamProxy != null) {
                OkHttpClient.Builder().connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES)).proxy(streamProxy).build()
            } else { client }
            httpClient.newCall(request).execute().use { response -> return@withContext response.isSuccessful }
        } catch (_: Exception) { return@withContext false }
    }

    fun findObjectsWithKey(element: JsonElement, key: String, result: MutableList<JsonObject>) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey(key)) { element[key]?.jsonObject?.let { result.add(it) } }
                for (value in element.values) { findObjectsWithKey(value, key, result) }
            }
            is JsonArray -> { for (value in element) { findObjectsWithKey(value, key, result) } }
            else -> {}
        }
    }

    fun findContinuationToken(element: JsonElement): String? {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("nextContinuationData")) return element["nextContinuationData"]?.jsonObject?.get("continuation")?.jsonPrimitive?.contentOrNull
                if (element.containsKey("continuationEndpoint")) return element["continuationEndpoint"]?.jsonObject?.get("continuationCommand")?.jsonObject?.get("token")?.jsonPrimitive?.contentOrNull
                for (value in element.values) {
                    val token = findContinuationToken(value)
                    if (token != null) return token
                }
            }
            is JsonArray -> {
                for (value in element) {
                    val token = findContinuationToken(value)
                    if (token != null) return token
                }
            }
            else -> {}
        }
        return null
    }

    fun extractAccountPlaylists(jsonString: String, settings: UmihiSettings): List<PlaylistItem> {
        val root = Json.parseToJsonElement(jsonString)
        val items = mutableListOf<JsonObject>()
        findObjectsWithKey(root, "musicTwoRowItemRenderer", items)
        findObjectsWithKey(root, "musicResponsiveListItemRenderer", items)

        val playlistsList = mutableListOf<PlaylistItem>()
        for (item in items) {
            var title = item["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            if (title == null) {
                title = item["flexColumns"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            }
            if (title == null) continue

            var browseId = item["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull
            if (browseId == null) {
                 browseId = item["overlay"]?.jsonObject?.get("musicItemThumbnailOverlayRenderer")?.jsonObject?.get("content")?.jsonObject?.get("musicPlayButtonRenderer")?.jsonObject?.get("playNavigationEndpoint")?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("playlistId")?.jsonPrimitive?.contentOrNull
            }
            if (browseId == null || browseId == "SE") continue

            val thumbnailUrl = item["thumbnailRenderer"]?.let { getBestThumbnailUrl(it) } ?: item["thumbnail"]?.let { getBestThumbnailUrl(it) }
            playlistsList.add(PlaylistItem(id = browseId, title = title, thumbnailUrl = thumbnailUrl))
        }

        val continuationToken = findContinuationToken(root)
        if (continuationToken != null) {
            try {
                val nextJson = YoutubeRequestHelper.requestContinuation(continuationToken, settings)
                playlistsList.addAll(extractAccountPlaylists(nextJson, settings))
            } catch (e: Exception) { UmihiHelper.printe("Error fetching playlists continuation: ${e.message}") }
        }
        return playlistsList.distinctBy { it.id }
    }

    fun extractAccountAlbums(jsonString: String, settings: UmihiSettings): List<AlbumItem> {
        val root = Json.parseToJsonElement(jsonString)
        val items = mutableListOf<JsonObject>()
        findObjectsWithKey(root, "musicTwoRowItemRenderer", items)
        findObjectsWithKey(root, "musicResponsiveListItemRenderer", items)

        val albumsList = mutableListOf<AlbumItem>()
        for (item in items) {
             var title = item["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            if (title == null) {
                title = item["flexColumns"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            }
            if (title == null) continue

            val browseId = item["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull ?: continue
            val thumbnailUrl = item["thumbnailRenderer"]?.let { getBestThumbnailUrl(it) } ?: item["thumbnail"]?.let { getBestThumbnailUrl(it) }

            var artist: String? = null
            val subtitleRuns = item["subtitle"]?.jsonObject?.get("runs")?.jsonArray
            if (subtitleRuns != null) {
                val filterWords = setOf("album", "ep", "single", "playlist", "artist", "•", "·", " ")
                artist = subtitleRuns.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.firstOrNull { runText -> runText.trim().lowercase() !in filterWords && runText.trim().isNotEmpty() }
            }
            if (artist == null) {
                 val flexRuns = item["flexColumns"]?.jsonArray?.getOrNull(1)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray
                 if (flexRuns != null) {
                     val filterWords = setOf("album", "ep", "single", "playlist", "artist", "•", "·", " ")
                     artist = flexRuns.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.firstOrNull { runText -> runText.trim().lowercase() !in filterWords && runText.trim().isNotEmpty() }
                 }
            }
            albumsList.add(AlbumItem(id = browseId, title = title, artist = artist, thumbnailUrl = thumbnailUrl))
        }

        val continuationToken = findContinuationToken(root)
        if (continuationToken != null) {
            try {
                val nextJson = YoutubeRequestHelper.requestContinuation(continuationToken, settings)
                albumsList.addAll(extractAccountAlbums(nextJson, settings))
            } catch (e: Exception) { UmihiHelper.printe("Error fetching albums continuation: ${e.message}") }
        }
        return albumsList.distinctBy { it.id }
    }

    fun extractAccountArtists(jsonString: String, settings: UmihiSettings): List<ArtistItem> {
        val root = Json.parseToJsonElement(jsonString)
        val items = mutableListOf<JsonObject>()
        findObjectsWithKey(root, "musicTwoRowItemRenderer", items)
        findObjectsWithKey(root, "musicResponsiveListItemRenderer", items)

        val artistsList = mutableListOf<ArtistItem>()
        for (item in items) {
             var title = item["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            if (title == null) {
                title = item["flexColumns"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            }
            if (title == null) continue

            val browseId = item["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull ?: continue
            val thumbnailUrl = item["thumbnailRenderer"]?.let { getBestThumbnailUrl(it) } ?: item["thumbnail"]?.let { getBestThumbnailUrl(it) }
            artistsList.add(ArtistItem(id = browseId, name = title, thumbnailUrl = thumbnailUrl))
        }

        val continuationToken = findContinuationToken(root)
        if (continuationToken != null) {
            try {
                val nextJson = YoutubeRequestHelper.requestContinuation(continuationToken, settings)
                artistsList.addAll(extractAccountArtists(nextJson, settings))
            } catch (e: Exception) { UmihiHelper.printe("Error fetching artists continuation: ${e.message}") }
        }
        return artistsList.distinctBy { it.id }
    }
}

enum class SongInfoType(val index: Int) {
    TITLE(0),
    ARTIST(1),
}

@Serializable
data class PlaylistItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String?
)

@Serializable
data class AlbumItem(
    val id: String,
    val title: String,
    val artist: String?,
    val thumbnailUrl: String?
)

@Serializable
data class ArtistItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String?
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface YoutubeHelperEntryPoint {
    fun connectivityStateHolder(): ConnectivityStateHolder
    fun userPreferencesRepository(): UserPreferencesRepository
}
