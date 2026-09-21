package com.saurav.pixelmusic.data.database

import androidx.room.ColumnInfo

/**
 * Minimal projection used to build the folder tree without loading full song rows.
 */
data class FolderSongRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "parent_directory_path") val parentDirectoryPath: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "album_art_uri_string") val albumArtUriString: String?,
    @ColumnInfo(name = "date_added") val dateAdded: Long = 0L
)
