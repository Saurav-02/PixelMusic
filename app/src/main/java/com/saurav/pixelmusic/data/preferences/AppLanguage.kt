package com.saurav.pixelmusic.data.preferences

import android.content.Context
import androidx.annotation.StringRes
import com.saurav.pixelmusic.R

enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.setcat_language_system),
    ENGLISH("en", R.string.setcat_language_english),
    SPANISH("es", R.string.setcat_language_spanish),
    GERMAN("de", R.string.setcat_language_german),
    FRENCH("fr", R.string.setcat_language_french),
    RUSSIAN("ru", R.string.setcat_language_russian),
    CHINESE("zh-CN", R.string.setcat_language_chinese),
    INDONESIAN("in", R.string.setcat_language_indonesian),
    ITALIAN("it", R.string.setcat_language_italian);

    companion object {
        val supportedLanguageTags: Set<String> = values().map { it.tag }.toSet()

        fun getLanguageOptions(context: Context): Map<String, String> {
            return values().associate { it.tag to context.getString(it.labelRes) }
        }

        fun normalize(languageTag: String?): String {
            val normalized = languageTag?.trim() ?: return SYSTEM.tag
            return values().find { it.tag.equals(normalized, ignoreCase = true) }?.tag ?: SYSTEM.tag
        }
    }
}
