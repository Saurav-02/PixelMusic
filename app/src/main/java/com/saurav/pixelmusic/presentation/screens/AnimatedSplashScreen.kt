package com.saurav.pixelmusic.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val iconAlpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val circleProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(300)) }
        launch { scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }

        launch {
            delay(100)
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 700
                    0f at 0
                    30f at 250 using FastOutSlowInEasing
                    -10f at 450 using FastOutSlowInEasing
                    0f at 700
                }
            )
        }

        launch {
            circleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
        
        delay(1200L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Top-Right Flying Circle (GPU Accelerated)
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer {
                    translationX = (circleProgress.value * 250).dp.toPx()
                    translationY = (circleProgress.value * -350).dp.toPx()
                    scaleX = 0.2f + (circleProgress.value * 1.5f)
                    scaleY = 0.2f + (circleProgress.value * 1.5f)
                    alpha = 1f - (circleProgress.value * 0.4f)
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        )

        // Bottom-Left Flying Circle (GPU Accelerated)
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer {
                    translationX = (circleProgress.value * -250).dp.toPx()
                    translationY = (circleProgress.value * 350).dp.toPx()
                    scaleX = 0.2f + (circleProgress.value * 1.5f)
                    scaleY = 0.2f + (circleProgress.value * 1.5f)
                    alpha = 1f - (circleProgress.value * 0.4f)
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        )

        // Main App Logo (GPU Accelerated)
        Icon(
            painter = painterResource(id = R.drawable.pixelmusic_base_monochrome),
            contentDescription = "App Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(110.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value
                    alpha = iconAlpha.value
                }
        )
    }
}
