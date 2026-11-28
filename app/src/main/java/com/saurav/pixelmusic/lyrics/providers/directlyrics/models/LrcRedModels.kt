package com.saurav.pixelmusic.lyrics.providers.directlyrics.models

import kotlinx.serialization.Serializable

@Serializable
internal data class LrcRedSearchResponse(val hits: List<Hit> = emptyList()) {
    @Serializable
    data class Hit(
        val isrc: String,
        val title: String = "",
        val artist: String = "",
        val duration: Double = 0.0,
    )
}
