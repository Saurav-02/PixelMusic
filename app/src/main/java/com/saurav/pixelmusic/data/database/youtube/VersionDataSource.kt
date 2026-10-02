package com.saurav.pixelmusic.data.database.youtube

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saurav.pixelmusic.data.model.youtube.Version

@Dao
interface VersionDataSource {
    @Query("SELECT * FROM versions")
    suspend fun getIgnoredVersions(): List<Version>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ignoreVersion(version: Version)

    @Query("DELETE FROM versions")
    suspend fun deleteAll()
}
