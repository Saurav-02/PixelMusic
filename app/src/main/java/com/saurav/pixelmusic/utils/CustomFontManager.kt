package com.saurav.pixelmusic.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.preferences.AppFontMode
import com.saurav.pixelmusic.ui.theme.ActualGoogleSansRounded
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CatalogFontEntry(
    val id: String,
    val googleFontName: String,
    val displayName: String,
    val group: String,
    val category: String,
    val sampleText: String = "Now Playing · PixelMusic"
)

data class InstalledFontPack(
    val id: String,
    val displayName: String,
    val category: String,
    val isCustomImport: Boolean,
    val regularFileName: String,
    val mediumFileName: String? = null,
    val boldFileName: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

object CustomFontManager {
    private const val FONTS_DIR_NAME = "custom_fonts"
    private const val MANIFEST_FILE_NAME = "installed_fonts.json"

    val filterGroups: List<String> = listOf(
        "All",
        "Vintage",
        "Calligraphy",
        "Artistic",
        "Playful",
        "Futuristic",
        "Pixel"
    )

    private val gmsFontProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val catalogFonts: List<CatalogFontEntry> = listOf(
        // Featured Expressive Picks
        CatalogFontEntry("gf_doto", "Doto", "Doto", "Pixel", "Dot Matrix Pixel"),
        CatalogFontEntry("gf_nova_round", "Nova Round", "Nova Round", "Futuristic", "Rounded Sci-Fi"),
        CatalogFontEntry("gf_tsukimi_rounded", "Tsukimi Rounded", "Tsukimi Rounded", "Playful", "Soft Rounded"),
        CatalogFontEntry("gf_bungee_shade", "Bungee Shade", "Bungee Shade", "Artistic", "3D Shaded Display"),
        CatalogFontEntry("gf_bagel_fat_one", "Bagel Fat One", "Bagel Fat One", "Playful", "Chubby Blobby"),
        CatalogFontEntry("gf_matemasie", "Matemasie", "Matemasie", "Playful", "Chunky Geometric"),
        CatalogFontEntry("gf_rubik_beastly", "Rubik Beastly", "Rubik Beastly", "Artistic", "Wild Monster Display"),
        CatalogFontEntry("gf_agbalumo", "Agbalumo", "Agbalumo", "Vintage", "Playful Vintage"),
        CatalogFontEntry("gf_rubik_glitch", "Rubik Glitch", "Rubik Glitch", "Futuristic", "Cyber Glitch"),
        CatalogFontEntry("gf_rubik_bubbles", "Rubik Bubbles", "Rubik Bubbles", "Playful", "Puffy Bubbles"),

        // Vintage & Decorative
        CatalogFontEntry("gf_special_elite", "Special Elite", "Special Elite", "Vintage", "Distressed Typewriter"),
        CatalogFontEntry("gf_cinzel_decorative", "Cinzel Decorative", "Cinzel Decorative", "Vintage", "Ornate Vintage"),
        CatalogFontEntry("gf_abril_fatface", "Abril Fatface", "Abril Fatface", "Vintage", "Bold Fatface"),
        CatalogFontEntry("gf_rye", "Rye", "Rye", "Vintage", "Wood Type Vintage"),
        CatalogFontEntry("gf_limelight", "Limelight", "Limelight", "Vintage", "Art Deco"),
        CatalogFontEntry("gf_poiret_one", "Poiret One", "Poiret One", "Vintage", "Art Deco Geometric"),
        CatalogFontEntry("gf_lobster", "Lobster", "Lobster", "Vintage", "Retro Script"),
        CatalogFontEntry("gf_playfair_display", "Playfair Display", "Playfair Display", "Vintage", "Classic Editorial"),

        // Calligraphy & Handwritten
        CatalogFontEntry("gf_jim_nightshade", "Jim Nightshade", "Jim Nightshade", "Calligraphy", "Expressive Calligraphy"),
        CatalogFontEntry("gf_dancing_script", "Dancing Script", "Dancing Script", "Calligraphy", "Lively Calligraphy"),
        CatalogFontEntry("gf_pacifico", "Pacifico", "Pacifico", "Calligraphy", "Brush Script"),
        CatalogFontEntry("gf_great_vibes", "Great Vibes", "Great Vibes", "Calligraphy", "Formal Calligraphy"),
        CatalogFontEntry("gf_caveat", "Caveat", "Caveat", "Calligraphy", "Handwritten Brush"),
        CatalogFontEntry("gf_sacramento", "Sacramento", "Sacramento", "Calligraphy", "Monoline Script"),
        CatalogFontEntry("gf_satisfy", "Satisfy", "Satisfy", "Calligraphy", "Vintage Cursive"),
        CatalogFontEntry("gf_permanent_marker", "Permanent Marker", "Permanent Marker", "Calligraphy", "Bold Marker"),
        CatalogFontEntry("gf_indie_flower", "Indie Flower", "Indie Flower", "Calligraphy", "Informal Handwritten"),
        CatalogFontEntry("gf_kalam", "Kalam", "Kalam", "Calligraphy", "Natural Handwriting"),

        // Artistic, Gothic & Medieval
        CatalogFontEntry("gf_unifraktur_maguntia", "UnifrakturMaguntia", "UnifrakturMaguntia", "Artistic", "Blackletter Gothic"),
        CatalogFontEntry("gf_medieval_sharp", "MedievalSharp", "MedievalSharp", "Artistic", "Medieval Fantasy"),
        CatalogFontEntry("gf_pirata_one", "Pirata One", "Pirata One", "Artistic", "Dark Blackletter"),
        CatalogFontEntry("gf_cinzel", "Cinzel", "Cinzel", "Artistic", "Engraved Classical"),
        CatalogFontEntry("gf_cormorant_garamond", "Cormorant Garamond", "Cormorant Garamond", "Artistic", "Sophisticated Serif"),
        CatalogFontEntry("gf_yeseva_one", "Yeseva One", "Yeseva One", "Artistic", "Artistic Display"),
        CatalogFontEntry("gf_righteous", "Righteous", "Righteous", "Artistic", "Retro Display"),
        CatalogFontEntry("gf_monoton", "Monoton", "Monoton", "Artistic", "Inline Neon"),

        // Playful, Blobby & Cute
        CatalogFontEntry("gf_fredoka", "Fredoka", "Fredoka", "Playful", "Soft Blobby"),
        CatalogFontEntry("gf_baloo_2", "Baloo 2", "Baloo 2", "Playful", "Playful Rounded"),
        CatalogFontEntry("gf_bangers", "Bangers", "Bangers", "Playful", "Loud Comic"),
        CatalogFontEntry("gf_chewy", "Chewy", "Chewy", "Playful", "Bouncy Display"),
        CatalogFontEntry("gf_luckiest_guy", "Luckiest Guy", "Luckiest Guy", "Playful", "Bold Cartoon"),
        CatalogFontEntry("gf_amatic_sc", "Amatic SC", "Amatic SC", "Playful", "Quirky Hand-Drawn"),

        // Futuristic & Techno
        CatalogFontEntry("gf_orbitron", "Orbitron", "Orbitron", "Futuristic", "Sci-Fi Futuristic"),
        CatalogFontEntry("gf_audiowide", "Audiowide", "Audiowide", "Futuristic", "Techno Wide"),
        CatalogFontEntry("gf_syne", "Syne", "Syne", "Futuristic", "Avant-Garde Display"),
        CatalogFontEntry("gf_chakra_petch", "Chakra Petch", "Chakra Petch", "Futuristic", "Cyber Mecha"),

        // Pixel & Arcade
        CatalogFontEntry("gf_silkscreen", "Silkscreen", "Silkscreen", "Pixel", "Crisp Pixel"),
        CatalogFontEntry("gf_press_start_2p", "Press Start 2P", "Press Start 2P", "Pixel", "8-Bit Arcade")
    )

    val installedPacks = mutableStateListOf<InstalledFontPack>()
    private val previewFamilyCache = HashMap<String, FontFamily>()
    private val installedFamilyCache = mutableStateMapOf<String, FontFamily>()

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            loadManifest(context.applicationContext)
            isInitialized = true
        }
    }

    fun createDynamicGoogleFontEntry(rawQuery: String): CatalogFontEntry {
        val cleanName = rawQuery.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
            }
        val slug = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return CatalogFontEntry(
            id = "gf_$slug",
            googleFontName = cleanName,
            displayName = cleanName,
            group = "All",
            category = "Google Fonts Search"
        )
    }

    fun getPreviewFontFamily(entry: CatalogFontEntry): FontFamily {
        installedFamilyCache[entry.id]?.let { return it }
        return previewFamilyCache.getOrPut(entry.id) {
            val gFont = GoogleFont(entry.googleFontName)
            FontFamily(
                Font(googleFont = gFont, fontProvider = gmsFontProvider, weight = FontWeight.Normal),
                Font(googleFont = gFont, fontProvider = gmsFontProvider, weight = FontWeight.Medium),
                Font(googleFont = gFont, fontProvider = gmsFontProvider, weight = FontWeight.SemiBold),
                Font(googleFont = gFont, fontProvider = gmsFontProvider, weight = FontWeight.Bold)
            )
        }
    }

    fun resolveFontFamily(fontMode: String): FontFamily {
        return when (fontMode) {
            AppFontMode.APP_DEFAULT -> ActualGoogleSansRounded
            AppFontMode.SYSTEM -> FontFamily.Default
            else -> installedFamilyCache[fontMode] ?: ActualGoogleSansRounded
        }
    }

    fun getDisplayNameForMode(fontMode: String): String {
        return when (fontMode) {
            AppFontMode.APP_DEFAULT -> "PixelMusic Default"
            AppFontMode.SYSTEM -> "System Default"
            else -> {
                installedPacks.firstOrNull { it.id == fontMode }?.displayName
                    ?: catalogFonts.firstOrNull { it.id == fontMode }?.displayName
                    ?: "PixelMusic Default"
            }
        }
    }

    suspend fun downloadCatalogFontPack(
        context: Context,
        entry: CatalogFontEntry
    ): Result<InstalledFontPack> = withContext(Dispatchers.IO) {
        runCatching {
            initialize(context)
            val existing = installedPacks.firstOrNull { it.id == entry.id }
            if (existing != null) return@runCatching existing

            val fontsDir = getFontsDir(context)
            val encodedFamily = URLEncoder.encode(entry.googleFontName, "UTF-8")

            // Try multi-weight CSS2 first, then fall back to single-weight CSS (display/calligraphy fonts often only have 400)
            val weightUrls = mutableMapOf<Int, String>()
            runCatching {
                val css2 = fetchText("https://fonts.googleapis.com/css2?family=$encodedFamily:wght@400;500;700&display=swap")
                weightUrls.putAll(parseTtfUrlsByWeight(css2))
            }
            if (weightUrls.isEmpty()) {
                runCatching {
                    val css2Single = fetchText("https://fonts.googleapis.com/css2?family=$encodedFamily&display=swap")
                    weightUrls.putAll(parseTtfUrlsByWeight(css2Single))
                }
            }
            if (weightUrls.isEmpty()) {
                val fallbackCss = fetchText("https://fonts.googleapis.com/css?family=$encodedFamily")
                weightUrls.putAll(parseTtfUrlsByWeight(fallbackCss))
            }

            val regularUrl = weightUrls[400] ?: weightUrls.values.firstOrNull()
                ?: throw IllegalStateException("Font \"${entry.displayName}\" was not found on Google Fonts.")
            val mediumUrl = weightUrls[500]
            val boldUrl = weightUrls[700]

            val regFile = File(fontsDir, "${entry.id}_400.ttf")
            downloadToFile(regularUrl, regFile)
            validateFontFile(regFile)

            val medFile = if (mediumUrl != null && mediumUrl != regularUrl) {
                val f = File(fontsDir, "${entry.id}_500.ttf")
                runCatching {
                    downloadToFile(mediumUrl, f)
                    validateFontFile(f)
                    f
                }.getOrNull()
            } else null

            val boldFile = if (boldUrl != null && boldUrl != regularUrl) {
                val f = File(fontsDir, "${entry.id}_700.ttf")
                runCatching {
                    downloadToFile(boldUrl, f)
                    validateFontFile(f)
                    f
                }.getOrNull()
            } else null

            val pack = InstalledFontPack(
                id = entry.id,
                displayName = entry.displayName,
                category = entry.category,
                isCustomImport = false,
                regularFileName = regFile.name,
                mediumFileName = medFile?.name,
                boldFileName = boldFile?.name
            )
            val family = buildFontFamilyFromPack(fontsDir, pack)
                ?: throw IllegalStateException("Downloaded font could not be loaded.")

            withContext(Dispatchers.Main) {
                installedFamilyCache[pack.id] = family
                installedPacks.removeAll { it.id == pack.id }
                installedPacks.add(0, pack)
            }
            saveManifest(context)
            pack
        }
    }

    suspend fun importCustomFontFromUri(
        context: Context,
        uri: Uri
    ): Result<InstalledFontPack> = withContext(Dispatchers.IO) {
        runCatching {
            initialize(context)

            val rawName = queryDisplayName(context, uri) ?: "Custom Font"
            val ext = rawName.substringAfterLast('.', "ttf").lowercase()
            if (ext != "ttf" && ext != "otf") {
                throw IllegalArgumentException("Please select a valid .ttf or .otf font file.")
            }

            val cleanDisplayName = rawName
                .substringBeforeLast('.')
                .replace(Regex("[_\\-]+"), " ")
                .trim()
                .ifBlank { "Custom Font" }

            val id = "custom_${System.currentTimeMillis()}"
            val fontsDir = getFontsDir(context)
            val targetFile = File(fontsDir, "$id.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to read selected font file.")

            validateFontFile(targetFile)

            val pack = InstalledFontPack(
                id = id,
                displayName = cleanDisplayName,
                category = "Imported Font",
                isCustomImport = true,
                regularFileName = targetFile.name
            )
            val family = buildFontFamilyFromPack(fontsDir, pack)
                ?: throw IllegalStateException("Invalid or unsupported font file.")

            withContext(Dispatchers.Main) {
                installedFamilyCache[pack.id] = family
                installedPacks.add(0, pack)
            }
            saveManifest(context)
            pack
        }
    }

    suspend fun deleteFontPack(
        context: Context,
        packId: String
    ): Boolean = withContext(Dispatchers.IO) {
        initialize(context)
        val pack = installedPacks.firstOrNull { it.id == packId } ?: return@withContext false
        val fontsDir = getFontsDir(context)
        runCatching { File(fontsDir, pack.regularFileName).delete() }
        pack.mediumFileName?.let { runCatching { File(fontsDir, it).delete() } }
        pack.boldFileName?.let { runCatching { File(fontsDir, it).delete() } }

        withContext(Dispatchers.Main) {
            installedPacks.removeAll { it.id == packId }
            installedFamilyCache.remove(packId)
        }
        saveManifest(context)
        true
    }

    private fun getFontsDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun validateFontFile(file: File) {
        if (!file.exists() || file.length() < 128L) {
            file.delete()
            throw IllegalStateException("Font file is empty or incomplete.")
        }
        val typeface = runCatching { Typeface.createFromFile(file) }.getOrNull()
        if (typeface == null || typeface == Typeface.DEFAULT) {
            file.delete()
            throw IllegalStateException("Corrupted or unsupported font file.")
        }
    }

    private fun buildFontFamilyFromPack(fontsDir: File, pack: InstalledFontPack): FontFamily? {
        return runCatching {
            val regFile = File(fontsDir, pack.regularFileName)
            if (!regFile.exists()) return null
            val medFile = pack.mediumFileName?.let { File(fontsDir, it) }?.takeIf { it.exists() } ?: regFile
            val boldFile = pack.boldFileName?.let { File(fontsDir, it) }?.takeIf { it.exists() } ?: medFile

            FontFamily(
                androidx.compose.ui.text.font.Font(file = regFile, weight = FontWeight.Light),
                androidx.compose.ui.text.font.Font(file = regFile, weight = FontWeight.Normal),
                androidx.compose.ui.text.font.Font(file = medFile, weight = FontWeight.Medium),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.SemiBold),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.Bold)
            )
        }.getOrNull()
    }

    private fun loadManifest(context: Context) {
        val fontsDir = getFontsDir(context)
        val manifestFile = File(fontsDir, MANIFEST_FILE_NAME)
        if (!manifestFile.exists()) return
        runCatching {
            val arr = JSONArray(manifestFile.readText())
            val loadedPacks = mutableListOf<InstalledFontPack>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pack = InstalledFontPack(
                    id = obj.getString("id"),
                    displayName = obj.getString("displayName"),
                    category = obj.optString("category", "Downloaded Font"),
                    isCustomImport = obj.optBoolean("isCustomImport", false),
                    regularFileName = obj.getString("regularFileName"),
                    mediumFileName = obj.optString("mediumFileName", "").takeIf { it.isNotBlank() },
                    boldFileName = obj.optString("boldFileName", "").takeIf { it.isNotBlank() },
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                )
                val family = buildFontFamilyFromPack(fontsDir, pack)
                if (family != null) {
                    installedFamilyCache[pack.id] = family
                    loadedPacks.add(pack)
                }
            }
            installedPacks.clear()
            installedPacks.addAll(loadedPacks)
        }
    }

    private fun saveManifest(context: Context) {
        runCatching {
            val fontsDir = getFontsDir(context)
            val manifestFile = File(fontsDir, MANIFEST_FILE_NAME)
            val arr = JSONArray()
            for (pack in installedPacks) {
                val obj = JSONObject().apply {
                    put("id", pack.id)
                    put("displayName", pack.displayName)
                    put("category", pack.category)
                    put("isCustomImport", pack.isCustomImport)
                    put("regularFileName", pack.regularFileName)
                    put("mediumFileName", pack.mediumFileName ?: "")
                    put("boldFileName", pack.boldFileName ?: "")
                    put("addedAt", pack.addedAt)
                }
                arr.put(obj)
            }
            manifestFile.writeText(arr.toString())
        }
    }

    private fun fetchText(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            // Old Android User-Agent instructs Google Fonts CSS API to return direct .ttf URLs instead of .woff2
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; U; Android 4.1.1; en-gb; Build/KLP) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30"
            )
        }
        return try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("Network error (${conn.responseCode}) while fetching font metadata.")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadToFile(urlString: String, target: File) {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("Failed to download font file (HTTP ${conn.responseCode}).")
            }
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseTtfUrlsByWeight(css: String): MutableMap<Int, String> {
        val result = mutableMapOf<Int, String>()
        val blockRegex = Regex("@font-face\\s*\\{([^}]+)\\}")
        val weightRegex = Regex("font-weight:\\s*(\\d+)")
        val urlRegex = Regex("url\\((https://[^)]+\\.(?:ttf|otf))\\)")

        for (match in blockRegex.findAll(css)) {
            val block = match.groupValues[1]
            val weight = weightRegex.find(block)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 400
            val url = urlRegex.find(block)?.groupValues?.getOrNull(1)
            if (url != null && !result.containsKey(weight)) {
                result[weight] = url
            }
        }
        return result
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }
}
