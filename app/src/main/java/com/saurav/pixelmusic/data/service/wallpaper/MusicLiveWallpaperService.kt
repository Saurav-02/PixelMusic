package com.saurav.pixelmusic.data.service.wallpaper

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.text.TextPaint
import android.text.TextUtils
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.compose.ui.util.lerp
import androidx.core.content.res.ResourcesCompat
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.preferences.MusicLiveWallpaperSettings
import com.saurav.pixelmusic.data.preferences.MusicWallpaperShapePosition
import com.saurav.pixelmusic.data.preferences.MusicWallpaperStyleMode
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.presentation.viewmodel.PlaybackStateHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MusicLiveWallpaperServiceEntryPoint {
    fun userPreferencesRepository(): UserPreferencesRepository
    fun playbackStateHolder(): PlaybackStateHolder
}

/**
 * Immutable snapshot of a fully prepared wallpaper visual state so outgoing and incoming scenes
 * never mutate mid-transition.
 */
private data class PreparedWallpaperScene(
    val sceneKey: String = "",
    val sharpBitmap: Bitmap? = null,
    val blurredBitmap: Bitmap? = null,
    val isFallbackIdle: Boolean = true,
    val songTitle: String = "",
    val artistName: String = ""
)

/**
 * Android Live Wallpaper service (`WallpaperService`) for PixelMusic.
 *
 * Key smoothness & battery optimizations:
 * - **5.5-Second Settle Debounce**: Rapid manual track skips or repeated Play/Pause taps are
 *   coalesced for 5.5 seconds. Intermediate tracks produce zero reaction; once the user settles,
 *   the wallpaper crossfades directly from the currently displayed wallpaper to the newest target.
 * - **True Dual-Scene Crossfade**: The outgoing scene stays untouched and fades out while the
 *   new scene fades in over `crossfadeDurationMs` (default 4.0s). `activeScene` is never mutated
 *   before the new 720p bitmap + blur are 100% ready in RAM.
 * - **Visibility-Aware Start**: If a transition is prepared while the user is inside the app or
 *   the screen is off, the crossfade clock starts the exact millisecond the wallpaper becomes visible.
 * - **No Home-Screen Touch Listener**: `setTouchEventsEnabled(false)` avoids intercepting launcher
 *   touches and saves battery.
 */
class MusicLiveWallpaperService : WallpaperService() {

    companion object {
        private const val PLAYBACK_SETTLE_DEBOUNCE_MS = 3500L
    }

    override fun onCreateEngine(): Engine {
        return MusicWallpaperEngine()
    }

    private inner class MusicWallpaperEngine : Engine(), Choreographer.FrameCallback {

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val choreographer = Choreographer.getInstance()

        private lateinit var userPreferencesRepository: UserPreferencesRepository
        private lateinit var playbackStateHolder: PlaybackStateHolder

        private var isEngineVisible = false
        private var isFrameCallbackPosted = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // Current settings & latest raw playback state
        private var settings = MusicLiveWallpaperSettings()
        private var latestRawPlayback = WallpaperPlaybackSnapshot()

        // Prepared visual scenes (never mutated until new bitmaps are 100% ready!)
        private var activeScene = PreparedWallpaperScene()
        private var fallbackBitmap: Bitmap? = null
        private var settleAndLoadJob: Job? = null
        private var settingsRefreshJob: Job? = null

        // Offscreen buffers for true outgoing fade-out + incoming fade-in
        private var outgoingFrameSnapshot: Bitmap? = null
        private var currentFrameBuffer: Bitmap? = null
        private var crossfadeStartMs = 0L
        private var crossfadeDurationMs = 4000L
        private var crossfadeProgress = 1f
        private var pendingCrossfadeClockStartOnVisible = false
        private var pendingShapeMorphOnVisible = false

        // Smooth Material You shape morph + rotation state
        private var fromShapeKind = WallpaperExpressiveShapeKind.COOKIE_8
        private var toShapeKind = WallpaperExpressiveShapeKind.COOKIE_8
        private var currentShapeIndex = 0
        private var shapeMorphStartMs = 0L
        private var shapeMorphDurationMs = 1100L
        private var shapeMorphProgress = 1f

        private var spinStartDeg = 0f
        private var spinTargetDeg = 0f
        private var currentSpinDeg = 0f
        private var continuousSpinDeg = 0f
        private var lastFrameRealtimeMs = 0L

        // Smooth vertical position & mode morph state
        private var posStartY = MusicWallpaperShapePosition.MIDDLE.verticalCenterFraction()
        private var posTargetY = MusicWallpaperShapePosition.MIDDLE.verticalCenterFraction()
        private var currentPosY = MusicWallpaperShapePosition.MIDDLE.verticalCenterFraction()
        private var posAnimStartMs = 0L
        private var posAnimProgress = 1f

        private var modeStartT = 1f
        private var modeTargetT = 1f
        private var currentModeT = 1f
        private var modeAnimStartMs = 0L
        private var modeAnimProgress = 1f

        // Pre-allocated drawing objects (zero per-frame allocations)
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(46, 255, 255, 255)
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(38, 255, 255, 255)
        }
        private val topScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(12f, 0f, 3f, Color.argb(140, 0, 0, 0))
        }
        private val artistTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            setShadowLayer(10f, 0f, 2f, Color.argb(130, 0, 0, 0))
        }

        private val shapePath = Path()
        private val haloPath = Path()
        private val drawMatrix = Matrix()
        private val tempRect = RectF()

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Keep home screen touch morphing disabled to conserve battery
            setTouchEventsEnabled(false)

            val appContext = applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                MusicLiveWallpaperServiceEntryPoint::class.java
            )
            userPreferencesRepository = entryPoint.userPreferencesRepository()
            playbackStateHolder = entryPoint.playbackStateHolder()

            val roundedFont = runCatching {
                ResourcesCompat.getFont(appContext, R.font.gflex_variable)
            }.getOrNull()
            titleTextPaint.typeface = roundedFont ?: Typeface.create("sans-serif", Typeface.BOLD)
            titleTextPaint.isFakeBoldText = false
            runCatching {
                titleTextPaint.fontVariationSettings = "'wght' 700, 'ROND' 100"
            }
            artistTextPaint.typeface = roundedFont ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
            artistTextPaint.isFakeBoldText = false
            runCatching {
                artistTextPaint.fontVariationSettings = "'wght' 500, 'ROND' 100"
            }

            // 1. Observe customization settings changes (apply immediately for live customization feedback)
            engineScope.launch {
                var isFirstSettings = true
                userPreferencesRepository.musicLiveWallpaperSettingsFlow.collectLatest { newSettings ->
                    val oldSettings = settings
                    settings = newSettings

                    if (isFirstSettings) {
                        isFirstSettings = false
                        currentShapeIndex = newSettings.shapeIndex
                        val initialKind = WallpaperExpressiveShapeKind.fromIndex(newSettings.shapeIndex)
                        fromShapeKind = initialKind
                        toShapeKind = initialKind
                        shapeMorphProgress = 1f
                        currentPosY = newSettings.shapePosition.verticalCenterFraction()
                        posStartY = currentPosY
                        posTargetY = currentPosY
                        currentModeT = if (newSettings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE) 1f else 0f
                        modeStartT = currentModeT
                        modeTargetT = currentModeT
                        prepareAndTransitionToTarget(immediate = true)
                    } else {
                        val nextKind = WallpaperExpressiveShapeKind.fromIndex(newSettings.shapeIndex)
                        if (nextKind != toShapeKind) {
                            currentShapeIndex = newSettings.shapeIndex
                            startShapeMorphTo(nextKind)
                        }
                        val nextPosY = newSettings.shapePosition.verticalCenterFraction()
                        if (kotlin.math.abs(nextPosY - posTargetY) > 0.001f) {
                            posStartY = currentPosY
                            posTargetY = nextPosY
                            posAnimStartMs = SystemClock.uptimeMillis()
                            posAnimProgress = 0f
                        }
                        val nextModeT = if (newSettings.styleMode == MusicWallpaperStyleMode.MATERIAL_YOU_SHAPE) 1f else 0f
                        if (kotlin.math.abs(nextModeT - modeTargetT) > 0.001f) {
                            modeStartT = currentModeT
                            modeTargetT = nextModeT
                            modeAnimStartMs = SystemClock.uptimeMillis()
                            modeAnimProgress = 0f
                        }

                        if (oldSettings.fallbackWallpaperPath != newSettings.fallbackWallpaperPath ||
                            oldSettings.fallbackWallpaperUpdatedAt != newSettings.fallbackWallpaperUpdatedAt ||
                            kotlin.math.abs(oldSettings.blurAmount - newSettings.blurAmount) > 0.005f
                        ) {
                            refreshExistingSceneBlurAndFallback(
                                animateCrossfade = oldSettings.fallbackWallpaperPath != newSettings.fallbackWallpaperPath &&
                                    activeScene.isFallbackIdle
                            )
                        } else {
                            scheduleNextFrame(immediate = true)
                        }
                    }
                }
            }

            // 2. Sync PlaybackStateHolder into MusicLiveWallpaperStateHolder
            engineScope.launch {
                playbackStateHolder.stablePlayerState.collectLatest { stable ->
                    val song = stable.currentSong
                    if (song != null) {
                        MusicLiveWallpaperStateHolder.updatePlaybackQuickState(
                            songTitle = song.title,
                            artistName = song.displayArtist.takeIf { it.isNotBlank() } ?: song.artist,
                            albumArtUriString = song.albumArtUriString,
                            isPlaying = stable.isPlaying
                        )
                    } else if (!stable.isPlaying && latestRawPlayback.isPlaying) {
                        MusicLiveWallpaperStateHolder.updatePlaybackQuickState(
                            songTitle = latestRawPlayback.songTitle,
                            artistName = latestRawPlayback.artistName,
                            albumArtUriString = latestRawPlayback.albumArtUriString,
                            isPlaying = false
                        )
                    }
                }
            }

            // 3. Observe playback changes with 5.2-second settle debounce so repeated manual skips
            //    or rapid play/pause taps keep the current wallpaper untouched and then smoothly
            //    crossfade once directly from the old wallpaper to the final settled state!
            engineScope.launch {
                var isInitialEmission = true
                MusicLiveWallpaperStateHolder.playbackState.collectLatest { snapshot ->
                    latestRawPlayback = snapshot
                    if (isInitialEmission) {
                        isInitialEmission = false
                        prepareAndTransitionToTarget(immediate = true)
                    } else {
                        scheduleSettledPlaybackTransition()
                    }
                }
            }
        }

        private fun scheduleSettledPlaybackTransition() {
            settleAndLoadJob?.cancel()
            settleAndLoadJob = engineScope.launch {
                // Wait ~5.2 seconds for the user to finish skipping songs or toggling play/pause
                delay(PLAYBACK_SETTLE_DEBOUNCE_MS)
                prepareAndTransitionToTarget(immediate = false)
            }
        }

        // Material You colors come ONLY from the gallery fallback image, never from album art,
        // so the system theme stays fixed across track changes.
        private var fallbackColors: WallpaperColors? = null
        private var fallbackColorsKey: String? = null

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? = fallbackColors

        private suspend fun updateColorsFromFallback(fb: Bitmap?) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
            if (fb == null || fb.isRecycled) return
            val key = "${settings.fallbackWallpaperPath}|${settings.fallbackWallpaperUpdatedAt}"
            if (key == fallbackColorsKey) return
            val colors = withContext(Dispatchers.Default) { WallpaperColors.fromBitmap(fb) }
            fallbackColors = colors
            fallbackColorsKey = key
            notifyColorsChanged()
        }

        private fun refreshExistingSceneBlurAndFallback(animateCrossfade: Boolean) {
            settingsRefreshJob?.cancel()
            settingsRefreshJob = engineScope.launch {
                val appContext = applicationContext
                val s = settings
                val fb = MusicLiveWallpaperStateHolder.loadFallbackWallpaperBitmap(
                    appContext,
                    s.fallbackWallpaperPath,
                    s.fallbackWallpaperUpdatedAt
                )
                fallbackBitmap = fb
                updateColorsFromFallback(fb)

                val curSharp = if (activeScene.isFallbackIdle) fb else (activeScene.sharpBitmap ?: fb)
                val curBlurred = if (curSharp != null && !curSharp.isRecycled) {
                    withContext(Dispatchers.Default) {
                        MusicLiveWallpaperStateHolder.getOrCreateBlurredBitmap(
                            source = curSharp,
                            sourceKey = activeScene.sceneKey.ifEmpty { "scene" },
                            blurAmount = s.blurAmount
                        )
                    }
                } else {
                    null
                }

                if (animateCrossfade && surfaceWidth > 0 && surfaceHeight > 0) {
                    captureCurrentVisibleFrameAsOutgoing()
                    activeScene = activeScene.copy(
                        sharpBitmap = curSharp,
                        blurredBitmap = curBlurred
                    )
                    beginCrossfadeAnimation()
                } else {
                    activeScene = activeScene.copy(
                        sharpBitmap = curSharp,
                        blurredBitmap = curBlurred
                    )
                    scheduleNextFrame(immediate = true)
                }
            }
        }

        private suspend fun prepareAndTransitionToTarget(immediate: Boolean) {
            val appContext = applicationContext
            val s = settings
            val p = latestRawPlayback

            val fb = MusicLiveWallpaperStateHolder.loadFallbackWallpaperBitmap(
                appContext,
                s.fallbackWallpaperPath,
                s.fallbackWallpaperUpdatedAt
            )
            fallbackBitmap = fb
            updateColorsFromFallback(fb)

            val shouldShowMusicArt = p.isPlaying && (!p.albumArtUriString.isNullOrBlank() || p.albumArtBytes != null)
            val targetKey = if (shouldShowMusicArt) {
                "music_${p.albumArtUriString ?: "embedded"}_${p.songTitle}"
            } else {
                "fallback_${s.fallbackWallpaperPath}_${s.fallbackWallpaperUpdatedAt}"
            }

            // If after settling for 5+ seconds the target is the exact same scene already on screen,
            // do nothing at all (zero flicker, zero redundant crossfade)!
            if (!immediate && targetKey == activeScene.sceneKey && activeScene.sharpBitmap != null) {
                return
            }

            val loadedSharp: Bitmap? = if (shouldShowMusicArt) {
                MusicLiveWallpaperStateHolder.loadCachedAlbumArtBitmap(
                    appContext,
                    p.albumArtUriString,
                    p.albumArtBytes
                ) ?: fb
            } else {
                fb
            }

            val loadedBlurred: Bitmap? = if (loadedSharp != null && !loadedSharp.isRecycled) {
                withContext(Dispatchers.Default) {
                    MusicLiveWallpaperStateHolder.getOrCreateBlurredBitmap(
                        source = loadedSharp,
                        sourceKey = targetKey,
                        blurAmount = s.blurAmount
                    )
                }
            } else {
                null
            }

            val newScene = PreparedWallpaperScene(
                sceneKey = targetKey,
                sharpBitmap = loadedSharp,
                blurredBitmap = loadedBlurred,
                isFallbackIdle = !shouldShowMusicArt && fb != null,
                songTitle = if (shouldShowMusicArt) p.songTitle.trim() else "",
                artistName = if (shouldShowMusicArt) p.artistName.trim() else ""
            )

            val hadPreviousScene = activeScene.sceneKey.isNotEmpty()
            val isTrackSwitchWhilePlaying = shouldShowMusicArt &&
                !activeScene.isFallbackIdle &&
                activeScene.sceneKey != targetKey

            if (!immediate && hadPreviousScene && surfaceWidth > 0 && surfaceHeight > 0) {
                // Render or capture the true outgoing scene BEFORE mutating activeScene!
                captureCurrentVisibleFrameAsOutgoing()
                activeScene = newScene

                if (isTrackSwitchWhilePlaying) {
                    val nextIdx = WallpaperExpressiveShapeKind.nextIndex(currentShapeIndex)
                    currentShapeIndex = nextIdx
                    val nextKind = WallpaperExpressiveShapeKind.fromIndex(nextIdx)
                    if (isEngineVisible) {
                        startShapeMorphTo(nextKind)
                    } else {
                        fromShapeKind = toShapeKind
                        toShapeKind = nextKind
                        pendingShapeMorphOnVisible = true
                    }
                }

                beginCrossfadeAnimation()
            } else {
                activeScene = newScene
                crossfadeProgress = 1f
                pendingCrossfadeClockStartOnVisible = false
                scheduleNextFrame(immediate = true)
            }
        }

        /**
         * Captures the exact currently visible wallpaper composition into [outgoingFrameSnapshot]
         * BEFORE [activeScene] is updated to the new scene, so the old wallpaper is 100% preserved
         * and fades out smoothly as the new wallpaper fades in.
         */
        private fun captureCurrentVisibleFrameAsOutgoing() {
            val w = surfaceWidth.coerceAtLeast(1)
            val h = surfaceHeight.coerceAtLeast(1)
            val snap = ensureSnapshotBitmap(outgoingFrameSnapshot, w, h)
            val snapCanvas = Canvas(snap)
            val curBuf = currentFrameBuffer
            if (curBuf != null && !curBuf.isRecycled && curBuf.width == w && curBuf.height == h) {
                snapCanvas.drawBitmap(curBuf, 0f, 0f, null)
            } else {
                renderSceneToCanvas(snapCanvas, activeScene, w.toFloat(), h.toFloat())
            }
            outgoingFrameSnapshot = snap
        }

        private fun beginCrossfadeAnimation() {
            crossfadeDurationMs = settings.crossfadeDurationMs.toLong().coerceIn(2000L, 8000L)
            crossfadeProgress = 0f
            if (isEngineVisible) {
                crossfadeStartMs = SystemClock.uptimeMillis()
                pendingCrossfadeClockStartOnVisible = false
                scheduleNextFrame(immediate = true)
            } else {
                // Wait until the user actually sees the home/lock screen to start the crossfade clock!
                pendingCrossfadeClockStartOnVisible = true
            }
        }

        private fun startShapeMorphTo(targetKind: WallpaperExpressiveShapeKind) {
            fromShapeKind = if (shapeMorphProgress < 0.5f) fromShapeKind else toShapeKind
            toShapeKind = targetKind
            shapeMorphStartMs = SystemClock.uptimeMillis()
            shapeMorphDurationMs = 1100L
            shapeMorphProgress = 0f
            spinStartDeg = currentSpinDeg
            spinTargetDeg = currentSpinDeg + 95f
            scheduleNextFrame(immediate = true)
        }

        private fun ensureSnapshotBitmap(existing: Bitmap?, w: Int, h: Int): Bitmap {
            if (existing != null && !existing.isRecycled && existing.width == w && existing.height == h) {
                return existing
            }
            return Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isEngineVisible = visible
            if (visible) {
                val now = SystemClock.uptimeMillis()
                lastFrameRealtimeMs = now
                if (pendingCrossfadeClockStartOnVisible) {
                    pendingCrossfadeClockStartOnVisible = false
                    crossfadeStartMs = now
                    crossfadeProgress = 0f
                }
                if (pendingShapeMorphOnVisible) {
                    pendingShapeMorphOnVisible = false
                    shapeMorphStartMs = now
                    shapeMorphDurationMs = 1100L
                    shapeMorphProgress = 0f
                    spinStartDeg = currentSpinDeg
                    spinTargetDeg = currentSpinDeg + 95f
                }
                scheduleNextFrame(immediate = true)
            } else {
                stopFrameCallback()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            val density = applicationContext.resources.displayMetrics.density
            titleTextPaint.textSize = 20f * density
            artistTextPaint.textSize = 14f * density
            borderPaint.strokeWidth = 1.5f * density
            topScrimPaint.shader = LinearGradient(
                0f,
                0f,
                0f,
                height * 0.24f,
                intArrayOf(
                    Color.argb(145, 0, 0, 0),
                    Color.argb(65, 0, 0, 0),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            scheduleNextFrame(immediate = true)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isEngineVisible = false
            stopFrameCallback()
        }

        override fun onDestroy() {
            stopFrameCallback()
            engineScope.cancel()
            super.onDestroy()
        }

        private fun scheduleNextFrame(immediate: Boolean = true) {
            if (!isEngineVisible) return
            if (!isFrameCallbackPosted) {
                isFrameCallbackPosted = true
                if (immediate) {
                    choreographer.postFrameCallback(this)
                } else {
                    choreographer.postFrameCallbackDelayed(this, 32L)
                }
            }
        }

        private fun stopFrameCallback() {
            if (isFrameCallbackPosted) {
                choreographer.removeFrameCallback(this)
                isFrameCallbackPosted = false
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            isFrameCallbackPosted = false
            if (!isEngineVisible || surfaceWidth <= 0 || surfaceHeight <= 0) return

            val nowMs = SystemClock.uptimeMillis()
            val dtMs = if (lastFrameRealtimeMs > 0L) (nowMs - lastFrameRealtimeMs).coerceIn(0L, 64L) else 16L
            lastFrameRealtimeMs = nowMs

            var highRateTransitionActive = false
            var ambientSpinActive = false

            // 1. Update scene crossfade progress
            if (crossfadeProgress < 1f) {
                val elapsed = (nowMs - crossfadeStartMs).coerceAtLeast(0L)
                val rawT = (elapsed.toFloat() / crossfadeDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                crossfadeProgress = evaluateSmoothWallpaperEasing(rawT)
                if (rawT < 1f) highRateTransitionActive = true else crossfadeProgress = 1f
            }

            // 2. Update Material You shape morph + spin
            if (shapeMorphProgress < 1f) {
                val elapsed = (nowMs - shapeMorphStartMs).coerceAtLeast(0L)
                val rawT = (elapsed.toFloat() / shapeMorphDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                val eased = evaluateSmoothWallpaperEasing(rawT)
                shapeMorphProgress = eased
                currentSpinDeg = lerp(spinStartDeg, spinTargetDeg, eased)
                if (rawT < 1f) highRateTransitionActive = true else shapeMorphProgress = 1f
            }

            // 3. Update slow ambient spin while playing in Material You Shape mode
            val shouldAmbientSpin = !activeScene.isFallbackIdle &&
                latestRawPlayback.isPlaying &&
                currentModeT > 0.02f
            if (shouldAmbientSpin) {
                continuousSpinDeg = (continuousSpinDeg + (dtMs * 0.009f)) % 360f
                ambientSpinActive = true
            }

            // 4. Update vertical position animation
            if (posAnimProgress < 1f) {
                val elapsed = (nowMs - posAnimStartMs).coerceAtLeast(0L)
                val rawT = (elapsed.toFloat() / 680f).coerceIn(0f, 1f)
                val eased = evaluateSmoothWallpaperEasing(rawT)
                posAnimProgress = eased
                currentPosY = lerp(posStartY, posTargetY, eased)
                if (rawT < 1f) highRateTransitionActive = true else posAnimProgress = 1f
            }

            // 5. Update style mode morph (Full Screen <-> Material You Shape)
            if (modeAnimProgress < 1f) {
                val elapsed = (nowMs - modeAnimStartMs).coerceAtLeast(0L)
                val rawT = (elapsed.toFloat() / 720f).coerceIn(0f, 1f)
                val eased = evaluateSmoothWallpaperEasing(rawT)
                modeAnimProgress = eased
                currentModeT = lerp(modeStartT, modeTargetT, eased)
                if (rawT < 1f) highRateTransitionActive = true else modeAnimProgress = 1f
            }

            drawWallpaperFrame()

            if (highRateTransitionActive) {
                scheduleNextFrame(immediate = true)
            } else if (ambientSpinActive) {
                // 30fps cadence for slow ambient spin when no morph/crossfade is running saves battery
                scheduleNextFrame(immediate = false)
            }
        }

        private fun drawWallpaperFrame() {
            val holder = surfaceHolder ?: return
            val w = surfaceWidth
            val h = surfaceHeight
            if (w <= 0 || h <= 0) return

            // 1. Render incoming scene into an offscreen bitmap
            val incomingBuffer = ensureSnapshotBitmap(currentFrameBuffer, w, h)
            currentFrameBuffer = incomingBuffer
            val incomingCanvas = Canvas(incomingBuffer)
            renderSceneToCanvas(incomingCanvas, activeScene, w.toFloat(), h.toFloat())

            // 2. If crossfading, blend outgoingFrameSnapshot + incomingBuffer directly inside currentFrameBuffer
            //    so currentFrameBuffer ALWAYS holds the exact blended pixels on screen, and we post a single
            //    opaque bitmap to SurfaceHolder (avoiding OEM HardwareCanvas alpha blending bugs!).
            val outgoing = outgoingFrameSnapshot
            if (crossfadeProgress < 0.999f && outgoing != null && !outgoing.isRecycled) {
                val t = crossfadeProgress.coerceIn(0f, 1f)
                // Draw outgoing onto incomingCanvas at (1 - t) or blend outgoing -> incoming
                // To blend in-place without an extra buffer: draw incoming into outgoing or draw on lockCanvas:
                var lockCanvas: Canvas? = null
                try {
                    // Use standard lockCanvas() during crossfades for 100% consistent alpha blending across all OEMs
                    lockCanvas = holder.lockCanvas()
                    if (lockCanvas != null) {
                        lockCanvas.drawColor(Color.BLACK)
                        bitmapPaint.alpha = ((1f - t * 0.85f) * 255f).roundToInt().coerceIn(0, 255)
                        lockCanvas.drawBitmap(outgoing, 0f, 0f, bitmapPaint)
                        bitmapPaint.alpha = (t * 255f).roundToInt().coerceIn(0, 255)
                        lockCanvas.drawBitmap(incomingBuffer, 0f, 0f, bitmapPaint)
                        bitmapPaint.alpha = 255
                    }
                } catch (_: Exception) {
                } finally {
                    if (lockCanvas != null) {
                        runCatching { holder.unlockCanvasAndPost(lockCanvas) }
                    }
                }

                // Also update incomingBuffer to hold the blended state in case another transition interrupts
                bitmapPaint.alpha = ((1f - t) * 255f).roundToInt().coerceIn(0, 255)
                incomingCanvas.drawBitmap(outgoing, 0f, 0f, bitmapPaint)
                bitmapPaint.alpha = 255
            } else {
                var lockCanvas: Canvas? = null
                try {
                    lockCanvas = holder.lockCanvas()
                    if (lockCanvas != null) {
                        bitmapPaint.alpha = 255
                        lockCanvas.drawBitmap(incomingBuffer, 0f, 0f, bitmapPaint)
                    }
                } catch (_: Exception) {
                } finally {
                    if (lockCanvas != null) {
                        runCatching { holder.unlockCanvasAndPost(lockCanvas) }
                    }
                }
            }
        }

        private fun renderSceneToCanvas(
            canvas: Canvas,
            scene: PreparedWallpaperScene,
            w: Float,
            h: Float
        ) {
            MusicLiveWallpaperStateHolder.renderWallpaperSceneToCanvas(
                context = applicationContext,
                canvas = canvas,
                w = w,
                h = h,
                settings = settings,
                sharpBmp = scene.sharpBitmap,
                blurredBmp = scene.blurredBitmap ?: scene.sharpBitmap,
                sourceKey = scene.sceneKey.ifEmpty { "scene" },
                isFallbackIdle = scene.isFallbackIdle,
                songTitle = scene.songTitle,
                artistName = scene.artistName,
                fromShape = fromShapeKind,
                toShape = toShapeKind,
                shapeMorphProgress = shapeMorphProgress,
                shapeRotationDeg = currentSpinDeg + continuousSpinDeg,
                positionCenterY = currentPosY,
                modeMorphT = if (scene.isFallbackIdle) 0f else currentModeT,
                drawSongInfoOverlay = true
            )
        }

        private fun drawSongInfoTextBlock(
            canvas: Canvas,
            title: String,
            artist: String,
            rightX: Float,
            y: Float,
            maxWidth: Float,
            alpha: Float,
            density: Float
        ) {
            if (alpha <= 0.01f) return
            titleTextPaint.alpha = (alpha * 245f).roundToInt().coerceIn(0, 255)
            artistTextPaint.alpha = (alpha * 205f).roundToInt().coerceIn(0, 255)

            val ellipsizedTitle = TextUtils.ellipsize(title, titleTextPaint, maxWidth, TextUtils.TruncateAt.END).toString()
            val titleWidth = titleTextPaint.measureText(ellipsizedTitle)
            val titleX = (rightX - titleWidth).coerceAtLeast(24f * density)
            val titleBaseline = y - titleTextPaint.ascent()
            canvas.drawText(ellipsizedTitle, titleX, titleBaseline, titleTextPaint)

            if (artist.isNotBlank()) {
                val ellipsizedArtist = TextUtils.ellipsize(artist, artistTextPaint, maxWidth, TextUtils.TruncateAt.END).toString()
                val artistWidth = artistTextPaint.measureText(ellipsizedArtist)
                val artistX = (rightX - artistWidth).coerceAtLeast(24f * density)
                val artistBaseline = titleBaseline + titleTextPaint.descent() + (4f * density) - artistTextPaint.ascent()
                canvas.drawText(ellipsizedArtist, artistX, artistBaseline, artistTextPaint)
            }
        }

        private fun drawCenterCropBitmap(canvas: Canvas, bitmap: Bitmap, dstW: Float, dstH: Float, alpha: Int) {
            tempRect.set(0f, 0f, dstW, dstH)
            drawCenterCropBitmapInRect(canvas, bitmap, tempRect, alpha)
        }

        private fun drawCenterCropBitmapInRect(canvas: Canvas, bitmap: Bitmap, dst: RectF, alpha: Int) {
            val bw = bitmap.width.toFloat().coerceAtLeast(1f)
            val bh = bitmap.height.toFloat().coerceAtLeast(1f)
            val dw = dst.width().coerceAtLeast(1f)
            val dh = dst.height().coerceAtLeast(1f)

            val scale = max(dw / bw, dh / bh)
            val dx = dst.left + (dw - bw * scale) * 0.5f
            val dy = dst.top + (dh - bh * scale) * 0.5f

            drawMatrix.reset()
            drawMatrix.postScale(scale, scale)
            drawMatrix.postTranslate(dx, dy)

            bitmapPaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawBitmap(bitmap, drawMatrix, bitmapPaint)
            bitmapPaint.alpha = 255
        }

        private fun drawFallbackGradient(canvas: Canvas, w: Float, h: Float) {
            val grad = LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    Color.rgb(24, 20, 38),
                    Color.rgb(16, 18, 28),
                    Color.rgb(10, 10, 14)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            dimPaint.shader = grad
            canvas.drawRect(0f, 0f, w, h, dimPaint)
            dimPaint.shader = null
        }
    }
}
