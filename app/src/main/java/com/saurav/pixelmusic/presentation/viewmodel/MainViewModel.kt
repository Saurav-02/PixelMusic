package com.saurav.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.repository.MusicRepository
import com.saurav.pixelmusic.data.worker.SyncManager
import com.saurav.pixelmusic.data.worker.SyncProgress
import com.saurav.pixelmusic.data.worker.YouTubeLibrarySyncManager
import com.saurav.pixelmusic.utils.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.saurav.pixelmusic.data.preferences.PlaylistPreferencesRepository

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val youTubeLibrarySyncManager: YouTubeLibrarySyncManager,
    private val datastoreRepository: com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    musicRepository: MusicRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            playlistPreferencesRepository.pruneExpiredPlaylists()
        }
        viewModelScope.launch {
            datastoreRepository.cookies.collect { cookies ->
                val rawCookie = cookies.toRawCookie()
                saurav.shru.pixelmusic.innertube.YouTube.cookie = rawCookie
                LogUtils.d(this@MainViewModel, "MainViewModel: Syncing cookies to YouTube singleton. Size = ${rawCookie.length}")
                if (rawCookie.isNotEmpty()) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            saurav.shru.pixelmusic.innertube.YouTube.accountInfo()
                                .onSuccess { info ->
                                    datastoreRepository.saveYtProfile(
                                        name = info.name,
                                        handle = info.channelHandle ?: "",
                                        avatarUrl = info.thumbnailUrl ?: ""
                                    )
                                }
                                .onFailure { e ->
                                    LogUtils.e(this@MainViewModel, e, "Failed to fetch YouTube account info")
                                }
                        } catch (e: Exception) {
                            LogUtils.e(this@MainViewModel, e, "Error fetching YouTube account info")
                        }
                    }
                } else {
                    viewModelScope.launch {
                        datastoreRepository.saveYtProfile("", "", "")
                    }
                }
            }
        }
        viewModelScope.launch {
            datastoreRepository.dataSyncId.collect { id ->
                saurav.shru.pixelmusic.innertube.YouTube.dataSyncId = id
                LogUtils.d(this@MainViewModel, "MainViewModel: Syncing dataSyncId to YouTube singleton.")
            }
        }
    }

    val isSetupComplete: StateFlow<Boolean?> = userPreferencesRepository.initialSetupDoneFlow
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val hasCompletedInitialSync: StateFlow<Boolean> = userPreferencesRepository.lastSyncTimestampFlow
        .map { it > 0L }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true // 乐观策略：默认已同步
        )

    /**
     * Un Flow que emite `true` si el SyncWorker está encolado o en ejecución.
     * Ideal para mostrar un indicador de carga.
     */
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * Flow that exposes detailed sync progress including file count and phase.
     */
    val syncProgress: StateFlow<SyncProgress> = syncManager.syncProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress()
        )

    /**
     * Un Flow que emite `true` si la base de datos de Room no tiene canciones.
     * Nos ayuda a saber si es la primera vez que se abre la app.
     */
    val isLibraryEmpty: StateFlow<Boolean> = musicRepository
        .getAudioFiles()
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Función para iniciar la sincronización de la biblioteca de música.
     * Se debe llamar después de que los permisos hayan sido concedidos.
     */
    fun startSync() {
        LogUtils.i(this, "startSync called")
        viewModelScope.launch {
            // For fresh installs after setup, SetupViewModel.setSetupComplete() triggers sync
            // For returning users (setup already complete), we trigger sync here
            if (isSetupComplete.value == true) {
                syncManager.sync()
                // Also sync YouTube library (subscribed artists + liked songs) in background
                youTubeLibrarySyncManager.syncNow()
            }
        }
    }
}
