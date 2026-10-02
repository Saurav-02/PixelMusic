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
        SearchableSetting("Color Palette", "Dynamic, Album Art, Sage, Purple, Blue, Orange, Yellow",
            listOf("color", "palette", "dynamic", "material you", "theme", "monochrome"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Background Wallpaper", "Default, Music Notes, Live Blur, or Custom Image from Gallery",
            listOf("background", "wallpaper", "image", "blur", "photo", "gallery", "custom"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Wallpaper Opacity", "Adjust opacity of custom background wallpaper",
            listOf("opacity", "transparent", "background", "wallpaper", "dim"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Wallpaper Blur", "Adjust blur intensity of background wallpaper",
            listOf("blur", "background", "wallpaper", "intensity", "glass"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Player Theme", "Album Art or Dynamic system colors for Now Playing",
            listOf("player theme", "now playing", "dynamic", "album art", "color"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Player Design Style", "Default, Immersive, or Immersive Extended",
            listOf("player", "design", "immersive", "now playing", "layout", "extended"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Immersive Lyrics Display", "Karaoke, Word by Word, Single Word Pop, Blur Focus, Gradient Sweep, or Slide & Fade",
            listOf("lyrics", "immersive", "karaoke", "word by word", "blur", "sweep", "display"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Show File Info", "Display audio format, bitrate, and codec in player",
            listOf("file info", "format", "bitrate", "codec", "flac", "mp3", "info", "tags"),
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
        SearchableSetting("Library Navigation Mode", "Tab Row or Compact Pill navigation in Library",
            listOf("library", "navigation", "tabs", "pill", "mode", "layout"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Collage Pattern", "Home screen album art collage layout",
            listOf("collage", "home", "pattern", "layout", "grid"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Auto Rotate Patterns", "Automatically rotate home screen collage patterns",
            listOf("collage", "rotate", "pattern", "home", "shuffle"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Carousel Style", "Peek mode for album carousels",
            listOf("carousel", "peek", "album"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Immersive Lyrics", "Fullscreen lyrics view in Now Playing",
            listOf("lyrics", "immersive", "fullscreen", "timed"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Auto Hide Delay", "Delay before controls auto-hide in lyrics view",
            listOf("auto hide", "delay", "timeout", "lyrics", "timer"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Performance Mode", "Optimizes animations, blurs, and images for budget devices and battery saving",
            listOf("performance", "battery", "low-end", "budget", "lag", "fast", "light", "optimization"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Hardware Audio Offload", "Delegate audio decoding to DSP to save battery",
            listOf("offload", "hardware", "dsp", "audio", "battery", "power", "cpu"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Palette Style", "Album art color palette & accuracy",
            listOf("palette", "color", "style", "accuracy", "album art"),
            SettingsCategory.APPEARANCE, Screen.PaletteStyle.route),
        SearchableSetting("Navigation Bar Corner Radius", "Adjust corner curvature of the bottom navigation bar",
            listOf("nav bar", "corner", "radius", "curvature", "bottom bar", "pill"),
            SettingsCategory.APPEARANCE, Screen.NavBarCrRad.route),

        // ─── Playback ───────────────────────────────────────────────────────────────
        SearchableSetting("Keep Playing in Background", "Continue audio playback when the app is minimized or screen is off",
            listOf("background", "keep playing", "minimize", "screen off", "playback"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Dynamic Island", "Live track pill in the status bar",
            listOf("dynamic island", "status bar", "origin os", "notch", "pill"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Island Visualizer Style", "Animated Notes, Progress Time, or Static Icon in dynamic island pill",
            listOf("island", "visualizer", "dynamic island", "pill", "notes", "progress", "style"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("AOD Screen", "Ambient glowing Now Playing view",
            listOf("aod", "always on", "ambient", "glow", "amoled"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Battery Optimization", "Allow playback in the background",
            listOf("battery", "optimization", "background", "keep alive"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Replay Gain", "Normalize volume across tracks",
            listOf("replaygain", "volume", "normalize", "loudness", "gain"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Gain Mode", "Apply track gain or album gain normalization",
            listOf("replay gain", "album gain", "track gain", "loudness", "volume", "normalize"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Cast Autoplay", "Prevent Chromecast from automatically playing the next queue track",
            listOf("cast", "chromecast", "autoplay", "queue", "tv", "stream"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Headphone Resume", "Resume playback when headphones reconnect",
            listOf("headphone", "resume", "headset", "bluetooth", "jack"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Crossfade", "Smooth transition between tracks",
            listOf("crossfade", "transition", "gapless", "smooth"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Crossfade Duration", "Set duration of track crossfade transition (1s to 12s)",
            listOf("crossfade", "duration", "transition", "seconds", "time", "overlap"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Hi-Fi Mode", "High-quality audio playback",
            listOf("hifi", "high quality", "lossless", "audio", "hi-res"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Persistent Shuffle", "Remember shuffle state between sessions",
            listOf("shuffle", "persistent", "random"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Show Queue History", "Display previously played songs in the queue sheet",
            listOf("queue", "history", "played", "previous", "songs", "list"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Auto Queue", "Continue with recommendations",
            listOf("auto queue", "autoqueue", "recommend", "continue", "radio"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Avoid Repetitive Songs", "Don't repeat songs too often",
            listOf("avoid", "repeat", "repetitive", "duplicate"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Preload Queue", "Pre-buffer upcoming tracks",
            listOf("preload", "buffer", "queue", "cache"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Preload Queue Size", "Number of upcoming tracks to pre-buffer (1 to 10 songs)",
            listOf("preload", "buffer", "queue", "cache", "size", "tracks"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Streaming Audio Quality (Wi-Fi)", "Bitrate for streaming over Wi-Fi",
            listOf("streaming", "quality", "bitrate", "audio", "wifi"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Streaming Audio Quality (Mobile)", "Bitrate for streaming over mobile data",
            listOf("streaming", "quality", "bitrate", "audio", "mobile", "cellular", "data"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Force High Quality on Mobile", "Always use highest audio bitrate even on mobile data",
            listOf("high quality", "mobile data", "cellular", "bitrate", "stream"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Download on Like", "Auto-download liked YouTube songs to storage",
            listOf("download", "like", "offline", "favorite"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Pure YouTube Music", "Filter out non-music video content and show audio only",
            listOf("pure", "music", "filter", "video", "youtube", "audio only"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),

        // ─── Listen Together ────────────────────────────────────────────────────────
        SearchableSetting("Listen Together", "Host or join synchronized music sessions with up to 4 people",
            listOf("listen together", "party", "session", "share", "room", "friends", "code", "group"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),
        SearchableSetting("Compact Member List", "Collapse participants into a dense horizontal avatar strip",
            listOf("listen together", "compact", "members", "participants", "avatars"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),
        SearchableSetting("Animated Reactions", "Display floating emoji reaction animations",
            listOf("listen together", "reactions", "animations", "emojis"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),
        SearchableSetting("Chat & Social Bar", "Show quick chat reaction chips and song request controls",
            listOf("listen together", "chat", "social", "requests"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),
        SearchableSetting("Keep Session Alive", "Maintain realtime synchronization in background",
            listOf("listen together", "background", "connection", "sync", "keep alive"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),
        SearchableSetting("Automatic Drift Correction", "Dynamic playback rate adjustment ensures all listeners stay in frame-accurate sync",
            listOf("listen together", "drift", "sync", "frame-accurate", "tempo", "pitch", "latency", "delay"),
            SettingsCategory.LISTEN_TOGETHER, Screen.ListenTogetherSettings.route),

        // ─── Library ────────────────────────────────────────────────────────────────
        SearchableSetting("Excluded Directories", "Folders to ignore while scanning",
            listOf("excluded", "folders", "directories", "ignore", "scan", "hide"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Artist Settings", "Multi-artist parsing, delimiters & organization",
            listOf("artist", "delimiters", "parsing", "group", "multi"),
            SettingsCategory.LIBRARY, Screen.ArtistSettings.route),
        SearchableSetting("Character Delimiters", "Split multi-artist strings by characters like commas, slashes, or ampersands",
            listOf("artist", "delimiter", "character", "split", "slash", "comma", "ampersand"),
            SettingsCategory.LIBRARY, Screen.DelimiterConfig.route),
        SearchableSetting("Word Delimiters", "Split multi-artist strings by words like feat., ft., vs., or with",
            listOf("artist", "delimiter", "word", "feat", "featuring", "ft", "vs", "with"),
            SettingsCategory.LIBRARY, Screen.WordDelimiterConfig.route),
        SearchableSetting("Extract Artists from Title", "Detect and extract featured artists enclosed in song titles",
            listOf("artist", "title", "featured", "feat", "extract", "title parsing"),
            SettingsCategory.LIBRARY, Screen.ArtistSettings.route),
        SearchableSetting("Group by Album Artist", "Organize library by album artist instead of track artist",
            listOf("album artist", "group", "organize", "compilation", "artist"),
            SettingsCategory.LIBRARY, Screen.ArtistSettings.route),
        SearchableSetting("Min Song Duration", "Skip tracks shorter than this duration",
            listOf("duration", "minimum", "short", "skip"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Min Tracks per Album", "Hide albums with fewer tracks",
            listOf("album", "tracks", "minimum", "hide"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Album Art Cache Limit", "Maximum disk space used for album artwork cache",
            listOf("album art", "cache", "limit", "storage", "size", "disk"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Music Storage Limit", "Maximum storage limit for cached music files or Unlimited",
            listOf("storage", "limit", "cache", "quota", "max space", "gb", "mb"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Clear Streaming Cache", "Free up space used by cached streaming audio",
            listOf("cache", "clear", "storage", "space", "clean"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Full Rescan", "Rescan all music files from scratch",
            listOf("rescan", "refresh", "library", "sync", "scan", "index"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Rebuild Database", "Clear and rebuild the library database",
            listOf("rebuild", "database", "clear", "reset"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Auto Scan LRC Files", "Discover local .lrc lyrics files alongside tracks",
            listOf("lyrics", "lrc", "auto scan", "subtitles"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Lyrics Source Priority", "Embedded, Online, or Local lyrics first",
            listOf("lyrics", "source", "priority", "embedded", "api", "local"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("AI Lyrics Search", "Extract clean song titles with AI when standard search finds no lyrics",
            listOf("ai lyrics", "rescue", "lyrics search", "gemini", "title cleanup", "subtitle"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Reset Imported Lyrics", "Clear all imported synchronized lyrics",
            listOf("lyrics", "reset", "clear", "imported"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Album Art Quality (Wi-Fi)", "High or Standard resolution artwork over Wi-Fi",
            listOf("album art", "quality", "wifi", "resolution", "cover"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Album Art Quality (Mobile)", "High or Standard resolution artwork over cellular data",
            listOf("album art", "quality", "mobile", "cellular", "data", "resolution"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Download Audio Quality", "Select default audio bitrate for offline music downloads",
            listOf("download", "quality", "offline", "bitrate", "audio"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Embed Rich Metadata & Lyrics", "Embed full song tags, high-res artwork, and synchronized lyrics directly into downloaded files",
            listOf("embed", "metadata", "tags", "lyrics", "cover art", "id3", "download"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),

        // ─── Content ────────────────────────────────────────────────────────────────
        SearchableSetting("Content Language", "Preferred language for YouTube Music recommendations",
            listOf("language", "content", "youtube", "region"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Content Country", "Region preference for YouTube Music content",
            listOf("country", "region", "content", "location"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Hide Explicit", "Hide tracks marked as explicit from search and feeds",
            listOf("explicit", "hide", "clean", "censored"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Hide Video", "Hide video content from YouTube Music search results",
            listOf("video", "hide", "audio only"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("My Top Playlist Size", "Number of tracks included in My Top playlist (20, 50, or 100)",
            listOf("my top", "top songs", "size", "count", "playlist", "limit"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Quick Picks", "Discover, Last Listen, or Don't Show",
            listOf("quick picks", "discover", "recommendations", "suggestions"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Quick Picks Display", "Card carousel or List grid layout",
            listOf("quick picks", "display", "card", "list", "layout"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),
        SearchableSetting("Playlist Suggestions Source", "Playlist title, content, or both for smart suggestions",
            listOf("playlist", "suggestion", "recommend", "smart"),
            SettingsCategory.CONTENT, Screen.SettingsCategory.createRoute(SettingsCategory.CONTENT.id)),

        // ─── Behavior ───────────────────────────────────────────────────────────────
        SearchableSetting("Folder Back Gesture", "Navigate up a folder with back gesture",
            listOf("folder", "back", "gesture", "navigate"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),
        SearchableSetting("Tap Background to Close", "Close Now Playing by tapping outside",
            listOf("tap", "background", "close", "player", "dismiss"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),
        SearchableSetting("Haptic Feedback", "Vibration feedback on app interactions",
            listOf("haptic", "vibrate", "feedback", "touch"),
            SettingsCategory.BEHAVIOR, Screen.SettingsCategory.createRoute(SettingsCategory.BEHAVIOR.id)),

        // ─── AI Integration ─────────────────────────────────────────────────────────
        SearchableSetting("AI Provider", "Gemini, DeepSeek, Groq, Mistral, OpenAI & more",
            listOf("ai", "provider", "gemini", "deepseek", "groq", "openai", "mistral", "nvidia", "kimi", "glm", "openrouter"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI API Key", "Credentials for your AI provider",
            listOf("ai", "api key", "credentials", "token", "key"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI Model Selection", "Choose which model to use for your selected AI provider",
            listOf("ai model", "gemini", "deepseek", "groq", "gpt", "model", "llm"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI System Prompt", "Customize assistant personality and instructions",
            listOf("ai", "system prompt", "personality", "customize", "prompt"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI Usage Report", "Token consumption statistics and history",
            listOf("ai", "usage", "tokens", "statistics", "report", "consumption"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("Safe Token Limit", "Prevent excessive token usage",
            listOf("ai", "token", "limit", "safe", "quota"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),

        // ─── Backup & Restore ───────────────────────────────────────────────────────
        SearchableSetting("Export Backup", "Save playlists, settings, and favorites to a file",
            listOf("export", "backup", "save", "restore", "file"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),
        SearchableSetting("Import Backup", "Restore app data and playlists from a backup file",
            listOf("import", "restore", "backup", "recover", "load"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),

        // ─── Accounts & Cloud ───────────────────────────────────────────────────────
        SearchableSetting("Connected Accounts", "Manage linked streaming, cloud, and scrobbling services",
            listOf("accounts", "google", "youtube", "spotify", "discord", "telegram", "cloud", "login", "sync"),
            null, Screen.Accounts.route),
        SearchableSetting("YouTube Music Account", "Log in to access your YouTube Music library, playlists, and likes",
            listOf("youtube", "google", "account", "login", "sign in", "channel", "yt music"),
            null, Screen.Accounts.route),
        SearchableSetting("Spotify Account", "Connect Spotify account for playlist and library importing",
            listOf("spotify", "account", "sync", "connect", "import"),
            null, Screen.Accounts.route),
        SearchableSetting("Discord Rich Presence", "Show what you're currently listening to on Discord status",
            listOf("discord", "rpc", "rich presence", "status", "activity"),
            null, Screen.Accounts.route),
        SearchableSetting("Telegram Cloud Storage", "Stream and download music directly from Telegram channels and chats",
            listOf("telegram", "cloud", "storage", "stream", "channel", "bot"),
            null, Screen.Accounts.route),
        SearchableSetting("Last.fm Login", "Connect your Last.fm account for tracking plays",
            listOf("lastfm", "last.fm", "login", "account"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),
        SearchableSetting("Last.fm Scrobbling", "Auto-scrobble played tracks to Last.fm profile",
            listOf("scrobble", "lastfm", "track", "history"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),

        // ─── Equalizer ──────────────────────────────────────────────────────────────
        SearchableSetting("Equalizer", "Launch system audio effects and sound enhancement panel",
            listOf("equalizer", "eq", "audio", "bass", "treble", "dolby", "sound", "effects"),
            SettingsCategory.EQUALIZER, Screen.SettingsCategory.createRoute(SettingsCategory.EQUALIZER.id)),

        // ─── Device Capabilities ────────────────────────────────────────────────────
        SearchableSetting("Device Capabilities", "Hardware decoding, sample rates, and audio output capabilities",
            listOf("device", "capabilities", "hardware", "audio", "specs", "dsp", "dac"),
            SettingsCategory.DEVICE_CAPABILITIES, Screen.DeviceCapabilities.route),
        SearchableSetting("Feature Compatibility", "OS and platform feature support matrix (Blur, AGSL, Dynamic Color)",
            listOf("compatibility", "features", "incompatible", "unsupported", "blur", "agsl", "android"),
            SettingsCategory.DEVICE_CAPABILITIES, Screen.FeatureCompatibility.route),

        // ─── Developer & Experimental ───────────────────────────────────────────────
        SearchableSetting("Experimental Features", "Full player tweaks, delays, and animation options",
            listOf("experimental", "developer", "beta", "tweak", "advanced"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Animated Lyrics", "Smoothly animated synchronized lyrics in player",
            listOf("animated lyrics", "experimental", "sync", "smooth", "lyric"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Lyrics Background Blur", "Real-time hardware blur behind synchronized lyrics",
            listOf("blur", "lyrics", "hardware", "experimental", "background"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Player Open Delay Tweaks", "Fine-tune transition delays for player sheet components",
            listOf("delay", "smooth", "transition", "player open", "animation", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Player Placeholders", "Show animated skeleton placeholders while player expands",
            listOf("placeholder", "skeleton", "transition", "player", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Transparent Placeholders", "Use translucent placeholders during player transition",
            listOf("transparent", "placeholder", "player", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Album Art Resolution", "Quality of downloaded album art",
            listOf("album art", "resolution", "quality", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Universal App Logging", "Record all app activity to logcat and internal log file",
            listOf("log", "logging", "debug", "verbose", "logcat", "diagnostics"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Export App Logs", "Share or save the recorded app log file (pixelmusic.log)",
            listOf("export logs", "share logs", "bug report", "debug", "file"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Clear App Logs", "Wipe the current log buffer and recorded log file",
            listOf("clear logs", "wipe", "reset logs", "clean", "debug"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Force Daily Mix", "Instantly regenerate your algorithmic Daily Mix",
            listOf("daily mix", "regenerate", "force", "maintenance", "refresh mix"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Force Stats Calculation", "Recalculate listening statistics and history graphs",
            listOf("stats", "statistics", "recalculate", "force", "history"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Force Palette Extraction", "Regenerate dynamic color palettes for songs and albums",
            listOf("palette", "colors", "extract", "regenerate", "artwork"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),

        // ─── About ──────────────────────────────────────────────────────────────────
        SearchableSetting("About PixelMusic", "App version, license information, and changelog",
            listOf("about", "version", "changelog", "info", "update", "github", "developer"),
            SettingsCategory.ABOUT, Screen.About.route)
    )

    // ─── Search ─────────────────────────────────────────────────────────────────────

    fun search(query: String): List<SearchableSetting> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return entries
            .asSequence()
            .filter { isSupportedOnCurrentDevice(it) }
            .map { entry -> entry to score(entry, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()
    }

    private fun isSupportedOnCurrentDevice(entry: SearchableSetting): Boolean {
        return when (entry.title) {
            "Motion Blur" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsMotionBlur
            "Dynamic Island", "Island Visualizer Style" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsLiveNotificationOrIsland()
            "Wallpaper Blur", "Lyrics Background Blur" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsHardwareBlur
            else -> true
        }
    }

    /**
     * Scoring:
     *  - Exact title match → 200
     *  - Title starts with query → 150
     *  - Title contains query → 100
     *  - Subtitle contains query → 40
     *  - Keyword exact match → 80, starts with → 50, contains → 25
     *  - Fallback fuzzy (Levenshtein distance ≤ 2) for typos
     */
    private fun score(entry: SearchableSetting, q: String): Int {
        var s = 0
        val title = entry.title.lowercase()
        val subtitle = entry.subtitle.lowercase()

        if (title == q) s += 200
        else if (title.startsWith(q)) s += 150
        else if (title.contains(q)) s += 100

        if (subtitle.contains(q)) s += 40

        entry.keywords.forEach { kw ->
            val lkw = kw.lowercase()
            if (lkw == q) s += 80
            else if (lkw.startsWith(q)) s += 50
            else if (lkw.contains(q)) s += 25
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
