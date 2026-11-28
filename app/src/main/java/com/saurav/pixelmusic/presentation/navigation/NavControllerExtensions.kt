package com.saurav.pixelmusic.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

private var lastNavTime = 0L
private var lastNavRoute = ""

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        // We allow navigation if the current entry is at least STARTED.
        // This is safer than strictly RESUMED as transitions can sometimes delay RESUMED state.
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    val now = System.currentTimeMillis()
    if (route == lastNavRoute && now - lastNavTime < 400L) {
        return false
    }
    lastNavTime = now
    lastNavRoute = route
    navigate(route) {
        launchSingleTop = false
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    val now = System.currentTimeMillis()
    if (route == lastNavRoute && now - lastNavTime < 400L) {
        return false
    }
    lastNavTime = now
    lastNavRoute = route
    navigate(route) {
        launchSingleTop = false
        builder()
    }
    return true
}

fun NavController.navigateSafelyReplacing(
    route: String,
    patternToPop: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = false
        popUpTo(patternToPop) {
            inclusive = true
        }
        builder()
    }
    return true
}

fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute == route) return true

    val now = System.currentTimeMillis()
    if (route == lastNavRoute && now - lastNavTime < 250L) {
        return false
    }
    lastNavTime = now
    lastNavRoute = route

    val startDestinationId = runCatching { graph.startDestinationId }.getOrNull() ?: return false
    val targetNodeId = runCatching { graph.findNode(route)?.id }.getOrNull()

    // When returning to the graph's start destination (e.g. from Settings or any detail screen),
    // pop everything stacked above it instead of saving/restoring stale nested back stacks.
    if (targetNodeId != null && targetNodeId == startDestinationId) {
        if (popBackStack(startDestinationId, inclusive = false)) {
            return true
        }
    }

    navigate(route) {
        popUpTo(startDestinationId) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
    return true
}

fun NavController.navigateToSettingsSafely(): Boolean {
    if (!isReadyForNavigation()) return false
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute == Screen.Settings.route) return true

    val now = System.currentTimeMillis()
    if (Screen.Settings.route == lastNavRoute && now - lastNavTime < 250L) {
        return false
    }
    lastNavTime = now
    lastNavRoute = Screen.Settings.route

    // If Settings is already in the back stack (e.g. user is inside a Settings sub-screen),
    // pop cleanly back to Settings root instead of stacking a duplicate Settings entry.
    if (popBackStack(Screen.Settings.route, inclusive = false)) {
        return true
    }

    navigate(Screen.Settings.route) {
        launchSingleTop = true
    }
    return true
}
