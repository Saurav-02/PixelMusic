package com.saurav.pixelmusic.ui.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.saurav.pixelmusic.R
import java.io.BufferedReader
import java.io.InputStreamReader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.recognitionRippleEffect(
    isTriggered: Boolean,
    durationSec: Float = 3.2f
): Modifier = composed {
    val context = LocalContext.current
    
    // Load and cache the shader text
    val shaderCode = remember {
        context.resources.openRawResource(R.raw.nfc_ripple).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
    val runtimeShader = remember { RuntimeShader(shaderCode) }
    val time = remember { Animatable(0f) }

    // Drive the animation when the trigger state changes
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            time.snapTo(0f)
            time.animateTo(
                targetValue = durationSec,
                animationSpec = tween((durationSec * 1000).toInt(), easing = LinearEasing)
            )
        } else {
            time.snapTo(0f)
        }
    }

    this.graphicsLayer {
        clip = true
        if (time.value > 0f && time.value < durationSec) {
            val density = context.resources.displayMetrics.density
            runtimeShader.setFloatUniform("uResolution", size.width, size.height)
            // Center the ripple inside the container bounds
            runtimeShader.setFloatUniform("uOrigin", size.width / 2f, size.height / 2f)
            runtimeShader.setFloatUniform("uAmplitude", 32f * density)
            runtimeShader.setFloatUniform("uFrequency", 12f)
            runtimeShader.setFloatUniform("uDecay", 4.5f)
            runtimeShader.setFloatUniform("uSpeed", 1400f * density)
            runtimeShader.setFloatUniform("uTime", time.value)

            // Bridge Android's RenderEffect to Compose's RenderEffect
            renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                runtimeShader, "inputShader"
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.successSweepEffect(isTriggered: Boolean): Modifier = composed {
    val context = LocalContext.current
    
    val shaderCode = remember {
        context.resources.openRawResource(R.raw.nfc_success_sweep).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
    val runtimeShader = remember { RuntimeShader(shaderCode) }
    val time = remember { Animatable(0f) }

    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            time.snapTo(0f)
            time.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 2500, 
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        } else {
            time.snapTo(0f)
        }
    }

    this.graphicsLayer {
        clip = true
        if (time.value > 0f && time.value < 1f) {
            runtimeShader.setFloatUniform("uResolution", size.width, size.height)
            runtimeShader.setFloatUniform("uTime", time.value)
            
            renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                runtimeShader, "inputShader"
            ).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

