package com.saurav.pixelmusic.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saurav.pixelmusic.MainActivity
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.utils.InAppUpdater
import com.saurav.pixelmusic.utils.UpdateState

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = packageInfo.versionName ?: "1.0.0"

        when (val state = InAppUpdater.checkForUpdate(currentVersion)) {
            is UpdateState.Available -> {
                showNotification(state.versionName)
            }
            else -> {}
        }
        return Result.success()
    }

    private fun showNotification(versionName: String) {
        val channelId = "pixelmusic_update_channel"
        
        // Create the NotificationChannel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifies when a new PixelMusic version is available"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Launch MainActivity and pass an extra to route to the About screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_ABOUT", true) 
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.pixelmusic_base_monochrome) 
            .setContentTitle("PixelMusic Update Available")
            .setContentText("Version $versionName is out! Tap to install.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // Your manifest already has POST_NOTIFICATIONS, just double checking permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(999, builder.build())
            }
        }
    }
}

