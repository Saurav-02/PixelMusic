package com.unshoo.pixelmusic.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import com.unshoo.pixelmusic.presentation.viewmodel.PlayerViewModel
import androidx.lifecycle.compose.currentStateAsState
import com.unshoo.pixelmusic.presentation.navigation.isMainRootRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding


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
    // It stays stable while entries are entering / exiting, unlike the restricted currentBackStack.
    val visibleEntries by navController.visibleEntries.collectAsStateWithLifecycle()
    val myEntry = lifecycleOwner as? androidx.navigation.NavBackStackEntry
    val myIndex = visibleEntries.indexOfFirst { it.id == myEntry?.id }
    val topIndex = visibleEntries.indexOfLast {
        it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    // currentBackStackEntry updates synchronously with navigate()/popBackStack(), so it
    // identifies the destination the user is moving TO. The incoming screen during a pop
    // shares STARTED state with the outgoing one for a few frames; without this check the
    // dim overlay would flash onto the screen the user is navigating back to.
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isNavigationTarget = myEntry != null && currentBackStackEntry?.id == myEntry.id
    val myRoute = myEntry?.destination?.route
    val isMainRootScreen = isMainRootRoute(myRoute)
    val hasVisibleNonMainRootScreen = visibleEntries.any { entry ->
        entry.destination.route?.let { route -> !isMainRootRoute(route) } == true
    }
    val shouldRunDepthEffects = !isMainRootScreen || hasVisibleNonMainRootScreen

    // Dim Logic:
    // If I am BACKGROUND (myIndex < topIndex) -> Dim.
    // If I am TOP (myIndex == topIndex) -> Clear.
    // If I am EXITING (myIndex > topIndex, effectively in front during pop) -> Clear.
    // If I am the navigation target (incoming during a pop) -> Clear.
    // Created entries are on their way out, so we keep them clear instead of dimming them for a frame.
    val shouldDim = remember(visibleEntries, myEntry, myIndex, topIndex, isNavigationTarget) {
        !isNavigationTarget &&
            myIndex != -1 &&
            topIndex != -1 &&
            myIndex < topIndex &&
            myEntry?.lifecycle?.currentState != Lifecycle.State.CREATED
    }

    // Declarative Animations
    // Radius: If NOT Resumed -> 32dp. (Background OR Popped)
    val targetRadius = if (shouldRunDepthEffects && !isResumed) 32f else 0f
    val cornerRadius by animateFloatAsState(
        targetValue = targetRadius,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cornerRadius"
    )

    // Dim: If strictly behind Top -> 0.4f. Else -> 0f.
    val targetDim = if (shouldRunDepthEffects && shouldDim) 0.4f else 0f
    val dimAlpha by animateFloatAsState(
        targetValue = targetDim,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dimAlpha"
    )

    // Hook into your data preference layer here or pass this state via parameters 
    // val navBarStyle by userPreferences.navBarStyle.collectAsStateWithLifecycle(initialValue = NavBarStyle.DEFAULT)
    val navBarStyle = "floating_pill" // Temporary hardcoded check matching step 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = if (shouldRunDepthEffects) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                if (shouldRunDepthEffects && cornerRadius > 0.5f) {
                    this.shape = RoundedCornerShape(cornerRadius.dp)
                    this.clip = true
                } else {
                    this.clip = false
                }
            }
            .background(MaterialTheme.colorScheme.background)
            // Add safety layout margins to clear space for the floating bar overlay
            .then(
                if (isMainRootScreen && navBarStyle == "floating_pill") {
                    Modifier.padding(bottom = 100.dp) // Accounts for bar height + padding space
                } else {
                    Modifier
                }
            )
    ) {
        content()

        // Dim Layer Overlay
        // Always composed with alpha-driven visibility instead of a conditional node.
        // Conditionally adding/removing this Box when dimAlpha crosses 0 added a node to
        // the composition tree mid-transition and contributed to the outgoing-screen flash.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dimAlpha }
                .background(Color.Black)
        )
    }
}
