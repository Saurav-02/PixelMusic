package com.saurav.pixelmusic.data.remote.lyrics_providers.apple

import com.google.gson.Gson
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.EmptyQueryException
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.toLrcTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Apple Music JSON Models ---
data class AppleMusicSearchResponse(
    val results: AppleMusicResults?,
    val resources: AppleMusicResources?
)
data class AppleMusicResults(val songs: AppleMusicSongDataList?)
data class AppleMusicSongDataList(val data: List<AppleMusicSongItem>?)
data class AppleMusicSongItem(val id: String)
data class AppleMusicResources(val songs: Map<String, AppleMusicSongDetail>?)
data class AppleMusicSongDetail(val attributes: AppleMusicAttributes)
data class AppleMusicAttributes(
    val name: String,
    val artistName: String,
    val url: String,
    val artwork: AppleMusicArtwork
)
data class AppleMusicArtwork(val url: String)

class AppleAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val lyricsBaseURL = "https://lyrics.paxsenix.org/"
    private val apiBaseURL = "https://amp-api.music.apple.com/v1/catalog/us"
    private val tokenManager = AppleTokenManager(client)

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo? = withContext(Dispatchers.IO) {
        val search = URLEncoder.encode("${query.songName} ${query.artistName}".trim(), StandardCharsets.UTF_8.toString())
        if (search.isBlank() || search == "+") throw EmptyQueryException()

        return@withContext try {
            val token = tokenManager.getToken()

            val url = "$apiBaseURL/search?term=$search&types=songs&limit=25&l=en-US&platform=web&format[resources]=map&include[songs]=artists&extend=artistUrl"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Origin", "https://music.apple.com")
                .addHeader("Referer", "https://music.apple.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("x-apple-renewal", "true")
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 401) {
                tokenManager.clearToken()
                return@withContext null
            }

            val body = response.body.string()
            if (!response.isSuccessful || body.isBlank()) return@withContext null

            val searchResponse = gson.fromJson(body, AppleMusicSearchResponse::class.java)
            val songs = searchResponse.results?.songs?.data ?: return@withContext null

            if (offset >= songs.size) return@withContext null

            val songId = songs[offset].id
            val songDetail = searchResponse.resources?.songs?.get(songId) ?: return@withContext null
            val attributes = songDetail.attributes

            val artworkUrl = attributes.artwork.url
                .replace("{w}", "500")
                .replace("{h}", "500")
                .replace("{f}", "png")

            SongInfo(
                songName = attributes.name,
                artistName = attributes.artistName,
                songLink = attributes.url,
                albumCoverLink = artworkUrl,
                appleID = songId.toLongOrNull() ?: return@withContext null
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSyncedLyrics(id: Long, multiPersonWordByWord: Boolean = false): String? = withContext(Dispatchers.IO) {
        val url = (lyricsBaseURL + "apple-music/lyrics").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("id", id.toString())
            ?.build() ?: return@withContext null

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body.string()

            if (!response.isSuccessful || body.isBlank() || body == "Not Found.") return@withContext null

            if (body.contains("<tt") || body.contains("<p")) {
                convertTtmlToLrc(body)
            } else {
                body
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertTtmlToLrc(ttml: String): String? {
        val lineRegex = Regex("<p\\b[^>]*\\bbegin=\"([^\"]+)\"[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val spanRegex = Regex("<span\\b[^>]*\\bbegin=\"([^\"]+)\"[^>]*>(.*?)</span>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        val lrcLines = mutableListOf<String>()
        lineRegex.findAll(ttml).forEach { lineMatch ->
            val lineStartMs = parseTtmlTimeToMs(lineMatch.groupValues[1]) ?: return@forEach
            val inner = lineMatch.groupValues[2]

            val words = mutableListOf<String>()
            spanRegex.findAll(inner).forEach { spanMatch ->
                val wordStartMs = parseTtmlTimeToMs(spanMatch.groupValues[1])
                val text = spanMatch.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                if (wordStartMs != null && text.isNotEmpty()) {
                    words.add("<${wordStartMs.toInt().toLrcTimestamp()}>$text")
                }
            }

            if (words.isNotEmpty()) {
                lrcLines += "[${lineStartMs.toInt().toLrcTimestamp()}]" + words.joinToString(" ")
            } else {
                val plainText = inner.replace(Regex("<[^>]+>"), "").trim()
                if (plainText.isNotEmpty()) {
                    lrcLines += "[${lineStartMs.toInt().toLrcTimestamp()}]$plainText"
                }
            }
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
            3 -> (parts[0].toLongOrNull() ?: 0L) * 3600000L + (parts[1].toLongOrNull() ?: 0L) * 60000L + (secondsPart * 1000.0).toLong()
            2 -> (parts[0].toLongOrNull() ?: 0L) * 60000L + (secondsPart * 1000.0).toLong()
            1 -> (secondsPart * 1000.0).toLong()
            else -> null
        }
    }
}

