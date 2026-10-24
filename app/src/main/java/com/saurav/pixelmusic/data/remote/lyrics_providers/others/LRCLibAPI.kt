package com.saurav.pixelmusic.data.remote.lyrics_providers.others

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saurav.pixelmusic.data.remote.lyrics_providers.model.SongInfo
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.EmptyQueryException
import com.saurav.pixelmusic.data.network.lyrics.LrcLibResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LRCLibAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val baseURL = "https://lrclib.net/api/"
    private val userAgent = "PixelMusic/1.0 (https://github.com/Xing1P/PixelMusic)"

    suspend fun searchCandidates(query: String): List<LrcLibResponse> = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        if (enc.isBlank() || enc == "+") return@withContext emptyList()

        val request = Request.Builder()
            .url(baseURL + "search?q=$enc")
            .header("User-Agent", userAgent)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val body = response.body.string()
            if (body.isBlank() || body == "[]") return@withContext emptyList()

            val listType = object : TypeToken<List<LrcLibResponse>>() {}.type
            gson.fromJson(body, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo? = withContext(Dispatchers.IO) {
        val search = URLEncoder.encode("${query.songName} ${query.artistName}", StandardCharsets.UTF_8.toString())
        if (search == "+") throw EmptyQueryException()

        val request = Request.Builder()
            .url(baseURL + "search?q=$search")
            .header("User-Agent", userAgent)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            
            val body = response.body.string()
            if (body.isBlank() || body == "[]") return@withContext null

            val listType = object : TypeToken<List<LrcLibResponse>>() {}.type
            val json: List<LrcLibResponse> = gson.fromJson(body, listType)

            val song = json.getOrNull(offset) ?: return@withContext null

            SongInfo(
                songName = song.name,
                artistName = song.artistName,
                lrcLibID = song.id
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSyncedLyrics(id: Int): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseURL + "get/$id")
            .header("User-Agent", userAgent)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            
            val body = response.body.string()
            if (body.isBlank() || body == "[]") return@withContext null

            val json = gson.fromJson(body, LrcLibResponse::class.java)
            json.syncedLyrics
        } catch (e: Exception) {
            null
        }
    }
}

