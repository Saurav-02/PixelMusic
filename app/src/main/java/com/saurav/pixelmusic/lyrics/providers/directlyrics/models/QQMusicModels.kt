package com.saurav.pixelmusic.lyrics.providers.directlyrics.models

import kotlinx.serialization.Serializable

@Serializable
internal data class QQSearchResponse(val req: Request? = null) {
    @Serializable
    data class Request(val data: Data? = null)

    @Serializable
    data class Data(val body: Body? = null)

    @Serializable
    data class Body(val song: SongList? = null)

    @Serializable
    data class SongList(val list: List<Song> = emptyList())

    @Serializable
    data class Song(
        val id: Long,
        val mid: String,
        val name: String = "",
        val interval: Int = 0,
        val singer: List<Singer> = emptyList(),
    )

    @Serializable
    data class Singer(val name: String = "")
}

@Serializable
internal data class QQLyricResponse(val req: Request? = null) {
    @Serializable
    data class Request(val data: Data? = null)

    @Serializable
    data class Data(val lyric: String? = null)
}
