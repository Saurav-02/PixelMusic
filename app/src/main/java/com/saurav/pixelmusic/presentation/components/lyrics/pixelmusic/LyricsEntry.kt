// LyricsEntry.kt

package com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic

import kotlinx.coroutines.flow.MutableStateFlow

data class WordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double
)

/**
 * One reading positioned over the characters it belongs to.
 *
 * [start] inclusive, [end] exclusive, both indices into the line's own text — that is what lets the
 * renderer put the reading physically above its word instead of dumping the whole line's
 * romanisation underneath as one run-on string.
 */
data class RubyToken(
    val start: Int,
    val end: Int,
    val text: String,
)

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    /** Per-word readings drawn above the line. Null when this script isn't romanised as ruby. */
    val rubyFlow: MutableStateFlow<List<RubyToken>?> = MutableStateFlow(null),
    val translatedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    /** The timed pieces of [romanizedTextFlow], when the line has word timing: the romanised line sung word by word like the original. */
    val romanizedWordsFlow: MutableStateFlow<List<WordTimestamp>?> = MutableStateFlow(null),
    val agent: String? = null,
    val isBackground: Boolean = false,
    /**
     * The timed pieces exactly as the source sent them, when they were syllables that
     * [LyricsUtils.parseLyrics] merged into whole [words]. Null when [words] already are those
     * pieces. The main lyrics style sweeps along the line's own text and wants these.
     */
    val syllables: List<WordTimestamp>? = null,
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}
