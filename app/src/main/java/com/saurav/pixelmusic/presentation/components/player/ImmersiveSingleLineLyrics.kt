package com.saurav.pixelmusic.presentation.components.player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ImmersiveSingleLineLyrics(
    lyrics: Lyrics?,
    playbackPositionFlow: StateFlow<Long>,
    syncOffsetMs: Int,
    textColor: Color, 
    modifier: Modifier = Modifier
) {
    val syncedLines = lyrics?.synced ?: emptyList()
    if (syncedLines.isEmpty()) {
        return
    }

    val currentPositionMs by playbackPositionFlow.collectAsStateWithLifecycle()
    val effectivePosition = currentPositionMs + syncOffsetMs

    val currentLine = remember(effectivePosition, syncedLines) {
        val index = syncedLines.indexOfLast { it.time <= effectivePosition }
        if (index >= 0) syncedLines[index].line else ""
    }

    AnimatedContent(
        targetState = currentLine,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) +
             scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f)) +
             slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(dampingRatio = 0.82f, stiffness = 100f))) togetherWith
            (fadeOut(animationSpec = tween(300)) +
             scaleOut(targetScale = 0.8f, animationSpec = tween(300)) +
             slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(300)))
        },
        label = "lyrics_crossfade",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) { line ->
        
        // FIX: Using animateDpAsState directly resolves the compiler type inference error
        val blurY by animateDpAsState(
            targetValue = if (line == currentLine) 0.dp else 12.dp,
            animationSpec = tween(300),
            label = "blurY"
        )

        if (line.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp), // Stable height to prevent UI jumps
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(radiusX = 0.dp, radiusY = blurY) // Applies the Y-axis motion blur
                )
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp).fillMaxWidth())
        }
    }
}
