package com.unshoo.pixelmusic.data.remote.youtube

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.unshoo.pixelmusic.data.database.youtube.AppDatabase
import com.unshoo.pixelmusic.data.model.youtube.Playlist
import com.unshoo.pixelmusic.data.model.youtube.Song
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(appContext: Context) {
    private val _appContext = appContext
    private val workManager: WorkManager = WorkManager.getInstance(_appContext)
    private val localPlaylistRepository = AppDatabase.getInstance(_appContext).playlistRepository()
    private val localSongRepository = AppDatabase.getInstance(_appContext).songRepository()

    suspend fun downloadPlaylist(playlist: Playlist) {
        val existingWork = getExistingJobs(playlist.info.id)
        if (existingWork.isNotEmpty()) {
            UmihiHelper.printd("Download is already ongoing for playlist ${playlist.info.title}")
            return
        }
        localPlaylistRepository.insertPlaylistWithSongs(playlist)
        val request = OneTimeWorkRequestBuilder<PlaylistDownloadWorker>().setInputData(
            workDataOf(
                PlaylistDownloadWorker.PLAYLIST_KEY to playlist.info.id
            )
        ).setConstraints(
            Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
                requiresStorageNotLow = true
            )
        ).build()

        workManager.enqueueUniqueWork(playlist.info.id, ExistingWorkPolicy.KEEP, request)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        localPlaylistRepository.deleteFullPlaylist(playlist.info.id)
        val audioDir =
            UmihiHelper.getDownloadDirectory(
                _appContext,
                Constants.Downloads.AUDIO_FILES_FOLDER
            )
        val imageDir =
            UmihiHelper.getDownloadDirectory(_appContext, Constants.Downloads.THUMBNAILS_FOLDER)

        File(imageDir, "${playlist.info.id}.jpg").takeIf { it.exists() }?.delete()

        val songIds = playlist.songs.map { it.youtubeId }
        val stillLinked = localPlaylistRepository.getSongIdsWithPlaylist(songIds).toSet()
        val songsToClear = mutableListOf<String>()
        playlist.songs.forEach { song ->
            if (song.youtubeId in stillLinked) return@forEach
            songsToClear.add(song.youtubeId)
            File(audioDir, "${song.youtubeId}.webm").takeIf { it.exists() }?.delete()
            File(imageDir, "${song.youtubeId}.jpg").takeIf { it.exists() }?.delete()
        }
        localSongRepository.deleteByIds(songsToClear)
    }

    suspend fun downloadSong(playlist: Playlist, song: Song) {
        val id = "${playlist.info.id}${song.youtubeId}"
        val existingWork = getExistingJobs(id)
        if (existingWork.isNotEmpty()) {
            UmihiHelper.printd("Download is already ongoing for song ${playlist.info.title}")
            return
        }

        localPlaylistRepository.insertPlaylistWithSongs(playlist)
        val request = OneTimeWorkRequestBuilder<SongDownloadWorker>().setInputData(
            workDataOf(
                SongDownloadWorker.PLAYLIST_KEY to playlist.info.id,
                SongDownloadWorker.SONG_KEY to song.youtubeId
            )
        ).setConstraints(
            Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
                requiresStorageNotLow = true
            )
        ).build()

        workManager.enqueueUniqueWork(
            id,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPlaylistDownload(playlist: Playlist) {
        UmihiHelper.printd("stopping work ${playlist.info.title}")
        workManager.cancelUniqueWork(playlist.info.id)
    }

    fun cancelSongDownload(youtubeId: String) {
        val id = "${Constants.Downloads.DOWNLOADED_PLAYLIST_ID}$youtubeId"
        UmihiHelper.printd("stopping song work $id")
        workManager.cancelUniqueWork(id)
    }

    fun cancelAllWorks() {
        workManager.cancelAllWork()
    }

    fun getExistingJobFlow(playlist: Playlist): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(playlist.info.id)
    }

    fun getSongDownloadWorkInfoFlow(youtubeId: String): Flow<List<WorkInfo>> {
        val id = "${Constants.Downloads.DOWNLOADED_PLAYLIST_ID}$youtubeId"
        return workManager.getWorkInfosForUniqueWorkFlow(id)
    }

    suspend fun isSongDownloaded(youtubeId: String): Boolean {
        val song = localSongRepository.getSong(youtubeId)
        val path = song?.audioFilePath
        
        if (path.isNullOrBlank() && song?.downloaded != true) {
            return false
        }
        
        if (!path.isNullOrBlank()) {
            return try {
                if (path.startsWith("content://")) {
                    val uri = android.net.Uri.parse(path)
                    var actuallyExists = false
                    
                    // 1. Try to get the actual physical path from MediaStore
                    val cursor = _appContext.contentResolver.query(
                        uri, 
                        arrayOf(android.provider.MediaStore.MediaColumns.DATA), 
                        null, null, null
                    )
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        val physicalPath = cursor.getString(0)
                        if (physicalPath != null) {
                            actuallyExists = File(physicalPath).exists()
                        }
                    }
                    cursor?.close()
                    
                    // 2. Fallback: try opening a file descriptor
                    if (!actuallyExists) {
                        try {
                            val afd = _appContext.contentResolver.openAssetFileDescriptor(uri, "r")
                            afd?.close()
                            actuallyExists = true
                        } catch (e: Exception) {
                            actuallyExists = false
                        }
                    }
                    
                    actuallyExists
                } else {
                    File(path).exists()
                }
            } catch (e: Exception) {
                false
            }
        }
        
        return song?.downloaded == true
    }

    suspend fun deleteSong(youtubeId: String) {
        val audioDir =
            UmihiHelper.getDownloadDirectory(
                _appContext,
                Constants.Downloads.AUDIO_FILES_FOLDER
            )
        val imageDir =
            UmihiHelper.getDownloadDirectory(_appContext, Constants.Downloads.THUMBNAILS_FOLDER)

        File(audioDir, "$youtubeId.webm").takeIf { it.exists() }?.delete()
        File(imageDir, "$youtubeId.jpg").takeIf { it.exists() }?.delete()

        localPlaylistRepository.deleteCrossRef(Constants.Downloads.DOWNLOADED_PLAYLIST_ID, youtubeId)

        val stillLinked = localPlaylistRepository.getSongIdsWithPlaylist(listOf(youtubeId)).isNotEmpty()
        if (!stillLinked) {
            localSongRepository.deleteByIds(listOf(youtubeId))
        }
    }

    private fun getExistingJobs(id: String): List<WorkInfo> {
        return workManager.getWorkInfosForUniqueWork(id).get().filter {
            it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.BLOCKED
        }
    }
}
