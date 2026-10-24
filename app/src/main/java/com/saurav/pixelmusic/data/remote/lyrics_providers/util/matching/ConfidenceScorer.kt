package com.saurav.pixelmusic.data.remote.lyrics_providers.util.matching

import kotlin.math.abs

object ConfidenceScorer {
    const val AUTO_ACCEPT_THRESHOLD = 0.85
    const val REVIEW_THRESHOLD = 0.60

    private const val W_TITLE = 0.40
    private const val W_ARTIST = 0.30
    private const val W_DURATION = 0.20
    private const val W_ALBUM = 0.10

    private const val DURATION_EXACT_SEC = 2.0   
    private const val DURATION_ZERO_SEC = 12.0   

    fun tierFor(score: Double): MatchTier = when {
        score >= AUTO_ACCEPT_THRESHOLD -> MatchTier.AUTO_ACCEPT
        score >= REVIEW_THRESHOLD -> MatchTier.REVIEW
        else -> MatchTier.REJECT
    }

    private fun durationSimilarity(a: Double, b: Double): Double {
        val diff = abs(a - b)
        return when {
            diff <= DURATION_EXACT_SEC -> 1.0
            diff >= DURATION_ZERO_SEC -> 0.0
            else -> 1.0 - (diff - DURATION_EXACT_SEC) / (DURATION_ZERO_SEC - DURATION_EXACT_SEC)
        }
    }

    fun score(local: LocalTrack, result: ProviderResult): ConfidenceBreakdown {
        // Pixel Music's existing TextMatch logic can be reused here!
        val titleSim = TextMatch.similarity(local.title, result.title)

        var weightSum = W_TITLE
        var weighted = W_TITLE * titleSim

        var artistSim = 0.0
        if (!local.artist.isNullOrBlank() && !result.artist.isNullOrBlank()) {
            artistSim = TextMatch.similarity(local.artist, result.artist)
            weightSum += W_ARTIST
            weighted += W_ARTIST * artistSim
        }

        var durationSim = 0.0
        var durationMatched = false
        if (local.durationSec != null && result.durationSec != null && local.durationSec > 0 && result.durationSec > 0) {
            durationSim = durationSimilarity(local.durationSec, result.durationSec)
            durationMatched = abs(local.durationSec - result.durationSec) <= DURATION_EXACT_SEC
            weightSum += W_DURATION
            weighted += W_DURATION * durationSim
        }

        var albumSim = 0.0
        if (!local.album.isNullOrBlank() && !result.album.isNullOrBlank()) {
            albumSim = TextMatch.similarity(local.album, result.album)
            weightSum += W_ALBUM
            weighted += W_ALBUM * albumSim
        }

        var score = if (weightSum > 0) weighted / weightSum else 0.0

        val artistComparable = !local.artist.isNullOrBlank() && !result.artist.isNullOrBlank()
        val artistDisagrees = artistComparable && artistSim < 0.50

        if (durationMatched && titleSim >= 0.80 && score >= REVIEW_THRESHOLD && score < AUTO_ACCEPT_THRESHOLD) {
            score = AUTO_ACCEPT_THRESHOLD
        }

        if (!durationMatched && titleSim < 0.55 && score >= REVIEW_THRESHOLD) {
            score = REVIEW_THRESHOLD - 0.01
        }

        var tier = tierFor(score)
        
        if (tier != MatchTier.REJECT && artistDisagrees && !durationMatched) {
            tier = MatchTier.REJECT
        }

        return ConfidenceBreakdown(
            score = score.coerceIn(0.0, 1.0),
            title = titleSim,
            artist = artistSim,
            duration = durationSim,
            album = albumSim,
            tier = tier,
            durationMatched = durationMatched,
        )
    }

    private fun similarity(s1: String?, s2: String?): Double = TextMatch.similarity(s1, s2)
}
