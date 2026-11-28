package com.saurav.pixelmusic.lyrics.providers.youlyplus.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val id: Int? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val lyrics: List<LyricsItem>? = null,
    val type: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
)

@Serializable
data class LyricsItem(
    val text: String? = null,
    val time: Long? = null,
    val duration: Long? = null,
    val syllabus: List<Syllable>? = null,
    val element: LyricsElement? = null,
)

@Serializable
data class LyricsElement(
    val singer: String? = null,
)

@Serializable
data class Syllable(
    val text: String? = null,
    val time: Long? = null,
    val duration: Long? = null,
    val isBackground: Boolean? = null,
)
