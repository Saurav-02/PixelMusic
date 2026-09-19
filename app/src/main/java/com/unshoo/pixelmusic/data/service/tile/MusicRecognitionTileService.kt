package com.unshoo.pixelmusic.data.service.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.unshoo.pixelmusic.presentation.screens.RecognitionOverlayActivity

class MusicRecognitionTileService : TileService() {
    
    override fun onStartListening() {
        super.onStartListening()
        // Force the tile into an ACTIVE state so custom ROMs register the click
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        
        // Removed CLEAR_TASK to prevent Android 14+ background launch blocking
        val intent = Intent(this, RecognitionOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        if (Build.VERSION.SDK_INT >= 34) { 
            val pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
