package com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching

enum class MatchTier { AUTO_ACCEPT, REVIEW, REJECT }

data class LocalTrack(
    val title: String?,
    val artist: String?,
    val durationSec: Double? = null,
    val album: String? = null,
)

data class ProviderResult(
    val title: String?,
    val artist: String?,
    val durationSec: Double? = null,
    val album: String? = null,
    val hasSyncedLyrics: Boolean = true,
)

data class ConfidenceBreakdown(
    val score: Double,
    val title: Double,
    val artist: Double,
    val duration: Double,
    val album: Double,
    val tier: MatchTier,
    val durationMatched: Boolean,
) {
    fun percent(): Int = (score * 100).toInt()
}

