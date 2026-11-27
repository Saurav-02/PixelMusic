package com.saurav.pixelmusic.presentation.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.saurav.pixelmusic.data.DailyMixManager
import com.saurav.pixelmusic.data.database.EngagementDao
import com.saurav.pixelmusic.data.database.MusicDao
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.stats.PlaybackStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Tracks listening statistics for songs.
 * Extracted from PlayerViewModel to reduce its size and improve modularity.
 *
 * Responsibilities:
 * - Track active listening sessions
 * - Record play statistics when session ends
 * - Handle voluntary vs automatic plays
 */
@Singleton
class ListeningStatsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyMixManager: DailyMixManager,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val engagementDao: EngagementDao,
    private val musicDao: MusicDao
) {
    private var currentSession: ActiveSession? = null
    private var pendingVoluntarySongId: String? = null
    private var scope: CoroutineScope? = null
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _playbackHistory = MutableStateFlow<List<PlaybackStatsRepository.PlaybackHistoryEntry>>(emptyList())
    val playbackHistory: StateFlow<List<PlaybackStatsRepository.PlaybackHistoryEntry>> = _playbackHistory.asStateFlow()

    /**
     * Must be called to set the coroutine scope for async operations.
     */
    fun initialize(coroutineScope: CoroutineScope) {
        val activeScope = scope
        if (activeScope == null || activeScope.coroutineContext[Job]?.isActive != true) {
            scope = coroutineScope
        }
        coroutineScope.launch(Dispatchers.IO) {
            _playbackHistory.value = playbackStatsRepository.loadPlaybackHistory(
                limit = MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS
            )
        }
    }

    @Synchronized
    fun onVoluntarySelection(songId: String) {
        pendingVoluntarySongId = songId
    }

    fun onSongChanged(
        song: Song?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        onTrackChanged(
            songId = song?.id,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = song?.duration ?: 0L,
            isPlaying = isPlaying,
            title = song?.title,
            artist = song?.displayArtist,
            thumbnail = song?.albumArtUriString,
            genre = song?.genre,
            album = song?.album
        )
    }

    @Synchronized
    fun onTrackChanged(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        onTrackChanged(
            songId = songId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun onTrackChanged(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        fallbackDurationMs: Long,
        isPlaying: Boolean,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
        genre: String? = null,
        album: String? = null
    ) {
        finalizeCurrentSession()
        val safeSongId = songId?.takeIf { it.isNotBlank() }
        if (safeSongId == null) {
            return
        }

        val nowRealtime = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        val normalizedDuration = normalizeDuration(durationMs, fallbackDurationMs)

        currentSession = ActiveSession(
            songId = safeSongId,
            totalDurationMs = normalizedDuration,
            startedAtEpochMs = nowEpoch,
            lastKnownPositionMs = positionMs.coerceAtLeast(0L),
            accumulatedListeningMs = 0L,
            lastRealtimeMs = nowRealtime,
            lastUpdateEpochMs = nowEpoch,
            isPlaying = isPlaying,
            isVoluntary = pendingVoluntarySongId == safeSongId,
            title = title,
            artist = artist,
            thumbnail = thumbnail,
            genre = genre,
            album = album
        )
        if (pendingVoluntarySongId == safeSongId) {
            pendingVoluntarySongId = null
        }
    }

    @Synchronized
    fun onPlayStateChanged(isPlaying: Boolean, positionMs: Long) {
        val session = currentSession ?: return
        val nowRealtime = SystemClock.elapsedRealtime()
        accumulateRealtimeListening(session, nowRealtime)
        session.isPlaying = isPlaying
        session.lastRealtimeMs = nowRealtime
        session.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
        session.lastUpdateEpochMs = System.currentTimeMillis()
    }

    @Synchronized
    fun onProgress(positionMs: Long, isPlaying: Boolean) {
        val session = currentSession ?: return
        val nowRealtime = SystemClock.elapsedRealtime()
        accumulateRealtimeListening(session, nowRealtime)
        session.isPlaying = isPlaying
        session.lastRealtimeMs = nowRealtime
        session.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
        session.lastUpdateEpochMs = System.currentTimeMillis()
    }

    fun ensureSession(
        song: Song?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        ensureSession(
            songId = song?.id,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = song?.duration ?: 0L,
            isPlaying = isPlaying,
            title = song?.title,
            artist = song?.displayArtist,
            thumbnail = song?.albumArtUriString,
            genre = song?.genre,
            album = song?.album
        )
    }

    @Synchronized
    fun ensureSession(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        ensureSession(
            songId = songId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun ensureSession(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        fallbackDurationMs: Long,
        isPlaying: Boolean,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
        genre: String? = null,
        album: String? = null
    ) {
        val safeSongId = songId?.takeIf { it.isNotBlank() }
        if (safeSongId == null) {
            finalizeCurrentSession()
            return
        }
        val existing = currentSession
        if (existing?.songId == safeSongId) {
            updateDuration(normalizeDuration(durationMs, fallbackDurationMs))
            val nowRealtime = SystemClock.elapsedRealtime()
            accumulateRealtimeListening(existing, nowRealtime)
            existing.isPlaying = isPlaying
            existing.lastRealtimeMs = nowRealtime
            existing.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
            existing.lastUpdateEpochMs = System.currentTimeMillis()
            return
        }
        onTrackChanged(
            songId = safeSongId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = fallbackDurationMs,
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            thumbnail = thumbnail,
            genre = genre,
            album = album
        )
    }

    @Synchronized
    fun updateDuration(durationMs: Long) {
        val session = currentSession ?: return
        if (durationMs > 0 && durationMs != C.TIME_UNSET) {
            session.totalDurationMs = durationMs
        }
    }

    @Synchronized
    fun finalizeCurrentSession(forceSynchronousPersistence: Boolean = false) {
        val session = currentSession ?: return
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        accumulateRealtimeListening(session, nowRealtime)
        val listened = session.accumulatedListeningMs.coerceAtLeast(0L)
        if (listened >= MIN_SESSION_LISTEN_MS) {
            val rawEndTimestamp = when {
                session.isPlaying -> nowEpoch
                session.lastUpdateEpochMs > 0L -> session.lastUpdateEpochMs
                else -> session.startedAtEpochMs + listened
            }
            val timestamp = rawEndTimestamp
                .coerceAtLeast(session.startedAtEpochMs.coerceAtLeast(0L))
                .coerceAtMost(nowEpoch)
            val songId = session.songId
            val historyEntry = PlaybackStatsRepository.PlaybackHistoryEntry(
                songId = songId,
                timestamp = timestamp,
                title = session.title,
                artist = session.artist,
                thumbnail = session.thumbnail
            )
            _playbackHistory.update { current ->
                (listOf(historyEntry) + current).take(MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS)
            }
            persistPlayback(
                songId = songId,
                listened = listened,
                timestamp = timestamp,
                forceSynchronous = forceSynchronousPersistence,
                title = session.title,
                artist = session.artist,
                thumbnail = session.thumbnail,
                genre = session.genre,
                album = session.album
            )
        } else if (listened >= 1000L) {
            // Log as negative feedback skip signal if song was started but skipped before 15 seconds.
            com.saurav.pixelmusic.data.remote.youtube.AutoQueueManager.registerSkip(session.songId)
        }
        currentSession = null
        if (pendingVoluntarySongId == session.songId) {
            pendingVoluntarySongId = null
        }
    }

    @Synchronized
    fun onPlaybackStopped() {
        finalizeCurrentSession()
    }

    @Synchronized
    fun onCleared() {
        finalizeCurrentSession(forceSynchronousPersistence = true)
        scope = null
    }

    private fun persistPlayback(
        songId: String,
        listened: Long,
        timestamp: Long,
        forceSynchronous: Boolean,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
        genre: String? = null,
        album: String? = null
    ) {
        if (forceSynchronous) {
            kotlinx.coroutines.runBlocking {
                runCatching {
                    persistPlaybackInternal(
                        songId = songId,
                        listened = listened,
                        timestamp = timestamp,
                        title = title,
                        artist = artist,
                        thumbnail = thumbnail,
                        genre = genre,
                        album = album
                    )
                }.onFailure { throwable ->
                    Timber.e(throwable, "Failed to persist listening session synchronously for song=%s", songId)
                }
            }
        } else {
            persistenceScope.launch {
                runCatching {
                    persistPlaybackInternal(
                        songId = songId,
                        listened = listened,
                        timestamp = timestamp,
                        title = title,
                        artist = artist,
                        thumbnail = thumbnail,
                        genre = genre,
                        album = album
                    )
                }.onFailure { throwable ->
                    Timber.e(throwable, "Failed to persist listening session for song=%s", songId)
                }
            }
        }
    }

    private suspend fun persistPlaybackInternal(
        songId: String,
        listened: Long,
        timestamp: Long,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
        genre: String? = null,
        album: String? = null
    ) {
        dailyMixManager.recordPlay(
            songId = songId,
            songDurationMs = listened,
            timestamp = timestamp
        )
        playbackStatsRepository.recordPlayback(
            songId = songId,
            durationMs = listened,
            timestamp = timestamp,
            title = title,
            artist = artist,
            thumbnail = thumbnail,
            genre = genre,
            album = album
        )
        // Auto-cache YouTube songs played 3+ times: trigger a background download
        // so frequently-listened songs become fully available offline.
        triggerAutoCacheIfNeeded(songId)
    }

    /**
     * Checks whether a YouTube song has been played enough times to warrant
     * automatic offline caching. (Disabled since download system is removed).
     */
    private suspend fun triggerAutoCacheIfNeeded(songId: String) {
        // Function disabled.
    }

    private fun accumulateRealtimeListening(session: ActiveSession, nowRealtime: Long) {
        if (!session.isPlaying) return
        val delta = (nowRealtime - session.lastRealtimeMs).coerceAtLeast(0L)
        if (delta > 0L) {
            session.accumulatedListeningMs += delta
        }
    }

    private fun normalizeDuration(durationMs: Long, fallbackDurationMs: Long): Long {
        return when {
            durationMs > 0 && durationMs != C.TIME_UNSET -> durationMs
            fallbackDurationMs > 0 && fallbackDurationMs != C.TIME_UNSET -> fallbackDurationMs
            else -> 0L
        }
    }

    companion object {
        private val MIN_SESSION_LISTEN_MS = 15000L
        private const val MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS = 500
        /** Number of plays before a YouTube song is auto-downloaded for offline use. */
        private const val AUTO_CACHE_PLAY_COUNT_THRESHOLD = 3
    }
}

/**
 * Represents an active listening session for a song.
 */
data class ActiveSession(
    val songId: String,
    var totalDurationMs: Long,
    val startedAtEpochMs: Long,
    var lastKnownPositionMs: Long,
    var accumulatedListeningMs: Long,
    var lastRealtimeMs: Long,
    var lastUpdateEpochMs: Long,
    var isPlaying: Boolean,
    val isVoluntary: Boolean,
    val title: String? = null,
    val artist: String? = null,
    val thumbnail: String? = null,
    val genre: String? = null,
    val album: String? = null
)
