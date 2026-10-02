package com.saurav.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.remote.youtube.DatastoreRepository
import com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper
import com.saurav.pixelmusic.data.remote.youtube.PlaylistItem
import com.saurav.pixelmusic.data.remote.youtube.AlbumItem
import com.saurav.pixelmusic.data.remote.youtube.ArtistItem
import com.saurav.pixelmusic.data.remote.youtube.YoutubeRequestHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val datastoreRepository: DatastoreRepository,
    private val syncManager: com.saurav.pixelmusic.data.worker.SyncManager
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)
    
    // Selected content type for chips
    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        fetchLibrary()
    }

    fun fetchLibrary() {
        viewModelScope.launch {
            try {
                if (datastoreRepository.cookies.first().toRawCookie().isNotEmpty()) {
                    syncManager.forceRefresh()
                }
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to auto-refresh YouTube library on load", e)
            }
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val settings = datastoreRepository.settings.first()
                
                withContext(Dispatchers.IO) {
                    try {
                        val playlistsJson = YoutubeRequestHelper.browse("FEmusic_liked_playlists", settings)
                        playlists.value = YoutubeHelper.extractAccountPlaylists(playlistsJson, settings)
                    } catch (e: Exception) {
                        Log.e("AccountViewModel", "Failed to fetch playlists", e)
                    }

                    try {
                        val albumsJson = YoutubeRequestHelper.browse("FEmusic_liked_albums", settings)
                        albums.value = YoutubeHelper.extractAccountAlbums(albumsJson, settings)
                    } catch (e: Exception) {
                        Log.e("AccountViewModel", "Failed to fetch albums", e)
                    }

                    try {
                        val artistsJson = YoutubeRequestHelper.browse("FEmusic_library_corpus_artists", settings)
                        artists.value = YoutubeHelper.extractAccountArtists(artistsJson, settings)
                    } catch (e: Exception) {
                        Log.e("AccountViewModel", "Failed to fetch artists", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to load library settings", e)
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
