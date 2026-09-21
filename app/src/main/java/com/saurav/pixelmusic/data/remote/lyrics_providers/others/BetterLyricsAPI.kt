package com.saurav.pixelmusic.data.remote.lyrics_providers.others

import com.google.gson.Gson
import com.saurav.pixelmusic.data.remote.lyrics_providers.util.toLrcTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class BetterLyricsTtmlResponse(val ttml: String? = null)

class BetterLyricsAPI(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {
    private val baseURL = "https://lyrics-api.boidu.dev"

    suspend fun getSyncedLyrics(
        title: String,
        artist: String,
        durationSec: Int? = null,
        album: String? = null,
        multiPersonWordByWord: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null

        val urlBuilder = ("$baseURL/getLyrics").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("s", title)
            ?.addQueryParameter("a", artist)

        if (durationSec != null && durationSec > 0) {
            urlBuilder?.addQueryParameter("d", durationSec.toString())
        }
        if (!album.isNullOrBlank()) {
            urlBuilder?.addQueryParameter("al", album)
        }

        val url = urlBuilder?.build() ?: return@withContext null
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) return@withContext null

            val ttml = gson.fromJson(body, BetterLyricsTtmlResponse::class.java).ttml ?: return@withContext null
            convertTtmlToLrc(ttml)
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

