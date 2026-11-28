package com.saurav.pixelmusic.lyrics.providers.paxsenix

import android.content.Context
import com.saurav.pixelmusic.lyrics.providers.betterlyrics.TTMLParser
import com.saurav.pixelmusic.lyrics.providers.paxsenix.models.LyricsResponse
import com.saurav.pixelmusic.lyrics.providers.paxsenix.models.SearchResponse
import com.saurav.pixelmusic.lyrics.providers.paxsenix.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs

object Paxsenix {
    @Volatile
    private var client: HttpClient? = null
    private var appVersion: String = "1.0"

    fun init(context: Context) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }
            client = HttpClient(OkHttp) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 5_000
                    connectTimeoutMillis = 4_000
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }
                defaultRequest {
                    url("https://lyrics.paxsenix.org")
                    header("User-Agent", "PixelMusic/$appVersion")
                }
                expectSuccess = true
            }
        }
    }

    private val httpClient: HttpClient
        get() = client ?: HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 4_000
            }
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
            defaultRequest {
                url("https://lyrics.paxsenix.org")
                header("User-Agent", "PixelMusic/$appVersion")
            }
            expectSuccess = true
        }.also { client = it }

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun search(query: String): List<SearchResult> = runCatching {
        httpClient.get("/apple-music/search") {
            parameter("q", query)
        }.body<SearchResponse>()
    }.getOrDefault(emptyList())

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val searchQueries = buildList {
            add("$cleanedTitle $cleanedArtist")
            add(cleanedTitle)
            if (!album.isNullOrBlank()) {
                add("$cleanedTitle $cleanedArtist $album")
            }
        }

        var allResults: List<Pair<SearchResult, Double>> = emptyList()
        for (query in searchQueries) {
            if (allResults.isEmpty()) {
                val searchResults = search(query)
                if (searchResults.isNotEmpty()) {
                    allResults = scoreAndFilterResults(searchResults, title, artist, duration)
                }
            }
        }

        if (allResults.isEmpty()) {
            throw IllegalStateException("No tracks found on Paxsenix")
        }

        val candidates = allResults.take(5)
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = candidates.map { (result, _) ->
            scope.async {
                result to runCatching { fetchLyricsForTrackWithType(result.id) }.getOrDefault("" to false)
            }
        }

        var plainFallback: String? = null
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val (_, lrcPair) = select {
                    remaining.forEach { deferred -> deferred.onAwait { it } }
                }
                remaining.removeAll { it.isCompleted }
                val (lrc, hasWordTimings) = lrcPair
                if (lrc.isNotEmpty()) {
                    if (hasWordTimings) {
                        return Result.success(lrc)
                    } else if (plainFallback == null) {
                        plainFallback = lrc
                    }
                }
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }

        plainFallback?.let { return Result.success(it) }
        return Result.failure(IllegalStateException("No lyrics available from Paxsenix"))
    }

    private fun scoreAndFilterResults(
        results: List<SearchResult>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<SearchResult, Double>> {
        val durationMs = duration * 1000
        val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")
        val cleanedTitle = title.replace(cleanupRegex, "").lowercase().trim()
        val cleanedArtist = cleanArtist(artist).lowercase()
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)

        return results.map { result ->
            var score = 0.0
            result.duration?.let { d ->
                val diff = abs(d - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }
            val resultTitleCleaned = result.displayName.replace(cleanupRegex, "").lowercase().trim()
            when {
                resultTitleCleaned == cleanedTitle -> score += 80
                resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) -> score += 40
            }
            if (result.displayName.contains("mixed", ignoreCase = true) && !targetIsMixed) score -= 60
            if (result.displayName.contains("remix", ignoreCase = true) && !targetIsRemix) score -= 40
            val resultArtistLower = result.displayArtist.lowercase()
            when {
                resultArtistLower.contains(cleanedArtist) -> score += 50
                else -> {
                    val artistWords = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }
                    if (artistWords.any { resultArtistLower.contains(it) }) score += 25
                }
            }
            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(10)
    }

    private suspend fun fetchLyricsForTrackWithType(id: String): Pair<String, Boolean> {
        val result = fetchLyricsForTrack(id)
        if (result.isSuccess) {
            val lrc = result.getOrNull()!!
            val hasWordTimings = lrc.contains("<") && lrc.contains(">")
            return lrc to hasWordTimings
        }
        return "" to false
    }

    private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
        val response = httpClient.get("/apple-music/lyrics") {
            parameter("id", id)
        }.body<LyricsResponse>()

        if (!response.ttmlContent.isNullOrBlank()) {
            val lrc = convertTTMLToAppFormat(response.ttmlContent)
            if (lrc.isNotEmpty()) return@runCatching lrc
        }
        if (!response.elrcMultiPerson.isNullOrBlank()) return@runCatching response.elrcMultiPerson
        if (!response.elrc.isNullOrBlank()) return@runCatching response.elrc
        if (!response.plain.isNullOrBlank()) return@runCatching response.plain
        if (response.content.isEmpty()) throw IllegalStateException("No lyrics found")

        val hasWordLevel = response.type == "Syllable"
        if (!hasWordLevel) {
            val timed = response.content.any { it.timestamp > 0 }
            val text = response.content
                .mapNotNull { line ->
                    val lineText = line.text.joinedLineText()
                    if (lineText.isBlank()) return@mapNotNull null
                    if (!timed) return@mapNotNull lineText
                    val timeMs = line.timestamp
                    String.format(
                        Locale.US,
                        "[%02d:%02d.%02d]%s",
                        timeMs / 1000 / 60,
                        (timeMs / 1000) % 60,
                        (timeMs % 1000) / 10,
                        lineText,
                    )
                }
                .joinToString("\n")
            return@runCatching text
        }

        val lrc = buildString {
            response.content.forEach { line ->
                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10
                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }
                val lineText = line.text.joinedLineText()
                if (lineText.isNotBlank()) {
                    appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", minutes, seconds, centiseconds, agent, lineText))
                    if (line.text.isNotEmpty()) {
                        val wordsData = line.text.joinToString("|") { word ->
                            "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
                        }
                        if (wordsData.isNotEmpty()) appendLine("<$wordsData>")
                    }
                }
            }
        }
        return@runCatching lrc
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val searchQueries = listOf("$cleanedTitle $cleanedArtist", cleanedTitle)
        var plainFallback: String? = null
        var scoredResults: List<Pair<SearchResult, Double>> = emptyList()

        for (query in searchQueries) {
            val results = search(query)
            if (results.isEmpty()) continue
            val filtered = scoreAndFilterResults(results, title, artist, duration)
            if (filtered.isNotEmpty()) {
                scoredResults = filtered
                break
            }
        }

        val candidates = scoredResults.take(3)
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = candidates.map { (result, _) ->
            scope.async {
                runCatching { fetchLyricsForTrackWithType(result.id) }.getOrDefault("" to false)
            }
        }
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val (lrc, hasWordTimings) = select {
                    remaining.forEach { deferred -> deferred.onAwait { it } }
                }
                remaining.removeAll { it.isCompleted }
                if (lrc.isNotEmpty()) {
                    if (hasWordTimings) {
                        callback(lrc)
                        return
                    } else if (plainFallback == null) {
                        plainFallback = lrc
                    }
                }
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }

        plainFallback?.let { callback(it) }
    }

    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val parsedLines = TTMLParser.parseTTML(ttml)
            TTMLParser.toLRC(parsedLines)
        } catch (e: Exception) {
            Timber.e(e, "TTML conversion failed: ${e.message}")
            ""
        }
    }
}

private fun List<com.saurav.pixelmusic.lyrics.providers.paxsenix.models.LyricText>.joinedLineText(): String {
    if (isEmpty()) return ""
    val spacedByApi = dropLast(1).any { it.text.lastOrNull()?.isWhitespace() == true }
    return buildString {
        this@joinedLineText.forEachIndexed { index, piece ->
            append(piece.text)
            val last = index == this@joinedLineText.lastIndex
            if (!spacedByApi && !last && !piece.part && piece.text.lastOrNull()?.isWhitespace() != true) {
                append(' ')
            }
        }
    }.replace(Regex("\\s+"), " ").trim()
}
