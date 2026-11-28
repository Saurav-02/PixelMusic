package com.saurav.pixelmusic.data.service.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.PixelToast
import com.saurav.pixelmusic.presentation.components.PixelToastType
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.utils.AndroidVersionCompat
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
 * Quick Settings tile to toggle the Live Activity / Dynamic Island feature
 * (supported on Android 16+ and compatible OEM status bar capsule ROMs),
 * staying synchronized with UserPreferencesRepository.dynamicIslandEnabledFlow.
 */
@RequiresApi(Build.VERSION_CODES.N)
class DynamicIslandTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DynamicIslandTileEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    private val prefsRepo: UserPreferencesRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DynamicIslandTileEntryPoint::class.java
        ).userPreferencesRepository()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        if (!AndroidVersionCompat.supportsLiveNotificationOrIsland()) {
            applyUnsupportedState()
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            val enabled = prefsRepo.dynamicIslandEnabledFlow.first()
            withContext(Dispatchers.Main) {
                applyEnabledState(enabled)
            }
        }
    }

    override fun onClick() {
        super.onClick()
        if (!AndroidVersionCompat.supportsLiveNotificationOrIsland()) {
            applyUnsupportedState()
            PixelToast.show(
                this,
                "Dynamic Island requires Android 16+ or a supported device",
                type = PixelToastType.ERROR
            )
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            val currentEnabled = prefsRepo.dynamicIslandEnabledFlow.first()
            val nextEnabled = !currentEnabled
            prefsRepo.setDynamicIslandEnabled(nextEnabled)
            withContext(Dispatchers.Main) {
                applyEnabledState(nextEnabled)
            }
        }
    }

    private fun applyEnabledState(enabled: Boolean) {
        qsTile?.apply {
            label = "Dynamic Island"
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@DynamicIslandTileService, R.drawable.ic_capsule_horizontal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (enabled) "On" else "Off"
            }
            updateTile()
        }
    }

    private fun applyUnsupportedState() {
        qsTile?.apply {
            label = "Dynamic Island"
            state = Tile.STATE_UNAVAILABLE
            icon = Icon.createWithResource(this@DynamicIslandTileService, R.drawable.ic_capsule_horizontal)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Android 16+"
            }
            updateTile()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
