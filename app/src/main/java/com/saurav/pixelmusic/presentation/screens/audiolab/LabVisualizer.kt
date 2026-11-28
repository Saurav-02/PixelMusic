package com.saurav.pixelmusic.presentation.screens.audiolab

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.audiolab.audio.SpectrumAnalyzer
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class LabSpectrum(val count: Int) {
    private val analyzer = SpectrumAnalyzer(DoubleArray(count) { i -> 45.0 * (14_000.0 / 45.0).pow(i / (count - 1.0)) })
    val values = FloatArray(count)
    private var lastUpdateMs = 0L

    fun update() {
        val now = SystemClock.uptimeMillis()
        if (now - lastUpdateMs < 8) return
        val dt = if (lastUpdateMs == 0L) 0f else ((now - lastUpdateMs) / 1000f).coerceAtMost(0.05f)
        lastUpdateMs = now
        val live = analyzer.update()
        val idleTime = now / 1000f
        val up = 1f - exp(-dt * 30f)
        val down = 1f - exp(-dt * 7f)
        for (i in 0 until count) {
            val target = if (live) {
                val tilt = 14f * i / (count - 1f)
                ((analyzer.levels[i] + tilt + 66f) / 54f).coerceIn(0f, 1f)
            } else {
                0.17f + 0.1f * sin(idleTime * 1.3f + i * 0.62f) + 0.05f * sin(idleTime * 2.1f - i * 1.13f)
            }
            values[i] += (target - values[i]) * (if (target > values[i]) up else down)
        }
    }
}

@Composable
fun rememberLabSpectrum(count: Int): LabSpectrum = remember(count) { LabSpectrum(count) }

@Composable
fun LabBars(
    spectrum: LabSpectrum,
    hidden: State<Boolean>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4C4C52),
    resolution: Float = 1f,
    depth: Float = 0f,
    fill: Float = 0.5f,
    detail: Boolean = false,
) {
    val onScreen = rememberOnScreen()
    val held = rememberPicturesHeld(onScreen)
    val paused = remember(held, hidden) { derivedStateOf { held.value || hidden.value } }
    val time = rememberLabTime(paused)
    val shown = remember { FloatArray(spectrum.count) }
    val lastSample = remember { floatArrayOf(-1f) }

    Canvas(modifier.trackOnScreen(onScreen)) {
        val t = time.value
        spectrum.update()
        val r = resolution.coerceIn(0f, 1f)
        val bars = (7 + (spectrum.count - 7) * r).roundToInt().coerceIn(1, spectrum.count)
        val hold = (1f - r) * 0.16f
        if (lastSample[0] < 0f || t - lastSample[0] >= hold || t < lastSample[0]) {
            lastSample[0] = t
            for (b in 0 until bars) {
                val from = b * spectrum.count / bars
                val to = ((b + 1) * spectrum.count / bars).coerceAtLeast(from + 1)
                var sum = 0f
                for (i in from until to) sum += spectrum.values[i]
                shown[b] = sum / (to - from)
            }
        }
        val levels = if (r >= 0.95f) 0 else (3 + 45 * r * r).roundToInt()

        if (depth > 0.01f) {
            for (layer in 2 downTo 1) {
                val shift = layer * 8.dp.toPx() * depth
                drawBars(shown, bars, levels, r, fill, detail, color.copy(alpha = color.alpha * 0.35f * depth / layer), shift, -shift)
            }
        }
        drawBars(shown, bars, levels, r, fill, detail, color, 0f, 0f)
    }
}

private fun DrawScope.drawBars(
    values: FloatArray,
    bars: Int,
    levels: Int,
    resolution: Float,
    fill: Float,
    detail: Boolean,
    color: Color,
    dx: Float,
    dy: Float,
) {
    val slot = size.width / bars
    val barWidth = (slot * fill).coerceAtLeast(1.dp.toPx())
    val maxHeight = size.height
    for (b in 0 until bars) {
        val q = if (levels == 0) values[b] else (values[b] * levels).roundToInt() / levels.toFloat()
        val x = b * slot + (slot - barWidth) / 2 + dx
        if (resolution < 0.45f) {
            val cell = barWidth
            val pitch = cell * 1.25f
            val count = ceil(q * maxHeight / pitch).toInt().coerceAtLeast(1)
            for (k in 0 until count) {
                drawRect(color, Offset(x, size.height - (k + 1) * pitch + dy), Size(cell, cell))
            }
        } else {
            val height = (q * maxHeight).coerceAtLeast(barWidth)
            drawRoundRect(
                color,
                Offset(x, size.height - height + dy),
                Size(barWidth, height),
                CornerRadius(barWidth / 2),
            )
            if (detail && resolution > 0.85f && b < bars - 1) {
                val between = (values[b] + values[b + 1]) / 2 * maxHeight * 0.7f
                val lineX = x + barWidth + (slot - barWidth) / 2
                drawLine(
                    color.copy(alpha = color.alpha * 0.55f * (resolution - 0.85f) / 0.15f),
                    Offset(lineX, size.height + dy),
                    Offset(lineX, size.height - between + dy),
                    1.dp.toPx(),
                )
            }
        }
    }
}
