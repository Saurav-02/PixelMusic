package com.unshoo.pixelmusic.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.unshoo.pixelmusic.utils.PixelLogger.Category

/**
 * Wraps all ExoPlayer callbacks into PixelLogger.
 * Attach to the raw ExoPlayer instance inside DualPlayerEngine.
 */
open class PixelPlayerListener(private val name: String) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        PixelLogger.d(Category.PLAYER, name, "state=${stateName(playbackState)}")
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        PixelLogger.d(Category.PLAYER, name, "playWhenReady=$playWhenReady reason=$reason")
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        PixelLogger.i(
            Category.PLAYER, name,
            "transition id=${mediaItem?.mediaId} reason=$reason"
        )
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        PixelLogger.d(Category.PLAYER, name, "isPlaying=$isPlaying")
    }

    override fun onPlayerError(error: PlaybackException) {
        PixelLogger.e(
            Category.PLAYER, name,
            "error code=${error.errorCodeName} message=${error.message}",
            error
        )
    }

    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
        PixelLogger.d(
            Category.PLAYER, name,
            "timelineChanged size=${timeline.windowCount} reason=$reason"
        )
    }

    private fun stateName(s: Int) = when (s) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($s)"
    }
}
