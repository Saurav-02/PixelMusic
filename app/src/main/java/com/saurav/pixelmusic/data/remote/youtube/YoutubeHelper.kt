package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import android.util.LruCache
import android.widget.Toast
import androidx.core.net.toUri
import com.saurav.pixelmusic.data.database.youtube.AppDatabase
import com.saurav.pixelmusic.data.model.youtube.PlaylistInfo
import com.saurav.pixelmusic.data.model.youtube.Song
import com.saurav.pixelmusic.data.model.youtube.UmihiSettings
import com.saurav.pixelmusic.data.preferences.StreamingAudioQuality
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.presentation.viewmodel.ConnectivityStateHolder
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
import saurav.shru.pixelmusic.innertube.models.YouTubeClient
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.IOS
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.MOBILE
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.TVHTML5
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.WEB
import saurav.shru.pixelmusic.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import saurav.shru.pixelmusic.innertube.utils.StreamClientUtils
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.PlaybackAuthState
import saurav.shru.pixelmusic.innertube.models.response.PlayerResponse
import com.saurav.pixelmusic.data.preferences.PlayerStreamClient
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import com.saurav.pixelmusic.utils.PixelHttpLoggingInterceptor

object YoutubeHelper {
    val client = OkHttpClient.Builder()
    .connectionPool(okhttp3.ConnectionPool(15, 5, java.util.concurrent.TimeUnit.MINUTES))
    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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

    private fun upgradeThumbnailUrlToHighQuality(url: String, quality: Int = 0): String {
        if (url.isBlank()) return url
        val targetQuality = if (quality > 0) {
            when {
                quality <= 256 -> com.saurav.pixelmusic.data.preferences.AlbumArtQuality.LOW
                quality <= 512 -> com.saurav.pixelmusic.data.preferences.AlbumArtQuality.MEDIUM
                quality <= 800 -> com.saurav.pixelmusic.data.preferences.AlbumArtQuality.HIGH
                else -> com.saurav.pixelmusic.data.preferences.AlbumArtQuality.ORIGINAL
            }
        } else {
            com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
        }
        return com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(url, targetQuality) ?: url
    }

    // Public method for Now Playing/Lockscreen/Notification to get the HQ art
    fun getHighResThumbnailUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        return upgradeThumbnailUrlToHighQuality(url)
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
        val playlistInfos = mutableListOf<PlaylistInfo>()
        try {
            val root = Json.parseToJsonElement(jsonString)
            val items = mutableListOf<JsonObject>()
            findObjectsWithKey(root, "musicTwoRowItemRenderer", items)
            findObjectsWithKey(root, "musicResponsiveListItemRenderer", items)

            for (item in items) {
                var title = item["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                if (title == null) {
                    title = item["flexColumns"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject?.get("text")?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                }
                if (title.isNullOrBlank()) continue

                var browseId = item["navigationEndpoint"]?.jsonObject?.get("browseEndpoint")?.jsonObject?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId == null) {
                    browseId = item["overlay"]?.jsonObject?.get("musicItemThumbnailOverlayRenderer")?.jsonObject?.get("content")?.jsonObject?.get("musicPlayButtonRenderer")?.jsonObject?.get("playNavigationEndpoint")?.jsonObject?.get("watchEndpoint")?.jsonObject?.get("playlistId")?.jsonPrimitive?.contentOrNull
                }
                if (browseId.isNullOrBlank() || browseId == "SE") continue

                val thumbnailUrl = item["thumbnailRenderer"]?.let { getBestThumbnailUrl(it) } ?: item["thumbnail"]?.let { getBestThumbnailUrl(it) }
                playlistInfos.add(PlaylistInfo(id = browseId, title = title, coverHref = upgradeThumbnailUrlToHighQuality(thumbnailUrl ?: "")))
            }

            val continuationToken = findContinuationToken(root)
            if (continuationToken != null) {
                try {
                    val nextJson = YoutubeRequestHelper.requestContinuation(continuationToken = continuationToken, settings = settings)
                    playlistInfos.addAll(extractPlaylists(nextJson, settings))
                } catch (e: Exception) {
                    UmihiHelper.printe("Error fetching playlists continuation: ${e.message}")
                }
            }
        } catch (e: Exception) {
            UmihiHelper.printe("Error in extractPlaylists: ${e.message}")
        }
        return playlistInfos.distinctBy { it.id }
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
        val root = Json.parseToJsonElement(jsonString)
        val shelfList = mutableListOf<JsonObject>()
        findObjectsWithKey(root, "musicPlaylistShelfRenderer", shelfList)
        if (shelfList.isEmpty()) {
            findObjectsWithKey(root, "musicShelfRenderer", shelfList)
        }
        val contents = shelfList.firstOrNull()?.get("contents")?.jsonArray
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
                when (targetQuality) {
                    StreamingAudioQuality.AUTO -> {
                        val bandwidthKbps = connectivityStateHolder.getDownstreamBandwidthKbps()
                        when {
                            bandwidthKbps >= 3000 -> 0
                            bandwidthKbps in 1000 until 3000 -> 128
                            bandwidthKbps in 1 until 1000 -> 64
                            else -> if (isMetered) 128 else 0
                        }
                    }
                    StreamingAudioQuality.HIGH -> 0
                    else -> targetQuality.maxBitrateKbps
                }
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

    fun getMimeTypeForCachedUrl(cacheKey: String): String? = streamMimeTypeLruCache.get(cacheKey)
    fun getBitrateForCachedUrl(cacheKey: String): Int? = streamBitrateLruCache.get(cacheKey)

private suspend fun getSongUrlFromYoutube(
    context: Context,
    song: Song,
    retries: Int = 3,
    lowQuality: Boolean = false,
    maxBitrateKbps: Int = 0,
    requireM4a: Boolean = false
): Triple<String, String?, Int?> = withContext(Dispatchers.IO) {
    val videoId = song.youtubeId
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
                    return@withContext expireTimeSecs > currentTimeSecs + 60
                }
            }
            return@withContext false
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
