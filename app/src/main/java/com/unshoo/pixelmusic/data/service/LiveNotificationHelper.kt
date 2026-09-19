package com.unshoo.pixelmusic.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.unshoo.pixelmusic.MainActivity
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.ui.glancewidget.PlayerActions
import java.util.Arrays

object LiveNotificationHelper {
    private const val LIVE_CHANNEL_ID = "pixelmusic_live_progress_v12"
    private const val LIVE_NOTIFICATION_ID = 1002

    private var lastArtworkBytes: ByteArray? = null
    private var cachedArtworkBitmap: Bitmap? = null

    // Animation state for the dynamic island
    private val animatedNotes = arrayOf("♫", "♩", "♪", "𝅗𝅥", "𝅘𝅥𝅯", "𝅘𝅥𝅰", "𝅘𝅥𝅱", "𝅘𝅥𝅲", "𝄞")
    private var noteIndex = 0

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Clean up the graveyard of testing channels from your settings
            for (i in 1..10) {
                notificationManager.deleteNotificationChannel("pixelmusic_live_progress_v$i")
            }
            notificationManager.deleteNotificationChannel("pixelmusic_live_progress")

            val channel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Dynamic Island Tracker",
                NotificationManager.IMPORTANCE_LOW // Keeps the top-left status bar clean of double icons!
            ).apply {
                description = "Keeps the OriginOS Dynamic Island active"
                setShowBadge(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatTimeMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }

    fun updateLiveNotification(
        context: Context,
        title: String,
        artist: String,
        positionMs: Long,
        durationMs: Long,
        artworkData: ByteArray?,
        style: String 
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_SHOW_PLAYER", true)
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            context, 1,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            context, 2,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            context, 3,
            Intent(context, MusicService::class.java).apply { action = PlayerActions.NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val safeDuration = durationMs.coerceAtLeast(0L)

        val criticalText = when (style) {
            "ANIMATED_NOTES" -> {
                val currentNote = animatedNotes[noteIndex]
                noteIndex = (noteIndex + 1) % animatedNotes.size
                currentNote
            }
            "PROGRESS_TIME" -> "${formatTimeMs(positionMs)}/${formatTimeMs(safeDuration)}"
            "STATIC_ICON" -> "🎧"
            else -> "🎧" 
        }

        // We MUST feed the builder the full UI so the Island can mirror it
        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("OriginOS Dynamic Island Tracker") // Explains the card's existence to the user
            .setContentIntent(pendingAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setRequestPromotedOngoing(true) 
            .setShortCriticalText(criticalText)
            .setSmallIcon(R.drawable.pixelmusic_base_monochrome)
            .setSortKey("zzzzz_ghost") // Forces the card to the bottom of the shade

        val progressPercent = if (safeDuration > 0L) {
            ((positionMs.toFloat() / safeDuration) * 100).toInt().coerceIn(0, 100)
        } else 0

        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val segment = NotificationCompat.ProgressStyle.Segment(100)
                segment.setColor(0xFFE91E63.toInt())

                val progressStyle = NotificationCompat.ProgressStyle()
                    .setProgressSegments(arrayListOf(segment))
                    .setStyledByProgress(true)
                    .setProgress(progressPercent)

                builder.setStyle(progressStyle)
            } catch (_: Throwable) {
                builder.setProgress(100, progressPercent, safeDuration == 0L)
            }
        } else {
            builder.setProgress(100, progressPercent, safeDuration == 0L)
        }

        // RESTORED: Give the Island its art and buttons back
        val bitmap = getOrDecodeArtwork(artworkData)
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        builder.addAction(android.R.drawable.ic_media_previous, "⏮", prevIntent)
            .addAction(android.R.drawable.ic_media_pause, "❚❚", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "⏭", nextIntent)

        notificationManager.notify(LIVE_NOTIFICATION_ID, builder.build())
    }

    private fun getOrDecodeArtwork(artworkData: ByteArray?): Bitmap? {
        if (artworkData == null || artworkData.isEmpty()) {
            lastArtworkBytes = null
            cachedArtworkBitmap = null
            return null
        }

        if (lastArtworkBytes != null && Arrays.equals(lastArtworkBytes, artworkData)) {
            return cachedArtworkBitmap
        }

        return try {
            val decoded = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            lastArtworkBytes = artworkData
            cachedArtworkBitmap = decoded
            decoded
        } catch (_: Exception) {
            null
        }
    }

    fun dismissLiveNotification(context: Context) {
        lastArtworkBytes = null
        cachedArtworkBitmap = null
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(LIVE_NOTIFICATION_ID)
    }
}
