package com.saurav.pixelmusic.lyrics.providers.directlyrics.models

import kotlinx.serialization.Serializable

@Serializable
internal data class UnisonResponse(
    val success: Boolean = false,
    val data: UnisonTrack? = null,
)

@Serializable
internal data class UnisonTrack(
    val id: Long = 0L,
    val videoId: String? = null,
    val song: String = "",
    val artist: String = "",
    val album: String? = null,
    val lyrics: String = "",
    val format: String = "",
    val syncType: String = "",
    val submitter: UnisonSubmitter? = null,
)

@Serializable
internal data class UnisonSubmitter(
    val keyId: String = "",
    val displayName: String = "",
)

/** One lyrics result from Unison, keeping its video id and submitter so attribution stays intact. */
data class UnisonLyrics(
    val lyrics: String,
    val format: String,
    val videoId: String?,
    val submitterKeyId: String?,
    val submitterName: String?,
)
