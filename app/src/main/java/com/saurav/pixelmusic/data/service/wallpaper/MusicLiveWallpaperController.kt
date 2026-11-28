package com.saurav.pixelmusic.data.service.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.PlayerInfo
import com.saurav.pixelmusic.data.preferences.AlbumArtQuality
import com.saurav.pixelmusic.data.preferences.MusicLiveWallpaperSettings
import com.saurav.pixelmusic.data.preferences.MusicWallpaperShapePosition
import com.saurav.pixelmusic.data.preferences.MusicWallpaperStyleMode
import com.saurav.pixelmusic.data.preferences.MusicWallpaperTargetScreen
import com.saurav.pixelmusic.presentation.components.SmartImageCache
import com.saurav.pixelmusic.utils.AlbumArtUtils
import com.saurav.pixelmusic.utils.ThumbnailUrlUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared silky cubic-bezier easing matching `SmoothMorphEasing` on the AOD screen.
 */
val WallpaperSmoothMorphEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Evaluates the same `CubicBezier(0.22, 1.0, 0.36, 1.0)` curve for raw `Float` progress in
 * Android `WallpaperService` Canvas animations without Compose runtime overhead.
 */
fun evaluateSmoothWallpaperEasing(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    return WallpaperSmoothMorphEasing.transform(x)
}

/**
 * Curated Material You Expressive shapes matching the AOD screen's mathematical polar definitions.
 * Ordered so consecutive taps cycle smoothly through Cookies, Rounded Triangle, Rounded Square,
 * Rounded Rectangle, Pill, Rounded Diamond, Oval, and Soft Pentagon.
 */
enum class WallpaperExpressiveShapeKind(val displayName: String, val shortLabel: String) {
    COOKIE_8("8-Sided Soft Cookie", "Cookie 8") {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(8.0 * th)
    },
    ROUNDED_TRIANGLE("Rounded Triangle", "Triangle") {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.064 * cos(3.0 * a) - 0.011 * cos(6.0 * a)
        }
    },
    COOKIE_6("6-Sided Soft Cookie", "Cookie 6") {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.042 * cos(6.0 * th)
    },
    ROUNDED_SQUIRCLE("Rounded Square", "Square") {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th))
            val s = abs(sin(th))
            return 1.0 / (c.pow(4.0) + s.pow(4.0)).pow(0.25)
        }
    },
    COOKIE_4("4-Sided Clover Cookie", "Cookie 4") {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.046 * cos(4.0 * th + PI / 4.0)
    },
    ROUNDED_RECTANGLE("Rounded Rectangle", "Rectangle") {
        override fun rawRadiusAt(th: Double): Double {
            val c = abs(cos(th)) / 0.84
            val s = abs(sin(th)) / 1.02
            return 1.0 / (c.pow(4.2) + s.pow(4.2)).pow(1.0 / 4.2)
        }
    },
    COOKIE_12("12-Sided Sunny Cookie", "Cookie 12") {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.025 * cos(12.0 * th)
    },
    PILL("Material You Pill", "Pill") {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - (28.0 * PI / 180.0)
            val c = abs(cos(a)) / 0.78
            val s = abs(sin(a)) / 1.00
            return 1.0 / (c.pow(4.6) + s.pow(4.6)).pow(1.0 / 4.6)
        }
    },
    COOKIE_7("7-Sided Soft Cookie", "Cookie 7") {
        override fun rawRadiusAt(th: Double): Double = 1.0 + 0.036 * cos(7.0 * th - PI / 2.0)
    },
    ROUNDED_DIAMOND("Rounded Diamond", "Diamond") {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 4.0
            val c = abs(cos(a))
            val s = abs(sin(a))
            return 1.0 / (c.pow(3.5) + s.pow(3.5)).pow(1.0 / 3.5)
        }
    },
    OVAL("Smooth Tilted Oval", "Oval") {
        override fun rawRadiusAt(th: Double): Double {
            val a = th + (22.0 * PI / 180.0)
            val c = cos(a) / 0.82
            val s = sin(a) / 1.00
            return 1.0 / sqrt(c * c + s * s)
        }
    },
    ROUNDED_PENTAGON("Soft Pentagon", "Pentagon") {
        override fun rawRadiusAt(th: Double): Double {
            val a = th - PI / 2.0
            return 1.0 + 0.034 * cos(5.0 * a) - 0.005 * cos(10.0 * a)
        }
    };

    abstract fun rawRadiusAt(th: Double): Double

    private val normScale: Double by lazy {
        val samples = 180
        var maxR = 0.001
        var sumR = 0.0
        for (i in 0 until samples) {
            val r = rawRadiusAt((2.0 * PI * i) / samples)
            if (r > maxR) maxR = r
            sumR += r
        }
        val meanR = sumR / samples
        (0.62 * maxR + 0.38 * meanR).coerceAtLeast(maxR * 0.94)
    }

    fun normalizedRadiusAt(th: Double): Float {
        return (rawRadiusAt(th) / normScale).toFloat().coerceAtMost(1.04f)
    }

    companion object {
        fun fromIndex(index: Int): WallpaperExpressiveShapeKind {
            val list = entries
            if (list.isEmpty()) return COOKIE_8
            return list[index.mod(list.size)]
        }

        fun nextIndex(currentIndex: Int): Int {
            val list = entries
            if (list.size <= 1) return 0
            return (currentIndex + 1).mod(list.size)
        }
    }
}

/**
 * Vertical center fraction on the screen/preview for Top, Middle, and Bottom shape placement.
 * Top leaves comfortable breathing room below the top-left song information overlay.
 */
fun MusicWallpaperShapePosition.verticalCenterFraction(): Float = when (this) {
    MusicWallpaperShapePosition.TOP -> 0.32f
    MusicWallpaperShapePosition.MIDDLE -> 0.51f
    MusicWallpaperShapePosition.BOTTOM -> 0.71f
}

/**
 * Builds an `android.graphics.Path` that smoothly morphs between:
 * - Full-screen rectangle (`modeMorphT = 0f`) and the Material You Expressive shape (`modeMorphT = 1f`)
 * - `fromShape` and `toShape` (`shapeMorphProgress = 0f..1f`) with `shapeRotationRad` spin.
 */
fun buildMorphingWallpaperAndroidPath(
    outPath: android.graphics.Path,
    canvasWidth: Float,
    canvasHeight: Float,
    centerX: Float,
    centerY: Float,
    shapeDiameterPx: Float,
    fromShape: WallpaperExpressiveShapeKind,
    toShape: WallpaperExpressiveShapeKind,
    shapeMorphProgress: Float,
    shapeRotationRad: Double,
    modeMorphT: Float = 1f,
    steps: Int = 240
) {
    outPath.reset()
    val w = canvasWidth.coerceAtLeast(1f)
    val h = canvasHeight.coerceAtLeast(1f)
    val baseRadius = (shapeDiameterPx * 0.482f).coerceAtLeast(1f)
    val clampedShapeMorph = shapeMorphProgress.coerceIn(0f, 1f)
    val clampedMode = modeMorphT.coerceIn(0f, 1f)
    val count = steps.coerceAtLeast(120)
    val twoPi = 2.0 * PI

    // Distance to screen edges from (centerX, centerY) for smooth Full Screen <-> Shape morphing
    val distLeft = centerX.coerceAtLeast(1f)
    val distRight = (w - centerX).coerceAtLeast(1f)
    val distTop = centerY.coerceAtLeast(1f)
    val distBottom = (h - centerY).coerceAtLeast(1f)
    val minScreenDim = min(w, h)
    val cornerR = minScreenDim * (clampedMode * 2.4f).coerceAtMost(0.45f)

    for (i in 0 until count) {
        val theta = (twoPi * i) / count
        val cosT = cos(theta).toFloat()
        val sinT = sin(theta).toFloat()

        val rotatedTheta = theta - shapeRotationRad
        val rFrom = fromShape.normalizedRadiusAt(rotatedTheta)
        val rTo = toShape.normalizedRadiusAt(rotatedTheta)
        val rShape = baseRadius * lerp(rFrom, rTo, clampedShapeMorph)

        val finalR = if (clampedMode >= 0.999f) {
            rShape
        } else {
            val rx = if (cosT >= 0f) distRight / cosT.coerceAtLeast(1e-5f) else distLeft / (-cosT).coerceAtLeast(1e-5f)
            val ry = if (sinT >= 0f) distBottom / sinT.coerceAtLeast(1e-5f) else distTop / (-sinT).coerceAtLeast(1e-5f)
            var rFull = min(rx, ry)
            if (cornerR > 1f) {
                val halfSpan = min(distLeft, distRight)
                val halfVert = min(distTop, distBottom)
                val absCos = abs(cosT).coerceAtLeast(1e-5f)
                val absSin = abs(sinT).coerceAtLeast(1e-5f)
                val icx = (halfSpan - cornerR).coerceAtLeast(0f)
                val icy = (halfVert - cornerR).coerceAtLeast(0f)
                val px = rFull * absCos
                val py = rFull * absSin
                if (px > icx && py > icy) {
                    val dot = icx * absCos + icy * absSin
                    val cVal = icx * icx + icy * icy - cornerR * cornerR
                    val disc = (dot * dot - cVal).coerceAtLeast(0f)
                    rFull = min(rFull, dot + sqrt(disc))
                }
            }
            lerp(rFull, rShape, clampedMode)
        }

        val x = (centerX + finalR * cosT).coerceIn(0f, w)
        val y = (centerY + finalR * sinT).coerceIn(0f, h)
        if (i == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
    }
    outPath.close()
}

/**
 * Compose [Shape] wrapper around the polar Material You Expressive shape equation for the preview screen.
 */
class WallpaperMorphingComposeShape(
    private val fromShape: WallpaperExpressiveShapeKind,
    private val toShape: WallpaperExpressiveShapeKind,
    private val shapeMorphProgress: Float,
    private val shapeRotationRad: Double,
    private val steps: Int = 240
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val baseRadius = min(w, h) * 0.482f
        val clampedMorph = shapeMorphProgress.coerceIn(0f, 1f)
        val count = steps.coerceAtLeast(120)
        val twoPi = 2.0 * PI

        val path = Path().apply {
            for (i in 0 until count) {
                val theta = (twoPi * i) / count
                val cosT = cos(theta).toFloat()
                val sinT = sin(theta).toFloat()
                val rotatedTheta = theta - shapeRotationRad
                val rFrom = fromShape.normalizedRadiusAt(rotatedTheta)
                val rTo = toShape.normalizedRadiusAt(rotatedTheta)
                val r = baseRadius * lerp(rFrom, rTo, clampedMorph)
                val x = (cx + r * cosT).coerceIn(0f, w)
                val y = (cy + r * sinT).coerceIn(0f, h)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

data class WallpaperPlaybackSnapshot(
    val songTitle: String = "",
    val artistName: String = "",
    val albumArtUriString: String? = null,
    val isPlaying: Boolean = false,
    val albumArtBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WallpaperPlaybackSnapshot) return false
        return songTitle == other.songTitle &&
            artistName == other.artistName &&
            albumArtUriString == other.albumArtUriString &&
            isPlaying == other.isPlaying &&
            ((albumArtBytes == null) == (other.albumArtBytes == null))
    }

    override fun hashCode(): Int {
        var result = songTitle.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + (albumArtUriString?.hashCode() ?: 0)
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + (albumArtBytes?.size ?: 0)
        return result
    }
}

/**
 * Central state & zero-network artwork cache manager for Music Live Wallpaper.
 *
 * Strictly reuses album art already downloaded by Now Playing, Mini Player, Notification, or
 * local storage (`CachePolicy.DISABLED` for network requests) so the wallpaper never consumes
 * extra mobile/Wi-Fi data.
 */
data class WallpaperAmbientPalette(
    val topColor: Int,
    val bottomColor: Int
)

object MusicLiveWallpaperStateHolder {
    private const val FALLBACK_FILE_NAME = "music_live_wallpaper_fallback.jpg"
    private const val MAX_ARTWORK_DIMENSION_PX = 1440
    private const val MAX_FALLBACK_DIMENSION_PX = 1920
    private const val MIN_HD_ARTWORK_DIMENSION_PX = 480
    private const val TRACK_SETTLE_DEBOUNCE_MS = 3500L

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialized = AtomicBoolean(false)

    private val _playbackState = MutableStateFlow(WallpaperPlaybackSnapshot())
    val playbackState: StateFlow<WallpaperPlaybackSnapshot> = _playbackState.asStateFlow()

    private val bitmapCache = LruCache<String, Bitmap>(16)
    private val blurredBitmapCache = LruCache<String, Bitmap>(24)
    private val ambientPaletteCache = LruCache<String, WallpaperAmbientPalette>(32)

    @Volatile
    private var cachedFallbackPath: String = ""

    @Volatile
    private var cachedFallbackUpdatedAt: Long = -1L

    @Volatile
    private var cachedFallbackBitmap: Bitmap? = null

    @Volatile
    private var latestSettings: MusicLiveWallpaperSettings = MusicLiveWallpaperSettings()

    @Volatile
    private var lastPushedSystemSignature: String = ""

    @Volatile
    private var cachedRoundedFont: Typeface? = null

    private var pseudoLivePlaybackJob: Job? = null
    private var pseudoLiveSettingsJob: Job? = null

    /**
     * Initializes the "Pseudo-Live" Static Wallpaper Manager (`WallpaperManager.setBitmap`).
     *
     * Why this survives manual Force Stop in Android System Settings:
     * When a wallpaper is pushed via `WallpaperManager.setBitmap()`, Android caches the image in
     * `/data/system/users/0/wallpaper` and renders it inside `SystemUI`'s built-in `ImageWallpaper`
     * process rather than binding to a `WallpaperService` inside PixelMusic. Even if the user
     * manually Force Stops PixelMusic in Android Settings, `SystemUI` keeps displaying the cached
     * wallpaper bitmap and NEVER resets to the device's factory default wallpaper.
     */
    fun initialize(context: Context) {
        // Music Live Wallpaper requires Android 12+ (API 31); do nothing on older versions.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        managerScope.launch {
            val entryPoint = runCatching {
                EntryPointAccessors.fromApplication(
                    appContext,
                    MusicLiveWallpaperServiceEntryPoint::class.java
                )
            }.getOrNull() ?: return@launch

            val prefsRepo = entryPoint.userPreferencesRepository()
            val playbackHolder = entryPoint.playbackStateHolder()

            // If the user previously had the legacy WallpaperService active on their device,
            // automatically migrate them to the Force-Stop-safe Pseudo-Live mode.
            runCatching {
                val wm = WallpaperManager.getInstance(appContext)
                if (wm.wallpaperInfo?.component == ComponentName(appContext, MusicLiveWallpaperService::class.java)) {
                    prefsRepo.setMusicWallpaperEnabled(true)
                }
            }

            // 1. Sync PlaybackStateHolder into _playbackState
            launch {
                playbackHolder.stablePlayerState.collectLatest { stable ->
                    val song = stable.currentSong
                    if (song != null) {
                        updatePlaybackQuickState(
                            songTitle = song.title,
                            artistName = song.displayArtist.takeIf { it.isNotBlank() } ?: song.artist,
                            albumArtUriString = song.albumArtUriString,
                            isPlaying = stable.isPlaying
                        )
                    } else if (!stable.isPlaying && _playbackState.value.isPlaying) {
                        markPlaybackStopped(appContext)
                    }
                }
            }

            // 2. Observe settings changes and update the Pseudo-Live static system wallpaper when enabled
            launch {
                var isFirstSettings = true
                prefsRepo.musicLiveWallpaperSettingsFlow.collectLatest { newSettings ->
                    val oldSettings = latestSettings
                    latestSettings = newSettings

                    if (isFirstSettings) {
                        // Never re-push on app start; the stored fallback is already in the system.
                        isFirstSettings = false
                        return@collectLatest
                    }
                    if (!newSettings.isEnabled) return@collectLatest

                    // Only fallback-related changes matter to the system wallpaper store. Shape, blur,
                    // style, song info etc. are rendered live by MusicLiveWallpaperService.
                    val fallbackChanged =
                        oldSettings.fallbackWallpaperPath != newSettings.fallbackWallpaperPath ||
                            oldSettings.fallbackWallpaperUpdatedAt != newSettings.fallbackWallpaperUpdatedAt ||
                            oldSettings.backgroundDim != newSettings.backgroundDim ||
                            oldSettings.targetScreen != newSettings.targetScreen
                    if (fallbackChanged) {
                        schedulePseudoLiveSettingsSync(appContext, delayMs = 280L, force = false)
                    }
                }
            }

            // 3. Observe playback state changes for Pseudo-Live static wallpaper updates
            launch {
                var isInitialPlayback = true
                _playbackState.collectLatest { snapshot ->
                    if (isInitialPlayback) {
                        isInitialPlayback = false
                        return@collectLatest
                    }
                    if (!latestSettings.isEnabled) return@collectLatest

                    pseudoLivePlaybackJob?.cancel()
                    pseudoLivePlaybackJob = managerScope.launch {
                        if (snapshot.isPlaying) {
                            // 5.2s settle debounce while skipping tracks so rapid skips do zero work
                            delay(TRACK_SETTLE_DEBOUNCE_MS)
                            if (latestSettings.isEnabled && _playbackState.value.isPlaying &&
                                latestSettings.animateShapeOnTrackChange &&
                                latestSettings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE
                            ) {
                                val nextShape = WallpaperExpressiveShapeKind.nextIndex(latestSettings.shapeIndex)
                                prefsRepo.setMusicWallpaperShapeIndex(nextShape)
                            }
                            // Playback changes never write to the system wallpaper store (see pushPseudoLiveWallpaperToSystem).
                        }
                    }
                }
            }
        }
    }

    private fun schedulePseudoLiveSettingsSync(context: Context, delayMs: Long, force: Boolean) {
        pseudoLiveSettingsJob?.cancel()
        pseudoLiveSettingsJob = managerScope.launch {
            if (delayMs > 0L) delay(delayMs)
            if (latestSettings.isEnabled) {
                pushPseudoLiveWallpaperToSystem(context.applicationContext, force = force)
            }
        }
    }

    fun updateFromPlayerInfo(playerInfo: PlayerInfo) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        _playbackState.update { current ->
            val resolvedArtBytes = playerInfo.albumArtBitmapData
                ?: if (current.albumArtUriString == playerInfo.albumArtUri) current.albumArtBytes else null
            WallpaperPlaybackSnapshot(
                songTitle = playerInfo.songTitle,
                artistName = playerInfo.artistName,
                albumArtUriString = playerInfo.albumArtUri,
                isPlaying = playerInfo.isPlaying,
                albumArtBytes = resolvedArtBytes
            )
        }
    }

    fun updatePlaybackQuickState(
        songTitle: String,
        artistName: String,
        albumArtUriString: String?,
        isPlaying: Boolean
    ) {
        _playbackState.update { current ->
            val keepBytes = if (current.albumArtUriString == albumArtUriString) current.albumArtBytes else null
            WallpaperPlaybackSnapshot(
                songTitle = songTitle,
                artistName = artistName,
                albumArtUriString = albumArtUriString,
                isPlaying = isPlaying,
                albumArtBytes = keepBytes
            )
        }
    }

    /**
     * Called when `MusicService` unloads, stops, or `onTaskRemoved` / `onDestroy` fires.
     * Immediately marks playback as stopped. Nothing is written to `WallpaperManager` here;
     * the stored gallery fallback is already in place.
     */
    fun markPlaybackStopped(context: Context? = null) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        _playbackState.update { current ->
            if (!current.isPlaying) current else current.copy(isPlaying = false)
        }
        pseudoLivePlaybackJob?.cancel()
    }

    /**
     * Immediately renders and applies the current Pseudo-Live wallpaper (or forces the fallback
     * wallpaper when disabling) directly into Android's `WallpaperManager` storage.
     */
    suspend fun applyPseudoLiveWallpaperNow(
        context: Context,
        settingsOverride: MusicLiveWallpaperSettings? = null,
        forceFallbackIdle: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        if (settingsOverride != null) {
            latestSettings = settingsOverride
        }
        pushPseudoLiveWallpaperToSystem(
            context = context.applicationContext,
            force = true,
            forceFallbackIdle = forceFallbackIdle
        )
    }

    /**
     * Persists ONLY the user's gallery fallback image into Android's WallpaperManager store.
     *
     * Album art is deliberately never written here: `WallpaperManager.setBitmap()` changes the system
     * WallpaperColors, so pushing art per track re-themes Material You and restarts apps on every skip.
     * Album art is drawn by [MusicLiveWallpaperService] on its own canvas, layered over the fallback
     * with no connection to the stored image.
     *
     * Never runs while the live service is bound, because `setBitmap()` would detach it.
     * [forceFallbackIdle] is kept only for call-site compatibility; the fallback is always what is pushed.
     */
    private suspend fun pushPseudoLiveWallpaperToSystem(
        context: Context,
        force: Boolean,
        forceFallbackIdle: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val s = latestSettings

            val wm = WallpaperManager.getInstance(appContext)
            val isLiveServiceActive = wm.wallpaperInfo?.component == ComponentName(
                appContext,
                MusicLiveWallpaperService::class.java
            )
            if (isLiveServiceActive) {
                // The live service already draws fallback + album art itself and picks up fallback
                // changes through settings; setBitmap() here would detach it.
                return@runCatching true
            }

            val fb = loadFallbackWallpaperBitmap(
                appContext,
                s.fallbackWallpaperPath,
                s.fallbackWallpaperUpdatedAt
            )

            // If the user hasn't picked a gallery fallback yet, leave their system wallpaper untouched.
            if (fb == null || fb.isRecycled) {
                return@runCatching false
            }

            val signature = "fallback|${s.fallbackWallpaperPath}|${s.fallbackWallpaperUpdatedAt}|${(s.backgroundDim * 100).roundToInt()}|${s.targetScreen}"
            if (!force && signature == lastPushedSystemSignature) {
                return@runCatching true
            }

            val dm = appContext.resources.displayMetrics
            val rawW = min(dm.widthPixels, dm.heightPixels).coerceAtLeast(720)
            val rawH = max(dm.widthPixels, dm.heightPixels).coerceAtLeast(1600)
            val scaleCap = min(1440f / rawW.toFloat(), 2560f / rawH.toFloat()).coerceAtMost(1f)
            val targetW = (rawW * scaleCap).roundToInt().coerceAtLeast(720)
            val targetH = (rawH * scaleCap).roundToInt().coerceAtLeast(1600)

            val rendered = renderWallpaperBitmap(
                context = appContext,
                width = targetW,
                height = targetH,
                settings = s,
                sharpBitmap = fb,
                blurredBitmap = null,
                sourceKey = "sys_fallback_${s.fallbackWallpaperUpdatedAt}",
                isFallbackIdle = true,
                songTitle = "",
                artistName = ""
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (s.targetScreen) {
                    MusicWallpaperTargetScreen.HOME -> WallpaperManager.FLAG_SYSTEM
                    MusicWallpaperTargetScreen.LOCK -> WallpaperManager.FLAG_LOCK
                    MusicWallpaperTargetScreen.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                // If legacy WallpaperService is currently bound, calling setBitmap(rendered) or
                // setBitmap with FLAG_SYSTEM detaches WallpaperService and switches to ImageWallpaper.
                runCatching {
                    wm.setBitmap(rendered, null, false, flags)
                }.onFailure {
                    wm.setBitmap(rendered)
                }
            } else {
                wm.setBitmap(rendered)
            }

            lastPushedSystemSignature = signature
            true
        }.getOrDefault(false)
    }

    fun getOrLoadPixelMusicTypeface(context: Context): Typeface? {
        cachedRoundedFont?.let { return it }
        val loaded = runCatching {
            ResourcesCompat.getFont(context.applicationContext, R.font.gflex_variable)
        }.getOrNull()
        if (loaded != null) {
            cachedRoundedFont = loaded
        }
        return loaded
    }

    fun getOrExtractAmbientPalette(source: Bitmap, sourceKey: String): WallpaperAmbientPalette {
        synchronized(ambientPaletteCache) {
            ambientPaletteCache.get(sourceKey)?.let { return it }
        }
        val soft = ensureSoftwareBitmap(source)
        val w = soft.width.coerceAtLeast(1)
        val h = soft.height.coerceAtLeast(1)

        val topSample = sampleRegionColor(soft, 0, 0, w, (h * 0.42f).roundToInt().coerceAtLeast(1))
        val bottomSample = sampleRegionColor(
            soft,
            0,
            (h * 0.35f).roundToInt().coerceIn(0, h - 1),
            w,
            h
        )
        val palette = WallpaperAmbientPalette(
            topColor = boostAmbientTone(topSample),
            bottomColor = boostAmbientTone(bottomSample)
        )
        synchronized(ambientPaletteCache) {
            ambientPaletteCache.put(sourceKey, palette)
        }
        return palette
    }

    private fun sampleRegionColor(bmp: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var weightSum = 0L
        val stepX = ((endX - startX) / 12).coerceAtLeast(1)
        val stepY = ((endY - startY) / 12).coerceAtLeast(1)
        val hsv = FloatArray(3)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val px = bmp.getPixel(x.coerceIn(0, bmp.width - 1), y.coerceIn(0, bmp.height - 1))
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                Color.RGBToHSV(r, g, b, hsv)
                // Favor colorful, non-black/non-white pixels so the fade matches vibrant artwork tones
                val vibrancyWeight = (1 + (hsv[1] * 6f).roundToInt()) *
                    (if (hsv[2] in 0.15f..0.92f) 2 else 1)
                rSum += r * vibrancyWeight
                gSum += g * vibrancyWeight
                bSum += b * vibrancyWeight
                weightSum += vibrancyWeight
                x += stepX
            }
            y += stepY
        }

        if (weightSum <= 0L) return Color.rgb(36, 32, 48)
        return Color.rgb(
            (rSum / weightSum).toInt().coerceIn(0, 255),
            (gSum / weightSum).toInt().coerceIn(0, 255),
            (bSum / weightSum).toInt().coerceIn(0, 255)
        )
    }

    private fun boostAmbientTone(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 1.18f).coerceIn(0.22f, 0.82f)
        hsv[2] = hsv[2].coerceIn(0.32f, 0.88f)
        return Color.HSVToColor(hsv)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    fun renderWallpaperBitmap(
        context: Context,
        width: Int,
        height: Int,
        settings: MusicLiveWallpaperSettings,
        sharpBitmap: Bitmap?,
        blurredBitmap: Bitmap?,
        sourceKey: String,
        isFallbackIdle: Boolean,
        songTitle: String,
        artistName: String,
        fromShape: WallpaperExpressiveShapeKind = WallpaperExpressiveShapeKind.fromIndex(settings.shapeIndex),
        toShape: WallpaperExpressiveShapeKind = WallpaperExpressiveShapeKind.fromIndex(settings.shapeIndex),
        shapeMorphProgress: Float = 1f,
        shapeRotationDeg: Float = 0f
    ): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        renderWallpaperSceneToCanvas(
            context = context,
            canvas = canvas,
            w = w.toFloat(),
            h = h.toFloat(),
            settings = settings,
            sharpBmp = sharpBitmap,
            blurredBmp = blurredBitmap,
            sourceKey = sourceKey,
            isFallbackIdle = isFallbackIdle,
            songTitle = songTitle,
            artistName = artistName,
            fromShape = fromShape,
            toShape = toShape,
            shapeMorphProgress = shapeMorphProgress,
            shapeRotationDeg = shapeRotationDeg,
            positionCenterY = settings.shapePosition.verticalCenterFraction(),
            modeMorphT = if (settings.styleMode == MusicWallpaperStyleMode.FULL_SCREEN || isFallbackIdle) 0f else 1f
        )
        return out
    }

    /**
     * Unified, allocation-light Canvas renderer for all 7 [MusicWallpaperStyleMode] styles,
     * shared across `WallpaperManager` ("Pseudo-Live" static wallpaper), `MusicLiveWallpaperService`,
     * and the in-app interactive preview & style cards.
     */
    fun renderWallpaperSceneToCanvas(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        settings: MusicLiveWallpaperSettings,
        sharpBmp: Bitmap?,
        blurredBmp: Bitmap?,
        sourceKey: String,
        isFallbackIdle: Boolean,
        songTitle: String,
        artistName: String,
        fromShape: WallpaperExpressiveShapeKind = WallpaperExpressiveShapeKind.fromIndex(settings.shapeIndex),
        toShape: WallpaperExpressiveShapeKind = WallpaperExpressiveShapeKind.fromIndex(settings.shapeIndex),
        shapeMorphProgress: Float = 1f,
        shapeRotationDeg: Float = 0f,
        positionCenterY: Float = settings.shapePosition.verticalCenterFraction(),
        modeMorphT: Float = 1f,
        drawSongInfoOverlay: Boolean = true
    ) {
        canvas.drawColor(Color.rgb(12, 12, 16))

        if (sharpBmp == null || sharpBmp.isRecycled) {
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        Color.rgb(34, 24, 56),
                        Color.rgb(20, 24, 42),
                        Color.rgb(12, 12, 18)
                    ),
                    floatArrayOf(0f, 0.52f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, gradPaint)
            return
        }

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val fullRect = RectF(0f, 0f, w, h)

        // When showing the user's Gallery Fallback Wallpaper (idle / no music playing),
        // always render the clean full-screen gallery image without music shape/blur overlays.
        val activeStyle = if (isFallbackIdle) MusicWallpaperStyleMode.FULL_SCREEN else settings.styleMode
        val effectiveBlurBmp = if (blurredBmp != null && !blurredBmp.isRecycled) blurredBmp else sharpBmp
        val blurStrength = settings.blurAmount.coerceIn(0.15f, 1.0f)

        when (activeStyle) {
            MusicWallpaperStyleMode.FULL_SCREEN -> {
                drawCenterCropBitmapInRect(canvas, sharpBmp, fullRect, bitmapPaint, 255)
            }

            MusicWallpaperStyleMode.TOP_ART_BOTTOM_BLUR -> {
                // Card 2 style: Sharp album art in top area + smooth blurry ambient color fade in bottom area
                val palette = getOrExtractAmbientPalette(effectiveBlurBmp, sourceKey)
                drawCenterCropBitmapInRect(canvas, effectiveBlurBmp, fullRect, bitmapPaint, 255)

                // Rich ambient color fade over the blurred bottom area
                val bottomWashAlpha = (blurStrength * 235f).roundToInt().coerceIn(90, 242)
                val midWashAlpha = (blurStrength * 175f).roundToInt().coerceIn(60, 200)
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.TRANSPARENT,
                        withAlpha(palette.bottomColor, (midWashAlpha * 0.22f).roundToInt()),
                        withAlpha(palette.bottomColor, midWashAlpha),
                        withAlpha(palette.bottomColor, bottomWashAlpha),
                        withAlpha(palette.bottomColor, (bottomWashAlpha * 1.03f).roundToInt().coerceAtMost(248))
                    ),
                    floatArrayOf(0f, 0.36f, 0.54f, 0.74f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                overlayPaint.shader = null

                // Feathered sharp album art in the top showcase zone (0%..70% height)
                val saveLayer = canvas.saveLayer(0f, 0f, w, h, null)
                val artShowcaseRect = RectF(0f, 0f, w, h * 0.72f)
                drawCenterCropBitmapInRect(canvas, sharpBmp, artShowcaseRect, bitmapPaint, 255)

                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    shader = LinearGradient(
                        0f, 0f, 0f, h,
                        intArrayOf(
                            Color.argb(255, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(135, 0, 0, 0),
                            Color.argb(28, 0, 0, 0),
                            Color.argb(0, 0, 0, 0),
                            Color.argb(0, 0, 0, 0)
                        ),
                        floatArrayOf(0f, 0.38f, 0.50f, 0.60f, 0.67f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, maskPaint)
                canvas.restoreToCount(saveLayer)

                // Subtle top status-bar shade matching Card 2 in reference screenshot
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h * 0.12f,
                    intArrayOf(
                        Color.argb(92, 20, 24, 30),
                        Color.argb(36, 20, 24, 30),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h * 0.12f, overlayPaint)
                overlayPaint.shader = null
            }

            MusicWallpaperStyleMode.TOP_AND_BOTTOM_BLUR -> {
                // Card 3 style: Blurry ambient band at top, sharp album art in upper-middle, blurry fade at bottom
                val palette = getOrExtractAmbientPalette(effectiveBlurBmp, sourceKey)
                drawCenterCropBitmapInRect(canvas, effectiveBlurBmp, fullRect, bitmapPaint, 255)

                val topGlowAlpha = (blurStrength * 215f).roundToInt().coerceIn(85, 235)
                val bottomWashAlpha = (blurStrength * 235f).roundToInt().coerceIn(90, 242)
                val midWashAlpha = (blurStrength * 175f).roundToInt().coerceIn(60, 200)
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        withAlpha(palette.bottomColor, topGlowAlpha),
                        withAlpha(palette.topColor, (topGlowAlpha * 0.55f).roundToInt()),
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        withAlpha(palette.bottomColor, midWashAlpha),
                        withAlpha(palette.bottomColor, bottomWashAlpha),
                        withAlpha(palette.bottomColor, (bottomWashAlpha * 1.03f).roundToInt().coerceAtMost(248))
                    ),
                    floatArrayOf(0f, 0.06f, 0.16f, 0.38f, 0.54f, 0.74f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                overlayPaint.shader = null

                val saveLayer = canvas.saveLayer(0f, 0f, w, h, null)
                val artShowcaseRect = RectF(0f, 0f, w, h * 0.72f)
                drawCenterCropBitmapInRect(canvas, sharpBmp, artShowcaseRect, bitmapPaint, 255)

                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    shader = LinearGradient(
                        0f, 0f, 0f, h,
                        intArrayOf(
                            Color.argb(0, 0, 0, 0),
                            Color.argb(48, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(130, 0, 0, 0),
                            Color.argb(25, 0, 0, 0),
                            Color.argb(0, 0, 0, 0),
                            Color.argb(0, 0, 0, 0)
                        ),
                        floatArrayOf(0f, 0.055f, 0.15f, 0.40f, 0.51f, 0.60f, 0.67f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, maskPaint)
                canvas.restoreToCount(saveLayer)
            }

            MusicWallpaperStyleMode.SUBTLE_BOTTOM_BLUR -> {
                // "Less Blur": ~72% of the screen stays crisp album art, soft blur only in the bottom dock area
                val palette = getOrExtractAmbientPalette(effectiveBlurBmp, sourceKey)
                drawCenterCropBitmapInRect(canvas, effectiveBlurBmp, fullRect, bitmapPaint, 255)

                val dockWashAlpha = (blurStrength * 205f).roundToInt().coerceIn(70, 225)
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        withAlpha(palette.bottomColor, (dockWashAlpha * 0.45f).roundToInt()),
                        withAlpha(palette.bottomColor, dockWashAlpha)
                    ),
                    floatArrayOf(0f, 0.62f, 0.80f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                overlayPaint.shader = null

                val saveLayer = canvas.saveLayer(0f, 0f, w, h, null)
                drawCenterCropBitmapInRect(canvas, sharpBmp, fullRect, bitmapPaint, 255)

                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    shader = LinearGradient(
                        0f, 0f, 0f, h,
                        intArrayOf(
                            Color.argb(255, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(135, 0, 0, 0),
                            Color.argb(0, 0, 0, 0),
                            Color.argb(0, 0, 0, 0)
                        ),
                        floatArrayOf(0f, 0.66f, 0.78f, 0.91f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, maskPaint)
                canvas.restoreToCount(saveLayer)
            }

            MusicWallpaperStyleMode.SOFT_VIGNETTE_BLUR -> {
                // Cinema Frame Blur: Soft blur at top (~18%) & bottom (~22%) with a large sharp center
                val palette = getOrExtractAmbientPalette(effectiveBlurBmp, sourceKey)
                drawCenterCropBitmapInRect(canvas, effectiveBlurBmp, fullRect, bitmapPaint, 255)

                val edgeWashAlpha = (blurStrength * 190f).roundToInt().coerceIn(65, 215)
                overlayPaint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        withAlpha(palette.topColor, edgeWashAlpha),
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        withAlpha(palette.bottomColor, edgeWashAlpha)
                    ),
                    floatArrayOf(0f, 0.22f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                overlayPaint.shader = null

                val saveLayer = canvas.saveLayer(0f, 0f, w, h, null)
                drawCenterCropBitmapInRect(canvas, sharpBmp, fullRect, bitmapPaint, 255)

                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    shader = LinearGradient(
                        0f, 0f, 0f, h,
                        intArrayOf(
                            Color.argb(0, 0, 0, 0),
                            Color.argb(95, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(255, 0, 0, 0),
                            Color.argb(95, 0, 0, 0),
                            Color.argb(0, 0, 0, 0)
                        ),
                        floatArrayOf(0f, 0.09f, 0.21f, 0.72f, 0.85f, 0.96f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, maskPaint)
                canvas.restoreToCount(saveLayer)
            }

            MusicWallpaperStyleMode.AMBIENT_GLASS_CARD -> {
                // Floating rounded album cover card over blurred backdrop
                drawCenterCropBitmapInRect(canvas, effectiveBlurBmp, fullRect, bitmapPaint, 255)
                val baseDim = (settings.backgroundDim + 0.12f).coerceIn(0f, 0.75f)
                overlayPaint.color = Color.argb((baseDim * 255f).roundToInt(), 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)

                val cx = w * 0.5f
                val cy = h * positionCenterY
                val cardSize = min(w, h) * 0.74f * settings.shapeScale
                val cornerRadius = cardSize * 0.14f
                val cardRect = RectF(
                    cx - cardSize * 0.5f,
                    cy - cardSize * 0.5f,
                    cx + cardSize * 0.5f,
                    cy + cardSize * 0.5f
                )

                // Soft outer glass halo
                val haloRect = RectF(cardRect).apply { inset(-cardSize * 0.028f, -cardSize * 0.028f) }
                overlayPaint.color = Color.argb(38, 255, 255, 255)
                canvas.drawRoundRect(haloRect, cornerRadius * 1.12f, cornerRadius * 1.12f, overlayPaint)

                val clipPath = android.graphics.Path().apply {
                    addRoundRect(cardRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
                }
                val saveCount = canvas.save()
                canvas.clipPath(clipPath)
                drawCenterCropBitmapInRect(canvas, sharpBmp, cardRect, bitmapPaint, 255)
                canvas.restoreToCount(saveCount)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(1.5f, min(w, h) * 0.0035f)
                    color = Color.argb(52, 255, 255, 255)
                }
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)
            }

            MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE -> {
                val effectiveModeT = modeMorphT.coerceIn(0f, 1f)
                val bgBmp = if (settings.blurAmount > 0.01f) effectiveBlurBmp else sharpBmp
                drawCenterCropBitmapInRect(canvas, bgBmp, fullRect, bitmapPaint, 255)

                val baseDim = lerp(
                    settings.backgroundDim,
                    (settings.backgroundDim + 0.14f).coerceAtMost(0.78f),
                    effectiveModeT
                )
                val dimAlpha = (baseDim * 255f).roundToInt().coerceIn(0, 210)
                if (dimAlpha > 0) {
                    overlayPaint.color = Color.argb(dimAlpha, 0, 0, 0)
                    canvas.drawRect(0f, 0f, w, h, overlayPaint)
                }

                val cx = w * 0.5f
                val cy = h * positionCenterY
                val shapeDiameter = min(w, h) * 0.72f * settings.shapeScale
                val totalRotRad = Math.toRadians(shapeRotationDeg.toDouble())

                if (effectiveModeT > 0.25f) {
                    val haloPath = android.graphics.Path()
                    buildMorphingWallpaperAndroidPath(
                        outPath = haloPath,
                        canvasWidth = w,
                        canvasHeight = h,
                        centerX = cx,
                        centerY = cy,
                        shapeDiameterPx = shapeDiameter * 1.065f,
                        fromShape = fromShape,
                        toShape = toShape,
                        shapeMorphProgress = shapeMorphProgress,
                        shapeRotationRad = totalRotRad + Math.toRadians(16.0),
                        modeMorphT = 1f
                    )
                    overlayPaint.color = Color.argb((effectiveModeT * 42f).roundToInt().coerceIn(0, 60), 255, 255, 255)
                    canvas.drawPath(haloPath, overlayPaint)
                }

                val shapePath = android.graphics.Path()
                buildMorphingWallpaperAndroidPath(
                    outPath = shapePath,
                    canvasWidth = w,
                    canvasHeight = h,
                    centerX = cx,
                    centerY = cy,
                    shapeDiameterPx = shapeDiameter,
                    fromShape = fromShape,
                    toShape = toShape,
                    shapeMorphProgress = shapeMorphProgress,
                    shapeRotationRad = totalRotRad,
                    modeMorphT = effectiveModeT
                )

                val saveCount = canvas.save()
                canvas.clipPath(shapePath)
                val boxSize = shapeDiameter * 1.02f
                val artRect = RectF(
                    lerp(0f, cx - boxSize * 0.5f, effectiveModeT),
                    lerp(0f, cy - boxSize * 0.5f, effectiveModeT),
                    lerp(w, cx + boxSize * 0.5f, effectiveModeT),
                    lerp(h, cy + boxSize * 0.5f, effectiveModeT)
                )
                drawCenterCropBitmapInRect(canvas, sharpBmp, artRect, bitmapPaint, 255)
                canvas.restoreToCount(saveCount)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(1.5f, min(w, h) * 0.0032f)
                    color = Color.argb((effectiveModeT * 40f).roundToInt().coerceIn(0, 60), 255, 255, 255)
                }
                canvas.drawPath(shapePath, borderPaint)
            }
        }

        // Apply contrast dim for non-shape/card styles (shape/card styles already dimmed their backdrop)
        if (activeStyle != MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE &&
            activeStyle != MusicWallpaperStyleMode.AMBIENT_GLASS_CARD
        ) {
            val dimAlpha = (settings.backgroundDim * 255f).roundToInt().coerceIn(0, 200)
            if (dimAlpha > 0) {
                overlayPaint.color = Color.argb(dimAlpha, 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
            }
        }

        // Top-right Song Information Overlay using the bundled PixelMusic default font (R.font.gflex_variable)
        if (drawSongInfoOverlay && settings.showSongInfo && !isFallbackIdle && songTitle.isNotBlank()) {
            val scaleFactor = (min(w, h) / 392f).coerceAtLeast(0.35f)
            val horizontalMargin = 24f * scaleFactor
            val rightX = w - horizontalMargin
            val topY = 58f * scaleFactor
            val maxTextWidth = (w - horizontalMargin * 2f).coerceAtLeast(60f)

            overlayPaint.shader = LinearGradient(
                0f, 0f, 0f, h * 0.22f,
                intArrayOf(
                    Color.argb(128, 0, 0, 0),
                    Color.argb(54, 0, 0, 0),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h * 0.22f, overlayPaint)
            overlayPaint.shader = null

            val roundedFont = getOrLoadPixelMusicTypeface(context)
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(245, 255, 255, 255)
                textSize = 20f * scaleFactor
                typeface = roundedFont ?: Typeface.create("sans-serif", Typeface.BOLD)
                isFakeBoldText = false
                runCatching { fontVariationSettings = "'wght' 700, 'ROND' 100" }
                setShadowLayer(10f * scaleFactor, 0f, 2.5f * scaleFactor, Color.argb(145, 0, 0, 0))
            }
            val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 255, 255, 255)
                textSize = 14f * scaleFactor
                typeface = roundedFont ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
                isFakeBoldText = false
                runCatching { fontVariationSettings = "'wght' 500, 'ROND' 100" }
                setShadowLayer(8f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(135, 0, 0, 0))
            }

            val ellipsizedTitle = TextUtils.ellipsize(songTitle, titlePaint, maxTextWidth, TextUtils.TruncateAt.END).toString()
            val titleWidth = titlePaint.measureText(ellipsizedTitle)
            val titleX = (rightX - titleWidth).coerceAtLeast(horizontalMargin)
            val titleBaseline = topY - titlePaint.ascent()
            canvas.drawText(ellipsizedTitle, titleX, titleBaseline, titlePaint)

            if (artistName.isNotBlank()) {
                val ellipsizedArtist = TextUtils.ellipsize(artistName, artistPaint, maxTextWidth, TextUtils.TruncateAt.END).toString()
                val artistWidth = artistPaint.measureText(ellipsizedArtist)
                val artistX = (rightX - artistWidth).coerceAtLeast(horizontalMargin)
                val artistBaseline = titleBaseline + titlePaint.descent() + (4f * scaleFactor) - artistPaint.ascent()
                canvas.drawText(ellipsizedArtist, artistX, artistBaseline, artistPaint)
            }
        }
    }

    private fun drawCenterCropBitmapInRect(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: RectF,
        paint: Paint,
        alpha: Int
    ) {
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val dw = dst.width().coerceAtLeast(1f)
        val dh = dst.height().coerceAtLeast(1f)

        val scale = max(dw / bw, dh / bh)
        val dx = dst.left + (dw - bw * scale) * 0.5f
        val dy = dst.top + (dh - bh * scale) * 0.5f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        val needsClip = dst.left > 0.5f || dst.top > 0.5f ||
            dst.right < canvas.width - 0.5f || dst.bottom < canvas.height - 0.5f
        val save = if (needsClip) {
            val sc = canvas.save()
            canvas.clipRect(dst)
            sc
        } else {
            -1
        }
        paint.alpha = alpha.coerceIn(0, 255)
        canvas.drawBitmap(bitmap, matrix, paint)
        paint.alpha = 255
        if (save != -1) {
            canvas.restoreToCount(save)
        }
    }

    /**
     * Loads the full-resolution (720p+) cached album art [Bitmap].
     *
     * Priority:
     * 1. Coil DiskCache snapshot for 720p (`AlbumArtQuality.HIGH`) & effective quality — because
     *    `ColorSchemeProcessor` and `extractDominantColor` overwrite `MemoryCache[canonicalDiskKey]`
     *    with a downsampled 128x128 / 160x160 bitmap, while `DiskCache` holds the untouched 720p file!
     * 2. Coil MemoryCache entries whose minimum dimension is at least [MIN_HD_ARTWORK_DIMENSION_PX] (480px+),
     *    skipping any 96x96 / 128x128 / 160x160 list or palette thumbnails.
     * 3. Local artwork stream (`content://`, `file://`, `LocalArtworkUri`).
     * 4. Full-size Coil execution with a dedicated HD memory cache key (`_wallpaper_hd`) so it never
     *    collides with 128x128 palette extraction bitmaps.
     */
    suspend fun loadCachedAlbumArtBitmap(
        context: Context,
        rawUriString: String?,
        embeddedArtBytes: ByteArray? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val cleanUri = rawUriString?.trim()?.takeIf { it.isNotEmpty() }

        if (cleanUri != null) {
            val effectiveQuality = SmartImageCache.getEffectiveQuality()
            // Always prioritize HIGH (720p) first so if 720p is cached on disk we always use it
            val candidateQualities = listOf(AlbumArtQuality.HIGH, effectiveQuality, AlbumArtQuality.LOW).distinct()
            val optimizedHigh = ThumbnailUrlUtils.optimizeArtworkUrl(cleanUri, AlbumArtQuality.HIGH) ?: cleanUri
            val canonicalHigh = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optimizedHigh, AlbumArtQuality.HIGH)
                ?: optimizedHigh

            synchronized(bitmapCache) {
                bitmapCache.get(canonicalHigh)?.takeIf {
                    !it.isRecycled && min(it.width, it.height) >= MIN_HD_ARTWORK_DIMENSION_PX
                }?.let { return@withContext it }
            }

            val isRemote = cleanUri.startsWith("http://", ignoreCase = true) ||
                cleanUri.startsWith("https://", ignoreCase = true)

            // 1. Check Coil DiskCache snapshots FIRST (stores the raw, full-resolution 720p file!)
            findInCoilDiskCache(appContext, cleanUri, candidateQualities)?.let { bmp ->
                synchronized(bitmapCache) {
                    bitmapCache.put(canonicalHigh, bmp)
                }
                return@withContext bmp
            }

            // 2. Check Coil MemoryCache ONLY for high-resolution bitmaps (>= 480px, ignoring 128x128 palette thumbs)
            findInCoilMemoryCache(appContext, cleanUri, candidateQualities)?.let { bmp ->
                val soft = ensureSoftwareBitmap(bmp)
                synchronized(bitmapCache) {
                    bitmapCache.put(canonicalHigh, soft)
                }
                return@withContext soft
            }

            // 3. Check local URI streams (content://, file://, LocalArtworkUri)
            if (!isRemote) {
                decodeLocalArtworkUri(appContext, cleanUri)?.let { bmp ->
                    synchronized(bitmapCache) {
                        bitmapCache.put(canonicalHigh, bmp)
                    }
                    return@withContext bmp
                }
            }

            // 4. Try Coil offline disk/memory execution with a dedicated HD memoryCacheKey so it decodes
            //    the full 720p file from disk rather than returning the 128x128 palette memoryCache entry.
            for (q in candidateQualities) {
                val optUrl = ThumbnailUrlUtils.optimizeArtworkUrl(cleanUri, q) ?: cleanUri
                val diskKey = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optUrl, q) ?: optUrl
                val hdMemKey = "${diskKey}_wallpaper_hd"
                val offlineRequest = ImageRequest.Builder(appContext)
                    .data(optUrl)
                    .diskCacheKey(diskKey)
                    .memoryCacheKey(hdMemKey)
                    .size(MAX_ARTWORK_DIMENSION_PX, MAX_ARTWORK_DIMENSION_PX)
                    .precision(Precision.INEXACT)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.DISABLED)
                    .build()

                val drawable = runCatching { appContext.imageLoader.execute(offlineRequest).drawable }.getOrNull()
                if (drawable != null) {
                    val w = drawable.intrinsicWidth.coerceIn(1, MAX_ARTWORK_DIMENSION_PX)
                    val h = drawable.intrinsicHeight.coerceIn(1, MAX_ARTWORK_DIMENSION_PX)
                    if (min(w, h) >= MIN_HD_ARTWORK_DIMENSION_PX || q == candidateQualities.last()) {
                        val bmp = runCatching {
                            drawable.toBitmap(width = w, height = h, config = Bitmap.Config.ARGB_8888)
                        }.getOrNull()
                        if (bmp != null && !bmp.isRecycled) {
                            synchronized(bitmapCache) {
                                bitmapCache.put(canonicalHigh, bmp)
                            }
                            return@withContext bmp
                        }
                    }
                }
            }

            // 5. If the user skipped tracks while on the Home Screen (so Now Playing wasn't open to cache 720p yet),
            //    fetch the effective quality artwork once via Coil into the shared canonicalDiskKey so both
            //    the wallpaper and Now Playing share the single 720p cached file.
            val targetQ = effectiveQuality
            val optUrl = ThumbnailUrlUtils.optimizeArtworkUrl(cleanUri, targetQ) ?: cleanUri
            val diskKey = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(optUrl, targetQ) ?: optUrl
            val hdMemKey = "${diskKey}_wallpaper_hd"
            val request = ImageRequest.Builder(appContext)
                .data(optUrl)
                .diskCacheKey(diskKey)
                .memoryCacheKey(hdMemKey)
                .size(MAX_ARTWORK_DIMENSION_PX, MAX_ARTWORK_DIMENSION_PX)
                .precision(Precision.INEXACT)
                .allowHardware(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            val drawable = runCatching { appContext.imageLoader.execute(request).drawable }.getOrNull()
            if (drawable != null) {
                val bmp = runCatching {
                    drawable.toBitmap(
                        width = drawable.intrinsicWidth.coerceIn(1, MAX_ARTWORK_DIMENSION_PX),
                        height = drawable.intrinsicHeight.coerceIn(1, MAX_ARTWORK_DIMENSION_PX),
                        config = Bitmap.Config.ARGB_8888
                    )
                }.getOrNull()
                if (bmp != null && !bmp.isRecycled) {
                    synchronized(bitmapCache) {
                        bitmapCache.put(canonicalHigh, bmp)
                    }
                    return@withContext bmp
                }
            }
        }

        // Only use embedded bytes when no URI is available (e.g. embedded-only local track)
        val fallbackBytes = embeddedArtBytes ?: _playbackState.value.albumArtBytes
        if (fallbackBytes != null && fallbackBytes.isNotEmpty()) {
            return@withContext decodeSampledByteArray(fallbackBytes, MAX_ARTWORK_DIMENSION_PX)
        }

        null
    }

    private fun findInCoilMemoryCache(
        context: Context,
        rawUri: String,
        qualities: List<AlbumArtQuality>
    ): Bitmap? {
        val memCache = context.imageLoader.memoryCache ?: return null
        val candidateKeys = LinkedHashSet<String>()
        for (q in qualities) {
            val opt = ThumbnailUrlUtils.optimizeArtworkUrl(rawUri, q) ?: rawUri
            val canonical = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(opt, q) ?: opt
            candidateKeys.add("${canonical}_wallpaper_hd")
            candidateKeys.add(canonical)
            candidateKeys.add(opt)
        }
        candidateKeys.add(rawUri)

        for (keyStr in candidateKeys) {
            val direct = memCache[MemoryCache.Key(keyStr)]?.bitmap
            if (direct != null && !direct.isRecycled &&
                min(direct.width, direct.height) >= MIN_HD_ARTWORK_DIMENSION_PX
            ) {
                return direct
            }
        }

        // Check prefix matches, strictly filtering out small list/palette thumbnails (< 480px)
        var bestMatch: Bitmap? = null
        var bestPixels = 0
        val allKeys = runCatching { memCache.keys.toList() }.getOrDefault(emptyList())
        for (cacheKey in allKeys) {
            val k = cacheKey.key
            if (candidateKeys.any { base -> k == base || k.startsWith("${base}_") }) {
                val bmp = memCache[cacheKey]?.bitmap
                if (bmp != null && !bmp.isRecycled &&
                    min(bmp.width, bmp.height) >= MIN_HD_ARTWORK_DIMENSION_PX
                ) {
                    val pxCount = bmp.width * bmp.height
                    if (pxCount > bestPixels) {
                        bestPixels = pxCount
                        bestMatch = bmp
                    }
                }
            }
        }
        return bestMatch
    }

    private fun findInCoilDiskCache(
        context: Context,
        rawUri: String,
        qualities: List<AlbumArtQuality>
    ): Bitmap? {
        val diskCache = context.imageLoader.diskCache ?: return null
        val candidateKeys = LinkedHashSet<String>()
        for (q in qualities) {
            val opt = ThumbnailUrlUtils.optimizeArtworkUrl(rawUri, q) ?: rawUri
            val canonical = ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(opt, q) ?: opt
            candidateKeys.add(canonical)
            candidateKeys.add(opt)
        }
        candidateKeys.add(rawUri)
        for (fallbackUrl in ThumbnailUrlUtils.getFallbackArtworkUrls(rawUri)) {
            candidateKeys.add(fallbackUrl)
            ThumbnailUrlUtils.getCanonicalArtworkDiskCacheKey(fallbackUrl)?.let(candidateKeys::add)
        }

        for (key in candidateKeys) {
            val bmp = runCatching {
                diskCache.openSnapshot(key)?.use { snapshot ->
                    decodeSampledFile(snapshot.data.toFile(), MAX_ARTWORK_DIMENSION_PX)
                }
            }.getOrNull()
            if (bmp != null && !bmp.isRecycled) return bmp
        }
        return null
    }

    private fun decodeLocalArtworkUri(context: Context, uriString: String): Bitmap? {
        return runCatching {
            val uri = if (uriString.startsWith("/")) {
                Uri.fromFile(File(uriString))
            } else {
                Uri.parse(uriString)
            }
            val bytes = AlbumArtUtils.openArtworkInputStream(context, uri)?.use { it.readBytes() }
                ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            decodeSampledByteArray(bytes, MAX_ARTWORK_DIMENSION_PX)
        }.getOrNull()
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        }
        return bitmap
    }

    /**
     * Copies and optimizes the user-selected gallery fallback wallpaper into internal app storage
     * so it persists across reboots and loads instantaneously.
     */
    suspend fun saveFallbackWallpaperFromUri(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val rawBytes = appContext.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: return@runCatching null
                if (rawBytes.isEmpty()) return@runCatching null

                val rotationDeg = runCatching {
                    val exif = ExifInterface( rawBytes.inputStream() )
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }.getOrDefault(0f)

                val decoded = decodeSampledByteArray(rawBytes, MAX_FALLBACK_DIMENSION_PX)
                    ?: return@runCatching null

                val oriented = if (rotationDeg != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationDeg) }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                } else {
                    decoded
                }

                val destFile = File(appContext.filesDir, FALLBACK_FILE_NAME)
                FileOutputStream(destFile).use { out ->
                    oriented.compress(Bitmap.CompressFormat.JPEG, 94, out)
                    out.flush()
                }

                cachedFallbackPath = destFile.absolutePath
                cachedFallbackUpdatedAt = System.currentTimeMillis()
                cachedFallbackBitmap = oriented
                // Step 1 of Pseudo-Live Static Wallpaper: when the user configures their fallback
                // wallpaper and no music is playing, immediately set it as the static system wallpaper
                // so it persists in /data/system/users/0/wallpaper across manual Force Stop.
                if (!_playbackState.value.isPlaying) {
                    pushPseudoLiveWallpaperToSystem(appContext, force = true, forceFallbackIdle = true)
                }
                destFile.absolutePath
            }.getOrNull()
        }

    suspend fun clearFallbackWallpaper(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.applicationContext.filesDir, FALLBACK_FILE_NAME)
            if (file.exists()) file.delete()
        }
        cachedFallbackPath = ""
        cachedFallbackUpdatedAt = 0L
        cachedFallbackBitmap = null
    }

    suspend fun loadFallbackWallpaperBitmap(
        context: Context,
        path: String,
        updatedAt: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val defaultInternalFile = File(appContext.filesDir, FALLBACK_FILE_NAME)
        val effectivePath = path.ifBlank {
            if (defaultInternalFile.exists() && defaultInternalFile.length() > 0L) {
                defaultInternalFile.absolutePath
            } else {
                return@withContext null
            }
        }
        val effectiveUpdatedAt = if (updatedAt > 0L) updatedAt else defaultInternalFile.lastModified()

        val existing = cachedFallbackBitmap
        if (existing != null && !existing.isRecycled &&
            cachedFallbackPath == effectivePath && cachedFallbackUpdatedAt == effectiveUpdatedAt
        ) {
            return@withContext existing
        }

        val loaded = runCatching {
            val file = File(effectivePath)
            if (file.exists() && file.length() > 0L) {
                decodeSampledFile(file, MAX_FALLBACK_DIMENSION_PX)
            } else if (defaultInternalFile.exists() && defaultInternalFile.length() > 0L) {
                decodeSampledFile(defaultInternalFile, MAX_FALLBACK_DIMENSION_PX)
            } else {
                val uri = Uri.parse(effectivePath)
                appContext.contentResolver.openInputStream(uri)?.use { stream ->
                    decodeSampledByteArray(stream.readBytes(), MAX_FALLBACK_DIMENSION_PX)
                }
            }
        }.getOrNull()

        if (loaded != null) {
            cachedFallbackPath = effectivePath
            cachedFallbackUpdatedAt = effectiveUpdatedAt
            cachedFallbackBitmap = loaded
        }
        loaded
    }

    /**
     * Fast, artifact-free 3-pass separable box/Gaussian approximation blur on a downscaled buffer.
     * Pre-computed once per artwork/blur-level change so 60/120fps Canvas rendering simply draws
     * a hardware-filtered Bitmap with zero frame drops.
     */
    fun getOrCreateBlurredBitmap(
        source: Bitmap,
        sourceKey: String,
        blurAmount: Float
    ): Bitmap {
        val quantizedBlur = (blurAmount.coerceIn(0f, 1f) * 50f).roundToInt()
        if (quantizedBlur <= 0) return source
        val cacheKey = "${sourceKey}_blur_$quantizedBlur"
        synchronized(blurredBitmapCache) {
            blurredBitmapCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }
        }

        val blurred = createBlurredBitmapInternal(source, blurAmount.coerceIn(0f, 1f))
        synchronized(blurredBitmapCache) {
            blurredBitmapCache.put(cacheKey, blurred)
        }
        return blurred
    }

    private fun createBlurredBitmapInternal(source: Bitmap, blurAmount: Float): Bitmap {
        val softSource = ensureSoftwareBitmap(source)
        val maxSmallSide = 200
        val srcW = softSource.width.coerceAtLeast(1)
        val srcH = softSource.height.coerceAtLeast(1)
        val scale = min(maxSmallSide.toFloat() / max(srcW, srcH).toFloat(), 1f)
        val w = (srcW * scale).roundToInt().coerceAtLeast(16)
        val h = (srcH * scale).roundToInt().coerceAtLeast(16)

        val small = Bitmap.createScaledBitmap(softSource, w, h, true)
        val radius = (blurAmount * 34f).roundToInt().coerceIn(1, 38)

        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)

        // 3-pass separable box blur closely approximates a true Gaussian kernel
        repeat(3) {
            boxBlurHorizontal(pixels, temp, w, h, radius)
            boxBlurVertical(temp, pixels, w, h, radius)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val div = radius * 2 + 1
        for (y in 0 until h) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            val rowOffset = y * w
            for (i in -radius..radius) {
                val px = src[rowOffset + i.coerceIn(0, w - 1)]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }
            for (x in 0 until w) {
                val r = rSum / div
                val g = gSum / div
                val b = bSum / div
                dst[rowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

                val leftIdx = (x - radius).coerceIn(0, w - 1)
                val rightIdx = (x + radius + 1).coerceIn(0, w - 1)
                val pOut = src[rowOffset + leftIdx]
                val pIn = src[rowOffset + rightIdx]
                rSum += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gSum += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bSum += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val div = radius * 2 + 1
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            for (i in -radius..radius) {
                val py = i.coerceIn(0, h - 1)
                val px = src[py * w + x]
                rSum += (px shr 16) and 0xFF
                gSum += (px shr 8) and 0xFF
                bSum += px and 0xFF
            }
            for (y in 0 until h) {
                val r = rSum / div
                val g = gSum / div
                val b = bSum / div
                dst[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

                val topIdx = (y - radius).coerceIn(0, h - 1)
                val bottomIdx = (y + radius + 1).coerceIn(0, h - 1)
                val pOut = src[topIdx * w + x]
                val pIn = src[bottomIdx * w + x]
                rSum += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gSum += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bSum += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }

    private fun decodeSampledFile(file: File, maxDimensionPx: Int): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimensionPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }

    private fun decodeSampledByteArray(bytes: ByteArray, maxDimensionPx: Int): Bitmap? {
        if (bytes.isEmpty()) return null
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimensionPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val longest = max(width, height)
        while (longest / inSampleSize > maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
