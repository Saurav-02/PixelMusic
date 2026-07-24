package com.unshoo.pixelmusic.presentation.screensaver

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingWaveScreen(onTimeout: () -> Unit) {
    // 40-second timeout to protect OLED screens
    LaunchedEffect(Unit) {
        delay(40_000L)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "circular_wave")
    
    // Slow, continuous rotation
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing), // 12 seconds per rotation (very slow)
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Gentle heartbeat pulse to simulate deep bass
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val center = Offset(size.width / 2, size.height / 2)
        // Base size of the circle
        val baseRadius = (size.width * 0.35f) * pulse

        fun drawCircularWave(
            peaks: Int, 
            amplitude: Float, 
            phaseOffset: Float, 
            strokeWidth: Float, 
            color: Color, 
            blurRadius: Float
        ) {
            val path = Path()
            
            // Draw 360 degrees of the circle
            for (degree in 0..360 step 2) {
                val theta = Math.toRadians(degree.toDouble())
                
                // The math to warp the circle into a smooth wave
                val r = baseRadius + amplitude * sin(peaks * theta + phase + phaseOffset).toFloat()
                
                // Convert Polar coordinates (radius, angle) to Cartesian (x, y)
                val x = center.x + r * cos(theta).toFloat()
                val y = center.y + r * sin(theta).toFloat()
                
                if (degree == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close() // Perfectly connects the end to the beginning

            // Draw the outer glow
            if (blurRadius > 0f) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.35f),
                    style = Stroke(width = strokeWidth * 4f),
                    colorFilter = ColorFilter.tint(color, BlendMode.SrcIn)
                )
            }
            
            // Draw the sharp inner line
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth)
            )
        }

        // Layer 1: Slow, deep purple outer ripples (3 peaks)
        drawCircularWave(
            peaks = 3,
            amplitude = size.width * 0.08f,
            phaseOffset = 0f,
            strokeWidth = 14f,
            color = Color(0xFF6C35DE),
            blurRadius = 40f
        )

        // Layer 2: Medium speed, pinkish inner wave (5 peaks)
        drawCircularWave(
            peaks = 5,
            amplitude = size.width * 0.05f,
            phaseOffset = 2f,
            strokeWidth = 6f,
            color = Color(0xFFBB86FC),
            blurRadius = 15f
        )
        
        // Layer 3: Sharp, slow white core (4 peaks)
        drawCircularWave(
            peaks = 4,
            amplitude = size.width * 0.06f,
            phaseOffset = 4f,
            strokeWidth = 2f,
            color = Color.White,
            blurRadius = 0f
        )
    }
}
