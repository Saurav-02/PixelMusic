package com.unshoo.pixelmusic.presentation.screensaver

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun GlowingWaveScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(40_000L) // 40-second timeout
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        fun drawWave(amplitude: Float, frequency: Float, phaseOffset: Float, strokeWidth: Float, color: Color, blurRadius: Float) {
            val path = Path()
            path.moveTo(0f, centerY)

            for (x in 0..width.toInt() step 5) {
                val y = centerY + amplitude * sin((x * frequency) + phase + phaseOffset)
                path.lineTo(x.toFloat(), y)
            }

            if (blurRadius > 0f) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.4f),
                    style = Stroke(width = strokeWidth * 3f),
                    colorFilter = ColorFilter.tint(color, BlendMode.SrcIn)
                )
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth)
            )
        }

        drawWave(height * 0.15f, 0.002f, 0f, 12f, Color(0xFF6C35DE), 40f)
        drawWave(height * 0.1f, 0.003f, 1.5f, 6f, Color(0xFFBB86FC), 15f)
        drawWave(height * 0.1f, 0.003f, 1.5f, 2f, Color.White, 0f)
    }
}

