package com.saurav.pixelmusic.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LyricsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lyrics: LyricsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lyrics: List<LyricsEntity>)

    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun getLyrics(songId: Long): LyricsEntity?

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun deleteLyrics(songId: Long)

    @Query("DELETE FROM lyrics")
    suspend fun deleteAll()

    @Query("SELECT songId FROM lyrics")
    suspend fun getAllSongIds(): List<Long>

    @Query("SELECT * FROM lyrics WHERE songId IN (:songIds)")
    suspend fun getByIdsRaw(songIds: List<Long>): List<LyricsEntity>

    @Transaction
    suspend fun getAll(): List<LyricsEntity> {
        val ids = getAllSongIds()
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(100).flatMap { chunk -> getByIdsRaw(chunk) }
    }

    @Query("SELECT songId FROM lyrics WHERE songId IN (:songIds) AND content != ''")
    suspend fun getSongIdsWithLyricsRaw(songIds: List<Long>): List<Long>

    suspend fun getSongIdsWithLyrics(songIds: List<Long>): List<Long> {
        if (songIds.isEmpty()) return emptyList()
        if (songIds.size <= 500) return getSongIdsWithLyricsRaw(songIds)
        return songIds.chunked(500).flatMap { chunk -> getSongIdsWithLyricsRaw(chunk) }
    }

    @Transaction
    suspend fun replaceAll(lyrics: List<LyricsEntity>) {
        deleteAll()
        if (lyrics.isNotEmpty()) insertAll(lyrics)
    }
}
