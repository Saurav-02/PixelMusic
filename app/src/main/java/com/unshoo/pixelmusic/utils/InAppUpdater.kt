package com.unshoo.pixelmusic.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

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
    object UpToDate : UpdateState()
    data class Available(
        val versionName: String, 
        val downloadUrl: String,
        val changelog: String? = null
    ) : UpdateState()
    data class Downloading(
        val progress: Float,
        val changelog: String? = null
    ) : UpdateState()
}

object InAppUpdater {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val REPO_URL = "https://pixelmusic-updater.atappu805.workers.dev/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REPO_URL).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                val release = gson.fromJson(body, GithubRelease::class.java)
                
                // Compare tags (e.g., "v2.1" vs "2.0.0")
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
            }
            return@withContext UpdateState.UpToDate
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateState.UpToDate
        }
    }

    fun downloadAndTrackProgress(context: Context, url: String, fileName: String): Flow<Float> = flow {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) file.delete()

            // 1. Start the download directly using OkHttp
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@flow

            val body = response.body ?: return@flow
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = java.io.FileOutputStream(file)

            var bytesCopied = 0L
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)

            var lastEmitTime = System.currentTimeMillis()

            // 2. Read the file stream and update the progress bar natively
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes
                
                // Throttle UI updates to 10 times a second so the Compose animation doesn't lag
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime > 100 || bytesCopied == totalBytes) {
                    if (totalBytes > 0) {
                        emit(bytesCopied.toFloat() / totalBytes.toFloat())
                    }
                    lastEmitTime = currentTime
                }
                bytes = inputStream.read(buffer)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            emit(1f) // Ensure it visually hits 100%
            
            // 3. Launch the Android installer safely on the main thread
            withContext(Dispatchers.Main) {
                promptInstall(context, fileName)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    private fun promptInstall(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
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
}
