package com.saurav.pixelmusic.data.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicLong
import com.saurav.pixelmusic.MainActivityIntentContract
import com.saurav.pixelmusic.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressSessionToken = AtomicLong(0L)
    @Volatile
    private var progressStartedAt = 0L

    companion object {
        const val CHANNEL_ID = "ai_generation_channel"
        const val RESULT_CHANNEL_ID = "ai_generation_result_channel"
        const val PROGRESS_NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
        const val LEGACY_COMPLETION_NOTIFICATION_ID = 1002

        private const val REQUEST_PROGRESS = 2001
        private const val REQUEST_RESULT = 2002
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "AI Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live progress while the AI is working"
            }
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "AI Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tells you when an AI mix or task has finished"
            }
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    /**
     * Opens the app. Depending on the flags it brings back the generation sheet, jumps straight to a
     * finished playlist or to the player.
     */
    private fun launchIntent(
        requestCode: Int,
        reopenSheet: Boolean = false,
        playlistId: String? = null,
        openPlayer: Boolean = false
    ): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            when {
                playlistId != null -> {
                    action = MainActivityIntentContract.ACTION_OPEN_PLAYLIST
                    putExtra(MainActivityIntentContract.EXTRA_PLAYLIST_ID, playlistId)
                }
                reopenSheet -> action = MainActivityIntentContract.ACTION_OPEN_AI_PLAYLIST_SHEET
                openPlayer -> putExtra("ACTION_SHOW_PLAYER", true)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showProgress(
        title: String,
        message: String,
        progress: Int,
        max: Int = 100,
        indeterminate: Boolean = false,
        reopenSheet: Boolean = false
    ) {
        progressSessionToken.incrementAndGet()
        if (progressStartedAt == 0L) progressStartedAt = System.currentTimeMillis()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText("Pixel Music · AI Playlist")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setWhen(progressStartedAt)
            .setTicker("Your AI playlist is being created")
            .setUsesChronometer(true)
            .setTimeoutAfter(120_000L)
            .setProgress(max, progress, indeterminate || progress == 0)
            .setContentIntent(launchIntent(REQUEST_PROGRESS, reopenSheet = reopenSheet))

        notificationManager.notify(PROGRESS_NOTIFICATION_ID, builder.build())
    }

    fun showCompletion(
        title: String,
        message: String,
        playlistId: String? = null,
        openPlayer: Boolean = false,
        reopenSheet: Boolean = false
    ) {
        hideProgress()

        val builder = NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_player)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText("Pixel Music · AI Playlist")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setUsesChronometer(false)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setContentIntent(
                launchIntent(
                    REQUEST_RESULT,
                    reopenSheet = reopenSheet,
                    playlistId = playlistId,
                    openPlayer = openPlayer
                )
            )

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, builder.build())
    }

    fun hideProgress() {
        val token = progressSessionToken.incrementAndGet()
        progressStartedAt = 0L
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        // Re-cancel shortly after to defeat OEM NotificationManagerService rate-limit lag
        // where a queued notify(PROGRESS_NOTIFICATION_ID) lands right after cancel().
        mainHandler.postDelayed({
            if (progressSessionToken.get() == token) {
                notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
            }
        }, 450L)
    }

    fun clearAll() {
        val token = progressSessionToken.incrementAndGet()
        progressStartedAt = 0L
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        notificationManager.cancel(COMPLETION_NOTIFICATION_ID)
        mainHandler.postDelayed({
            if (progressSessionToken.get() == token) {
                notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
                notificationManager.cancel(COMPLETION_NOTIFICATION_ID)
            }
        }, 450L)
    }
}
