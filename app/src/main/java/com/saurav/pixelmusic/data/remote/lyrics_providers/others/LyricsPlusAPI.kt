package com.saurav.pixelmusic.data.remote.lyrics_providers.others

import com.google.gson.Gson
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.toLrcTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

// --- LyricsPlus Models ---
data class LyricsPlusResponse(val type: String?, val lyrics: List<LyricsPlusLine>?)
data class LyricsPlusLine(
    val time: Double,
    val text: String,
    val syllabus: List<LyricsPlusWord>?,
    val element: LyricsPlusElement?
)
data class LyricsPlusWord(
    val time: Double,
    val duration: Double,
    val text: String,
    val isBackground: Boolean = false
)
data class LyricsPlusElement(val singer: String?)

class LyricsPlusAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    companion object {
        private val BASE_URLS = listOf(
            "https://lyricsplus.prjktla.my.id",
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.atomix.one",
            "https://lyricsplus-seven.vercel.app",
            "https://lyricsplus.prjktla.workers.dev",
            "https://lyrics-plus-backend.vercel.app"
        )

        @Volatile
        private var lastWorkingServer: String? = null
    }

    private fun prioritizedServers(): List<String> {
        val last = lastWorkingServer
        return if (last != null && last in BASE_URLS) listOf(last) + BASE_URLS.filter { it != last }
        else BASE_URLS
    }

    suspend fun getSyncedLyrics(
        title: String,
        artist: String,
        durationSec: Int? = null,
        album: String? = null,
        multiPersonWordByWord: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null

        for (baseUrl in prioritizedServers()) {
            val lyrics = runCatching {
                val urlBuilder = ("$baseUrl/v2/lyrics/get").toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("title", title)
                    ?.addQueryParameter("artist", artist)

                if (durationSec != null && durationSec > 0) {
                    urlBuilder?.addQueryParameter("duration", durationSec.toString())
                }
                if (!album.isNullOrBlank()) {
                    urlBuilder?.addQueryParameter("album", album)
                }

                val url = urlBuilder?.build() ?: return@runCatching null
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body.string()

                if (!response.isSuccessful || body.isBlank()) null
                else gson.fromJson(body, LyricsPlusResponse::class.java)
            }.getOrNull()

            if (!lyrics?.lyrics.isNullOrEmpty()) {
                lastWorkingServer = baseUrl
                return@withContext convertToLrc(lyrics, multiPersonWordByWord)
            }
        }
        null
    }

    private fun convertToLrc(response: LyricsPlusResponse, multiPersonWordByWord: Boolean): String? {
        val lyrics = response.lyrics?.takeIf { it.isNotEmpty() } ?: return null
        val wordSync = response.type.equals("Word", ignoreCase = true)

        val singers = lyrics.mapNotNull { it.element?.singer?.lowercase() }.distinct()
        val tagVoices = multiPersonWordByWord && singers.size > 1
        val primarySinger = singers.firstOrNull()

        val sb = StringBuilder(lyrics.size * 64)
        for (line in lyrics) {
            val mainWords = line.syllabus?.filter { !it.isBackground }.orEmpty()
            val bgWords = line.syllabus?.filter { it.isBackground }.orEmpty()

            val hasMain = if (wordSync && line.syllabus != null) mainWords.isNotEmpty() else line.text.isNotBlank()
            if (hasMain) {
                sb.append("[${line.time.toInt().toLrcTimestamp()}]")
                if (tagVoices) {
                    sb.append(if (line.element?.singer?.lowercase() == primarySinger) "v1:" else "v2:")
                }
                if (wordSync && mainWords.isNotEmpty()) appendWordByWord(sb, mainWords)
                else sb.append(line.text.trim())
                sb.append('\n')
            }

            if (bgWords.isNotEmpty() && multiPersonWordByWord && wordSync) {
                if (sb.endsWith("\n")) sb.setLength(sb.length - 1)
                sb.append("\n[bg:")
                appendWordByWord(sb, bgWords)
                sb.append("]\n")
            }
        }
        return sb.toString().trimEnd().ifBlank { null }
    }

    private fun appendWordByWord(sb: StringBuilder, words: List<LyricsPlusWord>) {
        for (word in words) {
            val text = word.text.trim()
            if (text.isEmpty()) continue
            val begin = "<${word.time.toInt().toLrcTimestamp()}>"
            val end = "<${(word.time + word.duration).toInt().toLrcTimestamp()}>"
            if (!sb.endsWith(begin)) sb.append(begin)
            sb.append(text).append(' ')
            sb.append(end)
        }
    }
}

