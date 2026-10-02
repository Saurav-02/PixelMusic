package com.saurav.pixelmusic.utils

import android.media.MediaCodecList
import android.os.Build

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
     * Motion blur relies on Android 13+ (API 33, TIRAMISU) AGSL RuntimeShader
     * in [com.saurav.pixelmusic.ui.modifiers.ScrollMotionBlurModifier].
     */
    val supportsMotionBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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
     * AGSL shaders ([android.graphics.RuntimeShader]) require Android 13+ (API 33, TIRAMISU).
     */
    val supportsAgsl: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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
                id = "motion_blur",
                name = "Motion Blur",
                category = "Graphics & Animation",
                requiredVersionLabel = "Android 13+ (API 33)",
                minApi = 33,
                isSupported = supportsMotionBlur,
                description = "Applies real-time directional velocity blur when scrolling song and album lists.",
                limitationNote = "Requires AGSL (Android Graphics Shading Language) RuntimeShader introduced in Android 13."
            ),
            FeatureCompatibilityItem(
                id = "hardware_blur",
                name = "Hardware & Live Artwork Blur",
                category = "Graphics & UI",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsHardwareBlur,
                description = "Hardware-accelerated RenderEffect blurs for wallpaper backgrounds, sheet backdrops, and lyric focus.",
                limitationNote = "GPU RenderEffect blurring is only available in Android 12 and above. On Android 10/11, solid/tint scrims are used."
            ),
            FeatureCompatibilityItem(
                id = "dynamic_color",
                name = "Material You Dynamic Theming",
                category = "Theming & Appearance",
                requiredVersionLabel = "Android 12+ (API 31)",
                minApi = 31,
                isSupported = supportsDynamicColor,
                description = "Extracts accent and surface colors from your system wallpaper using Google Monet.",
                limitationNote = "Wallpaper color extraction requires Android 12+. Preset themes (Album Art, Black & White, Sage, etc.) are available on all versions."
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
                limitationNote = "On Android 10-12, notifications are granted automatically at install time and do not need runtime requests."
            )
        )
    }
}
