package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * YouTube Liked Songs sync removed — local library syncing disabled.
 * This class is kept as a stub so all injection call sites still compile.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LikedSongsSyncHelperEntryPoint {
    fun pixelMusicDatabase(): com.saurav.pixelmusic.data.database.PixelMusicDatabase
    fun musicDao(): com.saurav.pixelmusic.data.database.MusicDao
    fun favoritesDao(): com.saurav.pixelmusic.data.database.FavoritesDao
}

object LikedSongsSyncHelper {
    /** No-op: YouTube liked songs sync has been removed. */
    fun syncLikedSongsIfNeeded(context: Context, scope: CoroutineScope) {
        // Sync removed intentionally
    }
}
