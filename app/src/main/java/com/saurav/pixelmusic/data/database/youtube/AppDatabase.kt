package com.saurav.pixelmusic.data.database.youtube

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saurav.pixelmusic.data.remote.youtube.Constants
import com.saurav.pixelmusic.data.model.youtube.PlaylistInfo
import com.saurav.pixelmusic.data.model.youtube.PlaylistSongCrossRef
import com.saurav.pixelmusic.data.model.youtube.Song
import com.saurav.pixelmusic.data.model.youtube.Version
import java.util.concurrent.Executors

@Database(
    entities = [Song::class, PlaylistInfo::class, PlaylistSongCrossRef::class, Version::class],
    version = Constants.Database.VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songRepository(): LocalSongDataSource
    abstract fun playlistRepository(): LocalPlaylistDataSource
    abstract fun versionRepository(): VersionDataSource

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

            suspend fun clearDownloads(context: Context) {
        val instance = getInstance(context)

        instance.songRepository().deleteAll()
        instance.playlistRepository().deleteAll()

        // NEW: Physically delete the gigabytes of orphaned audio files
        val downloadDir = com.saurav.pixelmusic.data.remote.youtube.UmihiHelper.getDownloadDirectory(context)
        if (downloadDir.exists()) {
            downloadDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
        }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, Constants.Database.NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * Utility method to run blocks on a dedicated background thread, used for io/database work.
         */
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()
        fun ioThread(f: () -> Unit) {
            IO_EXECUTOR.execute(f)
        }
    }
}
