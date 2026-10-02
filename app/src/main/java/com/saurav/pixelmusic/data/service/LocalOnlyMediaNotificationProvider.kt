package com.saurav.pixelmusic.data.service

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import androidx.media3.common.Player

/**
 * Wraps Media3's default provider and marks playback notifications as local-only
 * so they don't get bridged to Wear OS as generic remote media controls.
 */
@UnstableApi
class LocalOnlyMediaNotificationProvider(
    private val context: Context,
    private val delegate: DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(context).build(),
) : MediaNotification.Provider {

    fun setSmallIcon(iconResId: Int) {
        delegate.setSmallIcon(iconResId)
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val wrappedCallback = object : MediaNotification.Provider.Callback {
            override fun onNotificationChanged(notification: MediaNotification) {
                notification.notification.flags = notification.notification.flags or Notification.FLAG_LOCAL_ONLY
                notification.notification.category = Notification.CATEGORY_TRANSPORT
                
                // Android 14+ specific fix: Force ongoing flag if music is actively playing
                if (mediaSession.player.playWhenReady && mediaSession.player.playbackState == Player.STATE_READY) {
                    notification.notification.flags = notification.notification.flags or Notification.FLAG_ONGOING_EVENT
                }
                
                callback.onNotificationChanged(notification)
            }
        }
        val mediaNotification = delegate.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            wrappedCallback
        )
        mediaNotification.notification.flags = mediaNotification.notification.flags or Notification.FLAG_LOCAL_ONLY
        mediaNotification.notification.category = Notification.CATEGORY_TRANSPORT
        
        // Apply the same ongoing enforcement to the initial notification creation
        if (mediaSession.player.playWhenReady && mediaSession.player.playbackState == Player.STATE_READY) {
            mediaNotification.notification.flags = mediaNotification.notification.flags or Notification.FLAG_ONGOING_EVENT
        }
        
        return mediaNotification
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = delegate.handleCustomCommand(session, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.getNotificationChannelInfo()
}

