package com.saurav.pixelmusic.presentation.components.scoped

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.size.Size
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.presentation.components.albumArtMemoryCacheKey
import com.saurav.pixelmusic.presentation.components.safeAlbumArtTargetSize
import com.saurav.pixelmusic.utils.LocalArtworkUri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PrefetchAlbumNeighborsImg(
    current: Song?,
    queue: ImmutableList<Song>,
    radius: Int = 1
) {
    if (current == null) return
    val context = LocalContext.current
    val loader = coil.Coil.imageLoader(context)
    val index = remember(current, queue) { queue.indexOfFirst { it.id == current.id } }
    LaunchedEffect(index, queue) {
        if (index == -1) return@LaunchedEffect
        val bounds = (maxOf(0, index - radius))..(minOf(queue.lastIndex, index + radius))
        for (i in bounds) {
            if (i == index) continue
            queue[i].albumArtUriString?.let { data ->
                val effectiveQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
                val optimizedUri = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(data, effectiveQuality) ?: data
                val canonicalDiskKey = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optimizedUri) ?: optimizedUri
                val diskPolicy = if (LocalArtworkUri.isLocalArtworkUri(optimizedUri)) CachePolicy.DISABLED else CachePolicy.ENABLED
                val maxSize = effectiveQuality.maxSize
                val targetSize = if (maxSize > 0) Size(maxSize, maxSize) else safeAlbumArtTargetSize(Size.ORIGINAL)
                val memoryCacheKey = albumArtMemoryCacheKey(canonicalDiskKey, targetSize)
                val req = coil.request.ImageRequest.Builder(context)
                    .data(optimizedUri)
                    .diskCacheKey(if (diskPolicy == CachePolicy.DISABLED) null else canonicalDiskKey)
                    .apply {
                        if (memoryCacheKey != null) {
                            memoryCacheKey(memoryCacheKey)
                        }
                    }
                    .diskCachePolicy(diskPolicy)
                    .size(targetSize)
                    .build()
                loader.enqueue(req)
            }
        }
    }
}


@Composable
fun PrefetchAlbumNeighbors(
    isActive: Boolean,
    pagerState: PagerState,
    queue: ImmutableList<Song>,
    radius: Int = 1,
    targetSize: Size = Size(600, 600),
    anchorIndex: Int? = null
) {
    if (!isActive || queue.isEmpty()) return
    val context = LocalContext.current
    val imageLoader = coil.Coil.imageLoader(context)
    val requestTargetSize = remember(targetSize) {
        safeAlbumArtTargetSize(targetSize)
    }

    LaunchedEffect(pagerState, queue, anchorIndex, requestTargetSize) {
        snapshotFlow { 
            // If the user is manually scrolling, follow the PagerState.
            // If the Pager is idle, prioritize the provided anchorIndex (which is tied to the current song)
            // to avoid fetching neighbors of a stale index after a queue shift.
            if (pagerState.isScrollInProgress) pagerState.currentPage 
            else anchorIndex ?: pagerState.currentPage 
        }
            .distinctUntilChanged()
            .collect { page ->
                val indices = (page - radius..page + radius)
                    .filter { it in queue.indices && it != page }
                indices.forEach { idx ->
                    queue[idx].albumArtUriString?.let { uri ->
                        val effectiveQuality = com.saurav.pixelmusic.presentation.components.SmartImageCache.getEffectiveQuality()
                        val optimizedUri = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.optimizeArtworkUrl(uri, effectiveQuality) ?: uri
                        val diskPolicy = if (LocalArtworkUri.isLocalArtworkUri(optimizedUri)) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED
                        val maxSize = effectiveQuality.maxSize
                        val clampedSize = if (maxSize > 0) {
                            val widthPx = (requestTargetSize.width as? coil.size.Dimension.Pixels)?.px ?: maxSize
                            val heightPx = (requestTargetSize.height as? coil.size.Dimension.Pixels)?.px ?: maxSize
                            val clampedW = if (widthPx > maxSize) maxSize else widthPx
                            val clampedH = if (heightPx > maxSize) maxSize else heightPx
                            Size(clampedW, clampedH)
                        } else {
                            requestTargetSize
                        }
                        val canonicalDiskKey = com.saurav.pixelmusic.utils.ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optimizedUri) ?: optimizedUri
                        val memoryCacheKey = albumArtMemoryCacheKey(canonicalDiskKey, clampedSize)
                        val req = coil.request.ImageRequest.Builder(context)
                            .data(optimizedUri)
                            .diskCacheKey(if (diskPolicy == coil.request.CachePolicy.DISABLED) null else canonicalDiskKey)
                            .size(clampedSize)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .apply {
                                if (memoryCacheKey != null) {
                                    memoryCacheKey(memoryCacheKey)
                                }
                            }
                            .diskCachePolicy(diskPolicy)
                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                            .allowHardware(true)
                            .build()
                        imageLoader.enqueue(req) // fire-and-forget
                    }
                }
            }
    }
}
