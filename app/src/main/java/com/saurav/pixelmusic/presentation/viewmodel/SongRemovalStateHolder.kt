package com.saurav.pixelmusic.presentation.viewmodel

import android.app.Activity
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.PlaylistPreferencesRepository
import com.saurav.pixelmusic.data.repository.MusicRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlin.math.absoluteValue
import saurav.shru.pixelmusic.innertube.YouTube

@ViewModelScoped
class SongRemovalStateHolder @Inject constructor(
    private val musicRepository: MusicRepository,
    private val metadataEditStateHolder: MetadataEditStateHolder,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val libraryStateHolder: LibraryStateHolder
) {
    suspend fun showDeleteConfirmation(activity: Activity, song: Song): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@withContext false
                }

                val userChoice = CompletableDeferred<Boolean>()
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.dialog_delete_song_title))
                    .setMessage(
                        activity.getString(
                            R.string.dialog_delete_song_message,
                            song.title,
                            song.displayArtist
                        )
                    )
                    .setPositiveButton(activity.getString(R.string.delete_action)) { _, _ ->
                        userChoice.complete(true)
                    }
                    .setNegativeButton(activity.getString(R.string.cancel)) { _, _ ->
                        userChoice.complete(false)
                    }
                    .setOnCancelListener {
                        userChoice.complete(false)
                    }
                    .setCancelable(true)
                    .create()

                dialog.show()
                userChoice.await()
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun deleteSongFile(song: Song): Boolean {
        return metadataEditStateHolder.deleteSong(song)
    }

    suspend fun removeSongFromLibrary(song: Song) {
        libraryStateHolder.removeSong(song.id)
        
        val currentPlaylists = playlistPreferencesRepository.userPlaylistsFlow.first()
        val playlistsContainingSong = currentPlaylists.filter { it.songIds.contains(song.id) }

        // FIX: Safely extract the raw Video ID even if it lacks the "youtube_" prefix
        val videoId = song.youtubeId 
            ?: if (song.contentUriString.startsWith("youtube://")) song.contentUriString.substringAfter("youtube://")
            else if (song.id.startsWith("youtube_")) song.id.removePrefix("youtube_")
            else if (song.id.toLongOrNull() == null) song.id // It's a raw string cloud ID
            else null

        // Remove from local database using unified logic
        val localId = song.id.toLongOrNull()
        if (localId != null) {
            musicRepository.deleteById(localId)
        } else if (!videoId.isNullOrBlank()) {
            val unifiedId = -(15_000_000_000_000L + videoId.hashCode().toLong().absoluteValue)
            musicRepository.deleteById(unifiedId)
        }
        
        playlistPreferencesRepository.removeSongFromAllPlaylists(song.id)

        // Sync deletions to remote YouTube playlists
        if (!videoId.isNullOrBlank()) {
            playlistsContainingSong.filter { it.source == "YOUTUBE" }.forEach { playlist ->
                try {
                    withContext(Dispatchers.IO) {
                        val setVideoIds = YouTube.playlistEntrySetVideoIds(playlist.id, videoId).getOrNull()
                        setVideoIds?.forEach { setVideoId ->
                            YouTube.removeFromPlaylist(playlist.id, videoId, setVideoId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SongRemoval", "Failed to sync song removal to YouTube playlist ${playlist.id}", e)
                }
            }
        }
    }
}
