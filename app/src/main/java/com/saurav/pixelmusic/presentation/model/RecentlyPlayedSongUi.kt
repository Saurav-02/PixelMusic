package com.saurav.pixelmusic.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
import com.saurav.pixelmusic.data.stats.StatsTimeRange
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Parcelize
data class RecentlyPlayedSongUiModel(
    val song: Song,
    val lastPlayedTimestamp: Long
) : Parcelable

fun mapRecentlyPlayedSongs(
    playbackHistory: List<PlaybackStatsRepository.PlaybackHistoryEntry>,
    songs: List<Song>,
    range: StatsTimeRange? = null,
    nowMillis: Long = System.currentTimeMillis(),
    maxItems: Int = Int.MAX_VALUE
): List<RecentlyPlayedSongUiModel> {
    if (maxItems <= 0 || playbackHistory.isEmpty()) return emptyList()

    val songById = songs.associateBy { it.id }
    val songByYoutubeId = songs.filter { it.youtubeId != null }.associateBy { it.youtubeId }
    val recentSongIds = collectRecentlyPlayedSongIds(
        playbackHistory = playbackHistory,
        range = range,
        nowMillis = nowMillis,
        maxItems = maxItems
    )

    if (recentSongIds.isEmpty()) return emptyList()

    val recentSongIdSet = recentSongIds.toHashSet()
    val (startBound, endBound) = range.resolveBounds(
        nowMillis = nowMillis.coerceAtLeast(0L),
        zoneId = ZoneId.systemDefault()
    )

    // 1 Month (30 Days) dynamic cutoff window
    val thirtyDaysAgoMillis = nowMillis - (30L * 24 * 60 * 60 * 1000)

    val seenSongIds = HashSet<String>()
    val deduped = ArrayList<RecentlyPlayedSongUiModel>(maxItems.coerceAtMost(playbackHistory.size))

    val sortedHistory = playbackHistory.sortedWith(
        compareByDescending<PlaybackStatsRepository.PlaybackHistoryEntry> { it.timestamp }
            .thenBy { it.songId }
    )

    for (entry in sortedHistory) {
        if (deduped.size >= maxItems) break
        val safeTimestamp = entry.timestamp.coerceAtLeast(0L)
        
        // Filter out anything older than 1 month
        if (safeTimestamp < thirtyDaysAgoMillis) continue
        
        if (safeTimestamp > endBound) continue
        if (startBound != null && safeTimestamp < startBound) continue
        if (!seenSongIds.add(entry.songId)) continue
        if (entry.songId !in recentSongIdSet) continue

        val cleanYoutubeId = if (entry.songId.startsWith("youtube_")) entry.songId.substringAfter("youtube_") else entry.songId
        val song = songById[entry.songId]
            ?: songByYoutubeId[cleanYoutubeId]
            ?: run {
                val title = entry.title
                if (!title.isNullOrBlank()) {
                    Song.emptySong().copy(
                        id = entry.songId,
                        title = title,
                        artist = entry.artist.orEmpty(),
                        albumArtUriString = entry.thumbnail,
                        youtubeId = cleanYoutubeId
                    )
                } else {
                    null
                }
            } ?: continue

        deduped += RecentlyPlayedSongUiModel(
            song = song,
            lastPlayedTimestamp = safeTimestamp
        )
    }

    return deduped
}

fun collectRecentlyPlayedSongIds(
    playbackHistory: List<PlaybackStatsRepository.PlaybackHistoryEntry>,
    range: StatsTimeRange? = null,
    nowMillis: Long = System.currentTimeMillis(),
    maxItems: Int = Int.MAX_VALUE
): List<String> {
    if (maxItems <= 0 || playbackHistory.isEmpty()) return emptyList()

    val (startBound, endBound) = range.resolveBounds(
        nowMillis = nowMillis.coerceAtLeast(0L),
        zoneId = ZoneId.systemDefault()
    )

    // 1 Month (30 Days) dynamic cutoff window
    val thirtyDaysAgoMillis = nowMillis - (30L * 24 * 60 * 60 * 1000)

    val seenSongIds = LinkedHashSet<String>()
    val sortedHistory = playbackHistory.sortedWith(
        compareByDescending<PlaybackStatsRepository.PlaybackHistoryEntry> { it.timestamp }
            .thenBy { it.songId }
    )

    for (entry in sortedHistory) {
        if (seenSongIds.size >= maxItems) break
        val safeTimestamp = entry.timestamp.coerceAtLeast(0L)
        
        // Filter out anything older than 1 month
        if (safeTimestamp < thirtyDaysAgoMillis) continue
        
        if (safeTimestamp > endBound) continue
        if (startBound != null && safeTimestamp < startBound) continue
        seenSongIds.add(entry.songId)
    }

    return seenSongIds.toList()
}

private fun StatsTimeRange?.resolveBounds(
    nowMillis: Long,
    zoneId: ZoneId
): Pair<Long?, Long> {
    val safeNow = nowMillis.coerceAtLeast(0L)
    val zonedNow = java.time.Instant.ofEpochMilli(safeNow).atZone(zoneId)

    return when (this) {
        StatsTimeRange.DAY -> {
            val start = zonedNow.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
            start to safeNow
        }
        StatsTimeRange.WEEK -> {
            val startOfWeek = zonedNow.toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val start = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
            start to safeNow
        }
        StatsTimeRange.MONTH -> {
            val startOfMonth = zonedNow.toLocalDate().withDayOfMonth(1)
            val start = startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
            start to safeNow
        }
        StatsTimeRange.YEAR -> {
            val startOfYear = zonedNow.toLocalDate().withDayOfYear(1)
            val start = startOfYear.atStartOfDay(zoneId).toInstant().toEpochMilli()
            start to safeNow
        }
        StatsTimeRange.ALL, null -> {
            null to safeNow
        }
    }
}
