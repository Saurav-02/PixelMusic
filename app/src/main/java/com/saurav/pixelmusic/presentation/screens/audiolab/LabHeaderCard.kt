package com.saurav.pixelmusic.presentation.screens.audiolab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal val LabCardStart = 22.dp
internal val LabCardBottom = 18.dp
internal val LabCardWidth = 128.dp
internal val LabCardHeight = 186.dp
internal val LabCardBars: Color
    @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
internal val LabCardInk: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val CardShape = RoundedCornerShape(26.dp)

@Composable
internal fun LabHeaderCardFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(LabCardWidth, LabCardHeight)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CardShape),
        content = content,
    )
}

@Composable
internal fun LabCardFace(
    modifier: Modifier = Modifier,
    topStart: String? = null,
    topEnd: String? = null,
    label: String? = null,
    bars: @Composable (Modifier) -> Unit,
) {
    Column(modifier.fillMaxSize().padding(12.dp)) {
        if (topStart != null || topEnd != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                topStart?.let { Text(it, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = LabCardInk, maxLines = 1) }
                Spacer(Modifier.weight(1f))
                topEnd?.let { Text(it, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = LabCardInk, maxLines = 1) }
            }
        }
        if (label != null) {
            Text(
                label.uppercase(),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (20f * (7f / label.length).coerceAtMost(1f)).coerceAtLeast(13f).sp,
                letterSpacing = (-0.01).em,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            bars(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 14.dp))
        }
    }
}
