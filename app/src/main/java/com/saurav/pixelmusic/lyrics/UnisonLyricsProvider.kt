package com.saurav.pixelmusic.lyrics

import com.saurav.pixelmusic.lyrics.providers.betterlyrics.TTMLParser
import com.saurav.pixelmusic.lyrics.providers.directlyrics.Unison
import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.UnisonLyrics

/** Crowdsourced lyrics from Better Lyrics' Unison API, keeping song and submitter links as an LRC metadata tag. */
object UnisonLyricsProvider {
    const val Name = "Unison"

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = Unison.getLyrics(id, title, artist, duration, album).mapCatching { result ->
        val body = formatBody(result)
        check(body.isNotBlank()) { "unison returned empty lyrics for this track" }
        attachCredit(body, result)
    }

    internal fun formatBody(result: UnisonLyrics): String {
        val raw = result.lyrics.trim()
        val isTtml = result.format.equals("ttml", ignoreCase = true) || raw.startsWith("<tt")
        if (!isTtml) return raw
        val lines = TTMLParser.parseTTML(raw)
        check(lines.isNotEmpty()) { "unison ttml has no timed lyrics for this track" }
        return TTMLParser.toLRC(lines)
    }

    internal fun attachCredit(body: String, result: UnisonLyrics): String {
        val videoId = sanitize(result.videoId)
        val keyId = sanitize(result.submitterKeyId)
        val displayName = sanitize(result.submitterName)
        if (videoId.isEmpty() && displayName.isEmpty()) return body
        return "$TagPrefix$videoId|$keyId|$displayName]\n$body"
    }

    fun parseCredit(rawLyrics: String?): UnisonCredit? {
        if (rawLyrics.isNullOrBlank()) return null
        val line = rawLyrics.lineSequence().firstOrNull { it.trim().startsWith(TagPrefix) }?.trim() ?: return null
        val inner = line.removePrefix(TagPrefix).removeSuffix("]")
        val parts = inner.split("|", limit = 3)
        if (parts.size != 3) return null
        val videoId = parts[0].takeIf { it.isNotEmpty() }
        val keyId = parts[1].takeIf { it.isNotEmpty() }
        val displayName = parts[2].takeIf { it.isNotEmpty() }
        if (videoId == null && displayName == null) return null
        return UnisonCredit(
            songUrl = videoId?.let(Unison::songUrl) ?: Unison.BaseUrl,
            submitterName = displayName,
            submitterUrl = keyId?.let(Unison::curatorUrl),
        )
    }

    private fun sanitize(raw: String?): String =
        raw.orEmpty().replace(Regex("[|\\]\\r\\n]"), " ").trim()

    private const val TagPrefix = "[unison:"
}

data class UnisonCredit(
    val songUrl: String,
    val submitterName: String?,
    val submitterUrl: String?,
)
