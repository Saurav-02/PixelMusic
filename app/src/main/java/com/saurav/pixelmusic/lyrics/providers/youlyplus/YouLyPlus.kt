package com.saurav.pixelmusic.lyrics.providers.youlyplus

import com.saurav.pixelmusic.lyrics.providers.youlyplus.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object YouLyPlus {

    private val BASE_SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.binimum.org",
    )

    private val lastWorkingServer = AtomicReference<String?>(null)

    private val servers: List<String>
        get() {
            val lws = lastWorkingServer.get() ?: return BASE_SERVERS
            return listOf(lws) + BASE_SERVERS.filter { it != lws }
        }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 3_000
                requestTimeoutMillis = 8_000
            }
            install(UserAgent) { agent = "PixelMusic/1.0 (Android)" }
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            expectSuccess = true
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): Result<String> = runCatching {
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = servers.map { server ->
            server to scope.async {
                fetchFromServer(server, title, artist, duration, album, id, isrc)
            }
        }

        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val (winServer, winLyrics) = select {
                    remaining.forEach { (srv, deferred) ->
                        deferred.onAwait { response -> srv to response }
                    }
                }
                remaining.removeAll { it.first == winServer }

                val lrc = winLyrics?.let { resp ->
                    resp.syncedLyrics?.takeIf { it.isNotBlank() }
                        ?: resp.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                        ?: resp.plainLyrics?.takeIf { it.isNotBlank() }
                }
                if (!lrc.isNullOrBlank()) {
                    lastWorkingServer.set(winServer)
                    return@runCatching lrc
                }
            }
            throw IllegalStateException("No lyrics found from any YouLyPlus server")
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
        callback: (String) -> Unit,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        val jobs = servers.map { server ->
            scope.async {
                runCatching {
                    val response = fetchFromServer(server, title, artist, duration, album, id, isrc)
                    if (response != null) {
                        response.syncedLyrics?.takeIf { it.isNotBlank() }
                            ?: response.lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
                            ?: response.plainLyrics?.takeIf { it.isNotBlank() }
                    } else null
                }.getOrNull()
            }
        }
        try {
            val remaining = jobs.toMutableList()
            while (remaining.isNotEmpty()) {
                val lrc = select {
                    remaining.forEach { deferred -> deferred.onAwait { it } }
                }
                remaining.removeAll { it.isCompleted }
                if (!lrc.isNullOrBlank()) callback(lrc)
            }
        } finally {
            scope.coroutineContext.cancelChildren()
        }
    }

    private fun List<com.saurav.pixelmusic.lyrics.providers.youlyplus.models.LyricsItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val lineTime = item.time ?: 0L
            val isBg = item.syllabus?.any { it.isBackground == true } == true
            val lineTimestamp = formatTime(lineTime)
            val singer = item.element?.singer
            val marker = when {
                isBg -> "{bg}"
                !singer.isNullOrBlank() -> "{agent:$singer}"
                else -> ""
            }

            val syllabus = item.syllabus
            if (!syllabus.isNullOrEmpty()) {
                val sb = StringBuilder(lineTimestamp)
                sb.append(marker)
                val spacedByApi = syllabus.dropLast(1).any { it.text?.lastOrNull()?.isWhitespace() == true }
                syllabus.forEachIndexed { index, syl ->
                    val sylTime = syl.time ?: 0L
                    sb.append(formatTime(sylTime, isSyllable = true))
                    sb.append(syl.text ?: "")
                    if (!spacedByApi && index < syllabus.lastIndex && syl.text?.lastOrNull()?.isWhitespace() != true) {
                        sb.append(" ")
                    }
                }
                sb.toString().trim()
            } else {
                lineTimestamp + marker + (item.text ?: "")
            }
        }
    }

    private fun formatTime(timeMs: Long, isSyllable: Boolean = false): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format(Locale.US, "%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }

    private suspend fun fetchFromServer(
        baseUrl: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        id: String? = null,
        isrc: String? = null,
    ): LyricsResponse? = runCatching {
        client.get(baseUrl.let { if (it.endsWith("/")) it else "$it/" } + "v2/lyrics/get") {
            parameter("title", title)
            parameter("artist", artist)
            parameter("duration", duration)
            if (album != null) parameter("album", album)
            if (id != null) parameter("id", id)
            if (isrc != null) parameter("isrc", isrc)
        }.body<LyricsResponse>()
    }.getOrNull()
}
