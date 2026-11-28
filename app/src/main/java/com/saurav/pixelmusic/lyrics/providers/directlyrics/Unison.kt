package com.saurav.pixelmusic.lyrics.providers.directlyrics

import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.UnisonLyrics
import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.UnisonResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Crowdsourced lyrics API from Better Lyrics; looks up by YouTube video ID first, then metadata, then lead artist. */
object Unison {

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<UnisonLyrics> = runCatching {
        val found = lookupByVideo(videoId)
            ?: lookupByMetadata(title, artist, duration, album)
            ?: fallbackLookup(title, artist, duration, album)
            ?: throw IllegalStateException("No matching Unison lyrics")
        found
    }

    private suspend fun lookupByVideo(videoId: String): UnisonLyrics? {
        if (!YouTubeId.matches(videoId)) return null
        return fetch { parameter("v", videoId) }
    }

    private suspend fun lookupByMetadata(title: String, artist: String, duration: Int, album: String?): UnisonLyrics? {
        if (title.isBlank() || artist.isBlank()) return null
        return fetch {
            parameter("song", title)
            parameter("artist", artist)
            if (!album.isNullOrBlank()) parameter("album", album)
            if (duration > 0) parameter("duration", duration)
        }
    }

    private suspend fun fallbackLookup(title: String, artist: String, duration: Int, album: String?): UnisonLyrics? {
        val lead = artist.substringBefore(",").substringBefore("&").substringBefore(" feat.").trim()
        if (lead.isBlank() || (lead == artist && album.isNullOrBlank())) return null
        return fetch {
            parameter("song", title)
            parameter("artist", lead)
            if (duration > 0) parameter("duration", duration)
        }
    }

    private suspend fun fetch(params: io.ktor.client.request.HttpRequestBuilder.() -> Unit): UnisonLyrics? {
        val response = runCatching {
            lyricsHttp.get("$BaseUrl/lyrics", params).body<UnisonResponse>()
        }.getOrNull() ?: return null
        val data = response.data?.takeIf { response.success && it.lyrics.isNotBlank() } ?: return null
        return UnisonLyrics(
            lyrics = data.lyrics,
            format = data.format,
            videoId = data.videoId?.takeIf { it.isNotBlank() },
            submitterKeyId = data.submitter?.keyId?.takeIf { it.isNotBlank() },
            submitterName = data.submitter?.displayName?.takeIf { it.isNotBlank() },
        )
    }

    fun songUrl(videoId: String): String = "$BaseUrl/song/$videoId"

    fun curatorUrl(keyId: String): String = "$BaseUrl/curator/$keyId"

    const val BaseUrl = "https://unison.betterlyrics.org"
    const val RepoUrl = "https://github.com/better-lyrics/unison"
    private val YouTubeId = Regex("^[A-Za-z0-9_-]{11}$")
}
