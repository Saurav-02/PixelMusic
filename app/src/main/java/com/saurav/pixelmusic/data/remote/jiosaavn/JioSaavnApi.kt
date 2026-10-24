package com.saurav.pixelmusic.data.remote.jiosaavn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Minimal client for JioSaavn's unofficial web API.
 * Used ONLY as a fallback stream source when YouTube extraction fails.
 * All calls are best-effort: any failure returns empty, never throws.
 */
object JioSaavnApi {

    private const val BASE_URL = "https://www.jiosaavn.com/api.php"
    // Browser UA is mandatory: JioSaavn 403s requests without one.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    private data class SearchResponse(
        val results: List<SaavnSongRaw> = emptyList()
    )

    @Serializable
    private data class SaavnSongRaw(
        val id: String = "",
        val title: String = "",
        val subtitle: String = "",
        @SerialName("more_info") val moreInfo: SaavnMoreInfo? = null
    )

    @Serializable
    private data class SaavnMoreInfo(
        @SerialName("encrypted_media_url") val encryptedMediaUrl: String = "",
        val duration: String = ""
    )

    data class SaavnTrack(
        val id: String,
        val title: String,
        val artists: String,
        val durationSec: Long,
        val encryptedMediaUrl: String
    )

    /**
     * Searches JioSaavn for songs. Best-effort: returns empty on any failure.
     * Retries once on transport failure (their edge 403s intermittently).
     */
    suspend fun searchSongs(query: String, limit: Int = 10): List<SaavnTrack> {
        if (query.isBlank()) return emptyList()
        repeat(2) { attempt ->
            try {
                val result = searchOnce(query, limit)
                // Return non-empty immediately; on empty first attempt, retry once.
                if (result.isNotEmpty() || attempt == 1) return result
            } catch (_: Exception) {
                if (attempt == 1) return emptyList()
            }
        }
        return emptyList()
    }

    private suspend fun searchOnce(query: String, limit: Int): List<SaavnTrack> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL?__call=search.getResults&_format=json&api_version=4" +
                "&ctx=web6dot0&n=$limit&p=1&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body.string()
                val parsed = json.decodeFromString<SearchResponse>(body)
                parsed.results.mapNotNull { raw ->
                    val info = raw.moreInfo ?: return@mapNotNull null
                    if (info.encryptedMediaUrl.isBlank()) return@mapNotNull null
                    SaavnTrack(
                        id = raw.id,
                        title = htmlUnescape(raw.title),
                        artists = htmlUnescape(raw.subtitle),
                        durationSec = info.duration.toLongOrNull() ?: 0L,
                        encryptedMediaUrl = info.encryptedMediaUrl
                    )
                }
            }
        }

    /** Minimal HTML entity decoding (JioSaavn escapes some chars in names). */
    private fun htmlUnescape(s: String): String =
        s.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
