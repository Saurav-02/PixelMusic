package com.saurav.pixelmusic.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saurav.pixelmusic.data.model.ProfileAvatarCatalog
import java.io.File

private object DataUriBitmapCache {
    private val cache = LruCache<String, ImageBitmap>(32)

    fun decode(dataUri: String): ImageBitmap? {
        cache.get(dataUri)?.let { return it }
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex <= 0 || commaIndex >= dataUri.lastIndex) return null
        val decoded = runCatching {
            val base64 = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
        if (decoded != null) {
            cache.put(dataUri, decoded)
        }
        return decoded
    }
}

/**
 * Unified circular profile avatar used everywhere a user appears in PixelMusic
 * (Home, Settings, Profile editor, PixelJam lobby, participants, and chat).
 *
 * Rendering priority:
 * 1. Built-in illustrated avatar (`pm_avatar:<id>:<bgId>`)
 * 2. Compact Base64 data URI (`data:image/...`) from PixelJam peers
 * 3. Local photo file or remote URL (e.g. user photo or YouTube account photo)
 * 4. First letter of the user's display name (default when nothing is picked)
 */
@Composable
fun ProfileAvatar(
    avatarSpec: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cacheKey: Long = 0L,
    fallbackContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    fallbackContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    fallbackInitial: String = "P"
) {
    val builtIn = remember(avatarSpec) { ProfileAvatarCatalog.parseBuiltIn(avatarSpec) }
    val initial = remember(displayName, fallbackInitial) {
        displayName.trim().firstOrNull()?.uppercase() ?: fallbackInitial
    }
    val initialFontSize = remember(size) { (size.value * 0.44f).coerceIn(11f, 44f).sp }

    if (builtIn != null) {
        val (avatar, bg) = builtIn
        val bgBrush = remember(bg.id) {
            Brush.linearGradient(
                colors = listOf(Color(bg.topColorHex), Color(bg.bottomColorHex))
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(bgBrush)
        ) {
            Image(
                painter = painterResource(avatar.drawableRes),
                contentDescription = displayName.ifBlank { avatar.label },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    if (ProfileAvatarCatalog.isDataUri(avatarSpec)) {
        val dataBitmap = remember(avatarSpec) { DataUriBitmapCache.decode(avatarSpec) }
        if (dataBitmap != null) {
            Image(
                bitmap = dataBitmap,
                contentDescription = displayName.ifBlank { "Profile photo" },
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
            return
        }
    }

    val isImageSource = avatarSpec.isNotBlank() && avatarSpec != ProfileAvatarCatalog.INITIAL_ONLY

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackContainerColor)
    ) {
        // Always draw the initial underneath so loading or broken images gracefully show the first letter.
        Text(
            text = initial,
            fontSize = initialFontSize,
            fontWeight = FontWeight.Bold,
            color = fallbackContentColor
        )

        if (isImageSource) {
            val context = LocalContext.current
            val request = remember(avatarSpec, cacheKey) {
                val data: Any = if (avatarSpec.startsWith("/")) File(avatarSpec) else avatarSpec
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(true)
                    .apply {
                        if (cacheKey > 0L) {
                            memoryCacheKey("${avatarSpec}_$cacheKey")
                            diskCacheKey("${avatarSpec}_$cacheKey")
                        }
                    }
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = displayName.ifBlank { "Profile photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}
