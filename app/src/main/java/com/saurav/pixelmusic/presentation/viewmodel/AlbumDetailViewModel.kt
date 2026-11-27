package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.model.Album
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.repository.MusicRepository // Importar MusicRepository
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import saurav.shru.pixelmusic.innertube.YouTube as InnerTubeYouTube
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.saurav.pixelmusic.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        val albumIdString: String? = savedStateHandle.get("albumId")
        if (albumIdString != null) {
            viewModelScope.launch {
                val isLocalMode = userPreferencesRepository.localModeEnabledFlow.first()
                val albumId = albumIdString.toLongOrNull()
                val mappedBrowseId = if (isLocalMode) null else albumId?.let { SearchStateHolder.albumIdMap[it] }
                when {
                    mappedBrowseId != null -> loadOnlineAlbumData(mappedBrowseId)
                    albumId != null -> loadAlbumData(albumId, isLocalMode)
                    !isLocalMode -> loadOnlineAlbumData(albumIdString)
                    else -> _uiState.update {
                        it.copy(error = context.getString(R.string.album_not_found), isLoading = false)
                    }
                }
            }
        } else {
            _uiState.update { it.copy(error = context.getString(R.string.album_id_not_found), isLoading = false) }
        }
    }

    private fun loadAlbumData(id: Long, isLocalMode: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val localAlbum = musicRepository.getAlbumById(id).first()
                val localSongs = musicRepository.getSongsForAlbum(id).first()
                if (!isLocalMode) {
                // Check if any song in this album already carries a YouTube album browseId
                val songBrowseId = localSongs.firstNotNullOfOrNull { it.albumBrowseId?.takeIf { b -> b.isNotBlank() } }
                    ?: SearchStateHolder.albumIdMap[id]
                if (songBrowseId != null && (id < 0 || localSongs.all { it.contentUriString.startsWith("youtube://") })) {
                    SearchStateHolder.albumIdMap[id] = songBrowseId
                    loadOnlineAlbumData(songBrowseId, fallbackAlbum = localAlbum, fallbackSongs = localSongs)
                    return@launch
                }

                // For online/skeleton albums (id < 0) or missing local albums, try resolving on YouTube Music
                val isOnlineSkeletonAlbum = id < 0 || localSongs.isEmpty() || localSongs.all { it.contentUriString.startsWith("youtube://") }
                if (isOnlineSkeletonAlbum) {
                    val nameAndArtist = localAlbum?.let { it.title to it.artist } ?: SearchStateHolder.albumNameMap[id]
                    val albumTitle = nameAndArtist?.first?.trim()
                    val artistName = nameAndArtist?.second?.trim().orEmpty()
                    if (!albumTitle.isNullOrBlank() && !albumTitle.equals("YouTube Music", ignoreCase = true)) {
                        val query = if (artistName.isNotBlank() && !artistName.equals("Unknown Artist", ignoreCase = true)) {
                            "$albumTitle $artistName"
                        } else {
                            albumTitle
                        }
                        val searchResult = withContext(Dispatchers.IO) {
                            InnerTubeYouTube.search(query, InnerTubeYouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                        }
                        val matchedAlbum = searchResult?.items?.filterIsInstance<AlbumItem>()?.let { candidates ->
                            candidates.firstOrNull { it.title.equals(albumTitle, ignoreCase = true) }
                                ?: candidates.firstOrNull { it.title.contains(albumTitle, ignoreCase = true) || albumTitle.contains(it.title, ignoreCase = true) }
                                ?: candidates.firstOrNull()
                        }
                        if (matchedAlbum != null) {
                            SearchStateHolder.albumIdMap[id] = matchedAlbum.browseId
                            loadOnlineAlbumData(matchedAlbum.browseId, fallbackAlbum = localAlbum, fallbackSongs = localSongs)
                            return@launch
                        }
                    }
                }
                }

                val albumDetailsFlow = musicRepository.getAlbumById(id)
                val albumSongsFlow = musicRepository.getSongsForAlbum(id)

                combine(albumDetailsFlow, albumSongsFlow) { album, songs ->
                    val visibleSongs = if (isLocalMode) {
                        songs.filter { s ->
                            (!s.contentUriString.startsWith("youtube://", ignoreCase = true) &&
                                !s.contentUriString.startsWith("http://", ignoreCase = true) &&
                                !s.contentUriString.startsWith("https://", ignoreCase = true)) ||
                                (s.path.isNotBlank() && java.io.File(s.path).exists())
                        }
                    } else {
                        songs
                    }
                    val resolvedAlbum = album ?: visibleSongs.firstOrNull()?.let { first ->
                        Album(
                            id = id,
                            title = first.album.ifBlank { "Unknown Album" },
                            artist = first.albumArtist?.takeIf { it.isNotBlank() } ?: first.artist,
                            songCount = visibleSongs.size,
                            albumArtUriString = first.albumArtUriString,
                            year = first.year,
                            dateAdded = first.dateAdded
                        )
                    }
                    if (resolvedAlbum != null) {
                        AlbumDetailUiState(
                            album = resolvedAlbum,
                            songs = visibleSongs.sortedWith(
                                compareBy<Song> { it.discNumber ?: 1 }
                                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                    .thenBy { it.title.lowercase() }
                            ),
                            isLoading = false
                        )
                    } else {
                        AlbumDetailUiState(
                            error = context.getString(R.string.album_not_found),
                            isLoading = false
                        )
                    }
                }
                    .catch { e ->
                        emit(
                            AlbumDetailUiState(
                                error = context.getString(R.string.error_loading_album, e.localizedMessage ?: ""),
                                isLoading = false
                            )
                        )
                    }
                    .collect { newState ->
                        _uiState.value = newState
                    }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.error_loading_album, e.localizedMessage ?: ""),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun update(songs: List<Song>) {
        _uiState.update {
            it.copy(
                isLoading = false,
                songs = songs
            )
        }
    }

    private fun loadOnlineAlbumData(
        browseId: String,
        fallbackAlbum: Album? = null,
        fallbackSongs: List<Song> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.album(browseId)
                }
                result.onSuccess { albumPage ->
                    val albumItem = albumPage.album
                    val artistText = albumItem.artists?.joinToString { it.name }
                        ?.takeIf { it.isNotBlank() }
                        ?: fallbackAlbum?.artist
                        ?: ""
                    val albumModel = Album(
                        id = albumItem.browseId.hashCode().toLong(),
                        title = albumItem.title,
                        artist = artistText,
                        year = albumItem.year ?: fallbackAlbum?.year ?: 0,
                        dateAdded = System.currentTimeMillis(),
                        albumArtUriString = albumItem.thumbnail,
                        songCount = albumPage.songs.size,
                        albumArtist = artistText
                    )
                    val songsModels = albumPage.songs.mapIndexed { index, songItem ->
                        songItem.toNativeSong().copy(
                            trackNumber = index + 1,
                            year = albumItem.year ?: 0
                        )
                    }
                    val effectiveSongs = songsModels.ifEmpty { fallbackSongs }

                    if (songsModels.isNotEmpty()) {
                        musicRepository.insertYoutubeSongs(songsModels)
                    }

                    _uiState.value = AlbumDetailUiState(
                        album = albumModel,
                        songs = effectiveSongs,
                        isLoading = false,
                        error = null
                    )
                }.onFailure { e ->
                    if (fallbackAlbum != null && fallbackSongs.isNotEmpty()) {
                        _uiState.value = AlbumDetailUiState(
                            album = fallbackAlbum,
                            songs = fallbackSongs,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to load online album", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                if (fallbackAlbum != null && fallbackSongs.isNotEmpty()) {
                    _uiState.value = AlbumDetailUiState(
                        album = fallbackAlbum,
                        songs = fallbackSongs,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
                }
            }
        }
    }
}
