package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded

/**
 * Convx-style navigation and section header title with optional eyebrow label,
 * custom SF-style rounded stroke chevron, and action label.
 */
@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
) {
    val contentColor = color ?: MaterialTheme.colorScheme.onSurface
    val chevron = onClick != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        thumbnail?.invoke()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                label?.let { l ->
                    Text(
                        text = l,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = GoogleSansRounded,
                        color = contentColor.copy(alpha = 0.6f),
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = title,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.01).em,
                    color = contentColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }

            if (chevron) {
                val chevronColor = MaterialTheme.colorScheme.onSurfaceVariant
                Canvas(
                    Modifier
                        .padding(start = 6.dp, top = 1.dp)
                        .size(width = 9.dp, height = 15.dp)
                ) {
                    val stroke = 2.6.dp.toPx()
                    val inset = stroke / 2
                    drawPath(
                        Path().apply {
                            moveTo(inset, inset)
                            lineTo(size.width - inset, size.height / 2)
                            lineTo(inset, size.height - inset)
                        },
                        color = chevronColor,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }

        val action = onPlayAllClick ?: onClick.takeUnless { chevron }
        if (action != null) {
            Text(
                text = if (onPlayAllClick != null) "Play all" else "See all",
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Normal,
                color = contentColor.copy(alpha = 0.6f),
                maxLines = 1,
                modifier = Modifier.clickable(onClick = action),
            )
        }
    }
}
