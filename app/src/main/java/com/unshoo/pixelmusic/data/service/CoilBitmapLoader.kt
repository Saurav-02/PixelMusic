package com.unshoo.pixelmusic.data.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.unshoo.pixelmusic.data.preferences.AlbumArtQuality
import com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository
import com.unshoo.pixelmusic.presentation.viewmodel.ConnectivityStateHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoilBitmapLoaderEntryPoint {
    fun connectivityStateHolder(): ConnectivityStateHolder
    fun userPreferencesRepository(): UserPreferencesRepository
}

@OptIn(UnstableApi::class)
class CoilBitmapLoader(private val context: Context, private val scope: CoroutineScope) : BitmapLoader {

    companion object {
        private const val MAX_NOTIFICATION_ARTWORK_SIZE_PX = 1024
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return loadBitmapInternal(uri)
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return loadBitmapInternal(data)
    }

    private fun loadBitmapInternal(data: Any): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        scope.launch {
            try {
                val appContext = context.applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    CoilBitmapLoaderEntryPoint::class.java
                )
                val userPrefs = entryPoint.userPreferencesRepository()
                val connectivity = entryPoint.connectivityStateHolder()

                val isMetered = connectivity.isMeteredNetwork.value
                val qualityWifi = userPrefs.albumArtQualityFlow.first()
                val qualityMobile = userPrefs.albumArtQualityMobileFlow.first()
                val performanceMode = userPrefs.performanceModeEnabledFlow.first()

                val effectiveQuality = com.unshoo.pixelmusic.utils.ThumbnailUrlUtils.getEffectiveQuality(
                    isMetered = isMetered,
                    qualityWifi = qualityWifi,
                    qualityMobile = qualityMobile,
                    performanceMode = performanceMode
                )

                val requestedSizePx = if (effectiveQuality.maxSize > 0) {
                    minOf(effectiveQuality.maxSize, MAX_NOTIFICATION_ARTWORK_SIZE_PX)
                } else {
                    MAX_NOTIFICATION_ARTWORK_SIZE_PX
                }

                val finalData: Any = if (data is Uri || data is String) {
                    val rawUrl = data.toString()
                    com.unshoo.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(rawUrl, effectiveQuality) ?: data
                } else {
                    data
                }

                val finalDataStr = finalData.toString()
                val request = ImageRequest.Builder(context)
                    .data(finalData)
                    .diskCacheKey(finalDataStr)
                    .size(requestedSizePx, requestedSizePx)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                val result = context.imageLoader.execute(request)
                val drawable = result.drawable
                
                if (drawable != null) {
                    val bitmap = drawable.toBitmap()
                    future.set(bitmap)
                } else {
                    future.setException(IllegalStateException("Coil returned null drawable for data: $data"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }
}
