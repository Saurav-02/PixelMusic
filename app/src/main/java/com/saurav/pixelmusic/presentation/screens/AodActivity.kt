package com.saurav.pixelmusic.presentation.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.data.preferences.AodScreenStyle
import com.saurav.pixelmusic.data.preferences.ThemePreference
import com.saurav.pixelmusic.data.preferences.ThemePreferencesRepository
import com.saurav.pixelmusic.data.preferences.UserPreferencesRepository
import com.saurav.pixelmusic.data.service.AodScreenOffTrigger
import com.saurav.pixelmusic.presentation.components.LocalMaterialTheme
import com.saurav.pixelmusic.presentation.components.player.AodScreen
import com.saurav.pixelmusic.presentation.viewmodel.LyricsStateHolder
import com.saurav.pixelmusic.presentation.viewmodel.PlaybackStateHolder
import com.saurav.pixelmusic.ui.theme.PixelMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Lockscreen-style always-on display.
 *
 * Shown over the keyguard (without dismissing it) when the screen turns off while music is playing.
 * After [VISIBLE_MS] it fades to black at minimum brightness; a tap brings it back. The activity
 * finishes on the next ACTION_SCREEN_OFF, so waking the device with the power button or
 * double-tap lands on the normal system lockscreen.
 */
@AndroidEntryPoint
class AodActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    @Inject
    lateinit var lyricsStateHolder: LyricsStateHolder

    private var screenOffReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AodScreenOffTrigger.markAodEnded()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AodScreenOffTrigger.isAodShowing = true
        AodScreenOffTrigger.cancelLaunchNotification(this)

        // Show over the keyguard and wake the display, but do NOT request keyguard dismissal.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenOffReceiverRegistered = true
        playbackStateHolder.startProgressUpdates()

        setContent {
            val colorPalette by themePreferencesRepository.colorPalettePreferenceFlow
                .collectAsStateWithLifecycle(initialValue = "DYNAMIC")
            val playerThemePreference by themePreferencesRepository.playerThemePreferenceFlow
                .collectAsStateWithLifecycle(initialValue = ThemePreference.ALBUM_ART)
            val dynamicColorEnabled =
                (colorPalette == "DYNAMIC" || playerThemePreference == ThemePreference.DYNAMIC) &&
                    colorPalette != "BLACK_AND_WHITE"

            PixelMusicTheme(
                darkTheme = true,
                dynamicColor = dynamicColorEnabled,
                colorPalette = colorPalette,
                isAmoledBlack = true
            ) {
                CompositionLocalProvider(LocalMaterialTheme provides MaterialTheme.colorScheme) {
                    AodHost(onClose = { finish() })
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val pm = getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == true && !isFinishing) {
            AodScreenOffTrigger.markAodEnded()
            finish()
        }
    }

    override fun onDestroy() {
        if (screenOffReceiverRegistered) {
            runCatching { unregisterReceiver(screenOffReceiver) }
            screenOffReceiverRegistered = false
        }
        AodScreenOffTrigger.isAodShowing = false
        AodScreenOffTrigger.markAodEnded()
        super.onDestroy()
    }

    private companion object {
        const val VISIBLE_MS = 15_000L
        // If the system never turns the screen off after we went black, close ourselves so the
        // device isn't left on a black screen indefinitely.
        const val DIMMED_GRACE_MS = 30_000L
        const val AOD_BRIGHTNESS = 0.015f
    }

    @Composable
    private fun AodHost(onClose: () -> Unit) {
        val state by playbackStateHolder.stablePlayerState.collectAsStateWithLifecycle()
        val position by playbackStateHolder.currentPosition.collectAsStateWithLifecycle()
        val aodStyle by userPreferencesRepository.aodScreenStyleFlow
            .collectAsStateWithLifecycle(initialValue = AodScreenStyle.DEFAULT)
        val lyricsSyncOffset by lyricsStateHolder.currentSongSyncOffset.collectAsStateWithLifecycle()
        val latestState by rememberUpdatedState(state)
        val latestPosition by rememberUpdatedState(position)
        val latestOnClose by rememberUpdatedState(onClose)
        val view = LocalView.current
        val song = state.currentSong

        var dimmed by remember { mutableStateOf(false) }
        var interactionCount by remember { mutableIntStateOf(0) }

        // Nothing to show (e.g. the player is still connecting): give it a moment, then leave.
        LaunchedEffect(song == null) {
            if (song == null) {
                delay(3_000L)
                latestOnClose()
            }
        }

        // 15 s of visibility, restarted by every touch.
        LaunchedEffect(interactionCount) {
            delay(VISIBLE_MS)
            dimmed = true
        }

        LaunchedEffect(dimmed) {
            val lp = window.attributes
            if (dimmed) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                window.attributes = lp
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                view.keepScreenOn = false
                delay(DIMMED_GRACE_MS)
                latestOnClose()
            } else {
                lp.screenBrightness = AOD_BRIGHTNESS
                window.attributes = lp
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                view.keepScreenOn = true
            }
        }

        val overlayAlpha by animateFloatAsState(
            targetValue = if (dimmed) 1f else 0f,
            animationSpec = tween(durationMillis = 700),
            label = "aodDimOverlay"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                dimmed = false
                                interactionCount++
                            }
                        }
                    }
                }
        ) {
            if (song != null) {
                AodScreen(
                    songTitle = song.title,
                    artistName = song.displayArtist.takeIf { it.isNotBlank() } ?: song.artist,
                    albumArtUriString = song.albumArtUriString,
                    isPlayingProvider = { latestState.isPlaying },
                    currentPositionProvider = { latestPosition },
                    totalDurationProvider = { latestState.totalDuration },
                    onPlayPauseClick = { playbackStateHolder.playPause() },
                    onSkipNextClick = { playbackStateHolder.nextSong() },
                    onSkipPreviousClick = { playbackStateHolder.previousSong() },
                    onDismiss = onClose,
                    onSeekTo = { playbackStateHolder.seekTo(it) },
                    aodStyle = aodStyle,
                    lyrics = state.lyrics,
                    lyricsSyncOffsetMs = lyricsSyncOffset
                )
            }

            // Black cover once the AOD time is up. Swallows touches so a wake-up tap can't hit a button.
            if (dimmed || overlayAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = overlayAlpha }
                        .background(Color.Black)
                        .pointerInput(Unit) { detectTapGestures { } }
                )
            }
        }
    }
}
