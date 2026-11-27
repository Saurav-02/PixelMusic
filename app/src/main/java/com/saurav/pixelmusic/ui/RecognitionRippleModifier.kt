package com.saurav.pixelmusic.ui.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
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
