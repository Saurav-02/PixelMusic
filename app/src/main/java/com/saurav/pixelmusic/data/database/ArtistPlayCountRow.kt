package com.saurav.pixelmusic.data.database

import androidx.room.ColumnInfo

/**
 * Projection returned by [MusicDao.getArtistsByPlayCount].
 * Mirrors ArchiveTune's allArtistsByPlayTime() result — artists sorted
 * favourite-first, then by total play count descending.
 */
data class ArtistPlayCountRow(
    @ColumnInfo(name = "channelId") val channelId: String,
    @ColumnInfo(name = "totalPlayCount") val totalPlayCount: Long,
    @ColumnInfo(name = "isFavourite") val isFavourite: Int  // 1 if any song by this artist is favourited
)
