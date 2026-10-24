package com.saurav.pixelmusic.data.lastfm

import androidx.media3.common.MediaItem
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.min

class ScrobbleManager(
    private val scope: CoroutineScope,
    var minSongDuration: Int = 30,
    var scrobbleDelayPercent: Float = 0.5f,
    var scrobbleDelaySeconds: Int = 180
) {
    private var scrobbleJob: Job? = null
    private var scrobbleRemainingMillis: Long = 0L
    private var scrobbleTimerStartedAt: Long = 0L
    private var songStartedAt: Long = 0L
    private var songStarted = false
    var useNowPlaying = true
    var scrobblingEnabled = false

    private var currentMediaItem: MediaItem? = null
    private var currentDurationMs: Long = 0L

    fun destroy() {
        scrobbleJob?.cancel()
        scrobbleRemainingMillis = 0L
        scrobbleTimerStartedAt = 0L
        songStartedAt = 0L
        songStarted = false
        currentMediaItem = null
        currentDurationMs = 0L
    }

    fun onSongStart(mediaItem: MediaItem, durationMs: Long) {
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
        if (artist.isBlank() || title.isBlank()) return

        currentMediaItem = mediaItem
        currentDurationMs = durationMs

        songStartedAt = System.currentTimeMillis() / 1000
        songStarted = true
        startScrobbleTimer(mediaItem, durationMs)
        if (scrobblingEnabled && useNowPlaying) {
            updateNowPlaying(mediaItem, durationMs)
        }
    }

    fun onSongResume(mediaItem: MediaItem) {
        currentMediaItem = mediaItem
        resumeScrobbleTimer(mediaItem)
    }

    fun onSongPause() {
        pauseScrobbleTimer()
    }

    fun onSongStop() {
        stopScrobbleTimer()
        songStarted = false
        currentMediaItem = null
        currentDurationMs = 0L
    }

    private fun startScrobbleTimer(mediaItem: MediaItem, durationMs: Long) {
        scrobbleJob?.cancel()
        scrobbleJob = null
        
        val durationSec = (durationMs / 1000).toInt()
        if (durationSec <= minSongDuration) {
            Timber.tag("ScrobbleManager").d("Song too short to scrobble: ${durationSec}s <= min ${minSongDuration}s")
            return
        }

        val threshold = durationSec * 1000L * scrobbleDelayPercent
        scrobbleRemainingMillis = min(threshold.toLong(), scrobbleDelaySeconds * 1000L)

        if (scrobbleRemainingMillis <= 0) {
            if (scrobblingEnabled) {
                scrobbleSong(mediaItem, durationMs)
            }
            return
        }
        scrobbleTimerStartedAt = System.currentTimeMillis()
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            if (scrobblingEnabled) {
                scrobbleSong(mediaItem, durationMs)
            }
            scrobbleJob = null
        }
    }

    private fun pauseScrobbleTimer() {
        scrobbleJob?.cancel()
        scrobbleJob = null
        if (scrobbleTimerStartedAt != 0L) {
            val elapsed = System.currentTimeMillis() - scrobbleTimerStartedAt
            scrobbleRemainingMillis -= elapsed
            if (scrobbleRemainingMillis < 0) scrobbleRemainingMillis = 0
            scrobbleTimerStartedAt = 0L
        }
    }

    private fun resumeScrobbleTimer(mediaItem: MediaItem) {
        if (scrobbleRemainingMillis <= 0) return
        if (scrobbleJob != null) return // Already running, do not reset/extend
        scrobbleTimerStartedAt = System.currentTimeMillis()
        scrobbleJob = scope.launch {
            delay(scrobbleRemainingMillis)
            if (scrobblingEnabled) {
                scrobbleSong(mediaItem, currentDurationMs)
            }
            scrobbleJob = null
        }
    }

    private fun stopScrobbleTimer() {
        scrobbleJob?.cancel()
        scrobbleJob = null
        scrobbleRemainingMillis = 0
    }

    private fun scrobbleSong(mediaItem: MediaItem, durationMs: Long) {
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
        val album = mediaItem.mediaMetadata.albumTitle?.toString()
        val durationSec = (durationMs / 1000).toInt()

        scope.launch {
            try {
                if (!LastFM.isInitialized()) {
                    Timber.tag("ScrobbleManager").e("Last.fm client not initialized")
                    return@launch
                }
                if (LastFM.sessionKey.isNullOrEmpty()) {
                    Timber.tag("ScrobbleManager").d("No Last.fm session key set; skipping scrobble")
                    return@launch
                }
                LastFM.scrobble(
                    artist = artist,
                    track = title,
                    duration = durationSec,
                    timestamp = songStartedAt,
                    album = album
                )
                Timber.tag("ScrobbleManager").d("Scrobbled: $title by $artist")
            } catch (e: Exception) {
                Timber.tag("ScrobbleManager").e(e, "Failed to scrobble: $title")
            }
        }
    }

    private fun updateNowPlaying(mediaItem: MediaItem, durationMs: Long) {
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
        val album = mediaItem.mediaMetadata.albumTitle?.toString()
        val durationSec = (durationMs / 1000).toInt()

        scope.launch {
            try {
                if (!LastFM.isInitialized()) return@launch
                if (LastFM.sessionKey.isNullOrEmpty()) return@launch
                
                LastFM.updateNowPlaying(
                    artist = artist,
                    track = title,
                    album = album,
                    duration = durationSec
                )
                Timber.tag("ScrobbleManager").d("Updated now playing: $title")
            } catch (e: Exception) {
                Timber.tag("ScrobbleManager").e(e, "Failed to update now playing: $title")
            }
        }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, mediaItem: MediaItem?, durationMs: Long) {
        if (mediaItem == null) {
            onSongStop()
            return
        }

        val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
        if (artist.isBlank() || title.isBlank()) {
            onSongStop()
            return
        }

        if (isPlaying) {
            val isSameSong = currentMediaItem?.mediaId == mediaItem.mediaId
            val needsStart = !songStarted || !isSameSong || (currentDurationMs <= minSongDuration * 1000L && durationMs > minSongDuration * 1000L)
            if (needsStart) {
                onSongStart(mediaItem, durationMs)
            } else {
                onSongResume(mediaItem)
            }
        } else {
            onSongPause()
        }
    }
}
