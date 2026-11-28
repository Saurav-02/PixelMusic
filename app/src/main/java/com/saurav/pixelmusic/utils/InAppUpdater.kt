package com.saurav.pixelmusic.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GithubRelease(
    @SerializedName("tag_name", alternate = ["version", "versionName", "tagName", "tag"])
    val tagName: String? = null,
    @SerializedName("assets")
    val assets: List<GithubAsset>? = null,
    @SerializedName("body", alternate = ["changelog", "description", "notes"])
    val body: String? = null,
    @SerializedName("download_url", alternate = ["browser_download_url", "url"])
    val downloadUrl: String? = null,
    @SerializedName("previous_release")
    val previousRelease: GithubRelease? = null
)

data class GithubAsset(
    @SerializedName("download_url", alternate = ["browser_download_url", "url"])
    val downloadUrl: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("size")
    val size: Long? = null
)

sealed class UpdateState {
    object Checking : UpdateState()
    data class Error(val message: String) : UpdateState()
    data class UpToDate(
        val versionName: String? = null,
        val downloadUrl: String? = null,
        val changelog: String? = null,
        val apkName: String? = null,
        val apkSizeBytes: Long? = null,
        val previousVersionName: String? = null,
        val previousDownloadUrl: String? = null,
        val previousChangelog: String? = null,
        val previousApkName: String? = null,
        val previousApkSizeBytes: Long? = null
    ) : UpdateState()
    data class Available(
        val versionName: String, 
        val downloadUrl: String,
        val changelog: String? = null,
        val apkName: String? = null,
        val apkSizeBytes: Long? = null,
        val previousVersionName: String? = null,
        val previousDownloadUrl: String? = null,
        val previousChangelog: String? = null,
        val previousApkName: String? = null,
        val previousApkSizeBytes: Long? = null
    ) : UpdateState()
}

object InAppUpdater {
    private const val TAG = "InAppUpdater"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    // Update middleman (Cloudflare Worker): the app never talks to GitHub
    // directly, so a decompiled APK reveals no repo and no token.
    // Test builds get "?channel=test" appended by the Test.yaml workflow.
    private const val UPDATE_CHECK_URL = "https://pixelmusic-updater.atappu805.workers.dev/updater/check"

    /**
     * Parses release JSON directly via org.json so R8 Full Mode / ProGuard
     * minification can never strip or obfuscate model fields or constructors.
     */
    private fun parseGithubRelease(json: JSONObject): GithubRelease {
        fun JSONObject.firstNonBlankString(vararg keys: String): String? {
            for (key in keys) {
                if (!has(key) || isNull(key)) continue
                val value = optString(key, "").trim()
                if (value.isNotEmpty() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
            return null
        }

        val assetsList = mutableListOf<GithubAsset>()
        val assetsArray: JSONArray? = json.optJSONArray("assets")
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val obj = assetsArray.optJSONObject(i) ?: continue
                val assetUrl = obj.firstNonBlankString("download_url", "browser_download_url", "url")
                val assetName = obj.firstNonBlankString("name")
                val assetSize = if (obj.has("size") && !obj.isNull("size")) obj.optLong("size", -1L).takeIf { it > 0L } else null
                assetsList.add(
                    GithubAsset(
                        downloadUrl = assetUrl,
                        name = assetName,
                        size = assetSize
                    )
                )
            }
        }

        val previousObj = json.optJSONObject("previous_release")
        return GithubRelease(
            tagName = json.firstNonBlankString("tag_name", "version", "versionName", "tagName", "tag"),
            assets = assetsList,
            body = json.firstNonBlankString("body", "changelog", "description", "notes"),
            downloadUrl = json.firstNonBlankString("download_url", "browser_download_url", "url"),
            previousRelease = previousObj?.let { parseGithubRelease(it) }
        )
    }

    private fun resolveDownloadDir(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.cacheDir, "apk_updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    val latestUpdateState = MutableStateFlow<UpdateState?>(null)
    val openUpdateSheetEvent = MutableStateFlow(false)

    fun requestOpenUpdateSheet() {
        openUpdateSheetEvent.value = true
    }

    fun consumeOpenUpdateSheetEvent() {
        openUpdateSheetEvent.value = false
    }

    fun getDeviceArchitecture(): String {
        return android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    fun resolveApkFileName(
        downloadUrl: String?,
        versionName: String,
        assetName: String? = null
    ): String {
        if (!assetName.isNullOrBlank() && assetName.endsWith(".apk", ignoreCase = true)) {
            return assetName
        }
        val fromUrl = downloadUrl
            ?.substringBefore("?")
            ?.substringAfterLast("/")
            ?.takeIf { it.endsWith(".apk", ignoreCase = true) }
        if (!fromUrl.isNullOrBlank()) {
            return fromUrl
        }
        val arch = getDeviceArchitecture()
        val cleanVer = versionName.ifBlank { "latest" }
        return "PixelMusic-$arch-$cleanVer.apk"
    }

    fun isNewerVersion(latest: String?, current: String?): Boolean {
        if (latest.isNullOrBlank() || current.isNullOrBlank()) return false

        val isLatestTest = latest.contains("test-build", ignoreCase = true)
        val isCurrentTest = current.contains("test-build", ignoreCase = true)

        if (isLatestTest && isCurrentTest) {
            val numLatest = latest.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            val numCurrent = current.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            return numLatest > numCurrent
        }

        if (isLatestTest != isCurrentTest) {
            return false
        }

        val cleanL = latest.replace(Regex("[^0-9.]"), "").trim('.')
        val cleanC = current.replace(Regex("[^0-9.]"), "").trim('.')

        if (cleanL.isBlank() || cleanC.isBlank()) return false
        if (cleanL == cleanC) return false

        val partsL = cleanL.split('.').mapNotNull { it.toIntOrNull() }
        val partsC = cleanC.split('.').mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(partsL.size, partsC.size)
        for (i in 0 until maxLen) {
            val vL = partsL.getOrElse(i) { 0 }
            val vC = partsC.getOrElse(i) { 0 }
            if (vL > vC) return true
            if (vL < vC) return false
        }
        return false
    }

    suspend fun checkForUpdate(
        currentVersion: String,
        forceRefresh: Boolean = false
    ): UpdateState = withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (UPDATE_CHECK_URL.contains("?")) {
                UPDATE_CHECK_URL
            } else if (currentVersion.contains("test-build", ignoreCase = true)) {
                "$UPDATE_CHECK_URL?channel=test"
            } else {
                UPDATE_CHECK_URL
            }
            val targetUrl = if (forceRefresh) {
                val sep = if (baseUrl.contains("?")) "&" else "?"
                "$baseUrl${sep}_t=${System.currentTimeMillis()}"
            } else {
                baseUrl
            }

            val request = Request.Builder()
                .url(targetUrl)
                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "PixelMusic/$currentVersion (Android)")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check failed: HTTP ${response.code} for $targetUrl")
                    return@withContext UpdateState.Error("Update server returned HTTP ${response.code}")
                }
                val body = response.body.string()
                if (body.isBlank()) {
                    Log.w(TAG, "Update check returned empty body for $targetUrl")
                    return@withContext UpdateState.Error("Empty response from update server")
                }

                val release = parseGithubRelease(JSONObject(body))
                val tagName = release.tagName
                if (tagName.isNullOrBlank()) {
                    Log.w(TAG, "Update check missing tag_name in response")
                    return@withContext UpdateState.Error("Invalid update metadata")
                }

                val isUpdate = isNewerVersion(tagName, currentVersion)
                val apkAssets = release.assets?.filter { it.name?.endsWith(".apk", ignoreCase = true) == true } ?: emptyList()
                val apkAsset = selectBestApkForDevice(apkAssets)
                val downloadUrl = apkAsset?.downloadUrl ?: release.downloadUrl
                val resolvedApkName = resolveApkFileName(downloadUrl, tagName, apkAsset?.name)
                val resolvedApkSize = apkAsset?.size?.takeIf { it > 0L }

                // Resolve previous release (for in-place downgrade with frozen versionCode)
                val prevRel = release.previousRelease
                val prevTag = prevRel?.tagName?.takeIf { it.isNotBlank() }
                val prevApkAssets = prevRel?.assets?.filter { it.name?.endsWith(".apk", ignoreCase = true) == true } ?: emptyList()
                val prevApkAsset = selectBestApkForDevice(prevApkAssets)
                val prevDownloadUrl = prevApkAsset?.downloadUrl ?: prevRel?.downloadUrl
                val prevApkName = prevTag?.let { resolveApkFileName(prevDownloadUrl, it, prevApkAsset?.name) }
                val prevApkSize = prevApkAsset?.size?.takeIf { it > 0L }

                if (!downloadUrl.isNullOrBlank()) {
                    currentDownloadUrl = downloadUrl
                }
                currentVersionName = tagName
                currentChangelog = release.body
                currentApkDisplayName = resolvedApkName
                if (resolvedApkSize != null && totalBytes <= 0L) {
                    totalBytes = resolvedApkSize
                }

                if (isUpdate && !downloadUrl.isNullOrBlank()) {
                    val available = UpdateState.Available(
                        versionName = tagName,
                        downloadUrl = downloadUrl,
                        changelog = release.body,
                        apkName = resolvedApkName,
                        apkSizeBytes = resolvedApkSize,
                        previousVersionName = prevTag,
                        previousDownloadUrl = prevDownloadUrl,
                        previousChangelog = prevRel?.body,
                        previousApkName = prevApkName,
                        previousApkSizeBytes = prevApkSize
                    )
                    latestUpdateState.value = available
                    return@withContext available
                }
                val upToDate = UpdateState.UpToDate(
                    versionName = tagName,
                    downloadUrl = downloadUrl,
                    changelog = release.body,
                    apkName = resolvedApkName,
                    apkSizeBytes = resolvedApkSize,
                    previousVersionName = prevTag,
                    previousDownloadUrl = prevDownloadUrl,
                    previousChangelog = prevRel?.body,
                    previousApkName = prevApkName,
                    previousApkSizeBytes = prevApkSize
                )
                latestUpdateState.value = upToDate
                return@withContext upToDate
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check exception: ${e.message}", e)
            return@withContext UpdateState.Error(e.message ?: "Could not reach update server")
        }
    }

    private fun selectBestApkForDevice(assets: List<GithubAsset>): GithubAsset? {
        val validAssets = assets.filter { !it.downloadUrl.isNullOrBlank() }
        if (validAssets.isEmpty()) return null
        if (validAssets.size == 1) return validAssets.first() 

        val deviceAbis = android.os.Build.SUPPORTED_ABIS.map { it.lowercase() }

        for (abi in deviceAbis) {
            val abiMatch = when {
                abi.contains("arm64") -> validAssets.firstOrNull { it.name?.contains("arm64", ignoreCase = true) == true || it.name?.contains("v8a", ignoreCase = true) == true }
                abi.contains("v7") -> validAssets.firstOrNull { it.name?.contains("armv7", ignoreCase = true) == true || it.name?.contains("v7a", ignoreCase = true) == true }
                abi.contains("x86_64") -> validAssets.firstOrNull { it.name?.contains("x86_64", ignoreCase = true) == true }
                abi.contains("x86") -> validAssets.firstOrNull { it.name?.contains("x86", ignoreCase = true) == true }
                else -> null
            }
            if (abiMatch != null) return abiMatch
        }

        val universalMatch = validAssets.firstOrNull { it.name?.contains("universal", ignoreCase = true) == true }
        if (universalMatch != null) return universalMatch

        return validAssets.first()
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
    private var currentApkDisplayName: String? = null
    private var currentChangelog: String? = null
    private var downloadedBytes = 0L
    private var totalBytes = 0L

    fun getResolvedDownloadUrl(): String? = when (val state = latestUpdateState.value) {
        is UpdateState.Available -> state.downloadUrl
        is UpdateState.UpToDate -> state.downloadUrl ?: currentDownloadUrl
        else -> currentDownloadUrl
    }

    fun getResolvedVersionName(): String? = when (val state = latestUpdateState.value) {
        is UpdateState.Available -> state.versionName
        is UpdateState.UpToDate -> state.versionName ?: currentVersionName
        else -> currentVersionName
    }

    fun getResolvedChangelog(): String? = when (val state = latestUpdateState.value) {
        is UpdateState.Available -> state.changelog
        is UpdateState.UpToDate -> state.changelog ?: currentChangelog
        else -> currentChangelog
    }

    fun getResolvedApkName(): String? = when (val state = latestUpdateState.value) {
        is UpdateState.Available -> state.apkName ?: currentApkDisplayName
        is UpdateState.UpToDate -> state.apkName ?: currentApkDisplayName
        else -> currentApkDisplayName
    }

    fun getResolvedApkSizeBytes(): Long? = when (val state = latestUpdateState.value) {
        is UpdateState.Available -> state.apkSizeBytes ?: totalBytes.takeIf { it > 0L }
        is UpdateState.UpToDate -> state.apkSizeBytes ?: totalBytes.takeIf { it > 0L }
        else -> totalBytes.takeIf { it > 0L }
    }

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

    private fun buildOpenUpdateSheetPendingIntent(context: Context): android.app.PendingIntent {
        val openIntent = Intent(context, com.saurav.pixelmusic.MainActivity::class.java).apply {
            action = "OPEN_UPDATE_SHEET"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_UPDATE_SHEET", true)
        }
        return android.app.PendingIntent.getActivity(
            context,
            4,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val UPDATE_PROGRESS_CHANNEL_ID = "app_updates_progress"

    fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel(
                "app_updates", 
                "App Updates", 
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when a new app update is available or ready to install"
            }
            val progressChannel = android.app.NotificationChannel(
                UPDATE_PROGRESS_CHANNEL_ID,
                "App Update Downloads",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent progress notifications while downloading app updates"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(progressChannel)
        }
    }

    fun startOrResumeDownload(
        context: Context,
        url: String,
        versionName: String,
        apkName: String? = null,
        changelog: String? = null
    ) {
        if (url.isBlank()) {
            downloadState.value = GlobalDownloadState.Error("Invalid download URL")
            return
        }
        if (downloadJob?.isActive == true) return
        
        appContext = context.applicationContext
        createNotificationChannel(appContext!!)
        registerReceiverIfNeeded(appContext!!)

        if (currentDownloadUrl != null && currentDownloadUrl != url) {
            downloadedBytes = 0L
            totalBytes = 0L
        }

        currentDownloadUrl = url
        currentVersionName = versionName
        if (!apkName.isNullOrBlank()) currentApkDisplayName = apkName
        if (!changelog.isNullOrBlank()) currentChangelog = changelog
        currentFileName = resolveApkFileName(url, versionName, apkName)
        val file = File(resolveDownloadDir(context), currentFileName!!)

        fun finishDownload() {
            downloadState.value = GlobalDownloadState.Finished(file, versionName)
            
            try {
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
                    .setSmallIcon(com.saurav.pixelmusic.R.drawable.monochrome_player)
                    .setContentTitle("Download Complete")
                    .setContentText("Tap to install PixelMusic $versionName")
                    .setContentIntent(pendingInstall)
                    .addAction(android.R.drawable.stat_sys_download_done, "Install", pendingInstall)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .build()
                    
                val notificationManager = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(999)
                notificationManager.notify(999, finishedNotif)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "PixelMusic (Android)")
                
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

                var initialResponse = client.newCall(requestBuilder.build()).execute()
                if (initialResponse.code == 416 && downloadedBytes > 0L) {
                    // Range no longer valid (e.g. file replaced on server) — restart cleanly
                    initialResponse.close()
                    file.delete()
                    downloadedBytes = 0L
                    totalBytes = 0L
                    val freshRequest = Request.Builder()
                        .url(url)
                        .header("User-Agent", "PixelMusic (Android)")
                        .build()
                    initialResponse = client.newCall(freshRequest).execute()
                }

                initialResponse.use { response ->
                    if (!response.isSuccessful) {
                        downloadState.value = GlobalDownloadState.Error("Server rejected download (HTTP ${response.code}). Please retry.")
                        return@launch
                    }

                    val isPartial = response.code == 206
                    if (!isPartial) {
                        if (downloadedBytes > 0L) {
                            file.delete()
                            downloadedBytes = 0L
                        }
                        val len = response.body.contentLength()
                        if (len > 0L) totalBytes = len
                    } else {
                        val rangeTotal = response.header("Content-Range")
                            ?.substringAfter("/", "")
                            ?.toLongOrNull()
                        if (rangeTotal != null && rangeTotal > 0L) {
                            totalBytes = rangeTotal
                        } else if (totalBytes <= 0L) {
                            val len = response.body.contentLength()
                            if (len > 0L) totalBytes = len + downloadedBytes
                        }
                    }

                    val inputStream = response.body.byteStream()
                    java.io.FileOutputStream(file, isPartial && downloadedBytes > 0).use { outputStream ->
                        val buffer = ByteArray(16 * 1024)
                        var bytes = inputStream.read(buffer)
                        var lastEmitTime = System.currentTimeMillis()
                        var lastNotifTime = 0L

                        val notificationManager = appContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val notifId = 999

                        val pauseIntent = android.app.PendingIntent.getBroadcast(appContext, 1, Intent("PIXELMUSIC_PAUSE").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                        val cancelIntent = android.app.PendingIntent.getBroadcast(appContext, 3, Intent("PIXELMUSIC_CANCEL").setPackage(appContext!!.packageName), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                        val pendingOpenIntent = buildOpenUpdateSheetPendingIntent(appContext!!)

                        while (bytes >= 0) {
                            if (!isActive) break

                            outputStream.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            if (totalBytes > 0L && downloadedBytes >= totalBytes) {
                                break
                            }

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastEmitTime > 150) {
                                val progress = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                                downloadState.value = GlobalDownloadState.Downloading(progress, false, versionName, totalBytes, downloadedBytes)
                                lastEmitTime = currentTime

                                if (lastNotifTime == 0L || currentTime - lastNotifTime >= 750) {
                                    val totalMb = totalBytes / (1024f * 1024f)
                                    val downMb = downloadedBytes / (1024f * 1024f)
                                    val mbString = if (totalBytes > 0L) String.format(Locale.US, "%.1f / %.1f MB", downMb, totalMb) else String.format(Locale.US, "%.1f MB downloaded", downMb)

                                    val notif = androidx.core.app.NotificationCompat.Builder(appContext!!, UPDATE_PROGRESS_CHANNEL_ID)
                                        .setSmallIcon(android.R.drawable.stat_sys_download)
                                        .setContentTitle("Downloading Update $versionName")
                                        .setContentText(mbString)
                                        .setProgress(100, (progress * 100).toInt(), totalBytes <= 0L)
                                        .setContentIntent(pendingOpenIntent)
                                        .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
                                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                                        .setCategory(androidx.core.app.NotificationCompat.CATEGORY_PROGRESS)
                                        .setOnlyAlertOnce(true)
                                        .setSilent(true)
                                        .setOngoing(true)
                                        .build()
                                    notificationManager.notify(notifId, notif)
                                    lastNotifTime = currentTime
                                }
                            }
                            bytes = inputStream.read(buffer)
                        }
                        outputStream.flush()
                    }

                    // Verify complete download before marking Finished
                    if (isActive) {
                        val isComplete = file.exists() && file.length() > 0L && (totalBytes <= 0L || downloadedBytes >= totalBytes)
                        if (isComplete) {
                            finishDownload()
                        } else {
                            downloadState.value = GlobalDownloadState.Error("Download interrupted before finishing. Tap Retry to resume.")
                        }
                    }
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
                val pendingOpenIntent = buildOpenUpdateSheetPendingIntent(appContext!!)

                val pausedNotif = androidx.core.app.NotificationCompat.Builder(appContext!!, UPDATE_PROGRESS_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Update Paused ($it)")
                    .setProgress(100, (progress * 100).toInt(), false)
                    .setContentIntent(pendingOpenIntent)
                    .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                    .setCategory(androidx.core.app.NotificationCompat.CATEGORY_PROGRESS)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setOngoing(true)
                    .build()
                notificationManager.notify(999, pausedNotif)
            }
        }
    }

    fun cancelDownload(context: Context) {
        downloadJob?.cancel()
        currentFileName?.let {
            val file = File(resolveDownloadDir(context), it)
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
            val file = File(resolveDownloadDir(context), it)
            if (file.exists()) file.delete()
        }
        downloadedBytes = 0L
        totalBytes = 0L
        downloadState.value = GlobalDownloadState.Idle
    }

    fun installApk(context: Context, file: File) {
        if (file.exists()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        com.saurav.pixelmusic.presentation.components.PixelToast.makeText(
                            context,
                            "Please allow installing unknown apps to update",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(settingsIntent)
                        return
                    }
                }
                val authority = "${context.packageName}.provider"
                val apkUri = FileProvider.getUriForFile(context, authority, file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                com.saurav.pixelmusic.presentation.components.PixelToast.makeText(
                    context,
                    "Failed to open installer: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
