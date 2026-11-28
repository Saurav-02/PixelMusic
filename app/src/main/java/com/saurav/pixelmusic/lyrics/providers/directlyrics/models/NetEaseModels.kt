package com.saurav.pixelmusic.lyrics.providers.directlyrics.models

import kotlinx.serialization.Serializable

@Serializable
internal data class NetEaseSearchResponse(val result: SearchResult? = null) {
    @Serializable
    data class SearchResult(val songs: List<Song> = emptyList())

    @Serializable
    data class Song(
        val id: Long,
        val name: String = "",
        val duration: Long = 0,
        val artists: List<Artist> = emptyList(),
        val alias: List<String> = emptyList(),
    )

    @Serializable
    data class Artist(val name: String = "")
}

@Serializable
internal data class NetEaseLyricResponse(
    val lrc: Lyric? = null,
    val yrc: Lyric? = null,
) {
    @Serializable
    data class Lyric(val lyric: String? = null)
}
