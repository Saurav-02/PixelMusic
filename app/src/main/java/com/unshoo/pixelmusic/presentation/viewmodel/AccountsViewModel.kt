package com.unshoo.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unshoo.pixelmusic.data.gdrive.GDriveRepository
import com.unshoo.pixelmusic.data.repository.MusicRepository
import com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

enum class ExternalServiceAccount {
    GOOGLE_DRIVE,
    YOUTUBE,
    LASTFM
}

data class ExternalAccountUiModel(
    val service: ExternalServiceAccount,
    val title: String,
    val accountLabel: String,
    val syncedContentLabel: String,
    val isLoggingOut: Boolean
)

data class AccountsUiState(
    val connectedAccounts: List<ExternalAccountUiModel> = emptyList(),
    val disconnectedServices: List<ExternalServiceAccount> = emptyList(),
    val userName: String? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val gDriveRepository: GDriveRepository,
    private val datastoreRepository: com.unshoo.pixelmusic.data.remote.youtube.DatastoreRepository,
    private val syncManager: com.unshoo.pixelmusic.data.worker.SyncManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isSyncing = syncManager.isSyncing

    fun syncLibrary() {
        viewModelScope.launch {
            syncManager.sync()
        }
    }

    private val loggingOutServices = MutableStateFlow<Set<ExternalServiceAccount>>(emptySet())

    private val gDriveStateFlow = combine(
        gDriveRepository.isLoggedInFlow,
        gDriveRepository.getFolders().map { it.size }
    ) { connected, folderCount ->
        connected to folderCount
    }

    private val youtubeStateFlow = combine(
        datastoreRepository.cookies.map { it.toRawCookie().isNotEmpty() }.distinctUntilChanged(),
        com.unshoo.pixelmusic.data.database.youtube.AppDatabase.getInstance(context).playlistRepository().observeAll().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    private val lastfmStateFlow = combine(
        userPreferencesRepository.lastfmSessionFlow,
        userPreferencesRepository.lastfmUsernameFlow,
        userPreferencesRepository.lastfmScrobblingEnabledFlow
    ) { session, username, enabled ->
        Triple(session.isNotEmpty(), username, enabled)
    }

    val uiState: StateFlow<AccountsUiState> = combine(
        combine(
            listOf(
                gDriveStateFlow,
                youtubeStateFlow,
                lastfmStateFlow
            )
        ) { it.toList() },
        loggingOutServices,
        datastoreRepository.ytUsername
    ) { states, activeLogouts, ytName ->
        val (gDriveConnected, gDriveFolderCount) = states[0] as Pair<Boolean, Int>
        val (youtubeConnected, youtubePlaylistCount) = states[1] as Pair<Boolean, Int>
        val (lastfmConnected, lastfmUsername, lastfmScrobbleEnabled) = states[2] as Triple<Boolean, String, Boolean>

        val calculatedUserName = when {
            gDriveConnected && !gDriveRepository.userDisplayName.isNullOrBlank() -> gDriveRepository.userDisplayName
            youtubeConnected && ytName.isNotBlank() -> ytName
            else -> null
        }

        val connectedAccounts = buildList {
            if (gDriveConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.GOOGLE_DRIVE,
                        title = "Google Drive",
                        accountLabel = gDriveRepository.userDisplayName
                            ?.takeIf { it.isNotBlank() }
                            ?: gDriveRepository.userEmail
                                ?.takeIf { it.isNotBlank() }
                            ?: "Google account connected",
                        syncedContentLabel = formatCount(
                            count = gDriveFolderCount,
                            singular = "synced folder",
                            plural = "synced folders"
                        ),
                        isLoggingOut = ExternalServiceAccount.GOOGLE_DRIVE in activeLogouts
                    )
                )
            }
            if (youtubeConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.YOUTUBE,
                        title = "YouTube Client",
                        accountLabel = if (ytName.isNotBlank()) ytName else "YouTube session connected",
                        syncedContentLabel = formatCount(
                            count = youtubePlaylistCount,
                            singular = "synced playlist",
                            plural = "synced playlists"
                        ),
                        isLoggingOut = ExternalServiceAccount.YOUTUBE in activeLogouts
                    )
                )
            }
            if (lastfmConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.LASTFM,
                        title = "Last.fm",
                        accountLabel = if (lastfmUsername.isNotBlank()) lastfmUsername else "Last.fm session connected",
                        syncedContentLabel = if (lastfmScrobbleEnabled) "Scrobbling enabled" else "Scrobbling disabled",
                        isLoggingOut = ExternalServiceAccount.LASTFM in activeLogouts
                    )
                )
            }
        }

        val disconnectedServices = buildList {
            if (!gDriveConnected) add(ExternalServiceAccount.GOOGLE_DRIVE)
            if (!youtubeConnected) add(ExternalServiceAccount.YOUTUBE)
            if (!lastfmConnected) add(ExternalServiceAccount.LASTFM)
        }

        AccountsUiState(
            connectedAccounts = connectedAccounts,
            disconnectedServices = disconnectedServices,
            userName = calculatedUserName
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun logout(service: ExternalServiceAccount) {
        if (service in loggingOutServices.value) return

        viewModelScope.launch {
            loggingOutServices.update { it + service }
            try {
                runCatching {
                    when (service) {
                        ExternalServiceAccount.GOOGLE_DRIVE -> gDriveRepository.logout()
                        ExternalServiceAccount.YOUTUBE -> {
                            datastoreRepository.saveCookies(com.unshoo.pixelmusic.data.model.youtube.Cookies(""))
                            datastoreRepository.saveDataSyncId("")
                            com.unshoo.pixelmusic.data.database.youtube.AppDatabase.clearDownloads(context)
                        }
                        ExternalServiceAccount.LASTFM -> {
                            userPreferencesRepository.setLastfmSession("")
                            userPreferencesRepository.setLastfmUsername("")
                            userPreferencesRepository.setLastfmApiKey("")
                            userPreferencesRepository.setLastfmApiSecret("")
                            com.unshoo.pixelmusic.data.lastfm.LastFM.sessionKey = null
                            com.unshoo.pixelmusic.data.lastfm.LastFM.initialize(
                                apiKey = com.unshoo.pixelmusic.BuildConfig.LASTFM_API_KEY,
                                secret = com.unshoo.pixelmusic.BuildConfig.LASTFM_SECRET
                            )
                        }
                    }
                }
            } finally {
                loggingOutServices.update { it - service }
            }
        }
    }

    private fun formatCount(count: Int, singular: String, plural: String): String {
        return if (count == 1) {
            "1 $singular"
        } else {
            "$count $plural"
        }
    }
}
