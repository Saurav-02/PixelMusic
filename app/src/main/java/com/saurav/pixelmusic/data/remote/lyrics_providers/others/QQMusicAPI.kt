package com.saurav.pixelmusic.data.remote.lyrics_providers.others

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.EmptyQueryException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- QQ Music Models ---
data class QQMusicSearchResponse(val data: QQMusicData?)
data class QQMusicData(val song: QQMusicSongContainer?)
data class QQMusicSongContainer(val list: List<QQMusicSongItem>?)
data class QQMusicSongItem(
    val id: String?,
    val title: String?,
    val singer: List<QQMusicSinger>?,
    val album: QQMusicAlbum?
)
data class QQMusicSinger(val name: String?)
data class QQMusicAlbum(val name: String?)
data class PaxQQPayload(
    val artist: List<String>,
    val album: String?,
    val id: String?,
    val title: String?
)
data class PaxLyricsResponse(val lyrics: String?)

class QQMusicAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val baseURL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp"
    private val lyricsURL = "https://paxsenix.alwaysdata.net/getQQLyrics.php"

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo? = withContext(Dispatchers.IO) {
        val search = "${query.songName} ${query.artistName}".trim()
        if (search.isBlank()) throw EmptyQueryException()

        val url = baseURL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("inCharset", "utf8")
            ?.addQueryParameter("outCharset", "utf8")
            ?.addQueryParameter("platform", "yqq.json")
            ?.addQueryParameter("new_json", "1")
            ?.addQueryParameter("w", search)
            ?.build() ?: return@withContext null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body.string()
            if (!response.isSuccessful || body.isBlank()) return@withContext null

            val result = gson.fromJson(body, QQMusicSearchResponse::class.java)
            val songList = result.data?.song?.list ?: return@withContext null
            val song = songList.getOrNull(offset) ?: return@withContext null

            val artists = song.singer?.mapNotNull { it.name } ?: emptyList()
            val payload = PaxQQPayload(
                artist = artists,
                album = song.album?.name,
                id = song.id,
                title = song.title
            )

            SongInfo(
                songName = song.title,
                artistName = artists.joinToString(", "),
                qqPayload = gson.toJson(payload)
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSyncedLyrics(payload: String): String? = withContext(Dispatchers.IO) {
        if (payload.isBlank()) return@withContext null

        val requestBody = payload.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(lyricsURL)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body.string()
            if (!response.isSuccessful || body.isBlank()) return@withContext null

            val parsedJson = runCatching { gson.fromJson(body, PaxLyricsResponse::class.java) }.getOrNull()
            val lyrics = parsedJson?.lyrics ?: body
            lyrics.takeIf { it.isNotBlank() && it != "Not Found." }
        } catch (e: Exception) {
            null
        }
    }
}

