package com.saurav.pixelmusic.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.saurav.pixelmusic.R

enum class AvatarCategory(val label: String) {
    ALL("All"),
    ANIMALS("Animals"),
    CHARACTERS("Characters")
}

@Immutable
data class BuiltInAvatar(
    val id: String,
    val label: String,
    val category: AvatarCategory,
    @DrawableRes val drawableRes: Int
)

@Immutable
data class AvatarBackground(
    val id: String,
    val label: String,
    val topColorHex: Long,
    val bottomColorHex: Long
)

object ProfileAvatarCatalog {
    const val PREFIX = "pm_avatar:"
    const val INITIAL_ONLY = "pm_initial"

    val avatars: List<BuiltInAvatar> = listOf(
        BuiltInAvatar("cat", "Kitty", AvatarCategory.ANIMALS, R.drawable.avatar_cat),
        BuiltInAvatar("fox", "Fox", AvatarCategory.ANIMALS, R.drawable.avatar_fox),
        BuiltInAvatar("panda", "Panda", AvatarCategory.ANIMALS, R.drawable.avatar_panda),
        BuiltInAvatar("bunny", "Bunny", AvatarCategory.ANIMALS, R.drawable.avatar_bunny),
        BuiltInAvatar("bear", "Bear", AvatarCategory.ANIMALS, R.drawable.avatar_bear),
        BuiltInAvatar("shiba", "Shiba", AvatarCategory.ANIMALS, R.drawable.avatar_shiba),
        BuiltInAvatar("koala", "Koala", AvatarCategory.ANIMALS, R.drawable.avatar_koala),
        BuiltInAvatar("frog", "Frog", AvatarCategory.ANIMALS, R.drawable.avatar_frog),
        BuiltInAvatar("penguin", "Penguin", AvatarCategory.ANIMALS, R.drawable.avatar_penguin),
        BuiltInAvatar("owl", "Owl", AvatarCategory.ANIMALS, R.drawable.avatar_owl),
        BuiltInAvatar("music_buddy", "Beats", AvatarCategory.CHARACTERS, R.drawable.avatar_music_buddy),
        BuiltInAvatar("astronaut", "Astro", AvatarCategory.CHARACTERS, R.drawable.avatar_astronaut),
        BuiltInAvatar("robot", "Bot", AvatarCategory.CHARACTERS, R.drawable.avatar_robot),
        BuiltInAvatar("ghost", "Ghosty", AvatarCategory.CHARACTERS, R.drawable.avatar_ghost),
        BuiltInAvatar("alien", "Alien", AvatarCategory.CHARACTERS, R.drawable.avatar_alien),
        BuiltInAvatar("star_spirit", "Star", AvatarCategory.CHARACTERS, R.drawable.avatar_star_spirit)
    )

    val backgrounds: List<AvatarBackground> = listOf(
        AvatarBackground("coral", "Coral", 0xFFFFC1AE, 0xFFFF8A65),
        AvatarBackground("sunbeam", "Sunbeam", 0xFFFFF0A6, 0xFFFFCA28),
        AvatarBackground("mint", "Mint", 0xFFC2EED0, 0xFF66BB6A),
        AvatarBackground("sky", "Sky", 0xFFB0DEFF, 0xFF42A5F5),
        AvatarBackground("lavender", "Lavender", 0xFFE6C6EE, 0xFFAB47BC),
        AvatarBackground("rose", "Rose", 0xFFFBC4D7, 0xFFEC407A),
        AvatarBackground("seafoam", "Seafoam", 0xFFB2EFE9, 0xFF26A69A),
        AvatarBackground("periwinkle", "Periwinkle", 0xFFCBD1F2, 0xFF5C6BC0),
        AvatarBackground("apricot", "Apricot", 0xFFFFDFB0, 0xFFFFA726),
        AvatarBackground("plum", "Plum", 0xFFD9CCF2, 0xFF7E57C2),
        AvatarBackground("sage", "Sage", 0xFFE2F2CE, 0xFF9CCC65),
        AvatarBackground("cloud", "Cloud", 0xFFD8E1E6, 0xFF78909C)
    )

    private val avatarById: Map<String, BuiltInAvatar> = avatars.associateBy { it.id }
    private val backgroundById: Map<String, AvatarBackground> = backgrounds.associateBy { it.id }

    fun encodeBuiltIn(avatarId: String, backgroundId: String): String {
        val safeAvatar = avatarById[avatarId]?.id ?: avatars.first().id
        val safeBg = backgroundById[backgroundId]?.id ?: backgrounds.first().id
        return "$PREFIX$safeAvatar:$safeBg"
    }

    fun parseBuiltIn(spec: String?): Pair<BuiltInAvatar, AvatarBackground>? {
        if (spec.isNullOrBlank() || !spec.startsWith(PREFIX)) return null
        val parts = spec.removePrefix(PREFIX).split(":")
        val avatarId = parts.getOrNull(0)?.trim().orEmpty()
        val bgId = parts.getOrNull(1)?.trim().orEmpty()
        val avatar = avatarById[avatarId] ?: return null
        val bg = backgroundById[bgId] ?: backgrounds.first()
        return avatar to bg
    }

    fun isBuiltIn(spec: String?): Boolean = parseBuiltIn(spec) != null

    fun isDataUri(spec: String?): Boolean =
        !spec.isNullOrBlank() && spec.startsWith("data:image/", ignoreCase = true)

    fun isLocalFilePath(spec: String?): Boolean =
        !spec.isNullOrBlank() && (spec.startsWith("/") || spec.startsWith("file://"))

    fun randomBuiltIn(): Pair<BuiltInAvatar, AvatarBackground> =
        avatars.random() to backgrounds.random()
}

@Immutable
data class UserProfile(
    val displayName: String = "",
    val avatarSpec: String = "",
    val avatarWireData: String = "",
    val updatedAt: Long = 0L
) {
    fun effectiveDisplayName(ytUsername: String = "", fallback: String = ""): String {
        val custom = displayName.trim()
        if (custom.isNotEmpty()) return custom
        val yt = ytUsername.trim()
        if (yt.isNotEmpty()) return yt
        return fallback
    }

    fun effectiveLocalAvatar(ytAvatarUrl: String = ""): String {
        if (avatarSpec == ProfileAvatarCatalog.INITIAL_ONLY) return ""
        if (avatarSpec.isNotBlank()) return avatarSpec
        return ytAvatarUrl.trim()
    }

    fun effectiveWireAvatar(ytAvatarUrl: String = ""): String {
        if (avatarSpec == ProfileAvatarCatalog.INITIAL_ONLY) return ""
        if (avatarSpec.startsWith(ProfileAvatarCatalog.PREFIX)) return avatarSpec
        if (avatarWireData.isNotBlank()) return avatarWireData
        if (avatarSpec.startsWith("http://", ignoreCase = true) || avatarSpec.startsWith("https://", ignoreCase = true)) {
            return avatarSpec
        }
        return ytAvatarUrl.trim()
    }
}
