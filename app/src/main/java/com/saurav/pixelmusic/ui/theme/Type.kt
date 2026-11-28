package com.saurav.pixelmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.R


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurav.pixelmusic.data.preferences.AppFontMode
import com.saurav.pixelmusic.utils.CustomFontManager

object FontSettings {
    var appFontMode by mutableStateOf(AppFontMode.APP_DEFAULT)
    var useSystemFont: Boolean
        get() = appFontMode == AppFontMode.SYSTEM
        set(value) {
            appFontMode = if (value) AppFontMode.SYSTEM else AppFontMode.APP_DEFAULT
        }
}

private val montserrat = GoogleFont("Montserrat")
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

val MontserratFamily = FontFamily(
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Black),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
)

private fun createExpTitleTypography(family: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    displayMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    titleMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)

private val DefaultExpTitleTypography = createExpTitleTypography(MontserratFamily)
private var cachedExpMode: String? = null
private var cachedExpFamily: FontFamily? = null
private var cachedExpTypography: Typography? = null

val ExpTitleTypography: Typography
    get() {
        val mode = FontSettings.appFontMode
        if (mode == AppFontMode.APP_DEFAULT) return DefaultExpTitleTypography
        val family = CustomFontManager.resolveFontFamily(mode)
        if (cachedExpMode != mode || cachedExpFamily !== family || cachedExpTypography == null) {
            cachedExpMode = mode
            cachedExpFamily = family
            cachedExpTypography = createExpTitleTypography(family)
        }
        return cachedExpTypography ?: DefaultExpTitleTypography
    }

// Google Sans Flex variable font with rounded axis for Google Sans Rounded-like appearance.
private const val GoogleSansFlexRond = 100f

@OptIn(ExperimentalTextApi::class)
val ActualGoogleSansRounded = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
)

val GoogleSansRounded: FontFamily
    get() = CustomFontManager.resolveFontFamily(FontSettings.appFontMode)

private fun createBaseTypography(family: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

private val DefaultAppTypography = createBaseTypography(ActualGoogleSansRounded)
private val SystemFontTypography = createBaseTypography(FontFamily.Default)
private var cachedCustomMode: String? = null
private var cachedCustomFamily: FontFamily? = null
private var cachedCustomTypography: Typography? = null

val Typography: Typography
    get() {
        val mode = FontSettings.appFontMode
        return when (mode) {
            AppFontMode.APP_DEFAULT -> DefaultAppTypography
            AppFontMode.SYSTEM -> SystemFontTypography
            else -> {
                val family = CustomFontManager.resolveFontFamily(mode)
                if (family === ActualGoogleSansRounded) {
                    DefaultAppTypography
                } else {
                    if (cachedCustomMode != mode || cachedCustomFamily !== family || cachedCustomTypography == null) {
                        cachedCustomMode = mode
                        cachedCustomFamily = family
                        cachedCustomTypography = createBaseTypography(family)
                    }
                    cachedCustomTypography ?: DefaultAppTypography
                }
            }
        }
    }
