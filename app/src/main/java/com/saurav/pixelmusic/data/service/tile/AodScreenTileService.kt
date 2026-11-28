package com.saurav.pixelmusic.data.service.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.preferences.AodScreenStyle
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile that cycles the AOD Screen mode across:
 * Disabled -> Live Lyrics -> Ambient Artwork -> Disabled,
 * staying synchronized with UserPreferencesRepository.aodScreenStyleFlow.
 */
@RequiresApi(Build.VERSION_CODES.N)
class AodScreenTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AodScreenTileEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private val prefsRepo: UserPreferencesRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            AodScreenTileEntryPoint::class.java
        ).userPreferencesRepository()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch(Dispatchers.IO) {
            val currentStyle = prefsRepo.aodScreenStyleFlow.first()
            withContext(Dispatchers.Main) {
                applyStyleToTile(currentStyle)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch(Dispatchers.IO) {
            val currentStyle = prefsRepo.aodScreenStyleFlow.first()
            val nextStyle = when (currentStyle) {
                AodScreenStyle.DISABLED -> AodScreenStyle.LYRICS
                AodScreenStyle.LYRICS -> AodScreenStyle.DEFAULT
                AodScreenStyle.DEFAULT -> AodScreenStyle.DISABLED
            }
            prefsRepo.setAodScreenStyle(nextStyle)
            withContext(Dispatchers.Main) {
                applyStyleToTile(nextStyle)
            }
        }
    }

    private fun applyStyleToTile(style: AodScreenStyle) {
        qsTile?.apply {
            label = "AOD Screen"
            when (style) {
                AodScreenStyle.DISABLED -> {
                    state = Tile.STATE_INACTIVE
                    icon = Icon.createWithResource(this@AodScreenTileService, R.drawable.ic_aod_off)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = "Disabled"
                    }
                }
                AodScreenStyle.LYRICS -> {
                    state = Tile.STATE_ACTIVE
                    icon = Icon.createWithResource(this@AodScreenTileService, R.drawable.ic_aod_lyrics)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = "Live Lyrics"
                    }
                }
                AodScreenStyle.DEFAULT -> {
                    state = Tile.STATE_ACTIVE
                    icon = Icon.createWithResource(this@AodScreenTileService, R.drawable.ic_aod_album_art)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = "Ambient Artwork"
                    }
                }
            }
            updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
