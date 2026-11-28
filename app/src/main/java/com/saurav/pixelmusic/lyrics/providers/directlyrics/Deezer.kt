package com.saurav.pixelmusic.lyrics.providers.directlyrics

import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.DeezerAuthResponse
import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.DeezerLyricsResponse
import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.DeezerSearchResponse
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.util.Base64

object Deezer {

    private val tokenLock = Mutex()
    private var token: String? = null
    private var tokenExpiresAt = 0L

    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> = runCatching {
        val trackId = search(title, artist).bestMatch(title, artist, duration)
            ?: throw IllegalStateException("No matching Deezer track")
        val lyrics = fetchLyrics(trackId) ?: throw IllegalStateException("Deezer has no lyrics for this track")
        val lines = lyrics.synchronizedWordByWordLines.orEmpty().map { line ->
            TimedLine(
                startMs = line.start,
                text = joinWords(line.words.map { it.word }),
                words = line.words.map { TimedWord(it.word, it.start, it.end) },
            )
        }.ifEmpty {
            lyrics.synchronizedLines.orEmpty().map { TimedLine(it.milliseconds, it.line.trim()) }
        }
        when {
            lines.hasLyrics() -> lines.toLrc()
            !lyrics.text.isNullOrBlank() -> lyrics.text.replace("\r\n", "\n").trim()
            else -> throw IllegalStateException("Deezer has no lyrics for this track")
        }
    }

    private suspend fun search(title: String, artist: String): List<TrackCandidate<Long>> =
        lyricsHttp.get("https://api.deezer.com/search") {
            parameter("q", "$title $artist")
            parameter("limit", SearchLimit)
        }.body<DeezerSearchResponse>().data.map { track ->
            TrackCandidate(
                titles = listOf(track.title, track.titleShort),
                artists = listOfNotNull(track.artist?.name),
                durationSec = track.duration,
                ref = track.id,
            )
        }

    private suspend fun fetchLyrics(trackId: Long): DeezerLyricsResponse.Lyrics? {
        val query = "query { track(trackId: \"$trackId\") { lyrics { text " +
            "synchronizedLines { milliseconds line } " +
            "synchronizedWordByWordLines { start words { start end word } } } } }"
        return try {
            lyricsHttp.post("https://pipe.deezer.com/api") {
                bearerAuth(accessToken())
                setBody(TextContent(buildJsonObject { put("query", query) }.toString(), ContentType.Application.Json))
            }.body<DeezerLyricsResponse>().data?.track?.lyrics
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            tokenLock.withLock { token = null }
            throw e
        }
    }

    private suspend fun accessToken(): String {
        tokenLock.withLock {
            token?.takeIf { System.currentTimeMillis() < tokenExpiresAt }?.let { return it }
            val jwt = lyricsHttp.get("https://auth.deezer.com/login/anonymous") {
                parameter("jo", "p")
                parameter("rto", "c")
            }.body<DeezerAuthResponse>().jwt
            val expiresAt = expiryOf(jwt) ?: (System.currentTimeMillis() + FallbackTokenLifetimeMs)
            token = jwt
            tokenExpiresAt = expiresAt - TokenRenewMarginMs
            return jwt
        }
    }

    private fun expiryOf(jwt: String): Long? = runCatching {
        val payload = Base64.getUrlDecoder().decode(jwt.split('.')[1]).decodeToString()
        LyricsJson.parseToJsonElement(payload).jsonObject.getValue("exp").jsonPrimitive.long * 1000
    }.getOrNull()

    private fun joinWords(words: List<String>): String = buildString {
        words.map { it.trim() }.filter { it.isNotEmpty() }.forEach { word ->
            val attached = word.first() in NoSpaceBefore ||
                (isNotEmpty() && isUnspaced(last()) && isUnspaced(word.first()))
            if (isNotEmpty() && !attached) append(' ')
            append(word)
        }
    }

    private fun isUnspaced(char: Char): Boolean = Character.UnicodeScript.of(char.code) in UnspacedScripts

    private const val SearchLimit = 10
    private const val FallbackTokenLifetimeMs = 5 * 60_000L
    private const val TokenRenewMarginMs = 30_000L
    private const val NoSpaceBefore = ",.!?;:)]}…"
    private val UnspacedScripts = setOf(
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.THAI,
    )
}
