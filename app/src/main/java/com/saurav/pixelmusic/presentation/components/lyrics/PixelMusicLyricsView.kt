package com.saurav.pixelmusic.presentation.components.lyrics

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.lyrics.GoogleTranslateEngine
import com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences
import com.saurav.pixelmusic.lyrics.PixelMusicRomanizer
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.AppMotion
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LocalLyricsHighlight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsAlignment
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsBackdropBlend
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsEntry
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsGlowLine
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsInterludeDots
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsLineState
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsRecenterDurationMs
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsRecenterEasing
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsSmoothRecenterEasing
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsStagger
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.MinInterludeGapMs
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.RubyPlacement
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.SoftLightSupported
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.WordTimestamp
import com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.lyricsEdgeFade
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

// ---- PixelMusic's lyrics page numbers, unchanged ----

private const val ActiveLineViewportFraction = 0.055f
private val MinPastLineVisibleHeight = 72.dp
private const val IdleRecenterDelayMs = 3_500L
private const val TopFadeFraction = 0.10f
private const val BottomFadeStart = 0.62f
private const val NoInterlude = Int.MIN_VALUE
private const val LineLeadInMs = 220L
private const val UntimedLineDurationMs = 5_000L

private fun recenterEasingFor(risesWords: Boolean) =
    if (risesWords) LyricsSmoothRecenterEasing else LyricsRecenterEasing

private class LyricsListPadding : PaddingValues {
    var heightPx = 0
    var density = 1f
    private fun listHeight() = (heightPx / density).dp
    override fun calculateTopPadding() = maxOf(listHeight() * ActiveLineViewportFraction, MinPastLineVisibleHeight)
    override fun calculateBottomPadding() = listHeight() * 0.55f
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
}

// ---- PixelMusic lyrics -> PixelMusic's entries ----

private fun SyncedLine.toPixelMusicEntry(
    primaryText: String,
    useWords: Boolean,
    nextLineTimeMs: Int?,
): LyricsEntry {
    val pieces = words
    if (!useWords || pieces.isNullOrEmpty()) {
        return LyricsEntry(time = time.toLong(), text = primaryText, agent = agent, isBackground = isBackground)
    }
    val lastStartMs = pieces.last().time
    val avgWordMs = if (pieces.size >= 2) {
        ((lastStartMs - pieces.first().time) / (pieces.size - 1)).coerceIn(280, 900)
    } else {
        650
    }
    val fallbackLastEndMs = when {
        nextLineTimeMs != null && nextLineTimeMs > lastStartMs -> {
            val gapToNext = nextLineTimeMs - lastStartMs
            lastStartMs + gapToNext.coerceIn(260, maxOf(avgWordMs * 2, 1_800))
        }
        else -> lastStartMs + maxOf(avgWordMs, 900)
    }
    val timed = pieces.mapIndexed { i, w ->
        val startMs = w.time
        val nextStartMs = pieces.getOrNull(i + 1)?.time
        val rawEndMs = w.endTime?.takeIf { it > startMs }
            ?: nextStartMs?.takeIf { it > startMs }
            ?: if (i == pieces.lastIndex) fallbackLastEndMs else (startMs + avgWordMs)
        val start = startMs / 1000.0
        val end = rawEndMs.coerceAtLeast(startMs + 120) / 1000.0
        WordTimestamp(text = w.word, startTime = start, endTime = end)
    }
    // pieces the source split into syllables are merged into whole words, the pieces themselves kept for the sweep
    val merged = ArrayList<WordTimestamp>(timed.size)
    var anySplit = false
    pieces.forEachIndexed { i, w ->
        if (i > 0 && !w.startsNewWord && merged.isNotEmpty()) {
            anySplit = true
            val last = merged.removeAt(merged.lastIndex)
            merged.add(last.copy(text = last.text + timed[i].text, endTime = timed[i].endTime))
        } else {
            merged.add(timed[i])
        }
    }
    return LyricsEntry(
        time = time.toLong(),
        text = primaryText,
        words = merged,
        agent = agent,
        isBackground = isBackground,
        syllables = if (anySplit) timed else null,
    )
}

private fun lineEndMs(lines: List<LyricsEntry>, index: Int): Long {
    val entry = lines.getOrNull(index) ?: return 0L
    entry.words?.lastOrNull()?.let { return (it.endTime * 1000).toLong() }
    lines.getOrNull(index + 1)?.let { return it.time }
    return entry.time + UntimedLineDurationMs
}

private fun findCurrentLineIndex(lines: List<LyricsEntry>, position: Long): Int {
    if (lines.isEmpty()) return -1
    if (position >= lineEndMs(lines, lines.lastIndex)) return lines.size
    for (index in lines.indices) {
        if (lines[index].time >= position + LineLeadInMs) return index - 1
    }
    return lines.lastIndex
}

private fun lyricAnchorIndex(lines: List<LyricsEntry>, positionMs: Long): Int {
    var i = findCurrentLineIndex(lines, positionMs)
    if (i <= 0 || i >= lines.size) return i
    while (i > 0) {
        val previous = i - 1
        val endMs = lines[previous].words?.lastOrNull()?.let { (it.endTime * 1000).toLong() } ?: return i
        if (endMs <= positionMs) return i
        i = previous
    }
    return i
}

private sealed interface LyricRow {
    val key: Long
    data class Line(val index: Int, val entry: LyricsEntry) : LyricRow {
        override val key get() = index.toLong()
    }
    data class Interlude(val afterIndex: Int, val startMs: Long, val endMs: Long) : LyricRow {
        override val key get() = -1_000_000L - afterIndex
    }
}

private fun buildLyricRows(lines: List<LyricsEntry>): List<LyricRow> {
    if (lines.isEmpty()) return emptyList()
    val rows = ArrayList<LyricRow>(lines.size + 4)
    if (lines.first().time >= MinInterludeGapMs) {
        rows.add(LyricRow.Interlude(afterIndex = -1, startMs = 0L, endMs = lines.first().time))
    }
    for (index in lines.indices) {
        rows.add(LyricRow.Line(index, lines[index]))
        val next = lines.getOrNull(index + 1) ?: continue
        val gapStart = lineEndMs(lines, index)
        if (next.time - gapStart >= MinInterludeGapMs) {
            rows.add(LyricRow.Interlude(afterIndex = index, startMs = gapStart, endMs = next.time))
        }
    }
    return rows
}

private fun activeInterludeOf(rows: List<LyricRow>, positionMs: Long): Int =
    rows.firstOrNull {
        it is LyricRow.Interlude && positionMs >= it.startMs && positionMs + LineLeadInMs < it.endMs
    }?.let { (it as LyricRow.Interlude).afterIndex } ?: NoInterlude

private fun usesAgentAlignment(lines: List<LyricsEntry>): Boolean =
    lines.filter { !it.isBackground }.map { it.agent ?: "v1" }.distinct().size > 1

private data class LineLook(val state: LyricsLineState, val blurStage: Int)

private fun lineLook(index: Int, current: Int, anchor: Int, interlude: Boolean): LineLook {
    val distance = when {
        current < 0 -> index - current
        index < anchor -> index - anchor
        index > current -> index - current
        else -> 0
    }
    val state = when {
        current >= 0 && index in anchor..current ->
            if (interlude) LyricsLineState.PAST else LyricsLineState.ACTIVE
        index == current + 1 -> LyricsLineState.UPCOMING
        index < current -> LyricsLineState.PAST
        else -> LyricsLineState.DEFAULT
    }
    val blurStage = when (abs(distance)) {
        0 -> 0
        1 -> 1
        else -> 2
    }
    return LineLook(state, blurStage)
}

private fun alignmentFor(entry: LyricsEntry, useAgentAlignment: Boolean, mirrored: Boolean): LyricsAlignment {
    val side = when {
        !useAgentAlignment -> LyricsAlignment.START
        entry.isBackground -> LyricsAlignment.CENTER
        entry.agent == "v2" -> LyricsAlignment.END
        entry.agent == "v1000" -> LyricsAlignment.CENTER
        else -> LyricsAlignment.START
    }
    if (!mirrored) return side
    return when (side) {
        LyricsAlignment.START -> LyricsAlignment.END
        LyricsAlignment.END -> LyricsAlignment.START
        LyricsAlignment.CENTER -> LyricsAlignment.CENTER
    }
}

private fun lyricsReadRtl(lines: List<LyricsEntry>): Boolean {
    var rtl = 0
    var ltr = 0
    for (line in lines) {
        for (ch in line.text) {
            when (Character.getDirectionality(ch)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> { rtl++; break }
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> { ltr++; break }
            }
        }
    }
    return rtl > ltr
}

/**
 * Smoothly interpolates playback position between the player's 250ms ticks so word-by-word sweeps
 * and GPU shaders glide at 60–120fps without freezing or jumping between polls.
 */
private class LyricsClock(
    private val sample: () -> Long,
    private val isPlaying: () -> Boolean,
) {
    private var lastSample = Long.MIN_VALUE
    private var anchorMs = 0L
    private var anchorNanos = 0L

    fun now(): Long {
        val value = sample()
        val t = System.nanoTime()
        if (!isPlaying()) {
            lastSample = value
            anchorMs = value
            anchorNanos = t
            return value
        }
        if (lastSample == Long.MIN_VALUE) {
            lastSample = value
            anchorMs = value
            anchorNanos = t
            return value
        }
        val extrapolated = anchorMs + ((t - anchorNanos) / 1_000_000L).coerceIn(0L, 350L)
        if (value != lastSample) {
            lastSample = value
            val drift = value - extrapolated
            // Blend small poll jitter smoothly; snap immediately on real seeks or track jumps
            anchorMs = if (abs(drift) <= 60L) extrapolated + (drift / 2L) else value
            anchorNanos = t
            return anchorMs
        }
        return extrapolated
    }
}

/**
 * The lyrics page, PixelMusic's own: the four PixelMusic styles (Style 1, Style 2, Boring, Plain) run on PixelMusic's
 * renderer, rows, scrolling and cascade. The fifth, PixelMusic's, is drawn by the caller instead.
 */
data class LiveLyricsEnrichment(
    val liveTranslations: List<String>?,
    val romanized: List<String?>,
    val autoTranslate: Boolean,
    val autoRomanize: Boolean,
    val romanizationAsMain: Boolean,
)

@Composable
fun rememberLiveLyricsEnrichment(
    lines: List<SyncedLine>,
    showRomanizationPref: Boolean,
): LiveLyricsEnrichment {
    val context = androidx.compose.ui.platform.LocalContext.current
    val autoTranslate by remember(context) { PixelMusicLyricsPreferences.autoTranslateFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)
    val translateLang by remember(context) { PixelMusicLyricsPreferences.translateLanguageFlow(context) }
        .collectAsStateWithLifecycle(initialValue = "en")
    val useAiTranslation by remember(context) { PixelMusicLyricsPreferences.useAiTranslationFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)
    val autoRomanize by remember(context) { PixelMusicLyricsPreferences.autoRomanizeFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)
    val romanizationAsMain by remember(context) { PixelMusicLyricsPreferences.romanizationAsMainFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)

    // live translation (AI first when enabled, falling back to Google/Bing) and romanisation
    var liveTranslations by remember(lines, translateLang, autoTranslate, useAiTranslation) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(lines, translateLang, autoTranslate, useAiTranslation) {
        liveTranslations = if (autoTranslate && lines.isNotEmpty()) {
            val lineTexts = lines.map { it.line }
            val aiResult = if (useAiTranslation) {
                val langName = PixelMusicLyricsPreferences.languageDisplayName(translateLang)
                com.saurav.pixelmusic.lyrics.AiLyricsTranslator.translate(
                    context = context,
                    lines = lineTexts,
                    targetLanguageName = langName,
                )
            } else null
            aiResult ?: GoogleTranslateEngine.translate(lineTexts, targetLanguage = translateLang)
        } else null
    }
    var liveRomanizations by remember(lines, autoRomanize) { mutableStateOf<List<String?>?>(null) }
    LaunchedEffect(lines, autoRomanize) {
        liveRomanizations = if (autoRomanize && lines.isNotEmpty()) {
            val missingIndices = lines.indices.filter { i ->
                lines[i].romanization.isNullOrBlank() && PixelMusicRomanizer.hasNonLatinScript(lines[i].line)
            }
            if (missingIndices.isEmpty()) {
                null
            } else {
                val fetched = GoogleTranslateEngine.romanize(missingIndices.map { lines[it].line })
                if (fetched != null && fetched.size == missingIndices.size) {
                    MutableList<String?>(lines.size) { null }.also { out ->
                        missingIndices.forEachIndexed { idx, lineIndex ->
                            out[lineIndex] = fetched[idx]
                        }
                    }
                } else null
            }
        } else null
    }
    val romanized = remember(lines, autoRomanize, showRomanizationPref, liveRomanizations) {
        if (!autoRomanize && !showRomanizationPref) emptyList()
        else lines.mapIndexed { i, line ->
            line.romanization?.takeIf { it.isNotBlank() }
                ?: if (autoRomanize) {
                    liveRomanizations?.getOrNull(i)?.takeIf { it.isNotBlank() }
                        ?: PixelMusicRomanizer.romanize(line.line)
                } else null
        }
    }

    return LiveLyricsEnrichment(
        liveTranslations = liveTranslations,
        romanized = romanized,
        autoTranslate = autoTranslate,
        autoRomanize = autoRomanize,
        romanizationAsMain = romanizationAsMain,
    )
}

@Composable
fun PixelMusicSyncedLyricsView(
    lines: List<SyncedLine>,
    listState: LazyListState,
    playbackPositionFlow: StateFlow<Long>,
    lyricsSyncOffset: Int,
    positionOverrideMs: Long?,
    accentColor: Color,
    contentColor: Color,
    providerName: String?,
    lyricsAlignment: String,
    showTranslationPref: Boolean,
    showRomanizationPref: Boolean,
    performanceModeEnabled: Boolean,
    immersiveMode: Boolean,
    onLineClick: (SyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    isPlaying: Boolean = true,
    blurStrength: Float = 2.5f,
    fontSizeScale: Float = 1f,
    showFooter: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val style by remember(context) { PixelMusicLyricsPreferences.styleFlow(context) }
        .collectAsStateWithLifecycle(initialValue = com.saurav.pixelmusic.lyrics.PixelMusicLyricsStyle.MOTION)
    val softLight by remember(context) { PixelMusicLyricsPreferences.softLightFlow(context) }
        .collectAsStateWithLifecycle(initialValue = true)
    val textSizeSp by remember(context) { PixelMusicLyricsPreferences.textSizeFlow(context) }
        .collectAsStateWithLifecycle(initialValue = 26f)
    val lineSpacing by remember(context) { PixelMusicLyricsPreferences.lineSpacingFlow(context) }
        .collectAsStateWithLifecycle(initialValue = 1.25f)
    val enrichment = rememberLiveLyricsEnrichment(
        lines = lines,
        showRomanizationPref = showRomanizationPref,
    )
    val liveTranslations = enrichment.liveTranslations
    val romanized = enrichment.romanized
    val romanizationAsMain = enrichment.romanizationAsMain

    val cascade = style.risesWords
    val shaded = style.shaded
    val lineBlurAllowed = softLight && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useSoftLightBlend = softLight && SoftLightSupported
    SideEffect { AppMotion.elementBlur = lineBlurAllowed }

    // position, read only inside lambdas and derived states so a playback tick doesn't recompose the page
    val rawPosition = playbackPositionFlow.collectAsStateWithLifecycle()
    val currentOffset by rememberUpdatedState(lyricsSyncOffset)
    val currentOverride by rememberUpdatedState(positionOverrideMs)
    val currentPlaying by rememberUpdatedState(isPlaying)
    val clock = remember {
        LyricsClock(
            sample = { (currentOverride ?: rawPosition.value) + currentOffset },
            isPlaying = { currentPlaying && currentOverride == null },
        )
    }
    val positionProvider: () -> Long = remember(clock) { { clock.now().coerceAtLeast(0L) } }
    val statePosition = remember { derivedStateOf { ((currentOverride ?: rawPosition.value) + currentOffset).coerceAtLeast(0L) } }

    // each line as PixelMusic's renderer takes it, with the romanisation swapped on top when that's the setting
    val entries = remember(lines, romanized, romanizationAsMain) {
        lines.mapIndexed { i, line ->
            val rom = romanized.getOrNull(i)?.takeIf { it.isNotBlank() && it != line.line }
            val swap = romanizationAsMain && rom != null
            val nextLineTimeMs = lines.getOrNull(i + 1)?.time
            line.toPixelMusicEntry(
                primaryText = if (swap) rom else line.line,
                useWords = !swap,
                nextLineTimeMs = nextLineTimeMs,
            )
        }
    }
    val rows = remember(entries) { buildLyricRows(entries) }
    val rowOfLine = remember(rows, entries) {
        IntArray(entries.size).also { map ->
            rows.forEachIndexed { rowIndex, row -> if (row is LyricRow.Line) map[row.index] = rowIndex }
        }
    }

    val currentIndex by remember(entries) { derivedStateOf { findCurrentLineIndex(entries, statePosition.value) } }
    val anchorIndex by remember(entries) { derivedStateOf { lyricAnchorIndex(entries, statePosition.value) } }
    val activeInterlude by remember(rows) { derivedStateOf { activeInterludeOf(rows, statePosition.value) } }

    val useAgentAlignment = remember(entries) { usesAgentAlignment(entries) }
    val appRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val mirrored = remember(entries, appRtl) { lyricsReadRtl(entries) != appRtl }

    val stagger = remember(rows) { LyricsStagger(rows.size + 1) }
    LaunchedEffect(stagger) { stagger.run() }

    var isAutoScrolling by remember { mutableStateOf(false) }
    var followPlayback by remember { mutableStateOf(true) }
    val userScrolling by remember { derivedStateOf { listState.isScrollInProgress && !isAutoScrolling } }
    LaunchedEffect(userScrolling) { if (userScrolling) followPlayback = false }
    LaunchedEffect(userScrolling, followPlayback) {
        if (!followPlayback && !userScrolling) {
            delay(IdleRecenterDelayMs)
            followPlayback = true
        }
    }
    val suppressEffects = userScrolling

    var placed by remember(entries) { mutableStateOf(false) }

    // the active line rests one row from the top; rows below it trail the scroll on style 1 and boring
    LaunchedEffect(followPlayback, entries, style) {
        if (!followPlayback) return@LaunchedEffect
        snapshotFlow { anchorIndex }.collectLatest { target ->
            if (target !in entries.indices) return@collectLatest
            val targetRow = rowOfLine[target]
            val viewportHeight = snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
            val minOffsetPx = with(density) { MinPastLineVisibleHeight.toPx() }
            val restingOffset = maxOf(viewportHeight * ActiveLineViewportFraction, minOffsetPx).roundToInt()
            try {
                isAutoScrolling = true
                if (!placed) {
                    placed = true
                    listState.scrollToItem(targetRow, 0)
                    return@collectLatest
                }
                val layoutInfo = listState.layoutInfo
                val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetRow }
                if (visible != null) {
                    val current = visible.offset - layoutInfo.viewportStartOffset
                    val collapsing = layoutInfo.visibleItemsInfo
                        .filter { it.index < targetRow && rows.getOrNull(it.index) is LyricRow.Interlude }
                        .sumOf { it.size }
                    val distance = (current - restingOffset - collapsing).toFloat()
                    stagger.anchorRow = targetRow
                    listState.scroll {
                        var previous = 0f
                        animate(
                            initialValue = 0f,
                            targetValue = distance,
                            animationSpec = tween(durationMillis = LyricsRecenterDurationMs, easing = recenterEasingFor(cascade)),
                        ) { value, _ ->
                            val consumed = scrollBy(value - previous)
                            previous += consumed
                            if (cascade && consumed != 0f) {
                                for (item in listState.layoutInfo.visibleItemsInfo) {
                                    if (item.index > targetRow) stagger.holdBack(item.index, consumed)
                                }
                            }
                        }
                    }
                } else {
                    listState.animateScrollToItem(targetRow, 0)
                }
            } finally {
                isAutoScrolling = false
            }
        }
    }

    val listPadding = remember { LyricsListPadding() }
    // Keep blend null on the solid container background so resting lines don't dim against a flat backdrop
    val blend: LyricsBackdropBlend? = null
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalLyricsHighlight provides contentColor,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    listPadding.heightPx = constraints.maxHeight
                    listPadding.density = this.density
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
        ) {
            LazyColumn(
                state = listState,
                contentPadding = listPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { blend?.list = it }
                    .then(
                        if (blend == null) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithCache {
                                    val fade = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        TopFadeFraction to Color.Black,
                                        BottomFadeStart to Color.Black,
                                        1f to Color.Transparent,
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush = fade, blendMode = BlendMode.DstIn)
                                    }
                                }
                        } else {
                            Modifier
                        }
                    ),
            ) {
                itemsIndexed(
                    items = rows,
                    key = { _, row -> row.key },
                    contentType = { _, row -> if (row is LyricRow.Interlude) "interlude" else "lyric" },
                ) { listIndex, row ->
                    when (row) {
                        is LyricRow.Interlude -> {
                            LyricsInterludeDots(
                                modifier = Modifier
                                    .lyricsEdgeFade(blend)
                                    .graphicsLayer { translationY = stagger.lagOf(listIndex) },
                                startMs = row.startMs,
                                endMs = row.endMs,
                                positionProvider = positionProvider,
                                bloom = shaded,
                                highBloom = false,
                                visible = activeInterlude == row.afterIndex,
                                collapseEasing = recenterEasingFor(cascade),
                                alignment = entries.getOrNull(row.afterIndex + 1)
                                    ?.let { alignmentFor(it, useAgentAlignment, mirrored) }
                                    ?: LyricsAlignment.CENTER,
                            )
                        }

                        is LyricRow.Line -> {
                            val index = row.index
                            val look by remember(index, entries) {
                                derivedStateOf { lineLook(index, currentIndex, anchorIndex, activeInterlude != NoInterlude) }
                            }
                            val blurStage = if (suppressEffects || !lineBlurAllowed) 0 else look.blurStage
                            val rowIndex = rowOfLine[index]
                            val source = lines[index]
                            val translation = source.translation?.takeIf { it.isNotBlank() && showTranslationPref }
                                ?: liveTranslations?.getOrNull(index)?.takeIf { it.isNotBlank() && it != source.line }
                            val rom = romanized.getOrNull(index)?.takeIf { it.isNotBlank() && it != source.line }
                            // with the romanisation as the main line the original script goes underneath, otherwise the reading does
                            val subLine = if (romanizationAsMain && rom != null) source.line else rom

                            LyricsGlowLine(
                                modifier = Modifier.graphicsLayer { translationY = stagger.lagOf(rowIndex) },
                                entry = row.entry,
                                state = look.state,
                                blurStage = blurStage,
                                suppressEffects = suppressEffects,
                                positionProvider = positionProvider,
                                accentColor = accentColor,
                                subLine = subLine,
                                rubyPlacement = if (translation != null) RubyPlacement.ABOVE else RubyPlacement.BELOW,
                                translation = translation,
                                alignment = alignmentFor(row.entry, useAgentAlignment, mirrored),
                                waveEnabled = shaded && !suppressEffects,
                                motionStyle = style,
                                // Use 4-tap diagonal halo on both Style 1 and Style 2 for smooth 60-120fps GPU shader rendering
                                highBloom = false,
                                blend = blend,
                                fontSize = (textSizeSp * fontSizeScale).sp,
                                lineHeightMultiplier = lineSpacing,
                                blurStrength = blurStrength,
                                onClick = {
                                    onLineClick(source)
                                    followPlayback = true
                                },
                            )
                        }
                    }
                }

                if (showFooter) {
                    item(key = "pixelmusic_source_footer", contentType = "source") {
                        PixelMusicSourceFooter(
                            providerName = providerName,
                            contentColor = contentColor,
                            modifier = Modifier
                                .lyricsEdgeFade(blend)
                                .graphicsLayer { translationY = stagger.lagOf(rows.size) }
                                .padding(horizontal = com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic.LyricsHeaderArtX),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelMusicSourceFooter(
    providerName: String?,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (providerName.isNullOrBlank()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lyrics provided by $providerName",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = GoogleSansRounded,
            color = contentColor.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}
