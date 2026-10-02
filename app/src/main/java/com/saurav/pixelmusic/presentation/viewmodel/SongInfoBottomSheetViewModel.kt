package com.saurav.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.database.toArtist
import com.saurav.pixelmusic.data.model.Artist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.utils.AudioMeta
import com.saurav.pixelmusic.utils.AudioMetaUtils
import saurav.shru.pixelmusic.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SongInfoBottomSheetViewModel @Inject constructor(
    private val musicDao: MusicDao
) : ViewModel() {

    data class SongLocationInfo(
        val label: String,
        val value: String,
        val isCloud: Boolean,
    )

    private val _audioMeta = MutableStateFlow<AudioMeta?>(null)
    val audioMeta: StateFlow<AudioMeta?> = _audioMeta.asStateFlow()

    private val _resolvedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val resolvedArtists: StateFlow<List<Artist>> = _resolvedArtists.asStateFlow()

    fun loadArtistsForSong(song: Song) {
        val refs = song.artists
        if (refs.isEmpty() || refs.size < 2) {
            _resolvedArtists.value = emptyList()
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ids = refs.map { it.id }.filter { it != -1L && it != 0L }.distinct()
            val entitiesById = if (ids.isNotEmpty()) {
                musicDao.getArtistsByIds(ids).associateBy { it.id }
            } else {
                emptyMap()
            }
            val resolved = refs.map { ref ->
                entitiesById[ref.id]?.toArtist()
                    ?: Artist(id = ref.id, name = ref.name, songCount = 0)
            }
            _resolvedArtists.value = resolved
        }
    }

    fun loadAudioMeta(song: Song) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val meta = AudioMetaUtils.getAudioMetadata(
                musicDao = musicDao,
                id = song.id.toLongOrNull() ?: -1L,
                filePath = song.path,
                deepScan = false
            )
            _audioMeta.value = meta
        }
    }

    fun getSongLocationInfo(song: Song): SongLocationInfo {
        val provider = getCloudProviderLabel(song.contentUriString)
        return if (provider != null) {
            SongLocationInfo(
                label = "Provider",
                value = provider,
                isCloud = true,
            )
        } else {
            SongLocationInfo(
                label = "Path",
                value = song.path,
                isCloud = false,
            )
        }
    }

    private fun getCloudProviderLabel(contentUriString: String): String? {
        return when {
            contentUriString.startsWith("telegram://") -> "Telegram"
            contentUriString.startsWith("gdrive://") -> "Google Drive"
            contentUriString.startsWith("youtube://") || contentUriString.contains("youtube") -> "YouTube"
            else -> null
        }
    }

    fun likeOnYouTube(song: Song, like: Boolean, onResult: (Boolean) -> Unit) {
        val videoId = song.youtubeId ?: if (song.contentUriString.startsWith("youtube://")) {
            song.contentUriString.substringAfter("youtube://")
        } else if (song.id.startsWith("youtube_")) {
            song.id.substringAfter("youtube_")
        } else {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = YouTube.likeVideo(videoId, like)
            onResult(result.isSuccess)
        }
    }

    fun addToYouTubePlaylist(playlistId: String, song: Song, onResult: (String?) -> Unit) {
        val videoId = song.youtubeId ?: if (song.contentUriString.startsWith("youtube://")) {
            song.contentUriString.substringAfter("youtube://")
        } else if (song.id.startsWith("youtube_")) {
            song.id.substringAfter("youtube_")
        } else {
            onResult(null)
            return
        }
        viewModelScope.launch {
            val result = YouTube.addToPlaylist(playlistId, videoId)
            onResult(result.getOrNull())
        }
    }

    fun isLoggedIn(): Boolean = YouTube.hasLoginCookie()
}
