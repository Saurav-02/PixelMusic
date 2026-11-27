package com.saurav.pixelmusic.data.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.presentation.screens.AodActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Launches [AodActivity] when the screen turns off while music is playing.
 *
 * Background activity starts are blocked on Android 10+, so the launch goes through either the
 * "draw over other apps" exemption or a full-screen-intent notification (the same mechanism alarms use).
 */
object AodScreenOffTrigger {
    private const val TAG = "AodTrigger"
    private const val CHANNEL_ID = "aod_screen_launcher"
    private const val NOTIFICATION_ID = 7421
    private const val COOLDOWN_MS = 4_000L
    private const val MIN_TOTAL_MEM_BYTES = 5_500L * 1024L * 1024L

    /** True while [AodActivity] is alive; stops the screen-off that ends it from relaunching it. */
    @Volatile
    var isAodShowing = false

    @Volatile
    private var lastAodEndMs = 0L

    private var receiver: BroadcastReceiver? = null

    fun markAodEnded() {
        lastAodEndMs = SystemClock.elapsedRealtime()
    }

    /** Feature is meant for high-end devices only (Android 12+, >= ~6 GB RAM, not a low-RAM device). */
    fun isHighEndDevice(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val am = context.getSystemService(ActivityManager::class.java) ?: return false
        if (am.isLowRamDevice) return false
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem >= MIN_TOTAL_MEM_BYTES
    }

    fun register(
        context: Context,
        scope: CoroutineScope,
        prefs: UserPreferencesRepository,
        isPlaying: () -> Boolean
    ) {
        if (receiver != null) return
        val app = context.applicationContext
        val pm = app.getSystemService(PowerManager::class.java)
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_SCREEN_OFF) return
                if (isAodShowing || SystemClock.elapsedRealtime() - lastAodEndMs < COOLDOWN_MS) return
                if (!isHighEndDevice(app) || !isPlaying()) return
                scope.launch {
                    val enabled = prefs.aodScreenEnabledFlow.first()
                    if (!enabled) return@launch
                    // Let the screen finish turning off before we ask for it to come back on.
                    delay(400L)
                    if (isAodShowing || !isPlaying() || pm?.isInteractive == true) return@launch
                    launchAod(app)
                }
            }
        }
        ContextCompat.registerReceiver(
            app,
            r,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiver = r
    }

    fun unregister(context: Context) {
        val r = receiver ?: return
        runCatching { context.applicationContext.unregisterReceiver(r) }
        receiver = null
    }

    fun cancelLaunchNotification(context: Context) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
    }

    private fun launchAod(context: Context) {
        val intent = Intent(context, AodActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )

        // Overlay permission exempts us from the background-activity-start restriction.
        if (Settings.canDrawOverlays(context)) {
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Direct AOD start failed, falling back to full-screen intent")
            }
        }

        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !nm.canUseFullScreenIntent()) {
                Timber.tag(TAG).w("Full-screen intent permission not granted; AOD not launched")
                return
            }
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Always-on display", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                }
            )
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Now playing")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setTimeoutAfter(5_000L)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to launch AOD via full-screen intent")
        }
    }
}
