package com.unshoo.pixelmusic.utils

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.data.model.Lyrics
import com.unshoo.pixelmusic.data.model.SyncedLine
import com.unshoo.pixelmusic.data.model.SyncedWord
import kotlinx.coroutines.flow.Flow

import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// Roman Multilang (Removed to save space and simplify logic)
object MultiLangRomanizer {
    fun isJapanese(text: String) = false
    fun isKorean(text: String) = false
    fun isHindi(text: String) = false
    fun isPunjabi(text: String) = false
    fun isCyrillic(text: String) = false
    fun isChinese(text: String) = false

    fun romanizeJapanese(japaneseText: String): String? = null
    fun romanizeChinese(text: String): String? = null
    fun romanizeKorean(text: String): String? = null
    fun romanizeHindi(text: String): String? = null
    fun romanizePunjabi(text: String): String? = null
    fun romanizeCyrillic(text: String): String? = null
}

private fun String.capitalizeFirstLetter(): String {
    if (this.isEmpty()) return this
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}

object LyricsUtils {

    private val LRC_LINE_REGEX = Pattern.compile("^\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})](.*)$")
    private val LRC_WORD_REGEX = Pattern.compile("<(\\d{2}):(\\d{2})[.:](\\d{2,3})>([^<]*)")
    private val LRC_WORD_TAG_REGEX = Regex("<\\d{2}:\\d{2}[.:]\\d{2,3}>")
    private val LRC_WORD_SPLIT_REGEX = Regex("(?=<\\d{2}:\\d{2}[.:]\\d{2,3}>)")
    private val LRC_TIMESTAMP_TAG_REGEX = Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]")
    private val TRANSLATION_CREDIT_REGEX = Regex("^\\s*by\\s*[:：].+", RegexOption.IGNORE_CASE)
    private val LRC_METADATA_PATTERN = Pattern.compile("^\\[[a-zA-Z]+:.*]$")

    // Kugou / Paxsenix word-by-word format:
    //   Line header : [lineStartMs,lineDurationMs]
    //   Word token  : <wordOffsetMs,wordDurationMs,flags>word
    // The line-start value is always > 999 ms, which distinguishes it from a
    // standard LRC minute value (max 99).
    private val KUGOU_LINE_REGEX = Pattern.compile("^\\[(\\d+),(\\d+)](.*)$")
    private val KUGOU_WORD_PATTERN = Pattern.compile("<(\\d+),(\\d+),(\\d+)>([^<]*)")

    /**
     * Parses a String containing lyrics in LRC or plain-text format.
     * @param lyricsText The raw lyrics text to process.
     * @return A [Lyrics] object with either the 'plain' or 'synced' list populated.
     */
    fun parseLyrics(lyricsText: String?): Lyrics {
        if (lyricsText.isNullOrEmpty()) {
            return Lyrics(plain = emptyList(), synced = emptyList())
        }

        val normalizedInput = stripLeadingLyricsDocumentNoise(lyricsText)
        if (looksLikeTtmlDocument(normalizedInput)) {
            val converted = TtmlLyricsParser.parseToEnhancedLrc(normalizedInput)
                ?: return Lyrics(plain = emptyList(), synced = emptyList())
            return parseLyrics(converted)
        }

        // Kugou / Paxsenix word-by-word detection:
        // If any non-metadata line matches [number,number] where the first
        // number is > 999 (i.e. milliseconds, not minutes), treat the whole
        // file as Kugou format.
        if (looksLikeKugouFormat(lyricsText)) {
            return parseKugouLyrics(lyricsText)
        }

        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        var isSynced = false

        lyricsText.lines().forEach { rawLine ->
            val line = sanitizeLrcLine(rawLine)
            if (line.isEmpty() || LRC_METADATA_PATTERN.matcher(line).matches()) return@forEach

            val lineMatcher = LRC_LINE_REGEX.matcher(line)
            if (lineMatcher.matches()) {
                isSynced = true
                val minutes = lineMatcher.group(1)?.toLong() ?: 0
                val seconds = lineMatcher.group(2)?.toLong() ?: 0
                val fraction = lineMatcher.group(3)?.toLong() ?: 0
                val textWithTags = stripFormatCharacters(lineMatcher.group(4)?.trim() ?: "")
                val text = stripLrcTimestamps(textWithTags)

                val millis = if (lineMatcher.group(3)?.length == 2) fraction * 10 else fraction
                val lineTimestamp = minutes * 60 * 1000 + seconds * 1000 + millis

                // Enhanced word-by-word parsing
                if (text.contains(LRC_WORD_TAG_REGEX)) {
                    val words = mutableListOf<SyncedWord>()
                    val parts = text.split(LRC_WORD_SPLIT_REGEX)
                    val displayText = LRC_WORD_TAG_REGEX.replace(text, "")
                    var pendingWordBoundary = false

                    for (part in parts) {
                        if (part.isEmpty()) continue
                        val wordMatcher = LRC_WORD_REGEX.matcher(part)
                        if (wordMatcher.find()) {
                            val wordMinutes = wordMatcher.group(1)?.toLong() ?: 0
                            val wordSeconds = wordMatcher.group(2)?.toLong() ?: 0
                            val wordFraction = wordMatcher.group(3)?.toLong() ?: 0
                            val wordText = stripFormatCharacters(wordMatcher.group(4) ?: "")
                            val timedWordTextRaw = wordText
                                .substringBefore('\n')
                                .substringBefore('\r')
                                .substringBefore("\\n")
                                .substringBefore("\\r")
                            val startsNewWord = words.isEmpty() ||
                                pendingWordBoundary ||
                                timedWordTextRaw.firstOrNull()?.isWhitespace() == true
                            val timedWordText = timedWordTextRaw.trim()
                            pendingWordBoundary = timedWordTextRaw.lastOrNull()?.isWhitespace() == true
                            val wordMillis = if (wordMatcher.group(3)?.length == 2) wordFraction * 10 else wordFraction
                            val wordTimestamp = wordMinutes * 60 * 1000 + wordSeconds * 1000 + wordMillis
                            if (timedWordText.isNotEmpty()) {
                                words.add(
                                    SyncedWord(
                                        time = wordTimestamp.toInt(),
                                        word = timedWordText,
                                        startsNewWord = startsNewWord
                                    )
                                )
                            }
                        } else {
                            // Preserve only leading untagged text as a timed word.
                            // Trailing untagged chunks (e.g. inline translations) should remain visible in line text
                            // but must not steal word highlight timing.
                            if (words.isEmpty()) {
                                val leading = stripFormatCharacters(part)
                                val startsNewWord = pendingWordBoundary || leading.firstOrNull()?.isWhitespace() == true
                                val visibleLeading = leading.trim()
                                pendingWordBoundary = leading.lastOrNull()?.isWhitespace() == true
                                if (visibleLeading.isNotEmpty()) {
                                    words.add(
                                        SyncedWord(
                                            time = lineTimestamp.toInt(),
                                            word = visibleLeading,
                                            startsNewWord = words.isEmpty() || startsNewWord
                                        )
                                    )
                                }
                            } else if (part.any { it.isWhitespace() }) {
                                pendingWordBoundary = true
                            }
                        }
                    }

                    if (words.isNotEmpty()) {
                        syncedLines.add(SyncedLine(lineTimestamp.toInt(), displayText, words))
                    } else {
                        syncedLines.add(SyncedLine(lineTimestamp.toInt(), displayText))
                    }
                } else {
                    syncedLines.add(SyncedLine(lineTimestamp.toInt(), text))
                }
            } else {
                // Line WITHOUT timestamp
                val stripped = stripLrcTimestamps(stripFormatCharacters(line))
                // If the file was already detected as synced and at least one SyncedLine
                // exists, treat this line as a continuation of the previous one.
                if (isSynced && syncedLines.isNotEmpty()) {
                    val last = syncedLines.removeAt(syncedLines.lastIndex)
                    // Keep the previous text and append the new line with a newline break.
                    val mergedLineText = if (last.line.isEmpty()) {
                        stripped
                    } else {
                        last.line + "\n" + stripped
                    }
                    // Preserve the existing synced word list if present.
                    val merged = if (last.words?.isNotEmpty() == true) {
                        SyncedLine(last.time, mergedLineText, last.words)
                    } else {
                        SyncedLine(last.time, mergedLineText)
                    }

                    syncedLines.add(merged)
                } else {
                    // No sync markers found — treat as plain text.
                    plainLines.add(stripped)
                }
            }
        }

        return if (isSynced && syncedLines.isNotEmpty()) {
            val sortedSyncedLines = syncedLines.sortedBy { it.time }
            val pairedLines = pairTranslationLines(sortedSyncedLines).map { line ->

                val romanized = when {
                    MultiLangRomanizer.isJapanese(line.line) -> MultiLangRomanizer.romanizeJapanese(line.line)
                    MultiLangRomanizer.isChinese(line.line) -> MultiLangRomanizer.romanizeChinese(line.line)
                    MultiLangRomanizer.isKorean(line.line) -> MultiLangRomanizer.romanizeKorean(line.line)
                    MultiLangRomanizer.isHindi(line.line) -> MultiLangRomanizer.romanizeHindi(line.line)
                    MultiLangRomanizer.isPunjabi(line.line) -> MultiLangRomanizer.romanizePunjabi(line.line)
                    MultiLangRomanizer.isCyrillic(line.line) -> MultiLangRomanizer.romanizeCyrillic(line.line)
                    else -> null
                }?.capitalizeFirstLetter()?.trim()

                line.copy(romanization = romanized)
            }
            val plainVersion = pairedLines.map { line ->
                buildString {
                    append(line.line)
                    if (!line.romanization.isNullOrEmpty()) append("\n").append(line.romanization)
                    if (!line.translation.isNullOrEmpty()) append("\n").append(line.translation)
                }
            }
            Lyrics(synced = pairedLines, plain = plainVersion)
        } else {
            val processedPlain = plainLines.map { line ->
                val romanized = when {
                    MultiLangRomanizer.isJapanese(line) -> MultiLangRomanizer.romanizeJapanese(line)
                    MultiLangRomanizer.isChinese(line) -> MultiLangRomanizer.romanizeChinese(line)
                    MultiLangRomanizer.isKorean(line) -> MultiLangRomanizer.romanizeKorean(line)
                    MultiLangRomanizer.isHindi(line) -> MultiLangRomanizer.romanizeHindi(line)
                    MultiLangRomanizer.isPunjabi(line) -> MultiLangRomanizer.romanizePunjabi(line)
                    MultiLangRomanizer.isCyrillic(line) -> MultiLangRomanizer.romanizeCyrillic(line)
                    else -> null
                }?.capitalizeFirstLetter()?.trim()

                if (!romanized.isNullOrEmpty()) "$line\n$romanized" else line
            }
            Lyrics(plain = processedPlain)
        }
    }

    // ── Kugou / Paxsenix word-by-word helpers ─────────────────────────────

    /**
     * Returns true when the raw text contains at least one line in Kugou format:
     *   [lineStartMs,lineDurationMs]…
     * where lineStartMs > 999 (milliseconds, not an LRC minute value).
     */
    private fun looksLikeKugouFormat(text: String): Boolean {
        return text.lines().any { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@any false
            // Skip metadata tags like [ti:…] [ar:…] [offset:…]
            if (LRC_METADATA_PATTERN.matcher(line).matches()) return@any false
            val m = KUGOU_LINE_REGEX.matcher(line)
            m.matches() && (m.group(1)?.toLongOrNull() ?: 0L) > 999L
        }
    }

    /**
     * Parses a Kugou / Paxsenix word-by-word LRC file into [Lyrics].
     *
     * Line format:
     *   [lineStartMs,lineDurationMs]<wordOffset1Ms,dur,0>word1<wordOffset2Ms,dur,0>word2…
     *
     * Word offsets are *relative* to the line start.
     * A word token with offset 0 and no preceding tokens means the token is the
     * line itself; subsequent 0-offset tokens are continuations (startsNewWord = false).
     *
     * An optional [offset:N] header (milliseconds) shifts all timestamps.
     */
    private fun parseKugouLyrics(text: String): Lyrics {
        val globalOffsetMs = text.lines()
            .firstOrNull { it.trim().startsWith("[offset:", ignoreCase = true) }
            ?.trim()
            ?.removePrefix("[offset:")
            ?.removeSuffix("]")
            ?.trim()
            ?.toLongOrNull() ?: 0L

        val syncedLines = mutableListOf<SyncedLine>()

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || LRC_METADATA_PATTERN.matcher(line).matches()) continue

            val headerMatcher = KUGOU_LINE_REGEX.matcher(line)
            if (!headerMatcher.matches()) continue
            val lineStartMs = (headerMatcher.group(1)?.toLongOrNull() ?: continue) + globalOffsetMs
            if (lineStartMs <= 999L && globalOffsetMs == 0L) continue // guard: not a Kugou line
            val body = headerMatcher.group(3) ?: ""

            // Build word list
            val words = mutableListOf<SyncedWord>()
            val wordMatcher = KUGOU_WORD_PATTERN.matcher(body)
            var previousEndsWithSpace = true // first word always starts a new word

            while (wordMatcher.find()) {
                val wordOffsetMs = wordMatcher.group(1)?.toLongOrNull() ?: 0L
                // group(2) = duration, group(3) = flags — both unused for display
                val rawText = wordMatcher.group(4) ?: ""

                val startsNew = previousEndsWithSpace || rawText.firstOrNull()?.isWhitespace() == true
                val wordText = rawText.trim()
                previousEndsWithSpace = rawText.lastOrNull()?.isWhitespace() == true

                if (wordText.isEmpty()) continue

                words.add(
                    SyncedWord(
                        time = (lineStartMs + wordOffsetMs).toInt(),
                        word = wordText,
                        startsNewWord = startsNew
                    )
                )
            }

            // Plain text: strip all <…> tokens
            val plainText = KUGOU_WORD_PATTERN.toRegex().replace(body) { it.groupValues[4] }.trim()
            if (plainText.isEmpty() && words.isEmpty()) continue

            syncedLines.add(
                SyncedLine(
                    time = lineStartMs.toInt(),
                    line = plainText,
                    words = words.takeIf { it.isNotEmpty() }
                )
            )
        }

        if (syncedLines.isEmpty()) return Lyrics(plain = emptyList(), synced = emptyList())

        val sorted = syncedLines.sortedBy { it.time }
        val paired = pairTranslationLines(sorted)
        val plainVersion = paired.map { it.line }
        return Lyrics(synced = paired, plain = plainVersion, areFromRemote = false)
    }

    /**
     * Pairs consecutive synced lines that share the same timestamp.
     * The second line is treated as a translation of the first.
     * Only pairs one translation per original — a third line at the same timestamp stays separate.
     */
    internal fun pairTranslationLines(lines: List<SyncedLine>): List<SyncedLine> {
        if (lines.size < 2) return lines
        val result = mutableListOf<SyncedLine>()
        var i = 0
        while (i < lines.size) {
            val current = lines[i]
            val next = lines.getOrNull(i + 1)
            if (next != null && next.time == current.time && current.translation == null && current.line.isNotBlank() && next.line.isNotBlank()) {
                val translationParts = mutableListOf(next.line)
                var consumed = 2
                while (true) {
                    val trailing = lines.getOrNull(i + consumed) ?: break
                    if (trailing.time != current.time || !isTranslationCreditLine(trailing.line)) break
                    translationParts.add(trailing.line)
                    consumed++
                }
                result.add(current.copy(translation = translationParts.joinToString("\n")))
                i += consumed
            } else {
                result.add(current)
                i++
            }
        }
        return result
    }

    internal fun stripLrcTimestamps(value: String): String {
        if (value.isEmpty()) return value
        val withoutTags = LRC_TIMESTAMP_TAG_REGEX.replace(value, "")
        return withoutTags.trimStart()
    }

    internal fun isTranslationCreditLine(line: String): Boolean {
        val normalized = stripLrcTimestamps(line).trim()
        return normalized.isNotEmpty() && TRANSLATION_CREDIT_REGEX.matches(normalized)
    }

    /**
     * Converts synced lyrics to LRC format string.
     * Each line is formatted as [mm:ss.xx]text
     * @param syncedLines The list of synced lines to convert.
     * @return A string in LRC format.
     */
    fun syncedToLrcString(syncedLines: List<SyncedLine>): String {
        return syncedLines.sortedBy { it.time }.flatMap { line ->
            val totalMs = line.time
            val minutes = totalMs / 60000
            val seconds = (totalMs % 60000) / 1000
            val hundredths = (totalMs % 1000) / 10
            val timestamp = "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
            buildList {
                add("$timestamp${line.line}")
                if (!line.translation.isNullOrBlank()) {
                    line.translation
                        .lines()
                        .filter { it.isNotBlank() }
                        .forEach { translationLine ->
                            add("$timestamp$translationLine")
                        }
                }
            }
        }.joinToString("\n")
    }

    /**
     * Converts plain lyrics (list of lines) to a plain text string.
     * Strips any auto-generated romanization suffix (after '\n') before storage.
     * @param plainLines The list of plain text lines.
     * @return A string with each line separated by a newline.
     */
    fun plainToString(plainLines: List<String>): String {
        // Strip auto-generated romanization (if present after \n) when converting back to string for storage.
        return plainLines.joinToString("\n") { it.substringBefore('\n') }
    }

    /**
     * Converts Lyrics object to LRC or plain text format based on available data.
     * Prefers synced lyrics if available.
     * @param lyrics The Lyrics object to convert.
     * @param preferSynced Whether to prefer synced lyrics over plain. Default true.
     * @return A string representation of the lyrics.
     */
    fun toLrcString(lyrics: Lyrics, preferSynced: Boolean = true): String {
        return if (preferSynced && !lyrics.synced.isNullOrEmpty()) {
            syncedToLrcString(lyrics.synced)
        } else if (!lyrics.plain.isNullOrEmpty()) {
            plainToString(lyrics.plain)
        } else if (!lyrics.synced.isNullOrEmpty()) {
            syncedToLrcString(lyrics.synced)
        } else {
            ""
        }
    }
}

private fun stripLeadingLyricsDocumentNoise(value: String): String {
    return value.trimStart { char ->
        char.isWhitespace() ||
            char == '\uFEFF' ||
            Character.getType(char).toByte() == Character.FORMAT
    }
}

private fun looksLikeTtmlDocument(value: String): Boolean {
    if (value.startsWith("<tt", ignoreCase = true)) {
        return true
    }
    if (!value.startsWith("<?xml", ignoreCase = true)) {
        return false
    }

    val afterDeclaration = value.substringAfter("?>", missingDelimiterValue = "")
    return afterDeclaration
        .trimStart()
        .startsWith("<tt", ignoreCase = true)
}

private fun sanitizeLrcLine(rawLine: String): String {
    if (rawLine.isEmpty()) return rawLine

    val withoutTerminators = rawLine
        .trimEnd('\r', '\n')
        .filterNot { char ->
            Character.getType(char).toByte() == Character.FORMAT ||
                (Character.isISOControl(char) && char != '\t')
        }
        .trimEnd('\uFEFF')

    val trimmedPrefix = withoutTerminators.trimStart { it.isWhitespace() }
    val firstBracket = trimmedPrefix.indexOf('[')
    return if (firstBracket > 0) {
        trimmedPrefix.substring(firstBracket)
    } else {
        trimmedPrefix
    }
}

private fun stripFormatCharacters(value: String): String {
    val cleaned = value.filterNot { char ->
        char.category == CharCategory.FORMAT ||
            (char.isISOControl() && char != '\t')
    }

    return when (cleaned) {
        "\"", "'" -> ""
        else -> cleaned
    }
}

@Composable
fun ProviderText(
    providerText: String,
    uri: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    accentColor: Color? = null
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = accentColor ?: MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = textColor)) {
            append(providerText)
        }
        withLink(
            LinkAnnotation.Url(
                url = uri,
                styles = TextLinkStyles(style = SpanStyle(color = linkColor))
            )
        ) {
            append(" LRCLIB")
        }
    }

    val baseStyle = MaterialTheme.typography.bodySmall
    val finalStyle = textAlign?.let { baseStyle.copy(textAlign = it) } ?: baseStyle

    Text(
        text = annotatedString,
        style = finalStyle,
        modifier = modifier
    )
}

/**
 * A composable that displays a row of animated bubbles that morph into
 * musical notes as they rise and back into circles as they fall.
 *
 * @param positionFlow A flow emitting the current playback position in ms.
 * @param time The start time at which these bubbles become visible.
 * @param color The base colour for the bubbles and notes.
 * @param nextTime The end time at which these bubbles disappear.
 * @param modifier Modifier applied to this layout.
 */
@Composable
fun BubblesLine(
    positionFlow: Flow<Long>,
    time: Int,
    color: Color,
    nextTime: Int,
    modifier: Modifier = Modifier,
) {
    val position by positionFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val isCurrent = position in time until nextTime
    val transition = rememberInfiniteTransition(label = "bubbles_transition")

    // Animación ralentizada para apreciar mejor el efecto.
    val animatedValue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "bubble_animation_progress"
    )

    var show by remember { mutableStateOf(false) }
    LaunchedEffect(isCurrent) {
        show = isCurrent
    }

    if (show) {
        val density = LocalDensity.current
        // Smaller circles to accentuate the scale animation.
        val bubbleRadius = remember(density) { with(density) { 4.dp.toPx() } }

        val (morphableCircle, morphableNote) = remember(bubbleRadius) {
            val circleNodes = createCirclePathNodes(radius = bubbleRadius)
            val noteNodes = createVectorNotePathNodes(targetSize = bubbleRadius * 2.5f)

            makePathsCompatible(circleNodes, noteNodes)
            circleNodes to noteNodes
        }

        Canvas(modifier = modifier.size(64.dp, 48.dp)) {
            val bubbleCount = 3
            val bubbleColor = color.copy(alpha = 0.7f)

            for (i in 0 until bubbleCount) {
                val progress = (animatedValue + i * (1f / bubbleCount)) % 1f
                val yOffset = sin(progress * 2 * PI).toFloat() * 8.dp.toPx()

                val morphProgress = when {
                    progress in 0f..0.25f -> progress / 0.25f
                    progress in 0.25f..0.5f -> 1.0f - (progress - 0.25f) / 0.25f
                    else -> 0f
                }.coerceIn(0f, 1f)

                // Scale animation is more pronounced.
                val scale = lerpFloat(1.0f, 1.4f, morphProgress)

                // Dynamic horizontal offset that activates with morphing.
                val xOffsetCorrection = lerpFloat(0f, bubbleRadius * 1.8f, morphProgress)

                val morphedPath = lerpPath(
                    start = morphableCircle,
                    stop = morphableNote,
                    fraction = morphProgress
                ).toPath()

                // Position the animation container in its column.
                translate(left = (size.width / (bubbleCount + 1)) * (i + 1)) {
                    // Apply vertical offset (wave) and horizontal correction.
                    val drawOffset = Offset(x = xOffsetCorrection, y = size.height / 2 + yOffset)

                    translate(left = drawOffset.x, top = drawOffset.y) {
                        // Apply the scale transform before drawing.
                        scale(scale = scale, pivot = Offset.Zero) {
                            drawPath(
                                path = morphedPath,
                                color = bubbleColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Path Morphing Logic ---

private fun lerpPath(start: List<PathNode>, stop: List<PathNode>, fraction: Float): List<PathNode> {
    return start.mapIndexed { index, startNode ->
        val stopNode = stop[index]
        when (startNode) {
            is PathNode.MoveTo -> {
                val stopMoveTo = stopNode as PathNode.MoveTo
                PathNode.MoveTo(
                    lerpFloat(startNode.x, stopMoveTo.x, fraction),
                    lerpFloat(startNode.y, stopMoveTo.y, fraction)
                )
            }
            is PathNode.CurveTo -> {
                val stopCurveTo = stopNode as PathNode.CurveTo
                PathNode.CurveTo(
                    lerpFloat(startNode.x1, stopCurveTo.x1, fraction),
                    lerpFloat(startNode.y1, stopCurveTo.y1, fraction),
                    lerpFloat(startNode.x2, stopCurveTo.x2, fraction),
                    lerpFloat(startNode.y2, stopCurveTo.y2, fraction),
                    lerpFloat(startNode.x3, stopCurveTo.x3, fraction),
                    lerpFloat(startNode.y3, stopCurveTo.y3, fraction)
                )
            }
            else -> startNode
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun List<PathNode>.toPath(): Path = Path().apply {
    this@toPath.forEach { node ->
        when (node) {
            is PathNode.MoveTo -> moveTo(node.x, node.y)
            is PathNode.LineTo -> lineTo(node.x, node.y)
            is PathNode.CurveTo -> cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
            is PathNode.Close -> close()
            else -> {}
        }
    }
}

private fun makePathsCompatible(nodes1: MutableList<PathNode>, nodes2: MutableList<PathNode>): Pair<MutableList<PathNode>, MutableList<PathNode>> {
    while (nodes1.size < nodes2.size) {
        nodes1.add(nodes1.size - 1, nodes1[nodes1.size - 2])
    }
    while (nodes2.size < nodes1.size) {
        nodes2.add(nodes2.size - 1, nodes2[nodes2.size - 2])
    }
    return nodes1 to nodes2
}

private fun createVectorNotePathNodes(targetSize: Float): MutableList<PathNode> {
    val pathData = "M239.5,1.9c-4.6,1.1 -8.7,3.6 -12.2,7.3 -6.7,6.9 -6.3,-2.5 -6.3,151.9 0,76.9 -0.3,140 -0.7,140.2 -0.5,0.3 -4.2,-0.9 -8.3,-2.5 -48.1,-19.3 -102.8,-8.3 -138.6,27.7 -35.8,36.1 -41.4,85.7 -13.6,120.7 18.6,23.4 52.8,37.4 86.2,35.3 34.8,-2.1 65.8,-16 89.5,-39.9 14.5,-14.6 24.9,-31.9 30.7,-50.6l2.3,-7.5 0.2,-133c0.2,-73.2 0.5,-133.6 0.8,-134.2 0.8,-2.4 62,28.5 84.3,42.4 22.4,14.1 34.1,30.4 37.2,51.9 2.4,16.5 -2.2,34.5 -13,50.9 -6,9.1 -7,12.1 -4.8,14.3 2.2,2.2 5.3,1.2 13.8,-4.5 26.4,-17.9 45.6,-48 50,-78.2 1.9,-12.9 0.8,-34.3 -2.4,-46.1 -8.7,-31.7 -30.4,-58 -64.1,-77.8 -64.3,-37.9 -116,-67.3 -119.6,-68.1 -5,-1.2 -7.1,-1.2 -11.4,-0.2z"
    val parser = PathParser().parsePathString(pathData)

    val groupScale = 0.253f
    val bounds = Path().apply { parser.toPath(this) }.getBounds()
    val maxDimension = max(bounds.width, bounds.height)
    val scale = if (maxDimension > 0f) targetSize / (maxDimension * groupScale) else 1f

    val matrix = Matrix()
    matrix.translate(x = -bounds.left, y = -bounds.top)
    matrix.scale(x = groupScale * scale, y = groupScale * scale)
    val finalWidth = bounds.width * groupScale * scale
    val finalHeight = bounds.height * groupScale * scale

    // Center the path at origin (0,0) without static corrections.
    matrix.translate(x = -finalWidth / 2f, y = -finalHeight / 2f)

    return parser.toNodes().toAbsolute().transform(matrix).toCurvesOnly()
}

private fun createCirclePathNodes(radius: Float): MutableList<PathNode> {
    val kappa = 0.552284749831f
    val rk = radius * kappa
    return mutableListOf(
        PathNode.MoveTo(0f, -radius),
        PathNode.CurveTo(rk, -radius, radius, -rk, radius, 0f),
        PathNode.CurveTo(radius, rk, rk, radius, 0f, radius),
        PathNode.CurveTo(-rk, radius, -radius, rk, -radius, 0f),
        PathNode.CurveTo(-radius, -rk, -rk, -radius, 0f, -radius),
        PathNode.Close
    )
}

// --- PathNode Extension Functions ---

private fun List<PathNode>.toAbsolute(): MutableList<PathNode> {
    val absoluteNodes = mutableListOf<PathNode>()
    var currentX = 0f
    var currentY = 0f
    this.forEach { node ->
        when (node) {
            is PathNode.MoveTo -> { currentX = node.x; currentY = node.y; absoluteNodes.add(node) }
            is PathNode.RelativeMoveTo -> { currentX += node.dx; currentY += node.dy; absoluteNodes.add(PathNode.MoveTo(currentX, currentY)) }
            is PathNode.LineTo -> { currentX = node.x; currentY = node.y; absoluteNodes.add(node) }
            is PathNode.RelativeLineTo -> { currentX += node.dx; currentY += node.dy; absoluteNodes.add(PathNode.LineTo(currentX, currentY)) }
            is PathNode.CurveTo -> { currentX = node.x3; currentY = node.y3; absoluteNodes.add(node) }
            is PathNode.RelativeCurveTo -> {
                absoluteNodes.add(PathNode.CurveTo(currentX + node.dx1, currentY + node.dy1, currentX + node.dx2, currentY + node.dy2, currentX + node.dx3, currentY + node.dy3))
                currentX += node.dx3; currentY += node.dy3
            }
            is PathNode.Close -> absoluteNodes.add(node)
            else -> {}
        }
    }
    return absoluteNodes
}

private fun MutableList<PathNode>.toCurvesOnly(): MutableList<PathNode> {
    val curveNodes = mutableListOf<PathNode>()
    var lastX = 0f
    var lastY = 0f

    this.forEach { node ->
        when(node) {
            is PathNode.MoveTo -> { curveNodes.add(node); lastX = node.x; lastY = node.y }
            is PathNode.LineTo -> { curveNodes.add(PathNode.CurveTo(lastX, lastY, node.x, node.y, node.x, node.y)); lastX = node.x; lastY = node.y }
            is PathNode.CurveTo -> { curveNodes.add(node); lastX = node.x3; lastY = node.y3 }
            is PathNode.Close -> curveNodes.add(node)
            else -> {}
        }
    }
    return curveNodes
}

private fun List<PathNode>.transform(matrix: Matrix): MutableList<PathNode> {
    return this.map { node ->
        when (node) {
            is PathNode.MoveTo -> {
                val p = matrix.map(Offset(node.x, node.y))
                PathNode.MoveTo(p.x, p.y)
            }
            is PathNode.LineTo -> {
                val p = matrix.map(Offset(node.x, node.y))
                PathNode.LineTo(p.x, p.y)
            }
            is PathNode.CurveTo -> {
                val p1 = matrix.map(Offset(node.x1, node.y1))
                val p2 = matrix.map(Offset(node.x2, node.y2))
                val p3 = matrix.map(Offset(node.x3, node.y3))
                PathNode.CurveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            }
            else -> node
        }
    }.toMutableList()
}