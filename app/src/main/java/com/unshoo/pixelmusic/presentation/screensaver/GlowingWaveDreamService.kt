package com.unshoo.pixelmusic.presentation.screensaver

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.unshoo.pixelmusic.ui.theme.PixelMusicTheme

// 1. We must implement LifecycleOwner and SavedStateRegistryOwner
class GlowingWaveDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    // 2. Create the registries that Compose requires
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Tell Compose the screen is now visible
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        isInteractive = true
        isFullscreen = true
        isScreenBright = false 
        
        val composeView = ComposeView(this).apply {
            // 3. Attach our custom lifecycles directly to the ComposeView
            setViewTreeLifecycleOwner(this@GlowingWaveDreamService)
            setViewTreeSavedStateRegistryOwner(this@GlowingWaveDreamService)
            
            setContent {
                PixelMusicTheme {
                    GlowingWaveScreen(onTimeout = { finish() })
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Tell Compose the screen is going away
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
