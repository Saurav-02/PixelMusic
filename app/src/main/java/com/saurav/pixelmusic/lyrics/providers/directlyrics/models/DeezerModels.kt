package com.saurav.pixelmusic.lyrics.providers.directlyrics.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeezerAuthResponse(val jwt: String)

@Serializable
internal data class DeezerSearchResponse(val data: List<Track> = emptyList()) {
    @Serializable
    data class Track(
        val id: Long,
        val title: String = "",
        @SerialName("title_short") val titleShort: String = "",
        val duration: Int = 0,
        val artist: Artist? = null,
    )

    @Serializable
    data class Artist(val name: String = "")
}

@Serializable
internal data class DeezerLyricsResponse(val data: Data? = null) {
    @Serializable
    data class Data(val track: Track? = null)

    @Serializable
    data class Track(val lyrics: Lyrics? = null)

    @Serializable
    data class Lyrics(
        val text: String? = null,
        val synchronizedLines: List<Line>? = null,
        val synchronizedWordByWordLines: List<WordLine>? = null,
    )

    @Serializable
    data class Line(val milliseconds: Long = 0, val line: String = "")

    @Serializable
    data class WordLine(val start: Long = 0, val words: List<Word> = emptyList())

    @Serializable
    data class Word(val start: Long = 0, val end: Long = 0, val word: String = "")
}
