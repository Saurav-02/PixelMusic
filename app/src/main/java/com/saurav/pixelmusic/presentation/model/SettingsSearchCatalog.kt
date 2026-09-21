package com.saurav.pixelmusic.presentation.model

import com.saurav.pixelmusic.presentation.navigation.Screen

data class SearchableSetting(
    val title: String,
    val subtitle: String,
    val keywords: List<String>,
    val category: SettingsCategory?,
    val route: String
)

object SettingsSearchCatalog {

    val entries: List<SearchableSetting> = listOf(
        // ─── Appearance ─────────────────────────────────────────────────────────────
        SearchableSetting("App Theme", "Light, Dark, or Follow System",
            listOf("theme", "dark", "light", "night", "day", "mode"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("AMOLED Black", "Pure black background in dark mode",
            listOf("amoled", "black", "oled", "pure dark", "battery"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Font", "PixelMusic font or system font",
            listOf("font", "typeface", "text", "typography"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Color Palette", "Dynamic, Sage, Purple, Blue, Orange, Yellow",
            listOf("color", "palette", "dynamic", "material you", "theme"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Player Design Style", "Default, Immersive, or Immersive Extended",
            listOf("player", "design", "immersive", "now playing", "layout"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Language", "Change the app display language",
            listOf("language", "locale", "english", "translate"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Motion Blur", "Cinematic blur when scrolling lists",
            listOf("motion", "blur", "scroll", "animation"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Smooth Corners", "Softer rounded corner shapes",
            listOf("corner", "rounded", "smooth", "shape", "curves"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Launch Tab", "Tab the app opens to by default",
            listOf("launch", "start", "default", "home", "open"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Collage Pattern", "Home screen album art collage layout",
            listOf("collage", "home", "pattern", "layout", "grid"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Carousel Style", "Peek mode for album carousels",
            listOf("carousel", "peek", "album"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Palette Style", "Album art color palette & accuracy",
            listOf("palette", "color", "style", "accuracy", "album art"),
            SettingsCategory.APPEARANCE, Screen.PaletteStyle.route),

        // ─── Playback ───────────────────────────────────────────────────────────────
        SearchableSetting("Dynamic Island", "Live track pill in the status bar",
            listOf("dynamic island", "status bar", "origin os", "notch", "pill"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("AOD Screen", "Ambient glowing Now Playing view",
            listOf("aod", "always on", "ambient", "glow", "amoled"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Crossfade", "Smooth transition between tracks",
            listOf("crossfade", "transition", "gapless", "smooth"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Hi-Fi Mode", "High-quality audio playback",
            listOf("hifi", "high quality", "lossless", "audio", "hi-res"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Replay Gain", "Normalize volume across tracks",
            listOf("replaygain", "volume", "normalize", "loudness", "gain"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Streaming Audio Quality", "Bitrate for Wi-Fi and mobile streaming",
            listOf("streaming", "quality", "bitrate", "audio", "wifi", "mobile"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Headphone Resume", "Resume playback when headphones reconnect",
            listOf("headphone", "resume", "headset", "bluetooth", "jack"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Persistent Shuffle", "Remember shuffle state between sessions",
            listOf("shuffle", "persistent", "random"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Preload Queue", "Pre-buffer upcoming tracks",
            listOf("preload", "buffer", "queue", "cache"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Auto Queue", "Continue with recommendations",
            listOf("auto queue", "autoqueue", "recommend", "continue", "radio"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Avoid Repetitive Songs", "Don't repeat songs too often",
            listOf("avoid", "repeat", "repetitive", "duplicate"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Pure YouTube Music", "Filter out non-music video content",
            listOf("pure", "music", "filter", "video", "youtube"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Download on Like", "Auto-download liked YouTube songs",
            listOf("download", "like", "offline", "favorite"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Battery Optimization", "Allow playback in the background",
            listOf("battery", "optimization", "background", "keep alive"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),

        // ─── Library ────────────────────────────────────────────────────────────────
        SearchableSetting("Excluded Directories", "Folders to ignore while scanning",
            listOf("excluded", "folders", "directories", "ignore", "scan", "hide"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Artist Settings", "Multi-artist parsing & delimiters",
            listOf("artist", "delimiters", "parsing", "group", "multi"),
            SettingsCategory.LIBRARY, Screen.ArtistSettings.route),
        SearchableSetting("Full Rescan", "Rescan all music files from scratch",
            listOf("rescan", "refresh", "library", "sync", "scan", "index"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Rebuild Database", "Clear and rebuild the library",
            listOf("rebuild", "database", "clear", "reset"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Min Song Duration", "Skip tracks shorter than this",
            listOf("duration", "minimum", "short", "skip"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Min Tracks per Album", "Hide albums with fewer tracks",
            listOf("album", "tracks", "minimum", "hide"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Album Art Cache Limit", "Maximum size of the art cache",
            listOf("album art", "cache", "limit", "storage", "size"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Clear Streaming Cache", "Free up space used by cached audio",
            listOf("cache", "clear", "storage", "space", "clean"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Auto Scan LRC Files", "Discover local .lrc lyrics files",
            listOf("lyrics", "lrc", "auto scan", "subtitles"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Lyrics Source Priority", "Embedded, Online, or Local first",
            listOf("lyrics", "source", "priority", "embedded", "api", "local"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Reset Imported Lyrics", "Clear all imported lyrics",
            listOf("lyrics", "reset", "clear", "imported"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),

        // ─── Content ────────────────────────────────────────────────────────────────
        SearchableSetting("Content Language", "Preferred language for YouTube Music",
            listOf("language", "content", "youtube", "region"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Content Country", "Region preference for YouTube Music",
            listOf("country", "region", "content", "location"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Hide Explicit", "Hide tracks marked as explicit",
            listOf("explicit", "hide", "clean", "censored"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Hide Video", "Hide video content from results",
            listOf("video", "hide", "audio only"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Quick Picks", "Discover, Last Listen, or Don't Show",
            listOf("quick picks", "discover", "recommendations", "suggestions"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Quick Picks Display", "Card carousel or List grid",
            listOf("quick picks", "display", "card", "list", "layout"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Playlist Suggestions Source", "Playlist title, content, or both",
            listOf("playlist", "suggestion", "recommend"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),

        // ─── Behavior ───────────────────────────────────────────────────────────────
        SearchableSetting("Folder Back Gesture", "Navigate up a folder with back gesture",
            listOf("folder", "back", "gesture", "navigate"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),
        SearchableSetting("Tap Background to Close", "Close Now Playing by tapping outside",
            listOf("tap", "background", "close", "player", "dismiss"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),
        SearchableSetting("Haptic Feedback", "Vibration on interactions",
            listOf("haptic", "vibrate", "feedback", "touch"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),

        // ─── AI ─────────────────────────────────────────────────────────────────────
        SearchableSetting("AI Provider", "Gemini, DeepSeek, Groq, Mistral & more",
            listOf("ai", "provider", "gemini", "deepseek", "groq", "openai", "mistral"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI API Key", "Credentials for your AI provider",
            listOf("ai", "api key", "credentials", "token", "key"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI System Prompt", "Customize how the AI responds",
            listOf("ai", "system prompt", "personality", "customize"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI Usage Report", "Token usage statistics & history",
            listOf("ai", "usage", "tokens", "statistics", "report"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("Safe Token Limit", "Prevent excessive token usage",
            listOf("ai", "token", "limit", "safe"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),

        // ─── Backup & Restore ───────────────────────────────────────────────────────
        SearchableSetting("Export Backup", "Save your app data to a file",
            listOf("export", "backup", "save", "restore"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),
        SearchableSetting("Import Backup", "Restore app data from a file",
            listOf("import", "restore", "backup", "recover"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),

        // ─── Last.fm ────────────────────────────────────────────────────────────────
        SearchableSetting("Last.fm Login", "Connect your Last.fm account",
            listOf("lastfm", "last.fm", "login", "account"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),
        SearchableSetting("Scrobbling", "Auto-scrobble played tracks",
            listOf("scrobble", "lastfm", "track", "history"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),

        // ─── Equalizer ──────────────────────────────────────────────────────────────
        SearchableSetting("Equalizer", "System audio effects",
            listOf("equalizer", "eq", "audio", "bass", "treble", "dolby"),
            SettingsCategory.EQUALIZER, Screen.SettingsCategory.createRoute(SettingsCategory.EQUALIZER.id)),

        // ─── Device Capabilities ────────────────────────────────────────────────────
        SearchableSetting("Device Capabilities", "Hardware and audio capabilities",
            listOf("device", "capabilities", "hardware", "audio", "specs"),
            SettingsCategory.DEVICE_CAPABILITIES, Screen.DeviceCapabilities.route),

        // ─── Developer / Experimental ───────────────────────────────────────────────
        SearchableSetting("Experimental Features", "Full player tweaks & more",
            listOf("experimental", "developer", "beta", "tweak", "advanced"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Album Art Resolution", "Quality of downloaded album art",
            listOf("album art", "resolution", "quality", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),

        // ─── About ──────────────────────────────────────────────────────────────────
        SearchableSetting("About PixelMusic", "App information and changelog",
            listOf("about", "version", "changelog", "info", "update"),
            SettingsCategory.ABOUT, Screen.About.route)
    )

    // ─── Search ─────────────────────────────────────────────────────────────────────

    fun search(query: String): List<SearchableSetting> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return entries
            .asSequence()
            .map { entry -> entry to score(entry, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()
    }

    /**
     * Scoring:
     *  - Substring hits on title / subtitle / keywords → high, differentiated scores
     *  - If nothing matched at all, fall back to fuzzy (Levenshtein distance ≤ 2)
     *    against individual tokens of title + keywords.
     */
    private fun score(entry: SearchableSetting, q: String): Int {
        var s = 0
        val title = entry.title.lowercase()
        val subtitle = entry.subtitle.lowercase()

        if (title.contains(q)) s += 100
        if (subtitle.contains(q)) s += 40
        entry.keywords.forEach { kw ->
            if (kw.lowercase().contains(q)) s += 20
        }

        if (s == 0 && q.length >= 3) {
            // Title tokens — closest match wins
            val titleTokens = title.split(' ', '-', ',', '/', '\n').filter { it.isNotBlank() }
            val bestTitleDist = titleTokens.minOfOrNull { levenshtein(it, q) } ?: Int.MAX_VALUE
            if (bestTitleDist <= 2) s += 60 - (bestTitleDist * 10)

            // Keyword tokens
            entry.keywords.forEach { kw ->
                val kwTokens = kw.lowercase().split(' ', '-', ',', '/', '\n').filter { it.isNotBlank() }
                val dist = kwTokens.minOfOrNull { levenshtein(it, q) } ?: Int.MAX_VALUE
                if (dist <= 2) s += 30 - (dist * 10)
            }
        }

        return s
    }

    /** Standard Levenshtein distance — works fine for our short tokens. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) prev
                else 1 + minOf(prev, dp[j], dp[j - 1])
                prev = temp
            }
        }
        return dp[b.length]
    }
}
