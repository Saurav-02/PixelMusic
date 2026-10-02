package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.saurav.pixelmusic.data.media.CoverArtUpdate
import com.saurav.pixelmusic.data.media.ImageCacheManager
import com.saurav.pixelmusic.data.media.MetadataEditError
import com.saurav.pixelmusic.data.media.SongMetadataEditor
import com.saurav.pixelmusic.data.model.Lyrics
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.repository.MusicRepository
import com.saurav.pixelmusic.utils.FileDeletionUtils
import com.saurav.pixelmusic.utils.LyricsUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MetadataEditStateHolder @Inject constructor(
    private val songMetadataEditor: SongMetadataEditor,
    private val musicRepository: MusicRepository,
    private val imageCacheManager: ImageCacheManager,
    private val themeStateHolder: ThemeStateHolder,
    @ApplicationContext private val context: Context
) {

    data class MetadataEditResult(
        val success: Boolean,
        val updatedSong: Song? = null,
        val updatedAlbumArtUri: String? = null,
        val parsedLyrics: Lyrics? = null,
        val error: MetadataEditError? = null,
        val errorMessage: String? = null
    ) {
        /**
         * Returns a user-friendly error message based on the error type
         */
        fun getUserFriendlyErrorMessage(): String {
            return when (error) {
                MetadataEditError.FILE_NOT_FOUND -> "The song file could not be found. It may have been moved or deleted."
                MetadataEditError.NO_WRITE_PERMISSION -> "Cannot edit this file. You may need to grant additional permissions or the file is on read-only storage."
                MetadataEditError.INVALID_INPUT -> errorMessage ?: "Invalid input provided."
                MetadataEditError.UNSUPPORTED_FORMAT -> "This file format is not supported for editing."
                MetadataEditError.TAGLIB_ERROR -> "Failed to write metadata to the file. The file may be corrupted."
                MetadataEditError.TIMEOUT -> "The operation took too long and was cancelled."
                MetadataEditError.FILE_CORRUPTED -> "The file appears to be corrupted or in an unsupported format."
                MetadataEditError.IO_ERROR -> "An error occurred while accessing the file. Please try again."
                MetadataEditError.UNKNOWN, null -> errorMessage ?: "An unknown error occurred while editing metadata."
            }
        }
    }

    suspend fun saveMetadata(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newAlbumArtist: String,
        newComposer: String,
        newGenre: String,
        newLyrics: String,
        newTrackNumber: Int,
        newDiscNumber: Int?,
        newReplayGainTrackGainDb: String? = null,
        newReplayGainAlbumGainDb: String? = null,
        coverArtUpdate: CoverArtUpdate?
    ): MetadataEditResult = withContext(Dispatchers.IO) {
        
        Log.d("MetadataEditStateHolder", "Starting saveMetadata for: ${song.title}")

        // CRITICAL FIX: Preserve existing embedded artwork if the user didn't provide a new one.
        // Editing text metadata might strip the artwork if the underlying tagging library
        // overwrites the file structure. Explicitly re-saving the existing artwork prevents this.
        val finalCoverArtUpdate = if (coverArtUpdate == null) {
            val existingMetadata = try {
                 com.saurav.pixelmusic.data.media.AudioMetadataReader.read(java.io.File(song.path))
            } catch (e: Exception) {
                null
            }
            if (existingMetadata?.artwork != null) {
                Log.d("MetadataEditStateHolder", "Preserving existing embedded artwork")
                CoverArtUpdate(existingMetadata.artwork.bytes, existingMetadata.artwork.mimeType ?: "image/jpeg")
            } else {
                null
            }
        } else if (coverArtUpdate.isDeletion) {
            Log.d("MetadataEditStateHolder", "Artwork deletion requested, skipping preservation")
            coverArtUpdate
        } else {
            coverArtUpdate
        }

        val trimmedLyrics = newLyrics.trim()
        val normalizedLyrics = trimmedLyrics.takeIf { it.isNotBlank() }
        // We parse lyrics here just to ensure they are valid or to have them ready, 
        // essentially mirroring logic in ViewModel
        val parsedLyrics = normalizedLyrics?.let { LyricsUtils.parseLyrics(it) }
        val resolvedSongId = resolveSongIdForMetadataEdit(song)

        if (resolvedSongId == null) {
            Log.w("MetadataEditStateHolder", "Cannot edit metadata for non-numeric song id: ${song.id}")
            return@withContext MetadataEditResult(
                success = false,
                error = MetadataEditError.INVALID_INPUT,
                errorMessage = "This song source does not support metadata editing."
            )
        }

        val result = songMetadataEditor.editSongMetadata(
            newTitle = newTitle,
            newArtist = newArtist,
            newAlbum = newAlbum,
            newAlbumArtist = newAlbumArtist.trim().takeIf { it.isNotBlank() },
            newComposer = newComposer.trim().takeIf { it.isNotBlank() },
            newGenre = newGenre,
            newLyrics = trimmedLyrics,
            newTrackNumber = newTrackNumber,
            newDiscNumber = newDiscNumber,
            newReplayGainTrackGainDb = newReplayGainTrackGainDb,
            newReplayGainAlbumGainDb = newReplayGainAlbumGainDb,
            coverArtUpdate = finalCoverArtUpdate,
            songId = resolvedSongId,
        )

        Log.d("MetadataEditStateHolder", "Editor result: success=${result.success}, error=${result.error}")

        if (result.success) {
            val refreshedAlbumArtUri = if (coverArtUpdate?.isDeletion == true) {
                null
            } else {
                result.updatedAlbumArtUri ?: song.albumArtUriString
            }
            
            // Update Repository (Lyrics)
            if (normalizedLyrics != null) {
                musicRepository.updateLyrics(resolvedSongId, normalizedLyrics)
            } else {
                musicRepository.resetLyrics(resolvedSongId)
            }

            val updatedSong = song.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                albumArtist = newAlbumArtist.trim().takeIf { it.isNotBlank() },
                genre = newGenre,
                lyrics = normalizedLyrics,
                trackNumber = newTrackNumber,
                discNumber = newDiscNumber,
                albumArtUriString = refreshedAlbumArtUri,
            )

            // CRITICAL: Fetch the authoritative song object from the repository (MediaStore/DB).
            // When metadata changes (especially album/artist), MediaStore might re-index the song
            // and assign it a NEW album ID, resulting in a NEW albumArtUri.
            // Using the 'updatedSong' copy above might retain a STALE albumArtUri.
            val freshSongFromRepo = try {
                musicRepository.getSong(song.id).first() ?: updatedSong
            } catch (e: Exception) {
                updatedSong
            }

            // Ensure we use the refreshed artwork URI we just generated/cleared.
            // The repository emission may be stale for a split second.
            val freshSong = freshSongFromRepo.copy(
                albumArtUriString = refreshedAlbumArtUri
            )

            // Force cache invalidation if album art might have changed
            val uriToInvalidate = if (coverArtUpdate?.isDeletion == true) song.albumArtUriString else refreshedAlbumArtUri
            if (uriToInvalidate != null) {
                // Invalidate Coil/Glide caches for the affected URI (old or new)
                imageCacheManager.invalidateCoverArtCaches(uriToInvalidate)
            }
            
            // Force regenerate palette
            themeStateHolder.forceRegenerateColorScheme(refreshedAlbumArtUri)

            MetadataEditResult(
                success = true,
                updatedSong = freshSong,
                updatedAlbumArtUri = freshSong.albumArtUriString,
                parsedLyrics = parsedLyrics
            )
        } else {
            Log.w("MetadataEditStateHolder", "Metadata edit failed: ${result.error} - ${result.errorMessage}")
            MetadataEditResult(
                success = false,
                error = result.error,
                errorMessage = result.errorMessage
            )
        }
    }

    suspend fun deleteSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        val fileInfo = FileDeletionUtils.getFileInfo(song.path)
        // NOTE: Do NOT gate on fileInfo.canWrite.
        // On Android 10+ (Scoped Storage), File.canWrite() always returns false for files
        // in shared storage not created by the app — even when the file physically exists
        // and is legitimately deletable via ContentResolver. Checking canWrite here would
        // cause every delete to fail with "file not found" on modern Android.
        if (!fileInfo.exists) {
            return@withContext false
        }
        FileDeletionUtils.deleteFile(context, song.path)
    }

    private fun resolveSongIdForMetadataEdit(song: Song): Long? {
        song.id.toLongOrNull()?.let { return it }

        val uriCandidates = buildList {
            if (song.contentUriString.isNotBlank()) add(song.contentUriString)
            if (song.id.startsWith("external:")) add(song.id.removePrefix("external:"))
        }

        for (rawUri in uriCandidates) {
            val parsedUri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: continue
            if (parsedUri.scheme != "content") continue

            parsedUri.lastPathSegment?.toLongOrNull()?.let { return it }
        }

        return null
    }
}
