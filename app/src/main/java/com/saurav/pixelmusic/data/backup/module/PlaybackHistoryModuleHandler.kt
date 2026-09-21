package com.saurav.pixelmusic.data.backup.module

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saurav.pixelmusic.data.backup.model.BackupSection
import com.saurav.pixelmusic.data.backup.model.PlaybackHistoryBackupEntry
import com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
import com.saurav.pixelmusic.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackHistoryModuleHandler @Inject constructor(
    private val playbackStatsRepository: PlaybackStatsRepository,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.PLAYBACK_HISTORY

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = playbackStatsRepository.exportEventsForBackup().map { event ->
            PlaybackHistoryBackupEntry(
                songId = event.songId,
                timestamp = event.timestamp,
                durationMs = event.durationMs,
                startTimestamp = event.startTimestamp,
                endTimestamp = event.endTimestamp,
                title = event.title,
                artist = event.artist,
                thumbnail = event.thumbnail
            )
        }
        gson.toJson(entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        playbackStatsRepository.exportEventsForBackup().size
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val type = TypeToken.getParameterized(List::class.java, PlaybackHistoryBackupEntry::class.java).type
        val entries: List<PlaybackHistoryBackupEntry> = gson.fromJson(payload, type)
        playbackStatsRepository.importEventsFromBackup(
            events = entries.map { entry ->
                PlaybackStatsRepository.PlaybackEvent(
                    songId = entry.songId,
                    timestamp = entry.timestamp,
                    durationMs = entry.durationMs,
                    startTimestamp = entry.startTimestamp,
                    endTimestamp = entry.endTimestamp,
                    title = entry.title,
                    artist = entry.artist,
                    thumbnail = entry.thumbnail
                )
            },
            clearExisting = true
        )
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)
}
