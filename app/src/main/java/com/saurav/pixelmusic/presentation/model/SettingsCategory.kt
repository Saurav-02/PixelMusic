package com.saurav.pixelmusic.presentation.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.vector.ImageVector
import com.saurav.pixelmusic.R

enum class SettingsCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector? = null,
    val iconRes: Int? = null
) {
    LIBRARY(
        id = "library",
        titleRes = R.string.settings_category_library_title,
        subtitleRes = R.string.settings_category_library_subtitle,
        iconRes = R.drawable.rounded_library_music_24
    ),
    CONTENT(
        id = "content",
        titleRes = R.string.settings_category_content_title,
        subtitleRes = R.string.settings_category_content_subtitle,
        iconRes = R.drawable.rounded_language_24
    ),
    APPEARANCE(
        id = "appearance",
        titleRes = R.string.settings_category_appearance_title,
        subtitleRes = R.string.settings_category_appearance_subtitle,
        iconRes = R.drawable.rounded_palette_24
    ),
    PLAYBACK(
        id = "playback",
        titleRes = R.string.settings_category_playback_title,
        subtitleRes = R.string.settings_category_playback_subtitle,
        iconRes = R.drawable.rounded_music_note_24
    ),
    EQUALIZER(
        id = "equalizer",
        titleRes = R.string.settings_category_equalizer_title,
        subtitleRes = R.string.settings_category_equalizer_subtitle,
        iconRes = R.drawable.rounded_graphic_eq_24
    ),
    PIXEL_JAM(
        id = "pixel_jam",
        titleRes = R.string.pixeljam,
        subtitleRes = R.string.pixeljam_desc,
        iconRes = R.drawable.rounded_group_24
    ),
    AI_INTEGRATION(
        id = "ai",
        titleRes = R.string.settings_category_ai_title,
        subtitleRes = R.string.settings_category_ai_subtitle,
        iconRes = R.drawable.gemini_ai
    ),
    BACKUP_RESTORE(
        id = "backup_restore",
        titleRes = R.string.settings_category_backup_title,
        subtitleRes = R.string.settings_category_backup_subtitle,
        iconRes = R.drawable.rounded_upload_file_24
    ),
    DEVELOPER(
        id = "developer",
        titleRes = R.string.settings_category_developer_title,
        subtitleRes = R.string.settings_category_developer_subtitle,
        iconRes = R.drawable.rounded_developer_mode_24
    ),
    DEVICE_CAPABILITIES(
        id = "device_capabilities",
        titleRes = R.string.settings_category_device_capabilities_title,
        subtitleRes = R.string.settings_category_device_capabilities_subtitle,
        iconRes = R.drawable.rounded_developer_board_24
    ),
    ABOUT(
        id = "about",
        titleRes = R.string.settings_category_about_title,
        subtitleRes = R.string.settings_category_about_subtitle,
        icon = Icons.Rounded.Info
    ),
    LASTFM(
        id = "lastfm",
        titleRes = R.string.lastfm_integration,
        subtitleRes = R.string.lastfm_integration_desc,
        iconRes = R.drawable.rounded_music_note_24
    );

    companion object {
        fun fromId(id: String): SettingsCategory? = when (id) {
            "content" -> LIBRARY
            else -> entries.find { it.id == id }
        }
    }
}
