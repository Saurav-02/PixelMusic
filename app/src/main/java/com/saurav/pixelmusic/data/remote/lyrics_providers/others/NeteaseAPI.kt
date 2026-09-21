package com.saurav.pixelmusic.data.remote.lyrics_providers.others

import android.util.Log
import com.google.gson.Gson
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.EmptyQueryException
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.InternalErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

// --- Netease Data Models ---
data class NeteaseResponse(val result: NeteaseResult?)
data class NeteaseResult(val songs: List<NeteaseSong>?)
data class NeteaseSong(val id: Long, val name: String, val artists: List<NeteaseArtist>?, val duration: Long?, val album: NeteaseAlbum?)
data class NeteaseArtist(val name: String)
data class NeteaseAlbum(val name: String)
data class NeteaseLyricsResponse(val lrc: NeteaseLyric?, val tlyric: NeteaseLyric?, val romalrc: NeteaseLyric?)
data class NeteaseLyric(val lyric: String?)

class NeteaseAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val baseURL = "http://music.163.com/api/"

    private val reqHeaders = mapOf(
        "Accept" to "application/json",
        "Accept-Language" to "en-US,en;q=0.9,fa;q=0.8",
        "Cache-Control" to "max-age=0",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/109.0.0.0",
        "Cookie" to "os=pc" 
    )

    suspend fun searchCandidates(query: String, limit: Int = 8): List<NeteaseSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val url = (baseURL + "search/pc").toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("limit", limit.toString())
                ?.addQueryParameter("type", "1")
                ?.addQueryParameter("offset", "0")
                ?.addQueryParameter("s", query)
                ?.build() ?: return@withContext emptyList()

            val requestBuilder = Request.Builder().url(url)
            reqHeaders.forEach { requestBuilder.addHeader(it.key, it.value) }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank() || body == "[]" || body.contains("\"songCount\":0")) {
                return@withContext emptyList()
            }

            val parsed = gson.fromJson(body, NeteaseResponse::class.java)
            parsed.result?.songs ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo? = withContext(Dispatchers.IO) {
        val search = "${query.songName} ${query.artistName}".trim()
        if (search.isBlank()) throw EmptyQueryException()

        val url = (baseURL + "search/pc").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("limit", "1")
            ?.addQueryParameter("type", "1")
            ?.addQueryParameter("offset", offset.toString())
            ?.addQueryParameter("s", search)
            ?.build() ?: return@withContext null

        val requestBuilder = Request.Builder().url(url)
        reqHeaders.forEach { requestBuilder.addHeader(it.key, it.value) }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank() || body == "[]" || body.contains("\"songCount\":0")) {
                return@withContext null
            }

            val parsed = gson.fromJson(body, NeteaseResponse::class.java)
            val song = parsed.result?.songs?.firstOrNull() ?: return@withContext null
            val artists = song.artists?.joinToString(", ") { it.name } ?: ""

            SongInfo(
                songName = song.name,
                artistName = artists,
                neteaseID = song.id
            )
        } catch (e: Exception) {
            throw InternalErrorException(Log.getStackTraceString(e))
        }
    }

    suspend fun getSyncedLyrics(id: Long, includeTranslation: Boolean = false, includeRomanization: Boolean = false): String? = withContext(Dispatchers.IO) {
        val url = (baseURL + "song/lyric").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("id", id.toString())
            ?.addQueryParameter("lv", "1")
            ?.addQueryParameter("tv", "1")
            ?.addQueryParameter("rv", "1")
            ?.build() ?: return@withContext null

        val requestBuilder = Request.Builder().url(url)
        reqHeaders.forEach { requestBuilder.addHeader(it.key, it.value) }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank() || body == "[]") return@withContext null

            val json = gson.fromJson(body, NeteaseLyricsResponse::class.java)
            var lyric = json.lrc?.lyric

            if (lyric.isNullOrEmpty()) return@withContext null

            if (includeTranslation && !json.tlyric?.lyric.isNullOrEmpty()) {
                lyric += "\n\n" + json.tlyric?.lyric
            }
            if (includeRomanization && !json.romalrc?.lyric.isNullOrEmpty()) {
                lyric += "\n\n" + json.romalrc?.lyric
            }

            lyric
        } catch (e: Exception) {
            null
        }
    }
}

