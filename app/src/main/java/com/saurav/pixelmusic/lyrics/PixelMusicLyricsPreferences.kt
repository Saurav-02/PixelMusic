package com.saurav.pixelmusic.lyrics

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saurav.pixelmusic.data.preferences.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PixelMusicLyricsStyle(
    val displayName: String,
    val description: String,
    val risesWords: Boolean = false,
    val shaded: Boolean = false,
) {
    MOTION(
        displayName = "Style 1",
        description = "Words rise as they're sung, long notes swell, lines cascade",
        risesWords = true,
        shaded = true,
    ),
    LEGACY(
        displayName = "Style 2",
        description = "The original wave across the sung letters",
        risesWords = false,
        shaded = true,
    ),
    BORING(
        displayName = "Boring",
        description = "Style 1's rise, without the glow",
        risesWords = true,
        shaded = false,
    ),
    PLAIN(
        displayName = "Plain",
        description = "Style 2's sweep, without the glow or the wave",
        risesWords = false,
        shaded = false,
    ),
    PIXELMUSIC(
        displayName = "PixelMusic",
        description = "Original PixelMusic spring-animated synced lyrics",
        risesWords = false,
        shaded = false,
    );

    companion object {
        fun fromName(name: String?): PixelMusicLyricsStyle = when (name?.uppercase()) {
            "MOTION", "APPLE", "APPLE_V2", "PIXELMUSIC_1", "GLOW" -> MOTION
            "LEGACY", "KARAOKE", "METRO_LYRICS" -> LEGACY
            "BORING", "SLIDE" -> BORING
            "PLAIN", "LYRICS_V2", "FADE", "NONE" -> PLAIN
            "PIXELMUSIC" -> PIXELMUSIC
            else -> MOTION
        }
    }
}

object PixelMusicLyricsPreferences {
    val StyleKey = stringPreferencesKey("pixelmusic_lyrics_style")
    val TextSizeKey = floatPreferencesKey("pixelmusic_lyrics_text_size")
    val LineSpacingKey = floatPreferencesKey("pixelmusic_lyrics_line_spacing")
    val SoftLightKey = booleanPreferencesKey("pixelmusic_lyrics_soft_light")
    val AutoTranslateKey = booleanPreferencesKey("pixelmusic_lyrics_auto_translate")
    val TranslateLanguageKey = stringPreferencesKey("pixelmusic_lyrics_translate_lang")
    val UseAiTranslationKey = booleanPreferencesKey("pixelmusic_lyrics_use_ai_translate")
    val AutoRomanizeKey = booleanPreferencesKey("pixelmusic_lyrics_auto_romanize")
    val RomanizationAsMainKey = booleanPreferencesKey("pixelmusic_lyrics_romanization_main")
    val ProviderOrderKey = stringPreferencesKey("pixelmusic_lyrics_provider_order")
    val LyricsOrderOverrideKey = booleanPreferencesKey("pixelmusic_lyrics_order_override")

    val DefaultProviderOrder: List<String> = listOf(
        "BetterLyrics",
        "Unison",
        "YouLyPlus",
        "Paxsenix",
        "LrcRed",
        "SimpMusic",
        "LrcLib",
        "Deezer",
        "NetEase",
        "QQMusic",
        "Kugou",
    )

    val SupportedTranslateLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "ar" to "Arabic",
        "hy" to "Armenian",
        "az" to "Azerbaijani",
        "bn" to "Bengali",
        "my" to "Burmese",
        "zh-CN" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)",
        "hr" to "Croatian",
        "cs" to "Czech",
        "da" to "Danish",
        "nl" to "Dutch",
        "tl" to "Filipino",
        "fi" to "Finnish",
        "fr" to "French",
        "ka" to "Georgian",
        "de" to "German",
        "el" to "Greek",
        "gu" to "Gujarati",
        "he" to "Hebrew",
        "hi" to "Hindi",
        "hu" to "Hungarian",
        "id" to "Indonesian",
        "it" to "Italian",
        "ja" to "Japanese",
        "kn" to "Kannada",
        "kk" to "Kazakh",
        "km" to "Khmer",
        "ko" to "Korean",
        "ku" to "Kurdish (Kurmanji)",
        "ckb" to "Kurdish (Sorani)",
        "ms" to "Malay",
        "ml" to "Malayalam",
        "mr" to "Marathi",
        "ne" to "Nepali",
        "no" to "Norwegian",
        "ps" to "Pashto",
        "fa" to "Persian (Farsi)",
        "pl" to "Polish",
        "pt" to "Portuguese",
        "pa" to "Punjabi",
        "ro" to "Romanian",
        "ru" to "Russian",
        "sr" to "Serbian",
        "si" to "Sinhala",
        "sk" to "Slovak",
        "es" to "Spanish",
        "sw" to "Swahili",
        "sv" to "Swedish",
        "ta" to "Tamil",
        "te" to "Telugu",
        "th" to "Thai",
        "tr" to "Turkish",
        "uk" to "Ukrainian",
        "ur" to "Urdu",
        "uz" to "Uzbek",
        "vi" to "Vietnamese",
    )

    fun languageDisplayName(code: String): String =
        SupportedTranslateLanguages.firstOrNull { it.first == code }?.second ?: code

    fun styleFlow(context: Context): Flow<PixelMusicLyricsStyle> =
        context.dataStore.data.map { PixelMusicLyricsStyle.fromName(it[StyleKey]) }

    fun textSizeFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[TextSizeKey] ?: 26f).coerceIn(18f, 36f) }

    fun lineSpacingFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[LineSpacingKey] ?: 1.25f).coerceIn(1.0f, 1.7f) }

    fun softLightFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SoftLightKey] ?: true }

    fun autoTranslateFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[AutoTranslateKey] ?: false }

    fun translateLanguageFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[TranslateLanguageKey]?.ifBlank { "en" } ?: "en" }

    fun useAiTranslationFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[UseAiTranslationKey] ?: false }

    fun autoRomanizeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[AutoRomanizeKey] ?: false }

    fun romanizationAsMainFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[RomanizationAsMainKey] ?: false }

    fun orderOverrideFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[LyricsOrderOverrideKey] ?: false }

    fun providerOrderFlow(context: Context): Flow<List<String>> =
        context.dataStore.data.map { effectiveProviderOrder(it[ProviderOrderKey].orEmpty()) }

    fun deserializeProviderOrder(raw: String): List<String> {
        if (raw.isBlank()) return DefaultProviderOrder
        val saved = raw.split(",").map { it.trim() }.filter { it in DefaultProviderOrder }.distinct()
        return saved + DefaultProviderOrder.filter { it !in saved }
    }

    fun effectiveProviderOrder(orderString: String, preferredProviderId: String? = null): List<String> {
        if (orderString.isNotBlank()) return deserializeProviderOrder(orderString)
        val preferred = preferredProviderId?.takeIf { it in DefaultProviderOrder }
            ?: return DefaultProviderOrder
        return listOf(preferred) + DefaultProviderOrder.filter { it != preferred }
    }

    suspend fun setStyle(context: Context, style: PixelMusicLyricsStyle) {
        context.dataStore.edit { it[StyleKey] = style.name }
    }

    suspend fun setTextSize(context: Context, sizeSp: Float) {
        context.dataStore.edit { it[TextSizeKey] = sizeSp.coerceIn(18f, 36f) }
    }

    suspend fun setLineSpacing(context: Context, spacing: Float) {
        context.dataStore.edit { it[LineSpacingKey] = spacing.coerceIn(1.0f, 1.7f) }
    }

    suspend fun setSoftLight(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[SoftLightKey] = enabled }
    }

    suspend fun setAutoTranslate(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AutoTranslateKey] = enabled }
    }

    suspend fun setTranslateLanguage(context: Context, langCode: String) {
        context.dataStore.edit { it[TranslateLanguageKey] = langCode }
    }

    suspend fun setUseAiTranslation(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[UseAiTranslationKey] = enabled }
    }

    suspend fun setAutoRomanize(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AutoRomanizeKey] = enabled }
    }

    suspend fun setRomanizationAsMain(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[RomanizationAsMainKey] = enabled }
    }

    suspend fun setOrderOverride(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[LyricsOrderOverrideKey] = enabled }
    }

    suspend fun setProviderOrder(context: Context, order: List<String>) {
        val clean = order.filter { it in DefaultProviderOrder }.distinct()
        val full = clean + DefaultProviderOrder.filter { it !in clean }
        context.dataStore.edit { it[ProviderOrderKey] = full.joinToString(",") }
    }
}
