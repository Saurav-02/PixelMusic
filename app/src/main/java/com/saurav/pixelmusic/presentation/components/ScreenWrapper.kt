package com.saurav.pixelmusic.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import com.saurav.pixelmusic.presentation.viewmodel.PlayerViewModel
import androidx.lifecycle.compose.currentStateAsState
import com.saurav.pixelmusic.presentation.navigation.isMainRootRoute
import com.saurav.pixelmusic.utils.AndroidVersionCompat
import com.saurav.pixelmusic.data.preferences.AppBackgroundStyle

@OptIn(UnstableApi::class)
@Composable
fun ScreenWrapper(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle State
    var isResumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isResumed = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isResumed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial Check
    val currentState = lifecycleOwner.lifecycle.currentStateAsState().value
    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        isResumed = true
    }

    // Visible entries is the public Navigation API designed for transition-aware stacking.
    val visibleEntries by navController.visibleEntries.collectAsStateWithLifecycle()
    val myEntry = lifecycleOwner as? androidx.navigation.NavBackStackEntry
    val myIndex = visibleEntries.indexOfFirst { it.id == myEntry?.id }
    val topIndex = visibleEntries.indexOfLast {
        it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isNavigationTarget = myEntry != null && currentBackStackEntry?.id == myEntry.id
    val myRoute = myEntry?.destination?.route
    val isMainRootScreen = isMainRootRoute(myRoute)
    val hasVisibleNonMainRootScreen = visibleEntries.any { entry ->
        entry.destination.route?.let { route -> !isMainRootRoute(route) } == true
    }
    val shouldRunDepthEffects = !isMainRootScreen || hasVisibleNonMainRootScreen

    // True when this screen is receding/sitting behind the active top destination
    val isUnderneath = remember(visibleEntries, myEntry, myIndex, topIndex, isNavigationTarget) {
        !isNavigationTarget &&
            myIndex != -1 &&
            topIndex != -1 &&
            myIndex < topIndex &&
            myEntry?.lifecycle?.currentState != Lifecycle.State.CREATED
    }

    val depthEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    // Previous screen blur: smoothly blurs up to 24dp as it zooms out, and clears to 0dp on return
    val targetBlur = if (shouldRunDepthEffects && isUnderneath) 24f else 0f
    val blurRadius by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 420, easing = depthEasing),
        label = "depthBlur"
    )

    // All screens running depth effects (zooming in, zooming out, or sitting underneath) stay smoothly rounded with modern 32dp card corners
    val cornerRadius = if (shouldRunDepthEffects) 32f else 0f

    // Subtle dim overlay on the receding background page
    val targetDim = if (shouldRunDepthEffects && isUnderneath) 0.28f else 0f
    val dimAlpha by animateFloatAsState(
        targetValue = targetDim,
        animationSpec = tween(durationMillis = 420, easing = depthEasing),
        label = "dimAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (shouldRunDepthEffects && cornerRadius > 0.5f) {
                    this.shape = RoundedCornerShape(cornerRadius.dp)
                    this.clip = true
                } else {
                    this.clip = false
                }
            }
            .then(
                if (AndroidVersionCompat.supportsHardwareBlur && blurRadius > 0.5f) {
                    Modifier.blur(blurRadius.dp)
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()

        // Dim Layer Overlay
        if (dimAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dimAlpha }
                    .background(Color.Black)
            )
        }
    }
}
