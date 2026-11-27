package com.saurav.pixelmusic.ui.glancewidget

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.saurav.pixelmusic.data.service.MusicService
import timber.log.Timber

class PlayerControlActionCallback : ActionCallback {
    private val TAG = "PlayerControlCallback"

    @OptIn(UnstableApi::class)
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[PlayerActions.key]
        Timber.tag(TAG).d("onAction received: %s for glanceId: %s", action, glanceId)

        if (action == null) {
            Timber.tag(TAG).w("Action key not found in parameters.")
            return
        }

        val serviceIntent = Intent(context, MusicService::class.java).apply {
            this.action = action
            putExtra(MusicService.EXTRA_FORCE_FOREGROUND_ON_START, true)
            if (action == PlayerActions.PLAY_FROM_QUEUE) {
                val songId = parameters[PlayerActions.songIdKey]
                if (songId != null) {
                    putExtra("song_id", songId)
                } else {
                    Timber.tag(TAG).w("PLAY_FROM_QUEUE action received but no songId found.")
                    return
                }
            }
        }

        // Optimistically flip toggleable icons right away so the widget reacts instantaneously
        // even when MusicService is cold-starting after a reboot or force stop.
        applyOptimisticWidgetState(context, glanceId, action)

        // Start MusicService directly as a foreground service in one step (Android always
        // grants foreground-service start exemption to user-initiated AppWidget interactions).
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Timber.tag(TAG).d("Foreground service intent sent for action: %s", action)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting foreground service for action %s: %s", action, e.message)
        }
    }

    private suspend fun applyOptimisticWidgetState(
        context: Context,
        glanceId: GlanceId,
        action: String,
    ) {
        if (action != PlayerActions.PLAY_PAUSE &&
            action != PlayerActions.FAVORITE &&
            action != PlayerActions.SHUFFLE &&
            action != PlayerActions.REPEAT
        ) {
            return
        }
        runCatching {
            updateAppWidgetState(context, PlayerInfoStateDefinition, glanceId) { current ->
                when (action) {
                    PlayerActions.PLAY_PAUSE -> current.copy(isPlaying = !current.isPlaying)
                    PlayerActions.FAVORITE -> current.copy(isFavorite = !current.isFavorite)
                    PlayerActions.SHUFFLE -> current.copy(isShuffleEnabled = !current.isShuffleEnabled)
                    PlayerActions.REPEAT -> {
                        val nextMode = when (current.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                            else -> Player.REPEAT_MODE_OFF
                        }
                        current.copy(repeatMode = nextMode)
                    }
                    else -> current
                }
            }
            val glanceManager = GlanceAppWidgetManager(context)
            when (action) {
                PlayerActions.PLAY_PAUSE -> {
                    glanceManager.getGlanceIds(PixelMusicGlanceWidget::class.java).forEach {
                        PixelMusicGlanceWidget().update(context, it)
                    }
                    glanceManager.getGlanceIds(BarWidget4x1::class.java).forEach {
                        BarWidget4x1().update(context, it)
                    }
                    glanceManager.getGlanceIds(ControlWidget4x2::class.java).forEach {
                        ControlWidget4x2().update(context, it)
                    }
                    glanceManager.getGlanceIds(BlurSquareWidget2x2::class.java).forEach {
                        BlurSquareWidget2x2().update(context, it)
                    }
                }
                PlayerActions.FAVORITE -> {
                    glanceManager.getGlanceIds(PixelMusicGlanceWidget::class.java).forEach {
                        PixelMusicGlanceWidget().update(context, it)
                    }
                }
                PlayerActions.SHUFFLE, PlayerActions.REPEAT -> {
                    glanceManager.getGlanceIds(ControlWidget4x2::class.java).forEach {
                        ControlWidget4x2().update(context, it)
                    }
                }
            }
        }
    }
}

object PlayerActions {
    val key = ActionParameters.Key<String>("playerActionKey_v1")
    val songIdKey = ActionParameters.Key<Long>("songIdKey_v1")
    const val PLAY_PAUSE = "com.saurav.pixelmusic.ACTION_WIDGET_PLAY_PAUSE"
    const val NEXT = "com.saurav.pixelmusic.ACTION_WIDGET_NEXT"
    const val PREVIOUS = "com.saurav.pixelmusic.ACTION_WIDGET_PREVIOUS"
    const val FAVORITE = "com.saurav.pixelmusic.ACTION_WIDGET_FAVORITE"
    const val PLAY_FROM_QUEUE = "com.saurav.pixelmusic.ACTION_WIDGET_PLAY_FROM_QUEUE"
    const val SHUFFLE = "com.saurav.pixelmusic.ACTION_WIDGET_SHUFFLE"
    const val REPEAT = "com.saurav.pixelmusic.ACTION_WIDGET_REPEAT"
}
