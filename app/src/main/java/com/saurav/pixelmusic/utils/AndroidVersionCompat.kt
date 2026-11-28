package com.saurav.pixelmusic.utils

import android.media.MediaCodecList
import android.os.Build
import com.saurav.pixelmusic.data.service.player.HiFiCapabilityChecker

data class FeatureCompatibilityItem(
    val id: String,
    val name: String,
    val category: String,
    val requiredVersionLabel: String,
    val minApi: Int,
    val isSupported: Boolean,
    val description: String,
    val limitationNote: String
)

/**
 * Centralized Android API and platform feature compatibility checks.
 * Determines feature availability based on Android OS version (minSdk = 29)
 * and hardware capabilities, ensuring unsupported features are cleanly hidden.
 */
object AndroidVersionCompat {

    /**
     * Motion blur relies on Android 12+ (API 31, S) RenderEffect blur
     * in [com.saurav.pixelmusic.ui.modifiers.ScrollMotionBlurModifier].
     */
    val supportsMotionBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Hardware-accelerated RenderEffect blurs and Compose [androidx.compose.ui.draw.blur]
     * require Android 12+ (API 31, S). On Android 10/11 (API 29/30), RenderEffect is not
     * supported by the OS and Compose blur is a no-op.
     */
    val supportsHardwareBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Material You dynamic Monet system theming requires Android 12+ (API 31, S).
     */
    val supportsDynamicColor: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Music Live Wallpaper needs Android 12+ (API 31, S): its Material You colors come from
     * [android.app.WallpaperColors] reported by the live wallpaper engine, and its artwork blur
     * uses hardware RenderEffect. The feature, its settings entry and its live wallpaper service
     * are removed on Android 10/11.
     */
    val supportsMusicLiveWallpaper: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * AGSL shaders ([android.graphics.RuntimeShader]) require Android 13+ (API 33, TIRAMISU).
     */
    val supportsAgsl: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Android Spatializer API ([android.media.AudioManager.getSpatializer]) was introduced in
     * Android 12L (API 32, S_V2), and hardware E-AC3-JOC decoders expose Dolby Atmos bitstreams.
     */
    val supportsAndroidSpatializerApi: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

    /**
     * Hardware 32-bit float PCM AudioTrack output support.
     */
    val supportsHiFiPcmFloat: Boolean
        get() = HiFiCapabilityChecker.isSupported()

    /**
     * Predictive back gestures in Compose require Android 14+ (API 34, UPSIDE_DOWN_CAKE).
     */
    val supportsPredictiveBack: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /**
     * Post notifications runtime permission was introduced in Android 13 (API 33, TIRAMISU).
     */
    val supportsPostNotifications: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Exact alarm user scheduling check was introduced in Android 12 (API 31, S).
     */
    val supportsExactAlarmPermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Android 16+ Promoted Ongoing / Live Notifications (`setRequestPromotedOngoing`)
     * or OEM-supported dynamic status capsule (OriginOS, HyperOS, ColorOS/OxygenOS).
     */
    fun supportsLiveNotificationOrIsland(): Boolean {
        // Android 16+ (API 36) has native system Live Notifications (Promoted Ongoing)
        if (Build.VERSION.SDK_INT >= 36) return true

        // OEM Dynamic Island capsules (OriginOS, HyperOS, ColorOS/OxygenOS, Realme UI)
        // were only introduced in Android 14+ (API 34)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()
            val oemMatches = listOf("vivo", "iqoo", "xiaomi", "redmi", "oppo", "oneplus", "realme")
            return oemMatches.any { manufacturer.contains(it) || brand.contains(it) }
        }

        return false
    }

    /**
     * Checks if the device has a system equalizer / audio effects control panel
     * capable of handling [android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL].
     */
    fun supportsSystemEqualizer(context: android.content.Context): Boolean {
        val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        return try {
            intent.resolveActivity(context.packageManager) != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 30-second Video Sharing Engine requires Media3 Transformer video encoding support,
     * which reliably needs Android 11+ (API 30, R) and an available H.264 (AVC) video encoder.
     */
    fun supportsVideoSharing(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            codecList.codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals("video/avc", ignoreCase = true) }
            }
        } catch (_: Throwable) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }
    }

    /**
     * Returns the structured matrix of all platform/API-dependent features
     * with their required Android versions, current support state, and technical explanations.
     */
    fun getCompatibilityItems(): List<FeatureCompatibilityItem> {
        return listOf(
            FeatureCompatibilityItem(
                id = "progressive_header_blur",
                name = "Progressive Header & Glass Blur",
                category = "Graphics & UI",
                requiredVersionLabel = "Android 12+ (API 31) • AGSL Variable Blur on Android 13+",
                minApi = 31,
                isSupported = supportsHardwareBlur,
                description = "Real-time RenderNode glass backdrop blur across top headers, using multi-band AGSL variable-radius shaders on Android 13+.",
                limitationNote = "Requires Android 12+ RenderNode & RenderEffect blur. On Android 10/11, headers use a smooth multi-stop progressive gradient scrim."
            ),
            FeatureCompatibilityItem(
                id = "agsl_lyrics_shaders",
                name = "AGSL Lyrics Bloom & Wave Shaders",
                category = "Lyrics & Animation",
                requiredVersionLabel = "Android 13+ (API 33)",
                minApi = 33,
                isSupported = supportsAgsl,
                description = "GPU RuntimeShader bloom glow, held-note swell, and wave-lift effects on word-synced lyrics.",
                limitationNote = "Requires AGSL (Android Graphics Shading Language) RuntimeShader introduced in Android 13. On Android 10–12, lyrics use smooth shadow and alpha highlights."
            ),
            FeatureCompatibilityItem(
                id = "hardware_blur",
                name = "Live Lyrics Depth & Wallpaper Blur",
                category = "Graphics & UI",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsHardwareBlur,
                description = "Hardware-accelerated depth-of-field blur (2.5x default) on inactive lyrics, top-half Home/Explore wallpapers, and modal sheets.",
                limitationNote = "GPU RenderEffect blurring is only available in Android 12 and above. On Android 10/11, soft opacity dimming is used instead."
            ),
            FeatureCompatibilityItem(
                id = "motion_blur",
                name = "Scroll Motion Blur",
                category = "Graphics & Animation",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsMotionBlur,
                description = "Applies real-time directional velocity blur when scrolling song, album, and settings lists.",
                limitationNote = "Requires Android 12+ directional RenderEffect blur."
            ),
            FeatureCompatibilityItem(
                id = "hifi_float_audio",
                name = "Hi-Fi 32-Bit Float Audio & Audio FX DSP",
                category = "Audio & DSP",
                requiredVersionLabel = "PCM_FLOAT AudioTrack Hardware Support",
                minApi = 29,
                isSupported = supportsHiFiPcmFloat,
                description = "Outputs 32-bit IEEE float PCM from the 64-bit Audio FX DSP chain (Parametric EQ, AutoEQ, 3D Sims, 8D Audio, Reverb, Loudness, and Spatial Crossfeed).",
                limitationNote = "Your device's AudioTrack driver rejected ENCODING_PCM_FLOAT; Audio FX DSP still runs in full precision and dithers to 16-bit PCM output."
            ),
            FeatureCompatibilityItem(
                id = "dolby_atmos_spatializer",
                name = "Dolby Atmos & Android Spatializer",
                category = "Audio & DSP",
                requiredVersionLabel = "Android 12L+ (API 32) Spatializer • Built-in Head Model on All Versions",
                minApi = 32,
                isSupported = supportsAndroidSpatializerApi,
                description = "Renders 5.1, 7.1, and Atmos beds in 3D using PixelMusic's binaural head model and passes multichannel beds to the Android Spatializer when available.",
                limitationNote = "System Spatializer inspection requires Android 12L+ (API 32), though PixelMusic's built-in 3D Sims, 8D Audio, and binaural surround work on all supported Android versions."
            ),
            FeatureCompatibilityItem(
                id = "dynamic_color",
                name = "Material You Dynamic Theming & Gradient Blur",
                category = "Theming & Appearance",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsDynamicColor,
                description = "Extracts system Monet wallpaper colors and drives the top-half Material You Gradient Blur background on Home and Explore.",
                limitationNote = "System wallpaper color extraction requires Android 12+. Live Album Art, Black & White, and Monochrome Yellow palettes work on all versions."
            ),
            FeatureCompatibilityItem(
                id = "music_live_wallpaper",
                name = "Music Live Wallpaper",
                category = "Theming & Appearance",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsMusicLiveWallpaper,
                description = "Live wallpaper that draws the playing album art with Material You morphing shapes over your fallback wallpaper.",
                limitationNote = "Requires Android 12+ for WallpaperColors reporting and hardware RenderEffect blur."
            ),
            FeatureCompatibilityItem(
                id = "live_island",
                name = "Live Activity & Dynamic Island",
                category = "System & Notifications",
                requiredVersionLabel = "Android 16+ (API 36) or Android 14+ (OriginOS/ColorOS/HyperOS)",
                minApi = 34,
                isSupported = supportsLiveNotificationOrIsland(),
                description = "Displays live playback controls and animated notes in the status bar capsule or dynamic island.",
                limitationNote = "Requires Android 16 (API 36+) for native live notifications, or Android 14+ (API 34+) on supported OEM skins (OriginOS 4+, HyperOS, ColorOS 14+)."
            ),
            FeatureCompatibilityItem(
                id = "video_sharing",
                name = "30s Story Video Export",
                category = "Media & Sharing",
                requiredVersionLabel = "Android 11+ (API 30) & H.264 Encoder",
                minApi = 30,
                isSupported = supportsVideoSharing(),
                description = "Generates 30-second animated video cards with synced track audio for social media stories.",
                limitationNote = "Requires Media3 Transformer video muxing and an available H.264 (AVC) video encoder on the device."
            ),
            FeatureCompatibilityItem(
                id = "predictive_back",
                name = "Predictive Back Animations",
                category = "Navigation & Gestures",
                requiredVersionLabel = "Android 14+ (API 34)",
                minApi = 34,
                isSupported = supportsPredictiveBack,
                description = "Fluid predictive back gesture transitions when collapsing full player sheets and lyrics views.",
                limitationNote = "Android 14+ system gesture progress API is required for interactive predictive back animations."
            ),
            FeatureCompatibilityItem(
                id = "post_notifications",
                name = "Post Notification Permission",
                category = "System Permissions",
                requiredVersionLabel = "Android 13+ (API 33)",
                minApi = 33,
                isSupported = supportsPostNotifications,
                description = "Runtime permission prompt required to display background media playback controls.",
                limitationNote = "On Android 10–12, notifications are granted automatically at install time and do not need runtime requests."
            )
        )
    }
}
