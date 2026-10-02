package com.saurav.pixelmusic.presentation.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.SyncedLine
import com.saurav.pixelmusic.data.preferences.NowPlayingLyricsStyle
import com.saurav.pixelmusic.presentation.components.sanitizeLyricLineText
import com.saurav.pixelmusic.presentation.components.sanitizeSyncedWords
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.flow.StateFlow

internal data class DisplayWord(
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

internal fun resolveDisplayWords(
    syncedLine: SyncedLine?,
    nextLineTimeMs: Long
): List<DisplayWord> {
    if (syncedLine == null) return emptyList()

    val explicitWords = syncedLine.words?.let(::sanitizeSyncedWords)
    if (!explicitWords.isNullOrEmpty()) {
        return explicitWords.mapIndexed { idx, w ->
            val nextTime = if (idx < explicitWords.lastIndex) {
                explicitWords[idx + 1].time.toLong()
            } else {
                maxOf(w.time.toLong() + 400L, nextLineTimeMs)
            }
            DisplayWord(
                word = w.word,
                startTimeMs = w.time.toLong(),
                endTimeMs = nextTime
            )
        }
    }

    val sanitizedLine = sanitizeLyricLineText(syncedLine.line)
    val rawTokens = sanitizedLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (rawTokens.isEmpty()) return emptyList()

    val lineStart = syncedLine.time.toLong()
    val totalDuration = (nextLineTimeMs - lineStart).coerceIn(1200L, 8000L)
    val singingDuration = (totalDuration * 0.90f).toLong()
    val totalChars = rawTokens.sumOf { it.length }.coerceAtLeast(1)

    var currentStart = lineStart
    return rawTokens.map { token ->
        val wordDuration = ((token.length.toFloat() / totalChars) * singingDuration).toLong().coerceAtLeast(160L)
        val wordEnd = currentStart + wordDuration
        DisplayWord(
            word = token,
            startTimeMs = currentStart,
            endTimeMs = wordEnd
        ).also {
            currentStart = wordEnd
        }
    }
}

@Composable
fun ImmersiveSingleLineLyrics(
    lyrics: Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    syncOffsetMs: Int,
    textColor: Color,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    lyricsStyle: NowPlayingLyricsStyle = NowPlayingLyricsStyle.KARAOKE,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (lyricsStyle == NowPlayingLyricsStyle.HIDDEN) return

    val syncedLines = lyrics?.synced ?: emptyList()
    if (syncedLines.isEmpty()) {
        return
    }

    val currentPositionMs by playbackPositionFlow.collectAsStateWithLifecycle()
    val effectivePosition = currentPositionMs + syncOffsetMs

    val currentLineIndex = remember(effectivePosition, syncedLines) {
        syncedLines.indexOfLast { it.time <= effectivePosition }
    }

    val currentLine = if (currentLineIndex >= 0) syncedLines[currentLineIndex] else null
    val currentLineText = remember(currentLine) {
        currentLine?.line?.let { sanitizeLyricLineText(it) }.orEmpty()
    }

    val nextLineTimeMs = remember(currentLineIndex, syncedLines) {
        if (currentLineIndex in 0 until syncedLines.lastIndex) {
            syncedLines[currentLineIndex + 1].time.toLong()
        } else if (currentLineIndex >= 0) {
            syncedLines[currentLineIndex].time.toLong() + 7000L
        } else {
            0L
        }
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when (lyricsStyle) {
            NowPlayingLyricsStyle.HIDDEN -> { /* No-op */ }
            NowPlayingLyricsStyle.KARAOKE -> {
                KaraokeStyleLyrics(
                    lineText = currentLineText,
                    textColor = textColor
                )
            }
            NowPlayingLyricsStyle.WORD_BY_WORD -> {
                WordByWordStyleLyrics(
                    syncedLine = currentLine,
                    nextLineTimeMs = nextLineTimeMs,
                    effectivePosition = effectivePosition,
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
            NowPlayingLyricsStyle.SINGLE_WORD -> {
                SingleWordStyleLyrics(
                    syncedLine = currentLine,
                    nextLineTimeMs = nextLineTimeMs,
                    effectivePosition = effectivePosition,
                    accentColor = accentColor
                )
            }
            NowPlayingLyricsStyle.BLUR_FOCUS -> {
                BlurFocusStyleLyrics(
                    syncedLine = currentLine,
                    nextLineTimeMs = nextLineTimeMs,
                    effectivePosition = effectivePosition,
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
            NowPlayingLyricsStyle.GRADIENT_SWEEP -> {
                GradientSweepStyleLyrics(
                    lineText = currentLineText,
                    textColor = textColor,
                    accentColor = accentColor
                )
            }
            NowPlayingLyricsStyle.SLIDE_FADE -> {
                SlideFadeStyleLyrics(
                    lineText = currentLineText,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
private fun KaraokeStyleLyrics(
    lineText: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = lineText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) +
             scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f)) +
             slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f))) togetherWith
            (fadeOut(animationSpec = tween(300)) +
             scaleOut(targetScale = 0.85f, animationSpec = tween(300)) +
             slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(300)))
        },
        label = "karaoke_style_transition",
        modifier = modifier.fillMaxWidth()
    ) { text ->
        val blurY by animateDpAsState(
            targetValue = if (text == lineText) 0.dp else 12.dp,
            animationSpec = tween(300),
            label = "blurY"
        )

        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(radiusX = 0.dp, radiusY = blurY)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordByWordStyleLyrics(
    syncedLine: SyncedLine?,
    nextLineTimeMs: Long,
    effectivePosition: Long,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayWords = remember(syncedLine, nextLineTimeMs) {
        resolveDisplayWords(syncedLine, nextLineTimeMs)
    }

    if (displayWords.isEmpty()) {
        Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            maxLines = 2
        ) {
            displayWords.forEachIndexed { index, item ->
                if (effectivePosition >= item.startTimeMs) {
                    val isCurrent = effectivePosition < item.endTimeMs
                    key("${syncedLine?.time}_${item.startTimeMs}_${item.word}_$index") {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(180)) +
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = spring(
                                            dampingRatio = 0.72f,
                                            stiffness = 140f
                                        )
                                    ) +
                                    scaleIn(
                                        initialScale = 0.6f,
                                        animationSpec = spring(
                                            dampingRatio = 0.72f,
                                            stiffness = 140f
                                        )
                                    )
                        ) {
                            val wordScale by animateFloatAsState(
                                targetValue = if (isCurrent) 1.10f else 1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                ),
                                label = "wordScale"
                            )
                            val wordColor by animateColorAsState(
                                targetValue = if (isCurrent) accentColor else textColor,
                                animationSpec = tween(durationMillis = 180),
                                label = "wordColor"
                            )

                            Text(
                                text = item.word,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = wordColor,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = wordScale
                                    scaleY = wordScale
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleWordStyleLyrics(
    syncedLine: SyncedLine?,
    nextLineTimeMs: Long,
    effectivePosition: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayWords = remember(syncedLine, nextLineTimeMs) {
        resolveDisplayWords(syncedLine, nextLineTimeMs)
    }

    val activeWord = remember(effectivePosition, displayWords) {
        if (displayWords.isEmpty()) null
        else displayWords.firstOrNull { effectivePosition in it.startTimeMs until it.endTimeMs }
            ?: displayWords.lastOrNull { effectivePosition >= it.endTimeMs }
    }

    AnimatedContent(
        targetState = activeWord?.word.orEmpty(),
        transitionSpec = {
            (fadeIn(animationSpec = tween(180)) +
             scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.70f, stiffness = 160f)) +
             slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.70f, stiffness = 160f))) togetherWith
            (fadeOut(animationSpec = tween(140)) +
             scaleOut(targetScale = 0.85f, animationSpec = tween(140)) +
             slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(140)))
        },
        label = "single_word_pop_transition",
        modifier = modifier.fillMaxWidth()
    ) { wordText ->
        if (wordText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = wordText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp
                    ),
                    color = accentColor,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlurFocusStyleLyrics(
    syncedLine: SyncedLine?,
    nextLineTimeMs: Long,
    effectivePosition: Long,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayWords = remember(syncedLine, nextLineTimeMs) {
        resolveDisplayWords(syncedLine, nextLineTimeMs)
    }
    val lineText = remember(syncedLine) {
        syncedLine?.line?.let { sanitizeLyricLineText(it) }.orEmpty()
    }

    AnimatedContent(
        targetState = lineText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(450)) +
             scaleIn(initialScale = 0.94f, animationSpec = tween(450, easing = FastOutSlowInEasing))) togetherWith
            (fadeOut(animationSpec = tween(300)) +
             scaleOut(targetScale = 1.04f, animationSpec = tween(300, easing = FastOutSlowInEasing)))
        },
        label = "blur_focus_line_transition",
        modifier = modifier.fillMaxWidth()
    ) { currentText ->
        if (currentText.isBlank() || displayWords.isEmpty()) {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
            return@AnimatedContent
        }

        val lineBlurAnim = remember(currentText) { Animatable(14f) }
        LaunchedEffect(currentText) {
            lineBlurAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .blur(lineBlurAnim.value.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                maxLines = 2
            ) {
                displayWords.forEachIndexed { index, item ->
                    val isCurrent = effectivePosition in item.startTimeMs until item.endTimeMs
                    val isPast = effectivePosition >= item.endTimeMs
                    key("${syncedLine?.time}_${item.startTimeMs}_${item.word}_$index") {
                        val targetBlur = when {
                            isCurrent -> 0.dp
                            isPast -> 0.dp
                            else -> 3.5.dp
                        }
                        val wordBlur by animateDpAsState(
                            targetValue = targetBlur,
                            animationSpec = tween(durationMillis = 220),
                            label = "blurWord"
                        )
                        val wordAlpha by animateFloatAsState(
                            targetValue = when {
                                isCurrent -> 1.0f
                                isPast -> 0.85f
                                else -> 0.35f
                            },
                            animationSpec = tween(durationMillis = 220),
                            label = "blurAlpha"
                        )
                        val wordScale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.08f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
                            label = "blurScale"
                        )

                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 21.sp
                            ),
                            color = if (isCurrent) accentColor else textColor,
                            modifier = Modifier
                                .blur(wordBlur)
                                .graphicsLayer {
                                    scaleX = wordScale
                                    scaleY = wordScale
                                    alpha = wordAlpha
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientSweepStyleLyrics(
    lineText: String,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val gradientBrush = remember(shimmerOffset, textColor, accentColor) {
        Brush.linearGradient(
            colors = listOf(
                textColor.copy(alpha = 0.7f),
                accentColor,
                Color.White,
                accentColor,
                textColor.copy(alpha = 0.7f)
            ),
            start = androidx.compose.ui.geometry.Offset(shimmerOffset, 0f),
            end = androidx.compose.ui.geometry.Offset(shimmerOffset + 400f, 100f)
        )
    }

    AnimatedContent(
        targetState = lineText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) +
             slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(400, easing = FastOutSlowInEasing))) togetherWith
            (fadeOut(animationSpec = tween(280)) +
             slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(280, easing = FastOutSlowInEasing)))
        },
        label = "gradient_sweep_line_transition",
        modifier = modifier.fillMaxWidth()
    ) { text ->
        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        brush = gradientBrush
                    ),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun SlideFadeStyleLyrics(
    lineText: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = lineText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) +
             slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(400, easing = LinearOutSlowInEasing))) togetherWith
            (fadeOut(animationSpec = tween(280, easing = FastOutLinearInEasing)) +
             slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(280, easing = FastOutLinearInEasing)))
        },
        label = "slide_fade_line_transition",
        modifier = modifier.fillMaxWidth()
    ) { text ->
        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        }
    }
}
