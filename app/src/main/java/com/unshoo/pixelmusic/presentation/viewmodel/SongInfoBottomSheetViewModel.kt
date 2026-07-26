package com.unshoo.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.unshoo.pixelmusic.data.database.MusicDao
import com.unshoo.pixelmusic.data.database.toArtist
import com.unshoo.pixelmusic.data.model.Artist
import com.unshoo.pixelmusic.data.model.Song
import com.unshoo.pixelmusic.utils.AudioMeta
import com.unshoo.pixelmusic.utils.AudioMetaUtils
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SongInfoBottomSheetViewModel @Inject constructor(
    private val musicDao: MusicDao,
    private val downloadRepository: com.unshoo.pixelmusic.data.remote.youtube.DownloadRepository,
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

    private val _isSongDownloaded = MutableStateFlow(false)
    val isSongDownloaded: StateFlow<Boolean> = _isSongDownloaded.asStateFlow()

    private val _isSongDownloading = MutableStateFlow(false)
    val isSongDownloading: StateFlow<Boolean> = _isSongDownloading.asStateFlow()

    private var downloadJob: Job? = null

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

    fun loadDownloadState(song: Song) {
        _isSongDownloaded.value = false
        _isSongDownloading.value = false

        val youtubeId = song.youtubeId ?: return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isSongDownloaded.value = downloadRepository.isSongDownloaded(youtubeId)

            downloadRepository.getSongDownloadWorkInfoFlow(youtubeId).collect { workInfos ->
                val active = workInfos.any {
                    it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.RUNNING ||
                            it.state == WorkInfo.State.BLOCKED
                }
                _isSongDownloading.value = active

                if (workInfos.any { it.state == WorkInfo.State.SUCCEEDED }) {
                    _isSongDownloaded.value = true
                }
                if (workInfos.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
                    _isSongDownloaded.value = downloadRepository.isSongDownloaded(youtubeId)
                }
            }
        }
    }

    fun downloadYoutubeSong(song: Song) {
        val youtubeId = song.youtubeId ?: return
        viewModelScope.launch {
            val youtubeSong = com.unshoo.pixelmusic.data.model.youtube.Song(
                youtubeId = youtubeId,
                title = song.title,
                artist = song.artist,
                duration = com.unshoo.pixelmusic.utils.formatDuration(song.duration),
                thumbnailHref = song.albumArtUriString ?: ""
            )
            val playlist = com.unshoo.pixelmusic.data.model.youtube.Playlist(
                info = com.unshoo.pixelmusic.data.model.youtube.PlaylistInfo(
                    id = com.unshoo.pixelmusic.data.remote.youtube.Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                    title = "Downloaded Songs"
                ),
                unsortedSongs = listOf(youtubeSong),
                crossRefs = listOf(
                    com.unshoo.pixelmusic.data.model.youtube.PlaylistSongCrossRef(
                        playlistId = com.unshoo.pixelmusic.data.remote.youtube.Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                        songId = youtubeSong.youtubeId,
                        position = 0
                    )
                )
            )
            downloadRepository.downloadSong(playlist, youtubeSong)
        }
    }

    fun deleteYoutubeSong(song: Song) {
        val youtubeId = song.youtubeId ?: return
        viewModelScope.launch {
            downloadRepository.deleteSong(youtubeId)
            _isSongDownloaded.value = false
            _isSongDownloading.value = false
        }
    }

    fun cancelYoutubeSongDownload(song: Song) {
        val youtubeId = song.youtubeId ?: return
        downloadRepository.cancelSongDownload(youtubeId)
        _isSongDownloading.value = false
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
