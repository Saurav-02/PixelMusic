package com.saurav.pixelmusic.lyrics.providers.directlyrics

import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.QQLyricResponse
import com.saurav.pixelmusic.lyrics.providers.directlyrics.models.QQSearchResponse
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

object QQMusic {

    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> = runCatching {
        val song = search(title, artist).bestMatch(title, artist, duration)
            ?: throw IllegalStateException("No matching QQ Music song")
        val encoded = musicu(
            module = "music.musichallSong.PlayLyricInfo",
            method = "GetPlayLyricInfo",
            param = buildJsonObject {
                put("songMID", song.mid)
                put("songID", song.id)
            },
        ).body<QQLyricResponse>().req?.data?.lyric.orEmpty()
        val lrc = Base64.getDecoder().decode(encoded).decodeToString()
        parseLrc(lrc).withoutCredits().takeIf { it.hasLyrics() }?.toLrc()
            ?: throw IllegalStateException("QQ Music has no lyrics for this song")
    }

    private suspend fun search(title: String, artist: String): List<TrackCandidate<QQSearchResponse.Song>> =
        musicu(
            module = "music.search.SearchCgiService",
            method = "DoSearchForQQMusicDesktop",
            param = buildJsonObject {
                put("query", "$title $artist")
                put("num_per_page", SearchLimit)
                put("page_num", 1)
                put("search_type", 0)
            },
        ).body<QQSearchResponse>().req?.data?.body?.song?.list.orEmpty().map { song ->
            TrackCandidate(
                titles = listOf(song.name),
                artists = song.singer.map { it.name },
                durationSec = song.interval,
                ref = song,
            )
        }

    private suspend fun musicu(module: String, method: String, param: JsonObject) =
        lyricsHttp.post("https://u.y.qq.com/cgi-bin/musicu.fcg") {
            val body = buildJsonObject {
                put("req", buildJsonObject {
                    put("module", module)
                    put("method", method)
                    put("param", param)
                })
            }
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }

    private const val SearchLimit = 10
}
