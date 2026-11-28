package com.saurav.pixelmusic.lyrics.providers.directlyrics

import java.util.Locale

internal data class TimedWord(val text: String, val startMs: Long, val endMs: Long)

internal data class TimedLine(val startMs: Long, val text: String, val words: List<TimedWord> = emptyList())

private val LrcLineRegex = Regex("""^\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?](.*)$""")

internal fun parseLrc(lrc: String): List<TimedLine> = lrc.lineSequence().mapNotNull { raw ->
    val match = LrcLineRegex.matchEntire(raw.trim()) ?: return@mapNotNull null
    val (minutes, seconds, fraction, text) = match.destructured
    val millis = fraction.padEnd(3, '0').take(3).toLong()
    TimedLine(minutes.toLong() * 60_000 + seconds.toLong() * 1000 + millis, text.trim())
}.toList()

private val CreditRegex = Regex("""^[^:：]{1,24}[:：].+$""")
private const val HeadCreditWindow = 15
private const val TailCreditWindow = 6

private fun TimedLine.isCredit(): Boolean {
    if (!CreditRegex.matches(text)) return false
    val label = text.substringBefore(':').substringBefore('：').trim()
    return label.split(' ').size <= 4
}

internal fun List<TimedLine>.withoutCredits(): List<TimedLine> {
    val body = drop(take(HeadCreditWindow).indexOfLast { it.isCredit() } + 1)
    val tailStart = (body.size - TailCreditWindow).coerceAtLeast(0)
    val tailCut = body.subList(tailStart, body.size).indexOfFirst { it.isCredit() }
    return if (tailCut == -1) body else body.subList(0, tailStart + tailCut)
}

private const val InstrumentalMarker = "纯音乐"

internal fun List<TimedLine>.hasLyrics(): Boolean {
    val sung = filter { it.text.isNotBlank() }
    return sung.isNotEmpty() && sung.none { InstrumentalMarker in it.text }
}

private const val WordSeparators = ":|<>"

internal fun List<TimedLine>.toLrc(): String = buildString {
    this@toLrc.forEach { line ->
        append(stamp(line.startMs)).append(line.text).append('\n')
        val words = line.words
            .map { it.copy(text = it.text.trim().filterNot { char -> char in WordSeparators }) }
            .filter { it.text.isNotEmpty() }
        if (words.isNotEmpty()) {
            words.joinTo(this, separator = "|", prefix = "<", postfix = ">\n") { word ->
                "${word.text}:${word.startMs / 1000.0}:${word.endMs / 1000.0}"
            }
        }
    }
}.trimEnd()

private fun stamp(ms: Long): String =
    String.format(Locale.US, "[%02d:%02d.%03d]", ms / 60_000, ms / 1000 % 60, ms % 1000)
