package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.model.ProfileAvatarCatalog
import com.saurav.pixelmusic.data.model.UserProfile
import com.saurav.pixelmusic.data.preferences.ProfilePhotoHelper
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository
import com.saurav.pixelmusic.data.session.PixelJamManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileScreenUiState(
    val profile: UserProfile = UserProfile(),
    val ytUsername: String = "",
    val ytAvatarUrl: String = "",
    val isSavingPhoto: Boolean = false,
    val statusMessage: String? = null
) {
    val effectiveDisplayName: String
        get() = profile.effectiveDisplayName(ytUsername, "")

    val effectiveLocalAvatar: String
        get() = profile.effectiveLocalAvatar(ytAvatarUrl)
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val youtubeDatastoreRepository: DatastoreRepository,
    private val pixelJamManager: PixelJamManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSavingPhoto = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileScreenUiState> = combine(
        userPreferencesRepository.userProfileFlow,
        youtubeDatastoreRepository.ytUsername,
        youtubeDatastoreRepository.ytAvatarUrl,
        _isSavingPhoto,
        _statusMessage
    ) { profile, ytUser, ytAvatar, saving, message ->
        ProfileScreenUiState(
            profile = profile,
            ytUsername = ytUser,
            ytAvatarUrl = ytAvatar,
            isSavingPhoto = saving,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileScreenUiState()
    )

    fun saveDisplayName(name: String) {
        val clean = name.trim().take(32)
        viewModelScope.launch {
            userPreferencesRepository.setProfileDisplayName(clean)
            if (clean.isNotBlank()) {
                pixelJamManager.saveDisplayName(clean)
            }
        }
    }

    fun selectBuiltInAvatar(avatarId: String, backgroundId: String) {
        viewModelScope.launch {
            ProfilePhotoHelper.deleteLocalPhoto(context)
            userPreferencesRepository.setProfileBuiltInAvatar(avatarId, backgroundId)
        }
    }

    fun selectRandomBuiltInAvatar() {
        val (avatar, bg) = ProfileAvatarCatalog.randomBuiltIn()
        selectBuiltInAvatar(avatar.id, bg.id)
    }

    fun useFirstLetterAvatar() {
        viewModelScope.launch {
            ProfilePhotoHelper.deleteLocalPhoto(context)
            userPreferencesRepository.clearProfileAvatar(forceInitialOnly = true)
        }
    }

    fun useGoogleAccountPhoto() {
        viewModelScope.launch {
            ProfilePhotoHelper.deleteLocalPhoto(context)
            userPreferencesRepository.clearProfileAvatar(forceInitialOnly = false)
            _statusMessage.value = "Using Google account photo"
        }
    }

    fun setPickedPhoto(uri: Uri) {
        if (_isSavingPhoto.value) return
        viewModelScope.launch {
            _isSavingPhoto.value = true
            val result = ProfilePhotoHelper.savePickedPhoto(context, uri)
            result.onSuccess { (localPath, wireDataUri) ->
                userPreferencesRepository.setProfileCustomPhoto(localPath, wireDataUri)
                _statusMessage.value = "Profile photo updated"
            }.onFailure {
                _statusMessage.value = "Could not load selected photo"
            }
            _isSavingPhoto.value = false
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }
}
