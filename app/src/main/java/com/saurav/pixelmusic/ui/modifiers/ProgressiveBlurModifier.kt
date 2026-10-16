package com.saurav.pixelmusic.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val PROGRESSIVE_BLUR_SHADER = """
    uniform shader inputShader;
    uniform vec2 size;
    uniform float maxBlurRadius;
    uniform float startRatio; // 0.0 to 1.0 (where blur starts)
    uniform float endRatio;   // 0.0 to 1.0 (where max blur is reached)
    uniform int direction;    // 0: Top-to-Bottom, 1: Bottom-to-Top

    // Approximate box/gaussian blur sampling loop
    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / size;
        
        float progress = 0.0;
        if (direction == 0) {
            progress = smoothstep(startRatio, endRatio, uv.y);
        } else {
            progress = smoothstep(startRatio, endRatio, 1.0 - uv.y);
        }
        
        float currentBlurRadius = maxBlurRadius * progress;
        
        if (currentBlurRadius <= 0.5) {
            return inputShader.eval(fragCoord);
        }
        
        vec4 color = vec4(0.0);
        float totalWeight = 0.0;
        
        int samples = 4; 
        for (int x = -samples; x <= samples; x++) {
            for (int y = -samples; y <= samples; y++) {
                vec2 offset = vec2(float(x), float(y)) * (currentBlurRadius / float(samples));
                vec2 sampleCoord = fragCoord + offset;
                
                sampleCoord = clamp(sampleCoord, vec2(0.0), size);
                
                color += inputShader.eval(sampleCoord);
                totalWeight += 1.0;
            }
        }
        
        return color / totalWeight;
    }
"""

enum class ProgressiveBlurDirection(val value: Int) {
    TOP_TO_BOTTOM(0),
    BOTTOM_TO_TOP(1)
}

/**
 * Modifier that applies a progressive blur effect (gradient blur) across the composable.
 * Origin: Essentials (https://github.com/sameerasw/essentials)
 */
fun Modifier.progressiveBlur(
    maxBlurRadius: Float = 25f,
    startRatio: Float = 0.0f,
    endRatio: Float = 0.5f,
    direction: ProgressiveBlurDirection = ProgressiveBlurDirection.TOP_TO_BOTTOM
): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PROGRESSIVE_BLUR_SHADER) }
        
        this.graphicsLayer {
            if (size.width > 0 && size.height > 0) {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setFloatUniform("maxBlurRadius", maxBlurRadius)
                shader.setFloatUniform("startRatio", startRatio)
                shader.setFloatUniform("endRatio", endRatio)
                shader.setIntUniform("direction", direction.value)
                
                val renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
                this.renderEffect = renderEffect.asComposeRenderEffect()
            }
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(
                    maxBlurRadius / 2f,
                    maxBlurRadius / 2f,
                    Shader.TileMode.DECAL
                ).asComposeRenderEffect()
            }
        } else {
            this
        }
    }
}
