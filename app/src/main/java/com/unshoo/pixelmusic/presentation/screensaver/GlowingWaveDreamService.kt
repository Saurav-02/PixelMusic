package com.unshoo.pixelmusic.presentation.screensaver

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import com.unshoo.pixelmusic.ui.theme.PixelMusicTheme

class GlowingWaveDreamService : DreamService() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        isInteractive = true
        isFullscreen = true
        isScreenBright = false 
        
        val composeView = ComposeView(this).apply {
            setContent {
                PixelMusicTheme {
                    GlowingWaveScreen(onTimeout = { finish() })
                }
            }
        }
        setContentView(composeView)
    }
}

