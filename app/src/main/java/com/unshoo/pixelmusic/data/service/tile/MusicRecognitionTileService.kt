package com.unshoo.pixelmusic.data.service.tile

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.unshoo.pixelmusic.presentation.screens.RecognitionOverlayActivity

class MusicRecognitionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Initialize the tile state so FuntouchOS and Android know it is ready to be clicked
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        
        val intent = Intent(this, RecognitionOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            // Explicitly grant the PendingIntent permission to start from the background
            val options = ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
            
            // Added FLAG_UPDATE_CURRENT to ensure the intent data refreshes properly
            val pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                options.toBundle()
            )
            
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
