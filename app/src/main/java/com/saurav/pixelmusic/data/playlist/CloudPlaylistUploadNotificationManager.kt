package com.saurav.pixelmusic.data.playlist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.saurav.pixelmusic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudPlaylistUploadNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_ID = "cloud_playlist_upload_channel"
        const val PROGRESS_NOTIFICATION_ID = 5050
        const val COMPLETION_NOTIFICATION_ID = 5051
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cloud Playlist Upload"
            val descriptionText = "Shows progress when uploading playlists to YouTube Music"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun getLaunchPendingIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    fun showProgress(
        playlistName: String,
        current: Int,
        total: Int,
        notificationId: Int = PROGRESS_NOTIFICATION_ID
    ) {
        val nm = notificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle("Uploading to YouTube Music")
            .setContentText("Creating \"$playlistName\" • $current of $total songs added")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(if (total > 0) total else 0, current, total <= 0)
            .setContentIntent(getLaunchPendingIntent())

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) { }
    }

    fun showSuccess(
        playlistName: String,
        total: Int,
        notificationId: Int = COMPLETION_NOTIFICATION_ID
    ) {
        val nm = notificationManager ?: return
        try {
            nm.cancel(notificationId)
        } catch (_: SecurityException) { }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle("Playlist Created in Cloud")
            .setContentText("\"$playlistName\" ($total songs) added to YouTube Music")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(getLaunchPendingIntent())

        try {
            nm.notify(notificationId + 10000, builder.build())
        } catch (_: SecurityException) { }
    }

    fun showError(
        playlistName: String,
        error: String,
        notificationId: Int = COMPLETION_NOTIFICATION_ID
    ) {
        val nm = notificationManager ?: return
        try {
            nm.cancel(notificationId)
        } catch (_: SecurityException) { }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle("Cloud Upload Failed")
            .setContentText("Couldn't upload \"$playlistName\": $error")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(getLaunchPendingIntent())

        try {
            nm.notify(notificationId + 20000, builder.build())
        } catch (_: SecurityException) { }
    }

    fun cancel(notificationId: Int = PROGRESS_NOTIFICATION_ID) {
        try {
            notificationManager?.cancel(notificationId)
        } catch (_: SecurityException) { }
    }
}
