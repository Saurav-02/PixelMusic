package com.unshoo.pixelmusic.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import androidx.core.net.toUri
import com.google.gson.Gson
import com.kyant.taglib.TagLib
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.model.Lyrics
import com.unshoo.pixelmusic.data.model.SyncedLine
import com.unshoo.pixelmusic.data.model.LyricsSourcePreference
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.data.network.lyrics.LrcLibApiService
import com.unshoo.pixelmusic.data.network.lyrics.LrcLibResponse
import com.unshoo.pixelmusic.utils.LyricsImportSecurity
import com.unshoo.pixelmusic.utils.LyricsImportValidationResult
import com.unshoo.pixelmusic.utils.LogUtils
import com.unshoo.pixelmusic.utils.LyricsUtils
import com.unshoo.pixelmusic.utils.NetworkRetryUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import okhttp3.OkHttpClient
import okhttp3.Request

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private fun Lyrics.isValid(): Boolean = !synced.isNullOrEmpty() || !plain.isNullOrEmpty()

private data class LyricsData(
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val wordByWordLyrics: String? = null
) {
    fun hasLyrics(): Boolean =
        !plainLyrics.isNullOrBlank() ||
            !syncedLyrics.isNullOrBlank() ||
            !wordByWordLyrics.isNullOrBlank()
}

private data class RemoteSearchStrategy(
    val name: String,
    val request: suspend () -> Array<LrcLibResponse>?
)

private data class RemoteSearchBatch(
    val strategyName: String,
    val responses: List<LrcLibResponse>
)

private enum class RemoteLyricsMatchMode {
    AUTOMATIC,
    CANDIDATE
}

private data class RemoteLyricsMatch(
    val response: LrcLibResponse,
    val score: Int
)

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lrcLibApiService: LrcLibApiService,
    private val lyricsDao: com.unshoo.pixelmusic.data.database.LyricsDao,
    private val okHttpClient: OkHttpClient
) : LyricsRepository {

    companion object {
        private const val TAG = "LyricsRepository"
        private const val MAX_LYRICS_CACHE_SIZE = 150
        private const val LRCLIB_MIN_DELAY = 100L
        private const val MAX_CALLS_PER_MINUTE = 30
        private const val AMLLDB_NCM_LYRICS_BASE_URL = "https://amlldb.bikonoo.com/lyrics/ncm-lyrics/"
        private const val NETWORK_RETRY_ATTEMPTS = 3
        private const val NETWORK_RETRY_INITIAL_DELAY_MS = 500L

        private val BRACKETED_QUALIFIER_REGEX = Regex("""[\(\[\{]([^)\]\}]*)[\)\]\}]""")
        private val FEATURE_QUALIFIER_REGEX = Regex("""\b(feat(?:uring)?|ft)\.?\b""", RegexOption.IGNORE_CASE)
        private val TITLE_SEPARATOR_REGEX = Regex("""\s+[-\u2013\u2014:]\s+""")
        private val TIMING_VARIANT_KEYWORDS = setOf(
            "remix", "mix", "mashup", "bootleg", "edit", "extended", "radio", "club",
            "vip", "dub", "live", "acoustic", "unplugged", "sped", "slowed", "nightcore",
            "instrumental", "karaoke", "cover", "demo", "version", "rework", "flip", "refix"
        )
        private val TITLE_DROP_QUALIFIERS = setOf(
            "explicit", "clean", "mono", "stereo", "official audio", "official video"
        )
        private val UNKNOWN_ARTISTS = setOf(
            "", "<unknown>", "unknown", "unknown artist", "various artists", "various"
        )
        private val ARTIST_CONNECTOR_TOKENS = setOf(
            "feat", "featuring", "ft", "and", "with", "x", "vs", "the"
        )
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lyricsCache = LruCache<String, Lyrics>(MAX_LYRICS_CACHE_SIZE)
    private val lastApiCalls = ConcurrentHashMap<String, Long>()
    private val apiCallCounts = ConcurrentHashMap<String, Int>()
    private val gson = Gson()

    private suspend fun runSearchStrategiesFast(
        strategies: List<RemoteSearchStrategy>
    ): List<LrcLibResponse> = coroutineScope {
        if (strategies.isEmpty()) return@coroutineScope emptyList()

        val channel = Channel<RemoteSearchBatch>(capacity = strategies.size)
        val jobs = strategies.map { strategy ->
            launch {
                val responses = runCatching {
                    withNetworkRetry(operationName = "lrclib_strategy:${strategy.name}") {
                        strategy.request()
                    }
                }.getOrElse { error ->
                    Log.d(TAG, "Strategy ${strategy.name} failed after retries: ${error.message}")
                    null
                }?.toList().orEmpty()
                
                channel.trySend(
                    RemoteSearchBatch(
                        strategyName = strategy.name,
                        responses = responses
                    )
                )
            }
        }

        repeat(strategies.size) {
            val batch = channel.receive()
            if (batch.responses.isNotEmpty()) {
                Log.d(TAG, "Fast search hit from strategy: ${batch.strategyName} (${batch.responses.size} results)")
                jobs.forEach { it.cancel() }
                channel.close()
                return@coroutineScope batch.responses.distinctBy { it.id }
            }
        }

        channel.close()
        emptyList()
    }

    private suspend fun <T> withNetworkRetry(
        operationName: String,
        maxAttempts: Int = NETWORK_RETRY_ATTEMPTS,
        initialDelayMs: Long = NETWORK_RETRY_INITIAL_DELAY_MS,
        shouldRetry: (Throwable) -> Boolean = { it is IOException || (it is HttpException && (it.code() == 429 || it.code() >= 500)) },
        block: suspend () -> T
    ): T {
        return NetworkRetryUtils.withNetworkRetry(
            operationName = operationName,
            maxAttempts = maxAttempts,
            initialDelayMs = initialDelayMs,
            shouldRetry = shouldRetry,
            onRetry = { attempt, attempts, throwable ->
                Log.d(TAG, "Retrying $operationName after failure ($attempt/$attempts): ${throwable.message}")
            },
            block = block
        )
    }

    private fun Int.isRetryableHttpStatusCode(): Boolean {
        return this == 429 || this in 500..599
    }

    private fun calculateApiDelay(apiName: String, currentTime: Long): Long {
        val lastCall = lastApiCalls[apiName] ?: 0L
        val minDelay = when (apiName.lowercase()) {
            "lrclib" -> LRCLIB_MIN_DELAY
            else -> 250L
        }

        val timeSinceLastCall = currentTime - lastCall
        if (timeSinceLastCall < minDelay) {
            return minDelay - timeSinceLastCall
        }

        val callsInLastMinute = apiCallCounts[apiName] ?: 0
        if (callsInLastMinute >= MAX_CALLS_PER_MINUTE) {
            return minDelay * 2
        }

        return 0L
    }

    private fun updateLastApiCall(apiName: String, timestamp: Long) {
        lastApiCalls[apiName] = timestamp

        val currentCount = apiCallCounts[apiName] ?: 0
        apiCallCounts[apiName] = currentCount + 1

        if (currentCount == 0) {
            repositoryScope.launch {
                delay(60000)
                apiCallCounts[apiName] = 0
            }
        }
    }

    override suspend fun getLyrics(
        song: Song,
        sourcePreference: LyricsSourcePreference,
        forceRefresh: Boolean
    ): Lyrics? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(song.id)
        val isNeteaseTrack = isNeteaseSong(song)
        
        Log.d(TAG, "===== FETCH LYRICS START: ${song.displayArtist} - ${song.title} (forceRefresh=$forceRefresh, source=$sourcePreference) =====")

        if (!forceRefresh && !isNeteaseTrack) {
            lyricsCache.get(cacheKey)?.let { cached ->
                Log.d(TAG, "===== RETURNING IN-MEMORY CACHED LYRICS =====")
                return@withContext cached
            }
        }

        if (!forceRefresh) {
            loadStoredLyrics(song, cacheKey, includeMemoryCache = false)?.let { stored ->
                lyricsCache.put(cacheKey, stored.first)
                Log.d(TAG, "===== RETURNING STORED LYRICS WITHOUT REMOTE FETCH =====")
                return@withContext stored.first
            }
        }

        val fetchFromLocal: suspend () -> Lyrics? = { findLocalLyricsFile(song) }
        val fetchFromEmbedded: suspend () -> Lyrics? = { loadEmbeddedLyricsFromMetadata(song) }
        val fetchFromAPI: suspend () -> Lyrics? = { fetchLyricsFromAPI(song) }

        val sourceFetchers = when (sourcePreference) {
            LyricsSourcePreference.API_FIRST -> listOf(fetchFromAPI, fetchFromEmbedded, fetchFromLocal)
            LyricsSourcePreference.EMBEDDED_FIRST -> listOf(fetchFromEmbedded, fetchFromAPI, fetchFromLocal)
            LyricsSourcePreference.LOCAL_FIRST -> listOf(fetchFromLocal, fetchFromEmbedded, fetchFromAPI)
        }

        for ((index, fetcher) in sourceFetchers.withIndex()) {
            try {
                val lyrics = fetcher()
                if (lyrics != null && lyrics.isValid()) {
                    lyricsCache.put(cacheKey, lyrics)
                    saveLocalLyricsJson(song, lyrics)
                    return@withContext lyrics
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching from source ${index + 1}: ${e.message}")
            }
        }

        return@withContext null
    }

    override suspend fun getStoredLyrics(song: Song): Pair<Lyrics, String>? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(song.id)
        loadStoredLyrics(song, cacheKey, includeMemoryCache = true)?.also { stored ->
            lyricsCache.put(cacheKey, stored.first)
        }
    }

    private suspend fun fetchLyricsFromAPI(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        if (isNeteaseSong(song)) {
            val amlLyrics = fetchFromAmlldb(song)
            if (amlLyrics != null) return@withContext amlLyrics
        }

        val cachedJson = loadLocalLyricsJson(song)
        if (cachedJson != null) return@withContext cachedJson

        val currentTime = System.currentTimeMillis()
        val delayNeeded = calculateApiDelay("lrclib", currentTime)
        if (delayNeeded > 0) delay(delayNeeded)
        updateLastApiCall("lrclib", System.currentTimeMillis())

        try {
            val rawTitle = song.title.trim()
            val rawArtist = song.displayArtist.trim()
            val sanitizedTitle = sanitizeTitleForLyrics(rawTitle)
            val primaryArtist = extractPrimaryArtist(rawArtist)

            var results = emptyList<LrcLibResponse>()

            // Pass 1: Strict Sanitized Match on LRCLIB
            if (sanitizedTitle.isNotBlank()) {
                results = runCatching {
                    withNetworkRetry(operationName = "lrclib_sanitized") {
                        lrcLibApiService.searchLyrics(trackName = sanitizedTitle, artistName = primaryArtist)
                    }
                }.getOrNull()?.toList() ?: emptyList()
            }

            // Pass 2: Raw Data Fallback
            if (results.isEmpty() && (sanitizedTitle != rawTitle || primaryArtist != rawArtist)) {
                results = runCatching {
                    withNetworkRetry(operationName = "lrclib_raw") {
                        lrcLibApiService.searchLyrics(trackName = rawTitle, artistName = rawArtist)
                    }
                }.getOrNull()?.toList() ?: emptyList()
            }

            // Pass 3: Title Only Fallback
            if (results.isEmpty() && sanitizedTitle.isNotBlank()) {
                results = runCatching {
                    withNetworkRetry(operationName = "lrclib_title_only") {
                        lrcLibApiService.searchLyrics(trackName = sanitizedTitle)
                    }
                }.getOrNull()?.toList() ?: emptyList()
            }

            // Check if LRCLIB gave us a SYNCED match
            val rankedLrcLib = rankRemoteLyricsMatches(song = song, responses = results, mode = RemoteLyricsMatchMode.AUTOMATIC, primaryArtist = primaryArtist)
            val bestLrcLibSynced = rankedLrcLib.firstOrNull { hasSyncedLyrics(it.response) }?.response

            if (bestLrcLibSynced != null) {
                val rawLyrics = bestLrcLibSynced.syncedLyrics!!
                val parsed = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                if (parsed.isValid()) {
                    saveToDbAndCache(song, rawLyrics, isSynced = true)
                    return@withContext parsed
                }
            }

            // Provider 2 Fallback: Musixmatch Synced
            val mxLyrics = fetchFromMusixmatch(sanitizedTitle.ifBlank { rawTitle }, primaryArtist.ifBlank { rawArtist })
            if (mxLyrics != null && !mxLyrics.synced.isNullOrEmpty()) {
                val rawLyrics = lyricsToRawContent(mxLyrics) ?: ""
                saveToDbAndCache(song, rawLyrics, isSynced = true)
                return@withContext mxLyrics
            }

            // Provider 3 Fallback: NetEase Search Synced
            val neteaseLyrics = fetchFromNetEaseSearch(sanitizedTitle.ifBlank { rawTitle }, primaryArtist.ifBlank { rawArtist })
            if (neteaseLyrics != null && !neteaseLyrics.synced.isNullOrEmpty()) {
                val rawLyrics = lyricsToRawContent(neteaseLyrics) ?: ""
                saveToDbAndCache(song, rawLyrics, isSynced = true)
                return@withContext neteaseLyrics
            }

            // Ultimate Fallback: Return best available STATIC lyrics
            val bestLrcLibStatic = rankedLrcLib.firstOrNull()?.response
            if (bestLrcLibStatic != null) {
                val rawLyrics = bestLrcLibStatic.plainLyrics ?: bestLrcLibStatic.syncedLyrics
                if (!rawLyrics.isNullOrBlank()) {
                    val parsed = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                    if (parsed.isValid()) {
                        saveToDbAndCache(song, rawLyrics, isSynced = false)
                        return@withContext parsed
                    }
                }
            }

            if (mxLyrics != null) {
                val rawLyrics = lyricsToRawContent(mxLyrics) ?: ""
                saveToDbAndCache(song, rawLyrics, isSynced = false)
                return@withContext mxLyrics
            }

            if (neteaseLyrics != null) {
                val rawLyrics = lyricsToRawContent(neteaseLyrics) ?: ""
                saveToDbAndCache(song, rawLyrics, isSynced = false)
                return@withContext neteaseLyrics
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics fetch failed: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun fetchFromMusixmatch(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        val token = "2105191c62ef4f1412574e4f203003058b8d00344d15655a5b290df62706"
        val url = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&namespace=lyrics_richsynced&app_id=web-desktop-app-v1.0&usertoken=$token&q_track=${Uri.encode(title)}&q_artist=${Uri.encode(artist)}"
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        
        try {
            val responseBody = okHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val json = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
            val macroCalls = json.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.getAsJsonObject("macro_calls") ?: return@withContext null

            val subList = macroCalls.getAsJsonObject("track.subtitles.get")
                ?.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.getAsJsonArray("subtitle_list")

            if (subList != null && subList.size() > 0) {
                val subtitleBody = subList[0].asJsonObject
                    .getAsJsonObject("subtitle")
                    ?.get("subtitle_body")?.asString

                if (!subtitleBody.isNullOrBlank()) {
                    val parsed = LyricsUtils.parseLyrics(subtitleBody)
                    if (parsed.isValid()) return@withContext parsed.copy(areFromRemote = true)
                }
            }

            val plainBody = macroCalls.getAsJsonObject("track.lyrics.get")
                ?.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.getAsJsonObject("lyrics")
                ?.get("lyrics_body")?.asString

            if (!plainBody.isNullOrBlank()) {
                val parsed = LyricsUtils.parseLyrics(plainBody)
                if (parsed.isValid()) return@withContext parsed.copy(areFromRemote = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Musixmatch fetch failed: ${e.message}")
        }
        return@withContext null
    }

    private suspend fun fetchFromNetEaseSearch(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        val query = "$title $artist".trim()
        val searchUrl = "https://music.163.com/api/search/get/web?csrf_token=&hlpretag=&hlposttag=&s=${Uri.encode(query)}&type=1&offset=0&total=true&limit=3"
        val request = Request.Builder().url(searchUrl).header("User-Agent", "Mozilla/5.0").build()
        
        try {
            val responseBody = okHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val json = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
            val songs = json.getAsJsonObject("result")?.getAsJsonArray("songs") ?: return@withContext null
            if (songs.size() == 0) return@withContext null

            val songId = songs[0].asJsonObject.get("id")?.asLong ?: return@withContext null
            val lyricUrl = "https://music.163.com/api/song/lyric?id=$songId&lv=1&kv=1&tv=-1"
            val lyricReq = Request.Builder().url(lyricUrl).header("User-Agent", "Mozilla/5.0").build()
            
            val lyricJsonStr = okHttpClient.newCall(lyricReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val lyricJson = gson.fromJson(lyricJsonStr, com.google.gson.JsonObject::class.java)
            val lrcStr = lyricJson.getAsJsonObject("lrc")?.get("lyric")?.asString ?: return@withContext null

            if (lrcStr.isNotBlank()) {
                val parsed = LyricsUtils.parseLyrics(lrcStr)
                if (parsed.isValid()) return@withContext parsed.copy(areFromRemote = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "NetEase search fetch failed: ${e.message}")
        }
        return@withContext null
    }

    private suspend fun saveToDbAndCache(song: Song, rawLyrics: String, isSynced: Boolean) {
        try {
            lyricsDao.insert(
                com.unshoo.pixelmusic.data.database.LyricsEntity(
                    songId = song.id.toLong(),
                    content = rawLyrics,
                    isSynced = isSynced,
                    source = "remote"
                )
            )
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Skipping DB update for non-numeric ID: ${song.id}")
        }
    }

    private fun hasLyrics(response: LrcLibResponse): Boolean =
        !response.plainLyrics.isNullOrBlank() || !response.syncedLyrics.isNullOrBlank()

    private fun hasSyncedLyrics(response: LrcLibResponse): Boolean =
        !response.syncedLyrics.isNullOrBlank()

    private fun rankRemoteLyricsMatches(
        song: Song,
        responses: List<LrcLibResponse>,
        mode: RemoteLyricsMatchMode,
        primaryArtist: String
    ): List<RemoteLyricsMatch> {
        val songDurationSeconds = song.duration / 1000.0
        if (songDurationSeconds <= 0.0) return emptyList()

        return responses
            .mapNotNull { response ->
                val score = remoteLyricsMatchScore(
                    song = song,
                    response = response,
                    mode = mode,
                    songDurationSeconds = songDurationSeconds,
                    primaryArtist = primaryArtist
                ) ?: return@mapNotNull null
                RemoteLyricsMatch(response, score)
            }
            .sortedWith(
                compareByDescending<RemoteLyricsMatch> { hasSyncedLyrics(it.response) }
                    .thenByDescending { it.score }
                    .thenBy { abs(it.response.duration - songDurationSeconds) }
            )
    }

    private fun remoteLyricsMatchScore(
        song: Song,
        response: LrcLibResponse,
        mode: RemoteLyricsMatchMode,
        songDurationSeconds: Double,
        primaryArtist: String
    ): Int? {
        if (!hasLyrics(response) || response.duration <= 0.0) return null
        if (!variantDescriptorsCompatible(song, response)) return null

        val hasSynced = hasSyncedLyrics(response)
        val durationTolerance = remoteDurationToleranceSeconds(songDurationSeconds, hasSynced, mode)
        val durationDiff = abs(response.duration - songDurationSeconds)
        if (durationDiff > durationTolerance) return null

        val titleScore = titleMatchScore(song.title, response.name, mode) ?: return null
        val artistScore = artistMatchScore(song.displayArtist, response.artistName)
        if (!isUnknownArtist(song.displayArtist) && artistScore == null) return null

        val durationScore = (durationTolerance - durationDiff).coerceAtLeast(0.0).toInt()
        val syncedScore = if (hasSynced) 10 else 0
        return titleScore + (artistScore ?: 0) + durationScore + syncedScore
    }

    private fun remoteDurationToleranceSeconds(
        songDurationSeconds: Double,
        hasSyncedLyrics: Boolean,
        mode: RemoteLyricsMatchMode
    ): Double {
        return when (mode) {
            RemoteLyricsMatchMode.AUTOMATIC -> {
                if (hasSyncedLyrics) {
                    (songDurationSeconds * 0.02).coerceIn(5.0, 8.0)
                } else {
                    (songDurationSeconds * 0.04).coerceIn(8.0, 15.0)
                }
            }
            RemoteLyricsMatchMode.CANDIDATE -> 15.0
        }
    }

    private fun titleMatchScore(songTitle: String, responseTitle: String, mode: RemoteLyricsMatchMode): Int? {
        val songBase = baseTitleForMatching(songTitle)
        val responseBase = baseTitleForMatching(responseTitle)
        if (songBase.isBlank() || responseBase.isBlank()) return null

        if (songBase == responseBase) return 70

        val songTokens = matchTokens(songBase)
        val responseTokens = matchTokens(responseBase)
        if (songTokens.isEmpty() || responseTokens.isEmpty()) return null

        if (songTokens.size == 1 || responseTokens.size == 1) {
            return if (songTokens == responseTokens) 60 else null
        }

        if (containsWholePhrase(responseBase, songBase) || containsWholePhrase(songBase, responseBase)) {
            return if (mode == RemoteLyricsMatchMode.AUTOMATIC) 58 else 54
        }

        val overlap = songTokens.intersect(responseTokens).size
        val songCoverage = overlap.toDouble() / songTokens.size
        val responseCoverage = overlap.toDouble() / responseTokens.size
        val requiredSongCoverage = if (mode == RemoteLyricsMatchMode.AUTOMATIC) 0.85 else 0.75
        val requiredResponseCoverage = if (mode == RemoteLyricsMatchMode.AUTOMATIC) 0.70 else 0.55

        return if (songCoverage >= requiredSongCoverage && responseCoverage >= requiredResponseCoverage) {
            45
        } else {
            null
        }
    }

    private fun artistMatchScore(songArtist: String, responseArtist: String): Int? {
        if (isUnknownArtist(songArtist)) return 0

        val songBase = normalizeForMatch(songArtist)
        val responseBase = normalizeForMatch(responseArtist)
        if (songBase.isBlank() || responseBase.isBlank()) return null

        if (songBase == responseBase) return 30
        if (containsWholePhrase(responseBase, songBase) || containsWholePhrase(songBase, responseBase)) {
            return 22
        }

        val songTokens = artistTokens(songBase)
        val responseTokens = artistTokens(responseBase)
        if (songTokens.isEmpty() || responseTokens.isEmpty()) return null

        val overlap = songTokens.intersect(responseTokens).size
        val smallerArtistCoverage = overlap.toDouble() / minOf(songTokens.size, responseTokens.size)
        return if (smallerArtistCoverage >= 0.5) 12 else null
    }

    private fun variantDescriptorsCompatible(song: Song, response: LrcLibResponse): Boolean {
        val songVariants = timingVariantTokens(song.title) + timingVariantTokensFromFileName(song)
        val responseVariants = timingVariantTokens(response.name)

        if (songVariants.isEmpty()) {
            return responseVariants.isEmpty()
        }

        return responseVariants == songVariants
    }

    private fun baseTitleForMatching(title: String): String {
        var base = title.replace(Regex("""^\s*\d{1,3}\s*[\._-]\s+"""), "")

        base = BRACKETED_QUALIFIER_REGEX.replace(base) { match ->
            val qualifier = match.groupValues.getOrNull(1).orEmpty()
            if (shouldDropTitleQualifier(qualifier)) " " else " $qualifier "
        }

        var parts = TITLE_SEPARATOR_REGEX.split(base)
        while (parts.size > 1 && shouldDropTitleQualifier(parts.last())) {
            parts = parts.dropLast(1)
        }

        return normalizeForMatch(parts.joinToString(" "))
    }

    private fun shouldDropTitleQualifier(value: String): Boolean {
        val normalized = normalizeForMatch(value)
        if (normalized.isBlank()) return true
        return FEATURE_QUALIFIER_REGEX.containsMatchIn(value) ||
            timingVariantTokens(value).isNotEmpty() ||
            normalized in TITLE_DROP_QUALIFIERS
    }

    private fun timingVariantTokens(value: String): Set<String> {
        val normalized = normalizeForMatch(value)
        if (normalized.isBlank()) return emptySet()

        val tokens = matchTokens(normalized)
        val variants = tokens
            .filter { it in TIMING_VARIANT_KEYWORDS }
            .toMutableSet()

        if (Regex("""\bmash\s+up\b""").containsMatchIn(normalized)) {
            variants += "mashup"
        }
        if ("versus" in tokens || "vs" in tokens) {
            variants += "mashup"
        }

        return variants
    }

    private fun timingVariantTokensFromFileName(song: Song): Set<String> {
        val fileName = songFileName(song)
        if (fileName.isBlank()) return emptySet()

        val variants = BRACKETED_QUALIFIER_REGEX
            .findAll(fileName)
            .flatMap { match -> timingVariantTokens(match.groupValues.getOrNull(1).orEmpty()) }
            .toMutableSet()

        val titleBase = baseTitleForMatching(song.title)
        if (titleBase.isBlank()) return variants

        TITLE_SEPARATOR_REGEX.split(fileName).forEach { part ->
            val normalizedPart = normalizeForMatch(part)
            if (normalizedPart.startsWith("$titleBase ")) {
                variants += timingVariantTokens(normalizedPart.removePrefix(titleBase).trim())
            }
        }

        return variants
    }

    private fun songFileName(song: Song): String {
        if (song.path.isBlank()) return ""
        return runCatching { File(song.path).nameWithoutExtension }.getOrDefault("")
    }

    private fun artistTokens(normalizedArtist: String): Set<String> =
        matchTokens(normalizedArtist)
            .filterNot { it in ARTIST_CONNECTOR_TOKENS }
            .toSet()

    private fun matchTokens(normalizedValue: String): Set<String> =
        normalizedValue
            .split(' ')
            .filter { it.isNotBlank() }
            .toSet()

    private fun containsWholePhrase(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        return Regex("""(?:^|\s)${Regex.escape(needle)}(?:\s|$)""").containsMatchIn(haystack)
    }

    private fun normalizeForMatch(value: String): String {
        val withoutDiacritics = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")

        return withoutDiacritics
            .replace("&", " and ")
            .replace(Regex("""[\u2019'`]"""), "")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }

    private fun isUnknownArtist(value: String): Boolean =
        normalizeForMatch(value) in UNKNOWN_ARTISTS

    private fun isNeteaseSong(song: Song): Boolean =
        song.neteaseId != null || song.contentUriString.startsWith("netease://")

    private fun resolveNeteaseSongId(song: Song): Long? {
        song.neteaseId?.let { return it }
        if (!song.contentUriString.startsWith("netease://")) return null
        return Uri.parse(song.contentUriString).host?.toLongOrNull()
    }

    private suspend fun fetchFromAmlldb(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val neteaseSongId = resolveNeteaseSongId(song) ?: return@withContext null
        val request = Request.Builder()
            .url("$AMLLDB_NCM_LYRICS_BASE_URL$neteaseSongId")
            .get()
            .build()

        try {
            val ttml = withNetworkRetry(
                operationName = "amlldb_fetch:$neteaseSongId",
                shouldRetry = { throwable -> throwable is IOException }
            ) {
                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> response.body.string()
                        response.code.isRetryableHttpStatusCode() ->
                            throw IOException("AMLLDB HTTP ${response.code} for songId=$neteaseSongId")
                        else -> ""
                    }
                }
            }

            if (ttml.isBlank() || ttml.contains("歌词不存在")) return@withContext null
            val lrc = convertAmlTtmlToLrc(ttml) ?: return@withContext null
            val parsed = LyricsUtils.parseLyrics(lrc)
            if (!parsed.isValid()) return@withContext null
            return@withContext parsed.copy(areFromRemote = true)
        } catch (e: Exception) {
            Log.w(TAG, "AMLLDB fetch failed for $neteaseSongId: ${e.message}")
            return@withContext null
        }
    }

    private fun convertAmlTtmlToLrc(ttml: String): String? {
        val lineRegex = Regex(
            "<p\\b[^>]*\\bbegin=\"([^\"]+)\"[^>]*>(.*?)</p>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val spanRegex = Regex(
            "<span\\b([^>]*)>(.*?)</span>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val beginAttrRegex = Regex("\\bbegin=\"([^\"]+)\"")
        val roleAttrRegex = Regex("\\bttm:role=\"([^\"]+)\"")

        val lrcLines = mutableListOf<String>()
        lineRegex.findAll(ttml).forEach { lineMatch ->
            val lineStartMs = parseTtmlTimeToMs(lineMatch.groupValues[1]) ?: return@forEach
            var inner = lineMatch.groupValues[2]
            val markerRegex = Regex("§§TS\\(([^)]+)\\)§§")

            inner = spanRegex.replace(inner) { spanMatch ->
                val attributes = spanMatch.groupValues[1]
                val role = roleAttrRegex.find(attributes)?.groupValues?.getOrNull(1)?.lowercase()
                if (role == "x-roman") {
                    return@replace ""
                }
                val wordStartMs = beginAttrRegex
                    .find(attributes)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let(::parseTtmlTimeToMs)
                val text = decodeXmlEntities(spanMatch.groupValues[2])

                if (wordStartMs == null) {
                    return@replace text
                }

                "§§TS(${formatTimestamp(wordStartMs.toInt())})§§$text"
            }

            val withoutXmlTags = decodeXmlEntities(inner.replace(Regex("<[^>]+>"), ""))
            val lrcInlineTagged = markerRegex.replace(withoutXmlTags, "<$1>")
            if (lrcInlineTagged.isBlank()) return@forEach

            lrcLines += "[${formatTimestamp(lineStartMs.toInt())}]$lrcInlineTagged"
        }

        return lrcLines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun parseTtmlTimeToMs(value: String): Long? {
        val raw = value.trim()
        if (raw.isEmpty()) return null

        if (raw.endsWith("s")) {
            val seconds = raw.removeSuffix("s").toDoubleOrNull() ?: return null
            return (seconds * 1000.0).toLong()
        }

        val parts = raw.split(":")
        val secondsPart = parts.lastOrNull()?.toDoubleOrNull() ?: return null
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toLongOrNull() ?: return null
                val minutes = parts[1].toLongOrNull() ?: return null
                (hours * 3_600_000L) + (minutes * 60_000L) + (secondsPart * 1000.0).toLong()
            }
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: return null
                (minutes * 60_000L) + (secondsPart * 1000.0).toLong()
            }
            1 -> (secondsPart * 1000.0).toLong()
            else -> null
        }
    }

    private fun decodeXmlEntities(value: String): String =
        value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")

    private suspend fun findLocalLyricsFile(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        try {
            val songFile = File(song.path)
            val directory = songFile.parentFile ?: return@withContext null
            val songNameWithoutExt = songFile.nameWithoutExtension

            if (directory.exists()) {
                for (extension in LyricsImportSecurity.supportedFileExtensions()) {
                    val lyricsFile = File(directory, "$songNameWithoutExt.$extension")
                    if (!lyricsFile.exists() || !lyricsFile.canRead()) continue

                    val validated = readValidatedLocalLyrics(lyricsFile)
                    if (validated != null) {
                        return@withContext validated.parsedLyrics
                    }
                }

                val cleanArtist = song.displayArtist.replace(Regex("[^a-zA-Z0-9]"), "_")
                val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9]"), "_")

                for (extension in LyricsImportSecurity.supportedFileExtensions()) {
                    val alternativeLyricsFile = File(directory, "${cleanArtist}_${cleanTitle}.$extension")
                    if (!alternativeLyricsFile.exists() || !alternativeLyricsFile.canRead()) continue

                    val validated = readValidatedLocalLyrics(alternativeLyricsFile)
                    if (validated != null) {
                        return@withContext validated.parsedLyrics
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for local lyrics file", e)
        }
        return@withContext null
    }

    private fun readValidatedLocalLyrics(file: File): com.unshoo.pixelmusic.utils.ValidatedLyricsImport? {
        return when (val validation = LyricsImportSecurity.validateLocalLyricsFile(file)) {
            is LyricsImportValidationResult.Valid -> validation.value
            is LyricsImportValidationResult.Invalid -> null
        }
    }

    private fun saveLocalLyricsJson(song: Song, lyrics: Lyrics) {
        try {
            val fileName = "${song.id}.json"
            val lyricsDir = File(context.filesDir, "lyrics")
            lyricsDir.mkdirs()

            val wordByWordLyrics = lyrics.synced
                ?.takeIf { lines -> lines.any { !it.words.isNullOrEmpty() } }
                ?.let(::toWordByWordLrc)

            val lyricsData = LyricsData(
                plainLyrics = lyrics.plain?.joinToString("\n"),
                syncedLyrics = lyrics.synced?.joinToString("\n") { "[${formatTimestamp(it.time)}]${it.line}" },
                wordByWordLyrics = wordByWordLyrics
            )

            val file = File(lyricsDir, fileName)
            val json = gson.toJson(lyricsData)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving lyrics to JSON cache: ${e.message}", e)
        }
    }

    private suspend fun loadLocalLyricsJson(song: Song): Lyrics? {
        try {
            val data = readLyricsJsonCache(song) ?: return null
            if (data.hasLyrics()) {
                val rawLyrics = data.wordByWordLyrics ?: data.syncedLyrics ?: data.plainLyrics
                val parsed = LyricsUtils.parseLyrics(rawLyrics)
                if (parsed.isValid()) {
                    val hasWordTimestamps = parsed.synced?.any { !it.words.isNullOrEmpty() } == true
                    if (!hasWordTimestamps && data.wordByWordLyrics.isNullOrBlank()) {
                        val persistedContent = song.id.toLongOrNull()
                            ?.let { lyricsDao.getLyrics(it)?.content }
                            ?.takeIf { it.isNotBlank() }
                        if (persistedContent != null) {
                            val recovered = LyricsUtils.parseLyrics(persistedContent)
                            val recoveredHasWords = recovered.synced?.any { !it.words.isNullOrEmpty() } == true
                            if (recovered.isValid() && recoveredHasWords) {
                                saveLocalLyricsJson(song, recovered)
                                return recovered
                            }
                        }

                        if (looksLikeFlattenedWordByWordCache(parsed)) {
                            return null
                        }
                    }
                    return parsed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading JSON cache: ${e.message}", e)
        }
        return null
    }

    private fun formatTimestamp(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (timeMs % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    private fun toWordByWordLrc(lines: List<SyncedLine>): String {
        return lines.joinToString("\n") { line ->
            val linePrefix = "[${formatTimestamp(line.time)}]"
            val words = line.words
            if (words.isNullOrEmpty()) {
                linePrefix + line.line
            } else {
                val wordsPart = words.mapIndexed { index, word ->
                    val separator = if (index > 0 && word.startsNewWord) " " else ""
                    "$separator<${formatTimestamp(word.time)}>${word.word}"
                }.joinToString("")
                linePrefix + wordsPart
            }
        }
    }

    private fun looksLikeFlattenedWordByWordCache(lyrics: Lyrics): Boolean {
        val synced = lyrics.synced ?: return false
        var suspiciousLines = 0

        for (line in synced) {
            val text = line.line
            if (text.isBlank() || text.any { it.isWhitespace() }) continue

            val hasLongLatinRun = Regex("[A-Za-z]{10,}").containsMatchIn(text)
            if (hasLongLatinRun) {
                suspiciousLines += 1
                if (suspiciousLines >= 2) return true
            }
        }

        return false
    }

    private suspend fun loadEmbeddedLyricsFromMetadata(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        if (song.contentUriString.startsWith("telegram://") || song.contentUriString.isEmpty()) {
            return@withContext null
        }

        return@withContext try {
            val uri = song.contentUriString.toUri()
            val tempFile = createTempFileFromUri(uri) ?: return@withContext null

            try {
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    val metadata = TagLib.getMetadata(fd.detachFd())
                    val propertyMap = metadata?.propertyMap
                    val lyricsField = propertyMap?.get("LYRICS")?.firstOrNull()
                        ?: propertyMap?.get("UNSYNCEDLYRICS")?.firstOrNull()

                    if (!lyricsField.isNullOrBlank()) {
                        val parsedLyrics = LyricsUtils.parseLyrics(lyricsField)
                        if (parsedLyrics.isValid()) {
                            parsedLyrics.copy(areFromRemote = false)
                        } else null
                    } else null
                }
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            LogUtils.e(this@LyricsRepositoryImpl, e, "Error reading lyrics from file metadata")
            null
        }
    }

    private suspend fun loadStoredLyrics(
        song: Song,
        cacheKey: String,
        includeMemoryCache: Boolean
    ): Pair<Lyrics, String>? = withContext(Dispatchers.IO) {
        song.lyrics
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { rawLyrics ->
                parseStoredLyrics(rawLyrics)?.let { return@withContext it to rawLyrics }
            }

        song.id.toLongOrNull()
            ?.let { lyricsDao.getLyrics(it)?.content }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { rawLyrics ->
                parseStoredLyrics(rawLyrics)?.let { return@withContext it to rawLyrics }
            }

        readLyricsJsonCache(song)
            ?.takeIf { it.hasLyrics() }
            ?.let { data ->
                val rawLyrics = data.wordByWordLyrics ?: data.syncedLyrics ?: data.plainLyrics
                if (!rawLyrics.isNullOrBlank()) {
                    parseStoredLyrics(rawLyrics)?.let { return@withContext it to rawLyrics }
                }
            }

        if (includeMemoryCache) {
            lyricsCache.get(cacheKey)?.let { cached ->
                lyricsToRawContent(cached)?.let { rawLyrics ->
                    return@withContext cached to rawLyrics
                }
            }
        }

        null
    }

    private fun parseStoredLyrics(rawLyrics: String): Lyrics? {
        val parsedLyrics = LyricsUtils.parseLyrics(rawLyrics)
        return parsedLyrics
            .takeIf { it.isValid() }
            ?.copy(areFromRemote = false)
    }

    private fun lyricsToRawContent(lyrics: Lyrics): String? {
        val syncedLyrics = lyrics.synced
        if (!syncedLyrics.isNullOrEmpty()) {
            val hasWordTimestamps = syncedLyrics.any { !it.words.isNullOrEmpty() }
            return if (hasWordTimestamps) {
                toWordByWordLrc(syncedLyrics)
            } else {
                syncedLyrics.joinToString("\n") { line ->
                    "[${formatTimestamp(line.time)}]${line.line}"
                }
            }
        }

        return lyrics.plain
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
    }

    private fun readLyricsJsonCache(song: Song): LyricsData? {
        val fileName = "${song.id}.json"
        val file = File(context.filesDir, "lyrics/$fileName")
        if (!file.exists()) return null

        val json = file.readText()
        return gson.fromJson(json, LyricsData::class.java)
    }

    override suspend fun fetchFromRemote(song: Song): Result<Pair<Lyrics, String>> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(song.id)
            loadStoredLyrics(song, cacheKey, includeMemoryCache = true)?.let { stored ->
                lyricsCache.put(cacheKey, stored.first)
                return@withContext Result.success(stored)
            }

            val searchResult = searchRemote(song)
            if (searchResult.isSuccess) {
                val (_, results) = searchResult.getOrThrow()
                if (results.isNotEmpty()) {
                    val best = results.first()
                    val rawLyricsToSave = best.rawLyrics

                    try {
                        lyricsDao.insert(
                             com.unshoo.pixelmusic.data.database.LyricsEntity(
                                 songId = song.id.toLong(),
                                 content = rawLyricsToSave,
                                 isSynced = !best.lyrics.synced.isNullOrEmpty(),
                                 source = "remote"
                             )
                        )
                    } catch (e: NumberFormatException) {
                        Log.w(TAG, "Skipping DB update for non-numeric ID: ${song.id}")
                    }

                    lyricsCache.put(cacheKey, best.lyrics)
                    saveLocalLyricsJson(song, best.lyrics)

                    return@withContext Result.success(Pair(best.lyrics, rawLyricsToSave))
                }
            }

            Result.failure(NoLyricsFoundException())

        } catch (e: Exception) {
            LogUtils.e(this@LyricsRepositoryImpl, e, "Error fetching lyrics from remote")
            when {
                e is HttpException && e.code() == 404 -> Result.failure(NoLyricsFoundException())
                e is SocketTimeoutException -> Result.failure(LyricsException(context.getString(R.string.lyrics_fetch_timeout), e))
                e is UnknownHostException -> Result.failure(LyricsException(context.getString(R.string.lyrics_network_error), e))
                e is IOException -> Result.failure(LyricsException(context.getString(R.string.lyrics_network_error), e))
                e is HttpException -> Result.failure(LyricsException(context.getString(R.string.lyrics_server_error, e.code()), e))
                else -> Result.failure(LyricsException(context.getString(R.string.failed_to_fetch_lyrics_from_remote), e))
            }
        }
    }

    override suspend fun searchRemote(song: Song): Result<Pair<String, List<LyricsSearchResult>>> = withContext(Dispatchers.IO) {
        try {
            val rawTitle = song.title.trim()
            val rawArtist = song.displayArtist.trim()
            val sanitizedTitle = sanitizeTitleForLyrics(rawTitle)
            val primaryArtist = extractPrimaryArtist(rawArtist)
            val combinedQuery = "$sanitizedTitle $primaryArtist".trim()

            val strategies = buildList {
                add(RemoteSearchStrategy("sanitized_track+artist") {
                    lrcLibApiService.searchLyrics(trackName = sanitizedTitle, artistName = primaryArtist)
                })
                if (sanitizedTitle != rawTitle || primaryArtist != rawArtist) {
                    add(RemoteSearchStrategy("raw_track+artist") {
                        lrcLibApiService.searchLyrics(trackName = rawTitle, artistName = rawArtist)
                    })
                }
                add(RemoteSearchStrategy("sanitized_track_only") {
                    lrcLibApiService.searchLyrics(trackName = sanitizedTitle)
                })
                add(RemoteSearchStrategy("sanitized_query") {
                    lrcLibApiService.searchLyrics(query = combinedQuery)
                })
            }

            val uniqueResults = runSearchStrategiesFast(strategies)

            if (uniqueResults.isNotEmpty()) {
                val rankedMatches = rankRemoteLyricsMatches(
                    song = song,
                    responses = uniqueResults,
                    mode = RemoteLyricsMatchMode.CANDIDATE,
                    primaryArtist = primaryArtist
                )
                val results = rankedMatches.mapNotNull { match ->
                    val response = match.response
                    val rawLyrics = response.syncedLyrics ?: response.plainLyrics ?: return@mapNotNull null
                    val parsedLyrics = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                    if (!parsedLyrics.isValid()) return@mapNotNull null

                    LyricsSearchResult(response, parsedLyrics, rawLyrics)
                }.sortedWith(
                    compareByDescending<LyricsSearchResult> { !it.record.syncedLyrics.isNullOrEmpty() }
                )

                if (results.isNotEmpty()) {
                    Result.success(Pair(combinedQuery, results))
                } else {
                    Result.failure(NoLyricsFoundException(combinedQuery))
                }
            } else {
                Result.failure(NoLyricsFoundException(combinedQuery))
            }
        } catch (e: Exception) {
            LogUtils.e(this@LyricsRepositoryImpl, e, "Error searching remote for lyrics")
            Result.failure(LyricsException(context.getString(R.string.failed_to_search_for_lyrics), e))
        }
    }

    override suspend fun searchRemoteByQuery(title: String, artist: String?): Result<Pair<String, List<LyricsSearchResult>>> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.trim()
            val cleanArtist = artist?.trim()?.takeIf { it.isNotBlank() }
            val query = listOfNotNull(cleanTitle.takeIf { it.isNotBlank() }, cleanArtist).joinToString(" ")

            val strategies = buildList {
                add(RemoteSearchStrategy("manual_query") { lrcLibApiService.searchLyrics(query = query) })
                if (!cleanArtist.isNullOrBlank()) {
                    add(RemoteSearchStrategy("manual_track+artist") {
                        lrcLibApiService.searchLyrics(trackName = cleanTitle, artistName = cleanArtist)
                    })
                }
            }

            val responses = runSearchStrategiesFast(strategies)
            if (responses.isEmpty()) return@withContext Result.failure(NoLyricsFoundException(query))

            val results = responses.mapNotNull { response ->
                val rawLyrics = response.syncedLyrics ?: response.plainLyrics ?: return@mapNotNull null
                val parsed = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                if (!parsed.isValid()) return@mapNotNull null

                LyricsSearchResult(response, parsed, rawLyrics)
            }.sortedWith(
                compareByDescending<LyricsSearchResult> { !it.record.syncedLyrics.isNullOrEmpty() }
            )

            if (results.isEmpty()) {
                Result.failure(NoLyricsFoundException(query))
            } else {
                Result.success(Pair(query, results))
            }
        } catch (e: Exception) {
            Result.failure(LyricsException(context.getString(R.string.failed_to_search_for_lyrics), e))
        }
    }

    override suspend fun updateLyrics(songId: Long, lyricsContent: String): Unit = withContext(Dispatchers.IO) {
        val parsedLyrics = LyricsUtils.parseLyrics(lyricsContent)
        if (!parsedLyrics.isValid()) return@withContext

        lyricsDao.insert(
             com.unshoo.pixelmusic.data.database.LyricsEntity(
                 songId = songId,
                 content = lyricsContent,
                 isSynced = parsedLyrics.synced?.isNotEmpty() == true,
                 source = "manual"
             )
        )

        val cacheKey = generateCacheKey(songId.toString())
        lyricsCache.put(cacheKey, parsedLyrics)
    }

    override suspend fun resetLyrics(songId: Long): Unit = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(songId.toString())
        lyricsCache.remove(cacheKey)
        try {
            lyricsDao.deleteLyrics(songId)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing lyrics from DB for ID: $songId", e)
        }
        
        try {
            val file = File(context.filesDir, "lyrics/${songId}.json")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting JSON cache: ${e.message}")
        }
    }

    override suspend fun resetAllLyrics(): Unit = withContext(Dispatchers.IO) {
        lyricsCache.evictAll()
        lyricsDao.deleteAll()
        
        try {
            val lyricsDir = File(context.filesDir, "lyrics")
            if (lyricsDir.exists()) {
                lyricsDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing JSON cache: ${e.message}")
        }
    }

    override suspend fun scanAndAssignLocalLrcFiles(
        songs: List<Song>,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val updatedCount = AtomicInteger(0)
        val processedCount = AtomicInteger(0)
        val total = songs.size

        val idsWithPersistedLyrics = songs
            .mapNotNull { it.id.toLongOrNull() }
            .chunked(900)
            .flatMap { chunk -> lyricsDao.getSongIdsWithLyrics(chunk) }
            .toHashSet()

        val songsToScan = songs.filter { song ->
            val songId = song.id.toLongOrNull()
            song.lyrics.isNullOrBlank() && (songId == null || songId !in idsWithPersistedLyrics)
        }
        val skippedCount = total - songsToScan.size
        processedCount.addAndGet(skippedCount)
        
        onProgress(processedCount.get(), total)
        
        if (songsToScan.isEmpty()) return@withContext 0

        val semaphore = Semaphore(8)

        coroutineScope {
            songsToScan.map { song ->
                async {
                    semaphore.withPermit {
                        try {
                            val songFile = File(song.path)
                            val directory = songFile.parentFile
                            
                            if (directory != null && directory.exists()) {
                                var foundFile: File? = null
                                
                                for (extension in LyricsImportSecurity.supportedFileExtensions()) {
                                    val exactMatch = File(directory, "${songFile.nameWithoutExtension}.$extension")
                                    if (exactMatch.exists() && exactMatch.canRead()) {
                                        foundFile = exactMatch
                                        break
                                    }
                                }
                                
                                if (foundFile == null) {
                                    val cleanArtist = song.displayArtist.replace(Regex("[^a-zA-Z0-9]"), "_")
                                    val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9]"), "_")
                                    for (extension in LyricsImportSecurity.supportedFileExtensions()) {
                                        val altMatch = File(directory, "${cleanArtist}_${cleanTitle}.$extension")
                                        if (altMatch.exists() && altMatch.canRead()) {
                                            foundFile = altMatch
                                            break
                                        }
                                    }
                                }
                                
                                if (foundFile != null) {
                                    val validated = readValidatedLocalLyrics(foundFile)
                                    if (validated != null) {
                                        try {
                                            lyricsDao.insert(
                                                 com.unshoo.pixelmusic.data.database.LyricsEntity(
                                                     songId = song.id.toLong(),
                                                     content = validated.sanitizedContent,
                                                     isSynced = validated.parsedLyrics.synced?.isNotEmpty() == true,
                                                     source = "local_file"
                                                 )
                                            )
                                            updatedCount.incrementAndGet()
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Skipping DB update for ID in scanner: ${song.id}", e)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error scanning lyrics for ${song.title}: ${e.message}")
                        }
                        
                        val current = processedCount.incrementAndGet()
                        if (current % 20 == 0 || current == total) {
                            onProgress(current, total)
                        }
                    }
                }
            }.awaitAll()
        }
        
        return@withContext updatedCount.get()
    }

    override fun clearCache() {
        lyricsCache.evictAll()
    }

    private fun generateCacheKey(songId: String): String = songId

    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) cursor.getString(nameIndex) else "temp_audio"
                    } else "temp_audio"
                } ?: "temp_audio"

                val tempFile = File.createTempFile("lyrics_", "_$fileName", context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                tempFile
            }
        } catch (e: Exception) {
            LogUtils.e(this, e, "Error creating temp file from URI")
            null
        }
    }

    private fun sanitizeTitleForLyrics(title: String): String {
        return title
            .replace(Regex("(?i)\\((ft\\.|feat\\.|from|version|reprise|official|video|soundtrack|ost|remastered|live|remix|audio).*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("(?i)\\s*-\\s*(remastered|live|reprise|version|official).*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractPrimaryArtist(artist: String?): String {
        if (artist.isNullOrBlank()) return ""
        return artist
            .split(Regex("[,&\\/]|\\s+feat\\.\\s+|\\s+ft\\.\\s+"), limit = 2)
            .firstOrNull()
            ?.trim() ?: artist
    }
}

data class LyricsSearchResult(val record: LrcLibResponse, val lyrics: Lyrics, val rawLyrics: String)

data class NoLyricsFoundException(val query: String? = null) : Exception()

class LyricsException(message: String, cause: Throwable? = null) : Exception(message, cause)
