package com.saurav.pixelmusic.utils

import android.media.MediaCodecList
import android.os.Build

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
        if (Build.VERSION.SDK_INT >= 36) return true
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val oemMatches = listOf("vivo", "iqoo", "xiaomi", "redmi", "oppo", "oneplus", "realme")
        return oemMatches.any { manufacturer.contains(it) || brand.contains(it) }
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
}
