package com.saurav.pixelmusic.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.saurav.pixelmusic.R

enum class AppIconOption(
    val id: String,
    val label: String,
    val aliasClassName: String,
    @DrawableRes val previewRes: Int?,
    @DrawableRes val mipmapRes: Int,
    val isAdaptiveZoomPreview: Boolean = true
) {
    DEFAULT(
        id = "default",
        label = "Default",
        aliasClassName = "com.saurav.pixelmusic.launcher.DefaultLauncher",
        previewRes = null,
        mipmapRes = R.mipmap.ic_launcher,
        isAdaptiveZoomPreview = false
    ),
    PEACH_GLASS(
        id = "peach_glass",
        label = "Peach Glass",
        aliasClassName = "com.saurav.pixelmusic.launcher.PeachGlassLauncher",
        previewRes = R.drawable.ic_launcher_custom_peach_glass,
        mipmapRes = R.mipmap.ic_launcher_peach_glass
    ),
    BERRY_PULSE(
        id = "berry_pulse",
        label = "Berry Pulse",
        aliasClassName = "com.saurav.pixelmusic.launcher.BerryPulseLauncher",
        previewRes = R.drawable.ic_launcher_custom_berry_pulse,
        mipmapRes = R.mipmap.ic_launcher_berry_pulse
    ),
    SAGE_WAVE(
        id = "sage_wave",
        label = "Sage Wave",
        aliasClassName = "com.saurav.pixelmusic.launcher.SageWaveLauncher",
        previewRes = R.drawable.ic_launcher_custom_sage_wave,
        mipmapRes = R.mipmap.ic_launcher_sage_wave
    ),
    SKY_CLOUD(
        id = "sky_cloud",
        label = "Sky Cloud",
        aliasClassName = "com.saurav.pixelmusic.launcher.SkyCloudLauncher",
        previewRes = R.drawable.ic_launcher_custom_sky_cloud,
        mipmapRes = R.mipmap.ic_launcher_sky_cloud
    ),
    WARM_MINIMAL(
        id = "warm_minimal",
        label = "Warm Minimal",
        aliasClassName = "com.saurav.pixelmusic.launcher.WarmMinimalLauncher",
        previewRes = R.drawable.ic_launcher_custom_warm_minimal,
        mipmapRes = R.mipmap.ic_launcher_warm_minimal
    ),
    EMERALD_INK(
        id = "emerald_ink",
        label = "Emerald Ink",
        aliasClassName = "com.saurav.pixelmusic.launcher.EmeraldInkLauncher",
        previewRes = R.drawable.ic_launcher_custom_emerald_ink,
        mipmapRes = R.mipmap.ic_launcher_emerald_ink
    ),
    WOVEN_FABRIC(
        id = "woven_fabric",
        label = "Woven Fabric",
        aliasClassName = "com.saurav.pixelmusic.launcher.WovenFabricLauncher",
        previewRes = R.drawable.ic_launcher_custom_woven_fabric,
        mipmapRes = R.mipmap.ic_launcher_woven_fabric
    ),
    COZY_KNIT(
        id = "cozy_knit",
        label = "Cozy Knit",
        aliasClassName = "com.saurav.pixelmusic.launcher.CozyKnitLauncher",
        previewRes = R.drawable.ic_launcher_custom_cozy_knit,
        mipmapRes = R.mipmap.ic_launcher_cozy_knit
    ),
    FOLK_ART(
        id = "folk_art",
        label = "Folk Art",
        aliasClassName = "com.saurav.pixelmusic.launcher.FolkArtLauncher",
        previewRes = R.drawable.ic_launcher_custom_folk_art,
        mipmapRes = R.mipmap.ic_launcher_folk_art
    ),
    ROYAL_FILIGREE(
        id = "royal_filigree",
        label = "Royal Filigree",
        aliasClassName = "com.saurav.pixelmusic.launcher.RoyalFiligreeLauncher",
        previewRes = R.drawable.ic_launcher_custom_royal_filigree,
        mipmapRes = R.mipmap.ic_launcher_royal_filigree
    ),
    OIL_SPLASH(
        id = "oil_splash",
        label = "Oil Splash",
        aliasClassName = "com.saurav.pixelmusic.launcher.OilSplashLauncher",
        previewRes = R.drawable.ic_launcher_custom_oil_splash,
        mipmapRes = R.mipmap.ic_launcher_oil_splash
    ),
    LAVENDER_PLUSH(
        id = "lavender_plush",
        label = "Lavender Plush",
        aliasClassName = "com.saurav.pixelmusic.launcher.LavenderPlushLauncher",
        previewRes = R.drawable.ic_launcher_custom_lavender_plush,
        mipmapRes = R.mipmap.ic_launcher_lavender_plush
    ),
    STUDIO_CLAY(
        id = "studio_clay",
        label = "Studio Clay",
        aliasClassName = "com.saurav.pixelmusic.launcher.StudioClayLauncher",
        previewRes = R.drawable.ic_launcher_custom_studio_clay,
        mipmapRes = R.mipmap.ic_launcher_studio_clay
    ),
    SUNSET_WASH(
        id = "sunset_wash",
        label = "Sunset Wash",
        aliasClassName = "com.saurav.pixelmusic.launcher.SunsetWashLauncher",
        previewRes = R.drawable.ic_launcher_custom_sunset_wash,
        mipmapRes = R.mipmap.ic_launcher_sunset_wash
    ),
    CARVED_WOOD(
        id = "carved_wood",
        label = "Carved Wood",
        aliasClassName = "com.saurav.pixelmusic.launcher.CarvedWoodLauncher",
        previewRes = R.drawable.ic_launcher_custom_carved_wood,
        mipmapRes = R.mipmap.ic_launcher_carved_wood
    ),
    BOTANICAL_PAPER(
        id = "botanical_paper",
        label = "Botanical Paper",
        aliasClassName = "com.saurav.pixelmusic.launcher.BotanicalPaperLauncher",
        previewRes = R.drawable.ic_launcher_custom_botanical_paper,
        mipmapRes = R.mipmap.ic_launcher_botanical_paper
    )
}

object AppIconManager {

    fun getCurrentIcon(context: Context): AppIconOption {
        val pm = context.packageManager
        for (option in AppIconOption.entries) {
            if (option == AppIconOption.DEFAULT) continue
            val state = pm.getComponentEnabledSetting(
                ComponentName(context.packageName, option.aliasClassName)
            )
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return option
            }
        }
        return AppIconOption.DEFAULT
    }

    fun syncRecentsTaskIcon(activity: Activity) {
        updateRecentsTaskIcon(activity, getCurrentIcon(activity))
    }

    fun applyIcon(context: Context, target: AppIconOption) {
        // Update the current Recents task icon with a rendered Bitmap before disabling the
        // previous alias so Android Recents / OEM launchers do not fall back to the green robot.
        updateRecentsTaskIcon(context, target)

        val pm = context.packageManager
        val targetComponent = ComponentName(context.packageName, target.aliasClassName)
        if (pm.getComponentEnabledSetting(targetComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        for (option in AppIconOption.entries) {
            if (option == target) continue
            val component = ComponentName(context.packageName, option.aliasClassName)
            val currentState = pm.getComponentEnabledSetting(component)
            if (option == AppIconOption.DEFAULT) {
                if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } else {
                val isAlreadyOff = currentState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                if (!isAlreadyOff) {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }

        // Re-apply on the next UI frame in case SystemUI refreshes task metadata right after
        // the component-state change.
        context.findActivity()?.window?.decorView?.post {
            updateRecentsTaskIcon(context, target)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateRecentsTaskIcon(context: Context, target: AppIconOption) {
        val activity = context.findActivity() ?: return
        runCatching {
            val label = context.getString(R.string.app_name)
            val drawable = ContextCompat.getDrawable(context, target.mipmapRes)
                ?: target.previewRes?.let { ContextCompat.getDrawable(context, it) }
            val bitmap = drawable?.toBitmap(width = 192, height = 192)
            // Always pass a rendered Bitmap rather than TaskDescription.Builder.setIcon(resId):
            // on Android 13+ and OEM launchers (ColorOS/OxygenOS/MIUI), passing an adaptive-icon
            // XML resource ID fails to decode and falls back to the disabled alias's green robot.
            val taskDescription = ActivityManager.TaskDescription(label, bitmap)
            activity.setTaskDescription(taskDescription)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
