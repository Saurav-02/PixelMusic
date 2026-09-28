@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.saurav.pixelmusic.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import com.saurav.pixelmusic.presentation.viewmodel.ColorSchemePair

val LocalPixelMusicDarkTheme = staticCompositionLocalOf { false }
val LocalIsBlackAndWhiteTheme = staticCompositionLocalOf { false }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun PixelMusicStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

// --- Sage Green / Mint Palette (Soothing) ---
private val SageDarkBackground = Color(0xFF0D1210)
private val SageDarkSurface = Color(0xFF151B18)
private val SageDarkPrimary = Color(0xFF6EDBB1)
private val SageDarkSecondary = Color(0xFF4E9A7E)
private val SageDarkTertiary = Color(0xFF8AC7AC)
private val SageDarkOnPrimary = Color(0xFF003824)
private val SageDarkOnBackground = Color(0xFFE1E3DF)
private val SageDarkOnSurface = Color(0xFFE1E3DF)
private val SageDarkOnSurfaceVariant = Color(0xFFBFC9C2)

private val SageLightBackground = Color(0xFFF3FAF6)
private val SageLightSurface = Color(0xFFF7FCFA)
private val SageLightPrimary = Color(0xFF1F6C50)
private val SageLightOnPrimary = Color(0xFFFFFFFF)
private val SageLightPrimaryContainer = Color(0xFFC4F2DB)
private val SageLightOnPrimaryContainer = Color(0xFF002114)
private val SageLightSecondary = Color(0xFF4C6357)
private val SageLightSecondaryContainer = Color(0xFFCEE9DB)
private val SageLightOnSecondaryContainer = Color(0xFF092016)
private val SageLightTertiary = Color(0xFF3F6555)
private val SageLightOnBackground = Color(0xFF191D1A)
private val SageLightOnSurface = Color(0xFF191D1A)
private val SageLightSurfaceVariant = Color(0xFFDCE5DE)
private val SageLightOnSurfaceVariant = Color(0xFF404944)
private val SageLightOutline = Color(0xFF707973)

val SageDarkColorScheme = darkColorScheme(
    primary = SageDarkPrimary,
    secondary = SageDarkSecondary,
    tertiary = SageDarkTertiary,
    background = SageDarkBackground,
    surface = SageDarkSurface,
    onPrimary = SageDarkOnPrimary,
    onSecondary = SageDarkOnPrimary,
    onTertiary = SageDarkOnPrimary,
    onBackground = SageDarkOnBackground,
    onSurface = SageDarkOnSurface,
    onSurfaceVariant = SageDarkOnSurfaceVariant,
    error = Color(0xFFFF5252),
    onError = Color.White
)

val SageLightColorScheme = lightColorScheme(
    primary = SageLightPrimary,
    onPrimary = SageLightOnPrimary,
    primaryContainer = SageLightPrimaryContainer,
    onPrimaryContainer = SageLightOnPrimaryContainer,
    secondary = SageLightSecondary,
    onSecondary = SageLightOnPrimary,
    secondaryContainer = SageLightSecondaryContainer,
    onSecondaryContainer = SageLightOnSecondaryContainer,
    tertiary = SageLightTertiary,
    onTertiary = PixelMusicBlack,
    background = SageLightBackground,
    onBackground = SageLightOnBackground,
    surface = SageLightSurface,
    onSurface = SageLightOnSurface,
    surfaceVariant = SageLightSurfaceVariant,
    onSurfaceVariant = SageLightOnSurfaceVariant,
    outline = SageLightOutline,
    outlineVariant = SageLightOutline.copy(alpha = 0.6f),
    surfaceTint = SageLightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// Standard default schemas mapped to Sage to make soothing green the out-of-the-box default
val DarkColorScheme = SageDarkColorScheme
val LightColorScheme = SageLightColorScheme

// --- Classic Purple Palette ---
private val PurpleDarkBackground = Color(0xFF0A0714)
private val PurpleDarkSurface = Color(0xFF13101E)
private val PurpleDarkPrimary = Color(0xFFB29BF4)
private val PurpleDarkSecondary = Color(0xFFE57399)
private val PurpleDarkTertiary = Color(0xFFD4B2F7)
private val PurpleDarkOnPrimary = Color(0xFF280066)
private val PurpleDarkOnBackground = Color(0xFFE7E3EC)
private val PurpleDarkOnSurface = Color(0xFFE7E3EC)
private val PurpleDarkOnSurfaceVariant = Color(0xFFC9C4D0)

private val PurpleLightBackground = Color(0xFFF9F7FC)
private val PurpleLightSurface = Color(0xFFFAF9FC)
private val PurpleLightPrimary = Color(0xFF6C4FBB)
private val PurpleLightOnPrimary = Color(0xFFFFFFFF)
private val PurpleLightPrimaryContainer = Color(0xFFE9E3FB)
private val PurpleLightOnPrimaryContainer = Color(0xFF1F005C)
private val PurpleLightSecondary = Color(0xFF825272)
private val PurpleLightSecondaryContainer = Color(0xFFFFD8EC)
private val PurpleLightOnSecondaryContainer = Color(0xFF370B2C)
private val PurpleLightTertiary = Color(0xFF705574)
private val PurpleLightOnBackground = Color(0xFF1C1A22)
private val PurpleLightOnSurface = Color(0xFF1C1A22)
private val PurpleLightSurfaceVariant = Color(0xFFE7E0EC)
private val PurpleLightOnSurfaceVariant = Color(0xFF49454F)
private val PurpleLightOutline = Color(0xFF7A757F)

val PurpleDarkColorScheme = darkColorScheme(
    primary = PurpleDarkPrimary,
    secondary = PurpleDarkSecondary,
    tertiary = PurpleDarkTertiary,
    background = PurpleDarkBackground,
    surface = PurpleDarkSurface,
    onPrimary = PurpleDarkOnPrimary,
    onSecondary = PurpleDarkOnPrimary,
    onTertiary = PurpleDarkOnPrimary,
    onBackground = PurpleDarkOnBackground,
    onSurface = PurpleDarkOnSurface,
    onSurfaceVariant = PurpleDarkOnSurfaceVariant,
    error = Color(0xFFFF5252),
    onError = Color.White
)

val PurpleLightColorScheme = lightColorScheme(
    primary = PurpleLightPrimary,
    onPrimary = PurpleLightOnPrimary,
    primaryContainer = PurpleLightPrimaryContainer,
    onPrimaryContainer = PurpleLightOnPrimaryContainer,
    secondary = PurpleLightSecondary,
    onSecondary = PurpleLightOnPrimary,
    secondaryContainer = PurpleLightSecondaryContainer,
    onSecondaryContainer = PurpleLightOnSecondaryContainer,
    tertiary = PurpleLightTertiary,
    onTertiary = PixelMusicBlack,
    background = PurpleLightBackground,
    onBackground = PurpleLightOnBackground,
    surface = PurpleLightSurface,
    onSurface = PurpleLightOnSurface,
    surfaceVariant = PurpleLightSurfaceVariant,
    onSurfaceVariant = PurpleLightOnSurfaceVariant,
    outline = PurpleLightOutline,
    outlineVariant = PurpleLightOutline.copy(alpha = 0.6f),
    surfaceTint = PurpleLightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// --- Monochrome Yellow Palette (Minimalist) ---
private val YellowDarkBackground = Color(0xFF000000)
private val YellowDarkSurface = Color(0xFF141414)
private val YellowDarkPrimary = Color(0xFFFFC436) // Vibrant Amber/Yellow
private val YellowDarkSecondary = Color(0xFFFFFFFF) // White accents
private val YellowDarkTertiary = Color(0xFFCCCCCC)
private val YellowDarkOnPrimary = Color(0xFF221500)
private val YellowDarkOnBackground = Color(0xFFF0F0F0)
private val YellowDarkOnSurface = Color(0xFFF0F0F0)
private val YellowDarkOnSurfaceVariant = Color(0xFFAAAAAA)

private val YellowLightBackground = Color(0xFFFFFFFF) // Pure White
private val YellowLightSurface = Color(0xFFFAFAFA)
private val YellowLightPrimary = Color(0xFFFFC436) // Vibrant Amber/Yellow
private val YellowLightOnPrimary = Color(0xFF000000)
private val YellowLightPrimaryContainer = Color(0xFFFFF3D6)
private val YellowLightOnPrimaryContainer = Color(0xFF3E2723)
private val YellowLightSecondary = Color(0xFF111111) // Stark Black for secondary buttons (like your image)
private val YellowLightSecondaryContainer = Color(0xFFEAEAEA)
private val YellowLightOnSecondaryContainer = Color(0xFF111111)
private val YellowLightTertiary = Color(0xFF757575)
private val YellowLightOnBackground = Color(0xFF111111)
private val YellowLightOnSurface = Color(0xFF111111)
private val YellowLightSurfaceVariant = Color(0xFFF4F4F4)
private val YellowLightOnSurfaceVariant = Color(0xFF666666)
private val YellowLightOutline = Color(0xFFCCCCCC)

val YellowDarkColorScheme = darkColorScheme(
    primary = YellowDarkPrimary,
    secondary = YellowDarkSecondary,
    tertiary = YellowDarkTertiary,
    background = YellowDarkBackground,
    surface = YellowDarkSurface,
    onPrimary = YellowDarkOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = YellowDarkOnBackground,
    onSurface = YellowDarkOnSurface,
    onSurfaceVariant = YellowDarkOnSurfaceVariant,
    error = Color(0xFFFF5252),
    onError = Color.White
)

val YellowLightColorScheme = lightColorScheme(
    primary = YellowLightPrimary,
    onPrimary = YellowLightOnPrimary,
    primaryContainer = YellowLightPrimaryContainer,
    onPrimaryContainer = YellowLightOnPrimaryContainer,
    secondary = YellowLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = YellowLightSecondaryContainer,
    onSecondaryContainer = YellowLightOnSecondaryContainer,
    tertiary = YellowLightTertiary,
    onTertiary = Color.White,
    background = YellowLightBackground,
    onBackground = YellowLightOnBackground,
    surface = YellowLightSurface,
    onSurface = YellowLightOnSurface,
    surfaceVariant = YellowLightSurfaceVariant,
    onSurfaceVariant = YellowLightOnSurfaceVariant,
    outline = YellowLightOutline,
    outlineVariant = YellowLightOutline.copy(alpha = 0.6f),
    surfaceTint = YellowLightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// --- Slate Blue Palette ---
private val BlueDarkBackground = Color(0xFF0B0F14)
private val BlueDarkSurface = Color(0xFF12171E)
private val BlueDarkPrimary = Color(0xFF7DB0E6)
private val BlueDarkSecondary = Color(0xFF5A84B0)
private val BlueDarkTertiary = Color(0xFF8AB9E6)
private val BlueDarkOnPrimary = Color(0xFF00315C)
private val BlueDarkOnBackground = Color(0xFFE2E2E6)
private val BlueDarkOnSurface = Color(0xFFE2E2E6)
private val BlueDarkOnSurfaceVariant = Color(0xFFC2C7CF)

private val BlueLightBackground = Color(0xFFF3F7FA)
private val BlueLightSurface = Color(0xFFF7FAFC)
private val BlueLightPrimary = Color(0xFF22588F)
private val BlueLightOnPrimary = Color(0xFFFFFFFF)
private val BlueLightPrimaryContainer = Color(0xFFC4DEF6)
private val BlueLightOnPrimaryContainer = Color(0xFF001C3A)
private val BlueLightSecondary = Color(0xFF436080)
private val BlueLightSecondaryContainer = Color(0xFFC9E2FF)
private val BlueLightOnSecondaryContainer = Color(0xFF001D38)
private val BlueLightTertiary = Color(0xFF3E6080)
private val BlueLightOnBackground = Color(0xFF191C1E)
private val BlueLightOnSurface = Color(0xFF191C1E)
private val BlueLightSurfaceVariant = Color(0xFFDFE2E7)
private val BlueLightOnSurfaceVariant = Color(0xFF43474B)
private val BlueLightOutline = Color(0xFF73777C)

val BlueDarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    secondary = BlueDarkSecondary,
    tertiary = BlueDarkTertiary,
    background = BlueDarkBackground,
    surface = BlueDarkSurface,
    onPrimary = BlueDarkOnPrimary,
    onSecondary = BlueDarkOnPrimary,
    onTertiary = BlueDarkOnPrimary,
    onBackground = BlueDarkOnBackground,
    onSurface = BlueDarkOnSurface,
    onSurfaceVariant = BlueDarkOnSurfaceVariant,
    error = Color(0xFFFF5252),
    onError = Color.White
)

val BlueLightColorScheme = lightColorScheme(
    primary = BlueLightPrimary,
    onPrimary = BlueLightOnPrimary,
    primaryContainer = BlueLightPrimaryContainer,
    onPrimaryContainer = BlueLightOnPrimaryContainer,
    secondary = BlueLightSecondary,
    onSecondary = BlueLightOnPrimary,
    secondaryContainer = BlueLightSecondaryContainer,
    onSecondaryContainer = BlueLightOnSecondaryContainer,
    tertiary = BlueLightTertiary,
    onTertiary = PixelMusicBlack,
    background = BlueLightBackground,
    onBackground = BlueLightOnBackground,
    surface = BlueLightSurface,
    onSurface = BlueLightOnSurface,
    surfaceVariant = BlueLightSurfaceVariant,
    onSurfaceVariant = BlueLightOnSurfaceVariant,
    outline = BlueLightOutline,
    outlineVariant = BlueLightOutline.copy(alpha = 0.6f),
    surfaceTint = BlueLightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// --- Black & White Palette (Creamy White & Carbon Black) ---
private val BwDarkBackground = Color(0xFF0C0C0C)
private val BwDarkSurface = Color(0xFF141414)
private val BwDarkPrimary = Color(0xFFF7F5EE) // Creamy White
private val BwDarkOnPrimary = Color(0xFF121212) // Carbon Black
private val BwDarkPrimaryContainer = Color(0xFF282828)
private val BwDarkOnPrimaryContainer = Color(0xFFF7F5EE)
private val BwDarkSecondary = Color(0xFFD6D3CA) // Soft Creamy Pearl
private val BwDarkOnSecondary = Color(0xFF121212)
private val BwDarkSecondaryContainer = Color(0xFF222222)
private val BwDarkOnSecondaryContainer = Color(0xFFE5E2D9)
private val BwDarkTertiary = Color(0xFFB0ACA2) // Warm Ash
private val BwDarkOnTertiary = Color(0xFF121212)
private val BwDarkOnBackground = Color(0xFFF7F5EE)
private val BwDarkOnSurface = Color(0xFFF7F5EE)
private val BwDarkSurfaceVariant = Color(0xFF222222)
private val BwDarkOnSurfaceVariant = Color(0xFFB5B2A9)
private val BwDarkOutline = Color(0xFF6B6861)
private val BwDarkOutlineVariant = Color(0xFF383632)

private val BwLightBackground = Color(0xFFFAF7F0) // Creamy White
private val BwLightSurface = Color(0xFFF4F0E7) // Soft Creamy Ivory
private val BwLightPrimary = Color(0xFF141414) // Carbon Black
private val BwLightOnPrimary = Color(0xFFFAF7F0) // Creamy White
private val BwLightPrimaryContainer = Color(0xFFE2DDD2) // Alabaster Container
private val BwLightOnPrimaryContainer = Color(0xFF141414)
private val BwLightSecondary = Color(0xFF333333) // Deep Charcoal
private val BwLightOnSecondary = Color(0xFFFFFFFF)
private val BwLightSecondaryContainer = Color(0xFFEAE5DB)
private val BwLightOnSecondaryContainer = Color(0xFF1C1C1C)
private val BwLightTertiary = Color(0xFF5A5852)
private val BwLightOnTertiary = Color(0xFFFFFFFF)
private val BwLightOnBackground = Color(0xFF141414) // Carbon Black text
private val BwLightOnSurface = Color(0xFF141414)
private val BwLightSurfaceVariant = Color(0xFFE5E0D5)
private val BwLightOnSurfaceVariant = Color(0xFF524F49)
private val BwLightOutline = Color(0xFF8C8880)
private val BwLightOutlineVariant = Color(0xFFC7C2B6)

val BwDarkColorScheme = darkColorScheme(
    primary = BwDarkPrimary,
    onPrimary = BwDarkOnPrimary,
    primaryContainer = BwDarkPrimaryContainer,
    onPrimaryContainer = BwDarkOnPrimaryContainer,
    secondary = BwDarkSecondary,
    onSecondary = BwDarkOnSecondary,
    secondaryContainer = BwDarkSecondaryContainer,
    onSecondaryContainer = BwDarkOnSecondaryContainer,
    tertiary = BwDarkTertiary,
    onTertiary = BwDarkOnTertiary,
    background = BwDarkBackground,
    onBackground = BwDarkOnBackground,
    surface = BwDarkSurface,
    onSurface = BwDarkOnSurface,
    surfaceVariant = BwDarkSurfaceVariant,
    onSurfaceVariant = BwDarkOnSurfaceVariant,
    outline = BwDarkOutline,
    outlineVariant = BwDarkOutlineVariant,
    surfaceTint = BwDarkPrimary,
    error = Color(0xFFE0E0E0),
    onError = Color(0xFF121212)
)

val BwLightColorScheme = lightColorScheme(
    primary = BwLightPrimary,
    onPrimary = BwLightOnPrimary,
    primaryContainer = BwLightPrimaryContainer,
    onPrimaryContainer = BwLightOnPrimaryContainer,
    secondary = BwLightSecondary,
    onSecondary = BwLightOnSecondary,
    secondaryContainer = BwLightSecondaryContainer,
    onSecondaryContainer = BwLightOnSecondaryContainer,
    tertiary = BwLightTertiary,
    onTertiary = BwLightOnTertiary,
    background = BwLightBackground,
    onBackground = BwLightOnBackground,
    surface = BwLightSurface,
    onSurface = BwLightOnSurface,
    surfaceVariant = BwLightSurfaceVariant,
    onSurfaceVariant = BwLightOnSurfaceVariant,
    outline = BwLightOutline,
    outlineVariant = BwLightOutlineVariant,
    surfaceTint = BwLightPrimary,
    error = Color(0xFF222222),
    onError = Color(0xFFFAF7F0)
)

// --- Sunset Orange Palette ---
private val OrangeDarkBackground = Color(0xFF120E0A)
private val OrangeDarkSurface = Color(0xFF1A1510)
private val OrangeDarkPrimary = Color(0xFFF5A873)
private val OrangeDarkSecondary = Color(0xFFB37D56)
private val OrangeDarkTertiary = Color(0xFFF7BE98)
private val OrangeDarkOnPrimary = Color(0xFF4C1E00)
private val OrangeDarkOnBackground = Color(0xFFECE1DB)
private val OrangeDarkOnSurface = Color(0xFFECE1DB)
private val OrangeDarkOnSurfaceVariant = Color(0xFFD7C4B7)

private val OrangeLightBackground = Color(0xFFFAF6F2)
private val OrangeLightSurface = Color(0xFFFCFAF7)
private val OrangeLightPrimary = Color(0xFF8F4F20)
private val OrangeLightOnPrimary = Color(0xFFFFFFFF)
private val OrangeLightPrimaryContainer = Color(0xFFFADFC9)
private val OrangeLightOnPrimaryContainer = Color(0xFF341100)
private val OrangeLightSecondary = Color(0xFF7E5233)
private val OrangeLightSecondaryContainer = Color(0xFFFFDBC6)
private val OrangeLightOnSecondaryContainer = Color(0xFF301400)
private val OrangeLightTertiary = Color(0xFF79563C)
private val OrangeLightOnBackground = Color(0xFF221A15)
private val OrangeLightOnSurface = Color(0xFF221A15)
private val OrangeLightSurfaceVariant = Color(0xFFF4DFD0)
private val OrangeLightOnSurfaceVariant = Color(0xFF52443C)
private val OrangeLightOutline = Color(0xFF85736B)

val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    secondary = OrangeDarkSecondary,
    tertiary = OrangeDarkTertiary,
    background = OrangeDarkBackground,
    surface = OrangeDarkSurface,
    onPrimary = OrangeDarkOnPrimary,
    onSecondary = OrangeDarkOnPrimary,
    onTertiary = OrangeDarkOnPrimary,
    onBackground = OrangeDarkOnBackground,
    onSurface = OrangeDarkOnSurface,
    onSurfaceVariant = OrangeDarkOnSurfaceVariant,
    error = Color(0xFFFF5252),
    onError = Color.White
)

val OrangeLightColorScheme = lightColorScheme(
    primary = OrangeLightPrimary,
    onPrimary = OrangeLightOnPrimary,
    primaryContainer = OrangeLightPrimaryContainer,
    onPrimaryContainer = OrangeLightOnPrimaryContainer,
    secondary = OrangeLightSecondary,
    onSecondary = OrangeLightOnPrimary,
    secondaryContainer = OrangeLightSecondaryContainer,
    onSecondaryContainer = OrangeLightOnSecondaryContainer,
    tertiary = OrangeLightTertiary,
    onTertiary = PixelMusicBlack,
    background = OrangeLightBackground,
    onBackground = OrangeLightOnBackground,
    surface = OrangeLightSurface,
    onSurface = OrangeLightOnSurface,
    surfaceVariant = OrangeLightSurfaceVariant,
    onSurfaceVariant = OrangeLightOnSurfaceVariant,
    outline = OrangeLightOutline,
    outlineVariant = OrangeLightOutline.copy(alpha = 0.6f),
    surfaceTint = OrangeLightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private fun getStaticColorScheme(palette: String, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    return if (darkTheme) {
        when (palette) {
            "BLACK_AND_WHITE" -> BwDarkColorScheme
            "PURPLE" -> PurpleDarkColorScheme
            "BLUE" -> BlueDarkColorScheme
            "ORANGE" -> OrangeDarkColorScheme
            "YELLOW" -> YellowDarkColorScheme
            else -> SageDarkColorScheme
        }
    } else {
        when (palette) {
            "BLACK_AND_WHITE" -> BwLightColorScheme
            "PURPLE" -> PurpleLightColorScheme
            "BLUE" -> BlueLightColorScheme
            "ORANGE" -> OrangeLightColorScheme
            "YELLOW" -> YellowLightColorScheme
            else -> SageLightColorScheme
        }
    }
}

@Composable
fun PixelMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorSchemePairOverride: ColorSchemePair? = null,
    colorPalette: String = "DYNAMIC",
    useSystemFont: Boolean = false,
    isAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    FontSettings.useSystemFont = useSystemFont
    val context = LocalContext.current
    
    val isBwTheme = colorPalette == "BLACK_AND_WHITE"

    // 1. Calculate the base scheme like you normally do
    val baseColorScheme = when {
        isBwTheme -> getStaticColorScheme("BLACK_AND_WHITE", darkTheme)
        colorSchemePairOverride != null -> {
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        colorPalette == "DYNAMIC" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                getStaticColorScheme(colorPalette, darkTheme)
            }
        }
        else -> {
            getStaticColorScheme(colorPalette, darkTheme)
        }
    }

    // 2. Intercept it for AMOLED Black!
    val finalColorScheme = if (darkTheme && isAmoledBlack) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0A0A0A),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF141414),
            surfaceContainerHigh = Color(0xFF1F1F1F),
            surfaceContainerHighest = Color(0xFF292929)
        )
    } else {
        baseColorScheme
    }

    // 3. Smoothly animate ColorScheme changes when Live Album Art palette is active
    val activeColorScheme = if (colorPalette == "ALBUM_ART" && colorSchemePairOverride != null) {
        rememberAnimatedColorScheme(finalColorScheme)
    } else {
        finalColorScheme
    }

    PixelMusicStatusBarStyle(
        color = activeColorScheme.background,
        navigationColor = activeColorScheme.background
    )

    CompositionLocalProvider(
        LocalPixelMusicDarkTheme provides darkTheme,
        LocalIsBlackAndWhiteTheme provides isBwTheme
    ) {
        MaterialTheme(
            colorScheme = activeColorScheme,
            typography = Typography,
            shapes = Shapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

@Composable
fun rememberAnimatedColorScheme(
    target: ColorScheme,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 650, easing = FastOutSlowInEasing)
): ColorScheme {
    val progress = remember { Animatable(1f) }
    var fromScheme by remember { mutableStateOf(target) }
    var toScheme by remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        if (toScheme == target && progress.value == 1f) return@LaunchedEffect
        fromScheme = lerpColorScheme(fromScheme, toScheme, progress.value)
        toScheme = target
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec)
    }

    val interpolated by remember {
        derivedStateOf { lerpColorScheme(fromScheme, toScheme, progress.value) }
    }
    return interpolated
}

fun lerpColorScheme(from: ColorScheme, to: ColorScheme, t: Float): ColorScheme =
    to.copy(
        primary = lerp(from.primary, to.primary, t),
        onPrimary = lerp(from.onPrimary, to.onPrimary, t),
        primaryContainer = lerp(from.primaryContainer, to.primaryContainer, t),
        onPrimaryContainer = lerp(from.onPrimaryContainer, to.onPrimaryContainer, t),
        inversePrimary = lerp(from.inversePrimary, to.inversePrimary, t),
        secondary = lerp(from.secondary, to.secondary, t),
        onSecondary = lerp(from.onSecondary, to.onSecondary, t),
        secondaryContainer = lerp(from.secondaryContainer, to.secondaryContainer, t),
        onSecondaryContainer = lerp(from.onSecondaryContainer, to.onSecondaryContainer, t),
        tertiary = lerp(from.tertiary, to.tertiary, t),
        onTertiary = lerp(from.onTertiary, to.onTertiary, t),
        tertiaryContainer = lerp(from.tertiaryContainer, to.tertiaryContainer, t),
        onTertiaryContainer = lerp(from.onTertiaryContainer, to.onTertiaryContainer, t),
        background = lerp(from.background, to.background, t),
        onBackground = lerp(from.onBackground, to.onBackground, t),
        surface = lerp(from.surface, to.surface, t),
        onSurface = lerp(from.onSurface, to.onSurface, t),
        surfaceVariant = lerp(from.surfaceVariant, to.surfaceVariant, t),
        onSurfaceVariant = lerp(from.onSurfaceVariant, to.onSurfaceVariant, t),
        surfaceTint = lerp(from.surfaceTint, to.surfaceTint, t),
        inverseSurface = lerp(from.inverseSurface, to.inverseSurface, t),
        inverseOnSurface = lerp(from.inverseOnSurface, to.inverseOnSurface, t),
        error = lerp(from.error, to.error, t),
        onError = lerp(from.onError, to.onError, t),
        errorContainer = lerp(from.errorContainer, to.errorContainer, t),
        onErrorContainer = lerp(from.onErrorContainer, to.onErrorContainer, t),
        outline = lerp(from.outline, to.outline, t),
        outlineVariant = lerp(from.outlineVariant, to.outlineVariant, t),
        scrim = lerp(from.scrim, to.scrim, t),
        surfaceBright = lerp(from.surfaceBright, to.surfaceBright, t),
        surfaceDim = lerp(from.surfaceDim, to.surfaceDim, t),
        surfaceContainer = lerp(from.surfaceContainer, to.surfaceContainer, t),
        surfaceContainerHigh = lerp(from.surfaceContainerHigh, to.surfaceContainerHigh, t),
        surfaceContainerHighest = lerp(from.surfaceContainerHighest, to.surfaceContainerHighest, t),
        surfaceContainerLow = lerp(from.surfaceContainerLow, to.surfaceContainerLow, t),
        surfaceContainerLowest = lerp(from.surfaceContainerLowest, to.surfaceContainerLowest, t)
    )
