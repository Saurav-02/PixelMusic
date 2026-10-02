package com.saurav.pixelmusic.presentation.screens.youtube

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.remote.youtube.Constants
import com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository
import com.saurav.pixelmusic.data.remote.youtube.MAX_YT_ACCOUNTS
import com.saurav.pixelmusic.data.model.youtube.Cookies
import com.saurav.pixelmusic.data.worker.SyncManager
import saurav.shru.pixelmusic.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val datastoreRepository: DatastoreRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    //  val uiState = _uiState.asStateFlow()

    private val _eventsChannel = MutableSharedFlow<ScreenEvent.Out>()
    val eventFlow = _eventsChannel.asSharedFlow()

    fun onPageFinished(url: String?) {
        viewModelScope.launch {
            if (url?.contains(Constants.Auth.END_URL) == true && !_uiState.value.isLoggedIn) {
                val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
                // First login goes to slot 1; "Add another account" goes to slot 2
                // and becomes active atomically (no guest-state flicker).
                val slot = if (datastoreRepository.ytAccountCount() >= 1) 2 else 1
                _uiState.update { it.copy(isLoggedIn = true) }
                // Guard: the WebView may still carry an already-added account's
                // session — never store the same session twice.
                if (slot == 2 && isDuplicateSession(cookies, excludeSlot = 2)) {
                    _eventsChannel.emit(ScreenEvent.Out.LoginDuplicate)
                    return@launch
                }
                datastoreRepository.saveYtAccountSession(slot, Cookies(cookies), null, "", "", "")
                YouTube.cookie = cookies
                _eventsChannel.emit(ScreenEvent.Out.LoginCompleted)
                // Trigger an immediate background synchronization of user playlists and library
                syncManager.fullSync()
            }
        }
    }

    fun onDataSyncIdFound(dataSyncId: String) {
        viewModelScope.launch {
            datastoreRepository.saveDataSyncId(dataSyncId)
            YouTube.dataSyncId = dataSyncId
        }
    }

    fun saveManualCookie(tokenString: String) {
        viewModelScope.launch {
            var cookieStr = ""
            var dataSyncId = ""
            var accountName = ""
            var accountHandle = ""

            // 1. Check if it's the formatted ArchiveTune token string
            if (tokenString.contains("***INNERTUBE COOKIE***")) {
                // Helper to extract the value between the = and the next ***
                fun extractBlock(key: String): String {
                    val header = "***$key***"
                    val startIdx = tokenString.indexOf(header)
                    if (startIdx == -1) return ""
                    
                    val equalsIdx = tokenString.indexOf("=", startIdx + header.length)
                    if (equalsIdx == -1) return ""
                    
                    val nextHeaderIdx = tokenString.indexOf("***", equalsIdx)
                    return if (nextHeaderIdx != -1) {
                        tokenString.substring(equalsIdx + 1, nextHeaderIdx)
                    } else {
                        tokenString.substring(equalsIdx + 1)
                    }.trim()
                }

                // Extract all the pieces perfectly
                cookieStr = extractBlock("INNERTUBE COOKIE")
                dataSyncId = extractBlock("DATASYNC ID")
                accountName = extractBlock("ACCOUNT NAME")
                accountHandle = extractBlock("ACCOUNT CHANNEL HANDLE")
            } else {
                // Fallback: If they just pasted a raw raw cookie string without the extra metadata
                cookieStr = tokenString.trim()
            }

            // 2-4. Save the whole session (cookies, dataSyncId, profile) into
            // slot 1 for the first login or slot 2 when adding another account,
            // atomically making it the active account.
            if (cookieStr.isNotEmpty()) {
                val slot = if (datastoreRepository.ytAccountCount() >= 1) 2 else 1
                if (slot == 2 && isDuplicateSession(cookieStr, excludeSlot = 2)) {
                    _uiState.update { it.copy(isLoggedIn = true) }
                    _eventsChannel.emit(ScreenEvent.Out.LoginDuplicate)
                    return@launch
                }
                datastoreRepository.saveYtAccountSession(
                    slot = slot,
                    cookies = Cookies(cookieStr),
                    dataSyncId = dataSyncId.ifEmpty { null },
                    name = accountName,
                    handle = accountHandle,
                    avatarUrl = "",
                )
                YouTube.cookie = cookieStr
                if (dataSyncId.isNotEmpty()) {
                    YouTube.dataSyncId = dataSyncId
                }
            }

            // 5. Complete the login and trigger the background sync
            _uiState.update { it.copy(isLoggedIn = true) }
            _eventsChannel.emit(ScreenEvent.Out.LoginCompleted)
            syncManager.fullSync()
        }
    }

    fun getFullTokenString(): Flow<String> = kotlinx.coroutines.flow.flow {
        val cookies = datastoreRepository.cookies.first().toRawCookie()
        val settings = datastoreRepository.settings.first()
        val name = datastoreRepository.ytUsername.first()
        val handle = datastoreRepository.ytHandle.first()

        val token = buildString {
            if (cookies.isNotEmpty()) {
                append("***INNERTUBE COOKIE*** =\n")
                append(cookies)
                append("\n")
            }
            if (settings.dataSyncId.isNotEmpty()) {
                append("***DATASYNC ID*** =\n")
                append(settings.dataSyncId)
                append("\n")
            }
            if (name.isNotEmpty()) {
                append("***ACCOUNT NAME*** =\n")
                append(name)
                append("\n")
            }
            if (handle.isNotEmpty()) {
                append("***ACCOUNT CHANNEL HANDLE*** =\n")
                append(handle)
                append("\n")
            }
        }.trim()
        
        emit(token)
    }

    /**
     * True when [cookies] matches an already-stored account's session
     * (cookie order-insensitive), so the same account is never added twice.
     */
    private suspend fun isDuplicateSession(cookies: String, excludeSlot: Int): Boolean {
        val normalized = normalizeCookies(cookies)
        if (normalized.isEmpty()) return false
        for (slot in 1..MAX_YT_ACCOUNTS) {
            if (slot == excludeSlot) continue
            val existing = normalizeCookies(datastoreRepository.ytRawCookies(slot))
            if (existing.isNotEmpty() && existing == normalized) return true
        }
        return false
    }

    private fun normalizeCookies(raw: String): String =
        raw.split(";").map { it.trim() }.filter { it.isNotEmpty() }.sorted().joinToString(";")

    sealed interface ScreenEvent {
        sealed class Out {
            object LoginCompleted : Out()
            /** Tried to add an account whose session is already stored. */
            object LoginDuplicate : Out()
        }
    }
}
