package com.saurav.pixelmusic.presentation.components.subcomps

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizingTextToFill(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    minFontSize: TextUnit = 8.sp,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    maxFontSizeLimit: TextUnit = 36.sp,
    lineHeightRatio: Float = 1.2f
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(0)
        val maxHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(0)

        val currentFontSize = remember(
            text,
            style,
            minFontSize,
            maxFontSizeLimit,
            lineHeightRatio,
            fontFamily,
            fontWeight,
            maxWidthPx,
            maxHeightPx
        ) {
            if (maxWidthPx <= 0 || maxHeightPx <= 0 || text.isEmpty()) {
                return@remember minFontSize
            }

            var lowerBoundSp = minFontSize.value
            val maxHeightSp = with(density) { maxHeight.toSp().value }.coerceAtLeast(minFontSize.value)
            var upperBoundSp = minOf(maxFontSizeLimit.value, maxHeightSp).coerceAtLeast(minFontSize.value)

            if (lowerBoundSp > upperBoundSp + 0.01f) {
                return@remember minFontSize
            }

            val baseStyle = style.copy(
                fontFamily = fontFamily ?: style.fontFamily,
                fontWeight = fontWeight ?: style.fontWeight
            )
            val constraints = Constraints(maxWidth = maxWidthPx, maxHeight = maxHeightPx)
            val annotatedText = AnnotatedString(text)

            val minFontEffectiveStyle = baseStyle.copy(
                fontSize = minFontSize,
                lineHeight = minFontSize * lineHeightRatio
            )
            val minFontLayoutResult = textMeasurer.measure(
                text = annotatedText,
                style = minFontEffectiveStyle,
                overflow = TextOverflow.Clip,
                softWrap = true,
                maxLines = Int.MAX_VALUE,
                constraints = constraints
            )

            if (minFontLayoutResult.hasVisualOverflow) {
                return@remember minFontSize
            }

            var bestFitFontSize = minFontSize
            repeat(9) {
                if (upperBoundSp - lowerBoundSp < 0.25f) {
                    return@remember bestFitFontSize
                }

                val midSp = (lowerBoundSp + upperBoundSp) * 0.5f
                val candidateFontSize = midSp.sp
                val candidateStyle = baseStyle.copy(
                    fontSize = candidateFontSize,
                    lineHeight = candidateFontSize * lineHeightRatio
                )

                val layoutResult = textMeasurer.measure(
                    text = annotatedText,
                    style = candidateStyle,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = constraints
                )

                if (layoutResult.hasVisualOverflow) {
                    upperBoundSp = midSp - 0.1f
                } else {
                    bestFitFontSize = candidateFontSize
                    lowerBoundSp = midSp + 0.1f
                }
            }

            bestFitFontSize
        }

        val finalEffectiveLineHeight = currentFontSize * lineHeightRatio
        Text(
            text = text,
            modifier = Modifier,
            style = style.copy(
                fontSize = currentFontSize,
                lineHeight = finalEffectiveLineHeight
            ),
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            maxLines = Int.MAX_VALUE
        )
    }
}
