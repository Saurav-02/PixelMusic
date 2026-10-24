package com.saurav.pixelmusic.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import androidx.core.net.toUri
import com.google.gson.Gson
import com.kyant.taglib.TagLib
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.data.model.LyricsSourcePreference
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.remote.lyrics_providers.SmartLyricsMatcher
import com.saurav.pixelmusic.data.remote.lyrics_providers.ScoredHit
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.FilenameParser
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.LocalTrack
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.MatchTier
import com.saurav.pixelmusic.utils.LyricsImportSecurity
import com.saurav.pixelmusic.utils.LyricsImportValidationResult
import com.saurav.pixelmusic.utils.LogUtils
import com.saurav.pixelmusic.utils.LyricsUtils
import com.saurav.pixelmusic.utils.NetworkRetryUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request

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

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsDao: com.saurav.pixelmusic.data.database.LyricsDao,
    private val okHttpClient: OkHttpClient,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val aiOrchestrator: javax.inject.Provider<com.saurav.pixelmusic.data.ai.AiOrchestrator>? = null
) : LyricsRepository {

    private suspend fun aiLyricsRescue(song: Song): List<LyricsSearchResult> {
        val orchestrator = aiOrchestrator?.get() ?: return emptyList()
        val prefs = userPreferencesRepository ?: return emptyList()
        val isEnabled = runCatching { prefs.isAiLyricsRescueEnabledFlow.first() }.getOrDefault(false)
        if (!isEnabled) return emptyList()

        return try {
            val prompt = """
                Extract clean track details for lyrics search:
                Title: "${song.title}"
                Artist: "${song.displayArtist}"
                Album: "${song.album}"
            """.trimIndent()

            val response = orchestrator.generateContent(
                prompt = prompt,
                type = com.saurav.pixelmusic.data.ai.AiSystemPromptType.LYRICS_RESCUE
            )
            if (response.isBlank()) return emptyList()

            val cleanJson = response.replace("```json", "").replace("```", "").trim()
            val jsonObj = org.json.JSONObject(cleanJson)
            val cleanTitle = jsonObj.optString("cleanTitle").takeIf { it.isNotBlank() } ?: song.title
            val cleanArtist = jsonObj.optString("cleanArtist").takeIf { it.isNotBlank() }
            val searchQueries = jsonObj.optJSONArray("searchQueries")

            val aiCandidates = mutableListOf<com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.QueryCandidate>()
            aiCandidates.add(com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.QueryCandidate(cleanTitle, cleanArtist, com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.MatchStrategy.TAGS))

            if (searchQueries != null) {
                for (i in 0 until searchQueries.length()) {
                    val q = searchQueries.getString(i).trim()
                    if (q.isNotBlank()) {
                        aiCandidates.add(com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.QueryCandidate(q, null, com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching.MatchStrategy.FILENAME_TITLE_ONLY))
                    }
                }
            }

            val localTrack = LocalTrack(
                title = cleanTitle,
                artist = cleanArtist,
                durationSec = if (song.duration > 0) song.duration / 1000.0 else null,
                album = null
            )
            val hits = smartLyricsMatcher.search(localTrack, aiCandidates)

            coroutineScope {
                hits.map { hit ->
                    async {
                        val rawLyrics = smartLyricsMatcher.fetchLyrics(hit) ?: return@async null
                        val parsedLyrics = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                        if (!parsedLyrics.isValid()) return@async null

                        LyricsSearchResult(hit, parsedLyrics, rawLyrics)
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI lyrics rescue error: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "LyricsRepository"
        private const val MAX_LYRICS_CACHE_SIZE = 150
        private const val AMLLDB_NCM_LYRICS_BASE_URL = "https://amlldb.bikonoo.com/lyrics/ncm-lyrics/"
        private const val NETWORK_RETRY_ATTEMPTS = 3
        private const val NETWORK_RETRY_INITIAL_DELAY_MS = 500L
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lyricsCache = LruCache<String, Lyrics>(MAX_LYRICS_CACHE_SIZE)
    private val gson = Gson()
    
    // Initialize the new matching engine!
    private val smartLyricsMatcher = SmartLyricsMatcher(okHttpClient)

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
                return@withContext cached
            }
        }

        var fallbackStoredLyrics: Lyrics? = null
        if (!forceRefresh) {
            loadStoredLyrics(song, cacheKey, includeMemoryCache = false)?.let { stored ->
                if (!stored.first.synced.isNullOrEmpty()) {
                    lyricsCache.put(cacheKey, stored.first)
                    return@withContext stored.first
                } else {
                    fallbackStoredLyrics = stored.first
                }
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

        return@withContext fallbackStoredLyrics
    }

    override suspend fun getStoredLyrics(song: Song): Pair<Lyrics, String>? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(song.id)
        loadStoredLyrics(song, cacheKey, includeMemoryCache = true)?.also { stored ->
            lyricsCache.put(cacheKey, stored.first)
        }
    }

    private suspend fun fetchLyricsFromAPI(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        // Keep existing NCM/AMLLDB pre-fetch for specific NetEase URI songs
        if (isNeteaseSong(song)) {
            val amlLyrics = fetchFromAmlldb(song)
            if (amlLyrics != null) return@withContext amlLyrics
        }

        val cachedJson = loadLocalLyricsJson(song)
        if (cachedJson != null && !cachedJson.synced.isNullOrEmpty()) return@withContext cachedJson

        try {
            val localTrack = LocalTrack(
                title = song.title,
                artist = song.displayArtist,
                durationSec = if (song.duration > 0) song.duration / 1000.0 else null,
                album = null 
            )
            
            val candidates = FilenameParser.candidates(song.title, song.displayArtist, song.path)
            
            // The magic happens here! Concurrently search all 7 providers
            val hits = smartLyricsMatcher.search(localTrack, candidates)
            val eligibleHits = hits.filter { it.tier == MatchTier.AUTO_ACCEPT || it.tier == MatchTier.REVIEW }
            
            var fallbackPlainResult: Lyrics? = null
            for (hit in eligibleHits) {
                val rawLyrics = smartLyricsMatcher.fetchLyrics(hit)
                if (!rawLyrics.isNullOrBlank()) {
                    val parsed = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                    if (parsed.isValid()) {
                        if (parsed.synced?.isNotEmpty() == true) {
                            saveToDbAndCache(song, rawLyrics, isSynced = true)
                            return@withContext parsed
                        } else if (fallbackPlainResult == null) {
                            fallbackPlainResult = parsed
                        }
                    }
                }
            }
            if (fallbackPlainResult != null) {
                saveToDbAndCache(song, lyricsToRawContent(fallbackPlainResult) ?: "", isSynced = false)
                return@withContext fallbackPlainResult
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics fetch failed: ${e.message}", e)
            return@withContext null
        }
    }

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

    private suspend fun saveToDbAndCache(song: Song, rawLyrics: String, isSynced: Boolean) {
    val dbId = resolveLyricsDbId(song.id) ?: run {
        Log.w(TAG, "Cannot resolve DB id for songId=${song.id}, skipping lyrics DB write")
        return
    }
    try {
        lyricsDao.insert(
            com.saurav.pixelmusic.data.database.LyricsEntity(
                songId = dbId,
                content = rawLyrics,
                isSynced = isSynced,
                source = "remote"
            )
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to save lyrics to DB for songId=${song.id}: ${e.message}")
    }
    }

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

    private fun readValidatedLocalLyrics(file: File): com.saurav.pixelmusic.utils.ValidatedLyricsImport? {
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

        resolveLyricsDbId(song.id)
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
                if (!stored.first.synced.isNullOrEmpty()) {
                    lyricsCache.put(cacheKey, stored.first)
                    return@withContext Result.success(stored)
                }
            }

            val searchResult = searchRemote(song)
            if (searchResult.isSuccess) {
                val (_, results) = searchResult.getOrThrow()
                if (results.isNotEmpty()) {
                    val best = results.firstOrNull { it.lyrics.synced?.isNotEmpty() == true } ?: results.first()
                    val rawLyricsToSave = best.rawLyrics

                    try {
                        lyricsDao.insert(
                             com.saurav.pixelmusic.data.database.LyricsEntity(
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
            val localTrack = LocalTrack(
                title = song.title,
                artist = song.displayArtist,
                durationSec = if (song.duration > 0) song.duration / 1000.0 else null,
                album = null
            )
            val candidates = FilenameParser.candidates(song.title, song.displayArtist, song.path)
            
            val hits = smartLyricsMatcher.search(localTrack, candidates)
            
            var results = coroutineScope {
                hits.map { hit ->
                    async {
                        val rawLyrics = smartLyricsMatcher.fetchLyrics(hit) ?: return@async null
                        val parsedLyrics = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                        if (!parsedLyrics.isValid()) return@async null

                        LyricsSearchResult(hit, parsedLyrics, rawLyrics)
                    }
                }.awaitAll().filterNotNull()
            }

            if (results.isEmpty()) {
                val aiResults = aiLyricsRescue(song)
                if (aiResults.isNotEmpty()) {
                    results = aiResults
                }
            }

            val query = "${song.title} ${song.displayArtist}".trim()
            if (results.isNotEmpty()) {
                Result.success(Pair(query, results))
            } else {
                Result.failure(NoLyricsFoundException(query))
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

            val localTrack = LocalTrack(title = cleanTitle, artist = cleanArtist, durationSec = null, album = null)
            val candidates = FilenameParser.candidates(cleanTitle, cleanArtist, null)

            val hits = smartLyricsMatcher.search(localTrack, candidates)

            var results = coroutineScope {
                hits.map { hit ->
                    async {
                        val rawLyrics = smartLyricsMatcher.fetchLyrics(hit) ?: return@async null
                        val parsed = LyricsUtils.parseLyrics(rawLyrics).copy(areFromRemote = true)
                        if (!parsed.isValid()) return@async null

                        LyricsSearchResult(hit, parsed, rawLyrics)
                    }
                }.awaitAll().filterNotNull()
            }

            if (results.isEmpty()) {
                val dummySong = Song.emptySong().copy(title = cleanTitle, artist = cleanArtist.orEmpty())
                val aiResults = aiLyricsRescue(dummySong)
                if (aiResults.isNotEmpty()) {
                    results = aiResults
                }
            }

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
             com.saurav.pixelmusic.data.database.LyricsEntity(
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
                                                 com.saurav.pixelmusic.data.database.LyricsEntity(
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

/**
 * Maps any songId (numeric local id, "youtube_XXX", or "youtube://XXX")
 * to the SAME negative Long that PlayerViewModel.saveYoutubeSongsToDb uses
 * as the DB key for YouTube songs. This must match exactly so that lyrics
 * written here are found by PlayerViewModel and AutoQueueManager.
 */
private fun resolveLyricsDbId(songId: String): Long? {
    return when {
        songId.startsWith("youtube_") -> {
            val videoId = songId.substringAfter("youtube_")
            val hash = videoId.hashCode().toLong()
            -(15_000_000_000_000L + if (hash < 0) -hash else hash)
        }
        songId.startsWith("youtube://") -> {
            val videoId = songId.substringAfter("youtube://")
            val hash = videoId.hashCode().toLong()
            -(15_000_000_000_000L + if (hash < 0) -hash else hash)
        }
        else -> songId.toLongOrNull()
    }
}

    private fun createTempFileFromUri(uri: Uri): File? {
        // THE FIX: Skip virtual/remote URIs to prevent ContentResolver crashes
        val scheme = uri.scheme?.lowercase()
        if (scheme == "youtube" || scheme == "http" || scheme == "https") return null

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
}

// NOTE: This data class was updated! It now uses `ScoredHit` instead of `LrcLibResponse`.
data class LyricsSearchResult(val hit: ScoredHit, val lyrics: Lyrics, val rawLyrics: String)

data class NoLyricsFoundException(val query: String? = null) : Exception()

class LyricsException(message: String, cause: Throwable? = null) : Exception(message, cause)
