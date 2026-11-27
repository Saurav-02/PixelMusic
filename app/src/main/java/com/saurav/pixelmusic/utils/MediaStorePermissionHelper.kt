package com.saurav.pixelmusic.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Helper for requesting MediaStore write/delete permissions on Android 11+
 * without needing MANAGE_EXTERNAL_STORAGE.
 *
 * After the user approves the system dialog, both ContentResolver-based and
 * raw file-path operations are allowed (thanks to the FUSE virtual filesystem).
 */
object MediaStorePermissionHelper {

    /**
     * Checks whether a given Content URI exists and can be resolved by MediaStore.
     */
    fun isUriValidInMediaStore(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the MediaStore content URI for a given audio song ID.
     * Returns null for cloud songs (negative IDs).
     */
    fun getMediaStoreUri(songId: Long): Uri? {
        if (songId <= 0) return null
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    /**
     * Returns the MediaStore content URI for a given Song model, safely verifying
     * that the URI exists in MediaStore before returning it.
     */
    fun getMediaStoreUri(context: Context, song: com.saurav.pixelmusic.data.model.Song): Uri? {
        val songLongId = song.id.toLongOrNull()
        if (songLongId == null || songLongId <= 0) return null

        // 1. Try resolving via exact file path (resolves correct volume for SD card / secondary storage)
        if (song.path.isNotBlank()) {
            val uriByPath = getMediaStoreUri(context, song.path)
            if (uriByPath != null && isUriValidInMediaStore(context, uriByPath)) {
                return uriByPath
            }
        }

        // 2. Try the primary external content URI
        val defaultUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songLongId)
        if (isUriValidInMediaStore(context, defaultUri)) {
            return defaultUri
        }

        // 3. Try stored contentUriString if it's a content URI
        val storedUri = runCatching { Uri.parse(song.contentUriString) }.getOrNull()
        if (storedUri != null && storedUri.scheme == "content" && isUriValidInMediaStore(context, storedUri)) {
            return storedUri
        }

        return null
    }

    /**
     * Returns the MediaStore content URI for a given file path.
     * Useful for non-audio files like .lrc that are indexed by MediaStore.
     */
    fun getMediaStoreUri(context: Context, filePath: String): Uri? {
        val audioProjection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs = arrayOf(filePath)
        val audioQueryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val audioUri = try {
            context.contentResolver.query(audioQueryUri, audioProjection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    ContentUris.withAppendedId(audioQueryUri, id)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
        if (audioUri != null) return audioUri

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val queryUri = MediaStore.Files.getContentUri("external")

        return try {
            context.contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    ContentUris.withAppendedId(queryUri, id)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates an IntentSender that, when launched, asks the user to grant
     * write access to the given MediaStore URIs.
     *
     * Returns null on Android < 11 or if [uris] is empty.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createWriteRequestIntentSender(
        context: Context,
        uris: Collection<Uri>
    ): IntentSender? {
        if (uris.isEmpty()) return null
        return try {
            val validUris = uris.filter { isUriValidInMediaStore(context, it) }
            if (validUris.isEmpty()) return null
            MediaStore.createWriteRequest(context.contentResolver, validUris).intentSender
        } catch (e: Exception) {
            Timber.e(e, "Failed to create write request for URIs: $uris")
            null
        }
    }

    /**
     * Creates an IntentSender that, when launched, asks the user to confirm
     * deletion of the given MediaStore URIs.
     *
     * Returns null on Android < 11 or if [uris] is empty.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createDeleteRequestIntentSender(
        context: Context,
        uris: Collection<Uri>
    ): IntentSender? {
        if (uris.isEmpty()) return null
        return try {
            val validUris = uris.filter { isUriValidInMediaStore(context, it) }
            if (validUris.isEmpty()) return null
            MediaStore.createDeleteRequest(context.contentResolver, validUris).intentSender
        } catch (e: Exception) {
            Timber.e(e, "Failed to create delete request for URIs: $uris")
            null
        }
    }

    /**
     * Convenience: creates a write-request IntentSender for a single song ID.
     * Returns null for cloud songs or Android < 11.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createWriteRequestForSong(context: Context, songId: Long): IntentSender? {
        val uri = getMediaStoreUri(songId) ?: return null
        return createWriteRequestIntentSender(context, listOf(uri))
    }

    /**
     * Convenience: creates a delete-request IntentSender for a single song ID.
     * Returns null for cloud songs or Android < 11.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createDeleteRequestForSong(context: Context, songId: Long): IntentSender? {
        val uri = getMediaStoreUri(songId) ?: return null
        return createDeleteRequestIntentSender(context, listOf(uri))
    }

    /**
     * Convenience: creates a delete-request IntentSender for a Song model.
     * Accurately resolves the URI across volumes and falls back safely if not found.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createDeleteRequestForSong(context: Context, song: com.saurav.pixelmusic.data.model.Song): IntentSender? {
        val uri = getMediaStoreUri(context, song) ?: return null
        return createDeleteRequestIntentSender(context, listOf(uri))
    }
}
