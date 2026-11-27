package com.saurav.pixelmusic.lyrics.providers.directlyrics

import kotlin.math.abs

internal data class TrackCandidate<T>(
    val titles: List<String>,
    val artists: List<String>,
    val durationSec: Int,
    val ref: T,
)

private const val DurationTolerance = 8

internal fun <T> List<TrackCandidate<T>>.bestMatch(title: String, artist: String, durationSec: Int): T? {
    val wantedTitle = normalize(title)
    val wantedArtist = normalize(artist)
    if (wantedTitle.isEmpty()) return null
    return asSequence()
        .filter { durationSec <= 0 || abs(it.durationSec - durationSec) <= DurationTolerance }
        .map { it to titleScore(it, wantedTitle) }
        .filter { (_, score) -> score > 0 }
        .maxByOrNull { (candidate, score) -> score + artistScore(candidate, wantedArtist) }
        ?.first
        ?.ref
}

private fun titleScore(candidate: TrackCandidate<*>, wantedTitle: String): Int =
    candidate.titles.map(::normalize).filter { it.isNotEmpty() }.maxOfOrNull { title ->
        when {
            title == wantedTitle -> 2
            title.contains(wantedTitle) || wantedTitle.contains(title) -> 1
            else -> 0
        }
    } ?: 0

private fun artistScore(candidate: TrackCandidate<*>, wantedArtist: String): Int {
    if (wantedArtist.isEmpty()) return 0
    val matches = candidate.artists.map(::normalize).any { name ->
        name.isNotEmpty() && (wantedArtist.contains(name) || name.contains(wantedArtist))
    }
    return if (matches) 2 else 0
}

private val Decorations = Regex("""\s*[(（\[【「『《].*?[)）\]】」』》]""")

private fun normalize(text: String): String =
    text.replace(Decorations, "").lowercase().filter { it.isLetterOrDigit() }
