package com.saurav.pixelmusic.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("assets") val assets: List<GithubAsset>,
    @SerializedName("body") val body: String?
)

data class GithubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("name") val name: String
)

sealed class UpdateState {
    object Checking : UpdateState()
    data class UpToDate(val changelog: String? = null) : UpdateState()
    data class Available(
        val versionName: String, 
        val downloadUrl: String,
        val changelog: String? = null
    ) : UpdateState()
}

object InAppUpdater {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val REPO_URL = "https://api.github.com/repos/Saurav-02/PixelMusic/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REPO_URL).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body.string()
                val release = gson.fromJson(body, GithubRelease::class.java)
                
                val cleanLatest = release.tagName.replace(Regex("[^0-9.]"), "")
                val cleanCurrent = currentVersion.replace(Regex("[^0-9.]"), "")
                
                if (cleanLatest != cleanCurrent && release.assets.isNotEmpty()) {
                    val apkAssets = release.assets.filter { it.name.endsWith(".apk") }
                    val apkAsset = selectBestApkForDevice(apkAssets)
                    
                    if (apkAsset != null) {
                        return@withContext UpdateState.Available(
                            versionName = release.tagName, 
                            downloadUrl = apkAsset.downloadUrl,
                            changelog = release.body
                        )
                    }
                }
                return@withContext UpdateState.UpToDate(changelog = release.body)
            }
            return@withContext UpdateState.UpToDate(changelog = null)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateState.UpToDate(changelog = null)
        }
    }

    private fun selectBestApkForDevice(assets: List<GithubAsset>): GithubAsset? {
        if (assets.isEmpty()) return null
        if (assets.size == 1) return assets.first() 

        val deviceAbis = android.os.Build.SUPPORTED_ABIS.map { it.lowercase() }

        for (abi in deviceAbis) {
            val abiMatch = when {
                abi.contains("arm64") -> assets.firstOrNull { it.name.contains("arm64", ignoreCase = true) || it.name.contains("v8a", ignoreCase = true) }
                abi.contains("v7") -> assets.firstOrNull { it.name.contains("armv7", ignoreCase = true) || it.name.contains("v7a", ignoreCase = true) }
                abi.contains("x86_64") -> assets.firstOrNull { it.name.contains("x86_64", ignoreCase = true) }
                abi.contains("x86") -> assets.firstOrNull { it.name.contains("x86", ignoreCase = true) }
                else -> null
            }
            if (abiMatch != null) return abiMatch
        }

        val universalMatch = assets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
        if (universalMatch != null) return universalMatch

        return assets.first()
    }

    sealed class GlobalDownloadState {
        object Idle : GlobalDownloadState()
        data class Downloading(val progress: Float, val isPaused: Boolean, val versionName: String, val totalBytes: Long, val downloadedBytes: Long) : GlobalDownloadState()
        data class Finished(val apkFile: File, val versionName: String) : GlobalDownloadState()
        data class Error(val message: String) : GlobalDownloadState()
    }

    private val updaterScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: kotlinx.coroutines.Job? = null
    
    val downloadState = MutableStateFlow<GlobalDownloadState>(GlobalDownloadState.Idle)
    
    private var currentDownloadUrl: String? = null
    private var currentFileName: String? = null
    private var currentVersionName: String? = null
    private var downloadedBytes = 0L
    private var totalBytes = 0L

    private var isReceiverRegistered = false
    private var appContext: Context? = null
    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "PIXELMUSIC_PAUSE" -> pauseDownload()
                "PIXELMUSIC_RESUME" -> resumeDownload(context)
                "PIXELMUSIC_CANCEL" -> cancelDownload(context)
            }
        }
    }

    private fun registerReceiverIfNeeded(context: Context) {
        if (!isReceiverRegistered) {
            val filter = android.content.IntentFilter().apply {
                addAction("PIXELMUSIC_PAUSE")
                addAction("PIXELMUSIC_RESUME")
                addAction("PIXELMUSIC_CANCEL")
            }
            androidx.core.content.ContextCompat.registerReceiver(
                context.applicationContext, 
                actionReceiver, 
                filter, 
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    fun startOrResumeDownload(context: Context, url: String, versionName: String) {
        if (downloadJob?.isActive == true) return
        
        appContext = context.applicationContext
        registerReceiverIfNeeded(appContext!!)

        currentDownloadUrl = url
        currentVersionName = versionName
        currentFileName = "PixelMusic_$versionName.apk"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), currentFileName!!)

        fun finishDownload() {
            downloadState.value = GlobalDownloadState.Finished(file, versionName)
            
            val authority = "${appContext!!.packageName}.provider"
            val apkUri = FileProvider.getUriForFile(appContext!!, authority, file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val pendingInstall = android.app.PendingIntent.getActivity(
                appContext, 0, installIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val finishedNotif = androidx.core.app.NotificationCompat.Builder(appContext!!, "app_updates")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText("Tap to install PixelMusic $versionName")
                .setContentIntent(pendingInstall)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()
                
            val notificationManager = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(999, finishedNotif)
        }

        if (downloadState.value is GlobalDownloadState.Finished && file.exists()) {
            return
        }

        downloadState.value = GlobalDownloadState.Downloading(
            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f, 
            isPaused = false,
            versionName = versionName,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes
        )

        downloadJob = updaterScope.launch {
            try {
                val requestBuilder = Request.Builder().url(url)
                
                if (file.exists() && downloadedBytes > 0) {
                    if (totalBytes > 0 && downloadedBytes >= totalBytes) {
                        finishDownload()
                        return@launch
                    }
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                } else {
                    file.delete()
                    downloadedBytes = 0L
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    downloadState.value = GlobalDownloadState.Error("Server rejected request. Try restarting the download.")
                    return@launch
                }

                val body = response.body ?: return@launch
                if (totalBytes == 0L) {
                    totalBytes = body.contentLength() + downloadedBytes 
                }

                val inputStream = body.byteStream()
                val outputStream = java.io.FileOutputStream(file, downloadedBytes > 0)
                val buffer = ByteArray(8 * 1024)
                var bytes = inputStream.read(buffer)
                var lastEmitTime = System.currentTimeMillis()

                val notificationManager = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val notifId = 999

                val pauseIntent = android.app.PendingIntent.getBroadcast(appContext, 1, Intent("PIXELMUSIC_PAUSE").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                val cancelIntent = android.app.PendingIntent.getBroadcast(appContext, 3, Intent("PIXELMUSIC_CANCEL").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

                val openIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("pixelmusic://update_download")).apply {
                    setPackage(appContext!!.packageName)
                }
                val pendingOpenIntent = android.app.PendingIntent.getActivity(
                    appContext, 4, openIntent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                while (bytes >= 0) {
                    if (!isActive) break 

                    outputStream.write(buffer, 0, bytes)
                    downloadedBytes += bytes

                    val currentTime = System.currentTimeMillis()
                    // Push out progress if time elapsed
                    if (currentTime - lastEmitTime > 150) {
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                        downloadState.value = GlobalDownloadState.Downloading(progress, false, versionName, totalBytes, downloadedBytes)
                        
                        val totalMb = totalBytes / (1024f * 1024f)
                        val downMb = downloadedBytes / (1024f * 1024f)
                        val mbString = if (totalBytes > 0) String.format(Locale.US, "%.1f / %.1f MB", downMb, totalMb) else String.format(Locale.US, "%.1f MB downloaded", downMb)

                        val notif = androidx.core.app.NotificationCompat.Builder(appContext!!, "app_updates")
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setContentTitle("Downloading Update $versionName")
                            .setContentText(mbString)
                            .setProgress(100, (progress * 100).toInt(), false)
                            .setContentIntent(pendingOpenIntent)
                            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
                            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                            .setOngoing(true)
                            .build()
                        notificationManager.notify(notifId, notif)

                        lastEmitTime = currentTime
                    }
                    bytes = inputStream.read(buffer)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Finished condition: If the stream reached EOF naturally without being paused or cancelled
                if (isActive) {
                    finishDownload()
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    downloadState.value = GlobalDownloadState.Error(e.message ?: "Download failed")
                }
            }
        }
    }

    fun resumeDownload(context: Context) {
        if (currentDownloadUrl != null && currentVersionName != null) {
            startOrResumeDownload(context, currentDownloadUrl!!, currentVersionName!!)
        }
    }

    fun pauseDownload() {
        downloadJob?.cancel() // This safely exits the while loop due to !isActive
        currentVersionName?.let {
            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
            downloadState.value = GlobalDownloadState.Downloading(progress, isPaused = true, versionName = it, totalBytes = totalBytes, downloadedBytes = downloadedBytes)
            
            if (appContext != null) {
                val notificationManager = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val resumeIntent = android.app.PendingIntent.getBroadcast(appContext, 2, Intent("PIXELMUSIC_RESUME").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                val cancelIntent = android.app.PendingIntent.getBroadcast(appContext, 3, Intent("PIXELMUSIC_CANCEL").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                
                val openIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("pixelmusic://update_download")).apply {
                    setPackage(appContext!!.packageName)
                }
                val pendingOpenIntent = android.app.PendingIntent.getActivity(
                    appContext, 4, openIntent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val pausedNotif = androidx.core.app.NotificationCompat.Builder(appContext!!, "app_updates")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Update Paused ($it)")
                    .setProgress(100, (progress * 100).toInt(), false)
                    .setContentIntent(pendingOpenIntent)
                    .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                    .setOngoing(true)
                    .build()
                notificationManager.notify(999, pausedNotif)
            }
        }
    }

    fun cancelDownload(context: Context) {
        downloadJob?.cancel()
        currentFileName?.let {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), it)
            if (file.exists()) file.delete()
        }
        downloadedBytes = 0L
        totalBytes = 0L
        downloadState.value = GlobalDownloadState.Idle
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(999)
    }

    fun deleteApk(context: Context) {
        currentFileName?.let {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), it)
            if (file.exists()) file.delete()
        }
        downloadedBytes = 0L
        totalBytes = 0L
        downloadState.value = GlobalDownloadState.Idle
    }

    fun installApk(context: Context, file: File) {
        if (file.exists()) {
            val authority = "${context.packageName}.provider"
            val apkUri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }
}
