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
        SearchableSetting("Background Wallpaper", "Default, Gradient Blur (Material You), Music Notes, Live Blur, or Custom Image for Home & Explore",
            listOf("background", "wallpaper", "image", "blur", "gradient", "material you", "photo", "gallery", "custom", "top half"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Wallpaper Opacity", "Adjust opacity of top-half background wallpaper",
            listOf("opacity", "transparent", "background", "wallpaper", "dim"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Wallpaper Blur", "Adjust blur intensity of background wallpaper",
            listOf("blur", "background", "wallpaper", "intensity", "glass"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Language", "Choose the language used across the app interface",
            listOf("language", "locale", "english", "translate", "app language"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Theme", "Light, Dark, or Follow System appearance",
            listOf("theme", "dark", "light", "night", "day", "mode"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Font", "PixelMusic font or system default font",
            listOf("font", "typeface", "text", "typography"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Color Palette", "Dynamic (System), Live (Album Art), Black & White, Yellow, Peach Coral, Sage Mint, Ocean Blue, Lavender, Rose Quartz, Crimson, Deep Teal, or Mocha",
            listOf("color", "palette", "dynamic", "material you", "theme", "monochrome", "black and white", "yellow", "peach", "sage", "ocean", "lavender", "rose", "crimson", "teal", "mocha"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("App Icon", "Customize your PixelMusic home screen launcher icon",
            listOf("icon", "app icon", "launcher", "logo", "custom icon", "home screen", "peach", "knit", "fabric"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Player Theme", "Album Art or System Dynamic colors for Now Playing",
            listOf("player theme", "now playing", "dynamic", "album art", "color"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Player Design Style", "Default, Immersive, Immersive Extended, Complex, Wave, Pillars, or Expressive layout for Now Playing",
            listOf("player", "design", "immersive", "now playing", "layout", "extended", "complex", "wave", "pillars", "expressive"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Show player file info", "Show codec, bitrate, and sample rate in the player progress section",
            listOf("file info", "format", "bitrate", "codec", "sample rate", "flac", "mp3", "info", "tags"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Album Art Palette Style", "Tonal Spot, Vibrant, Expressive, or Fruit Salad album color palette & accuracy",
            listOf("palette", "color", "style", "accuracy", "album art", "tonal", "vibrant", "expressive"),
            SettingsCategory.APPEARANCE, Screen.PaletteStyle.route),
        SearchableSetting("Default Tab", "Choose which tab (Home, Explore, Library, or Search) opens when PixelMusic starts",
            listOf("default", "tab", "launch", "startup", "start", "home", "explore", "library", "search", "navigation", "opening"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Library Navigation", "Tab row or Compact pill & grid navigation in Library",
            listOf("library", "navigation", "tabs", "pill", "mode", "layout"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Motion Blur", "Applies a cinematic directional blur when scrolling lists",
            listOf("motion", "blur", "scroll", "animation", "velocity"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Blur Intensity", "Adjust directional scroll motion blur intensity (Subtle to Extreme)",
            listOf("blur", "intensity", "motion blur", "scroll", "strength"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Performance Mode (Low-End & Battery Saver)", "Optimizes animations, blurs, and downsamples images for budget devices and battery saving",
            listOf("performance", "battery", "low-end", "budget", "lag", "fast", "light", "optimization", "saver"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Hardware Audio Offload", "Delegate audio decoding to hardware DSP to save battery during playback",
            listOf("offload", "hardware", "dsp", "audio", "battery", "power", "cpu"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Lyrics Style", "Choose between Motion, Legacy, Boring, or Plain synchronized lyrics rendering styles",
            listOf("lyrics", "style", "motion", "legacy", "boring", "plain", "shader", "wave", "animation"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Lyrics Providers", "Drag to reorder priority across all 10 synced lyrics providers",
            listOf("lyrics", "providers", "priority", "order", "reorder", "betterlyrics", "youlyplus", "paxsenix", "lrcred", "simpmusic", "lrclib", "deezer", "netease", "qqmusic", "kugou"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Auto-hide Delay", "Delay before controls auto-hide in lyrics view (3s to 6s)",
            listOf("auto hide", "auto-hide", "delay", "timeout", "lyrics", "timer"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Romanisation", "Show Latin script readings for non-Latin lyrics",
            listOf("lyrics", "romanisation", "romanization", "latin", "hindi", "japanese", "kanji", "kana", "korean", "chinese", "cyrillic", "arabic", "bengali", "hinglish"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Romanisation as the main line", "Put romanised lyrics on top and original script underneath",
            listOf("lyrics", "romanisation", "romanization", "main line", "primary", "script"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Translation", "Translate lyrics into your preferred language",
            listOf("lyrics", "translation", "translate", "language", "google translate", "ai"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Translation Language", "Choose the language to translate lyrics into",
            listOf("lyrics", "translation", "translate", "language", "english", "hindi", "spanish", "japanese", "korean", "bengali", "persian", "farsi", "kurdish", "sorani", "kurmanji", "urdu", "arabic"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Use AI for translation", "Try your AI provider first, fall back to Google",
            listOf("lyrics", "translation", "ai", "gemini", "openai", "deepseek", "groq", "api key"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Text Size", "Adjust lyrics text size from 18 sp to 36 sp",
            listOf("lyrics", "text size", "font size", "sp", "typography"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Line Spacing", "Adjust spacing between lyric lines (100% to 170%)",
            listOf("lyrics", "line spacing", "line height", "typography"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Blur Strength", "Adjust depth-of-field blur strength for inactive lyrics (2.5x default)",
            listOf("blur", "strength", "lyrics", "hardware", "visuals", "depth"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Soft Light & Depth Blur", "Subtly blur inactive lines to keep focus on the active lyric",
            listOf("lyrics", "soft light", "depth blur", "visuals", "blur", "focus"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),
        SearchableSetting("Reset Imported Lyrics", "Remove all imported synchronized lyrics from the database",
            listOf("lyrics", "reset", "clear", "imported"),
            SettingsCategory.APPEARANCE, Screen.SettingsCategory.createRoute(SettingsCategory.APPEARANCE.id)),

        // ─── Playback ───────────────────────────────────────────────────────────────
        SearchableSetting("Keep Playing in Background", "Continue audio playback when the app is minimized or the screen is turned off",
            listOf("background", "keep playing", "minimize", "screen off", "playback"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Live Activity / Dynamic Island", "Displays real-time track progress and controls in the status bar pill",
            listOf("dynamic island", "live activity", "status bar", "origin os", "hyperos", "coloros", "notch", "pill"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Island Visualizer Style", "Animated Notes, Progress Time, or Static Icon in the dynamic island pill",
            listOf("island", "visualizer", "dynamic island", "pill", "notes", "progress", "style"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("AOD Screen", "AMOLED ambient view (Disabled, Ambient Artwork, or Live Lyrics)",
            listOf("aod", "always on", "ambient", "glow", "amoled", "live lyrics"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Battery Optimization", "Disable battery optimization to prevent background playback interruptions",
            listOf("battery", "optimization", "background", "keep alive"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("WiFi Streaming Quality", "Select audio quality when streaming on WiFi networks",
            listOf("streaming", "quality", "bitrate", "audio", "wifi"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Mobile Data Streaming Quality", "Select audio quality when streaming on mobile or metered networks",
            listOf("streaming", "quality", "bitrate", "audio", "mobile", "cellular", "data"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Force high quality on mobile data", "Stream in high quality even when connected to mobile or metered networks",
            listOf("high quality", "mobile data", "cellular", "bitrate", "stream"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Download on like", "Automatically download audio of liked YouTube songs to the public Music folder",
            listOf("download", "like", "offline", "favorite", "cache"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Persistent Shuffle", "Remember shuffle setting even after closing the app",
            listOf("shuffle", "persistent", "random", "queue"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Show queue history", "Show previously played songs in the queue sheet",
            listOf("queue", "history", "played", "previous", "songs", "list"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Dynamic Queue (Autoplay)", "Automatically refill queue with related tracks when remaining count is low",
            listOf("auto queue", "autoqueue", "dynamic queue", "autoplay", "recommend", "continue", "radio"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Avoid Repetitive Songs", "Filter out recently played or highly rotated tracks from the dynamic queue",
            listOf("avoid", "repeat", "repetitive", "duplicate", "queue"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Queue Preloading", "Proactively resolve stream URLs and cache album art for upcoming songs",
            listOf("preload", "preloading", "buffer", "queue", "cache"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Queue Preload Size", "Number of upcoming songs to resolve and cache in the queue (1 to 10 songs)",
            listOf("preload", "buffer", "queue", "cache", "size", "tracks"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Download Audio Quality", "Select the default audio quality for offline music downloads",
            listOf("download", "quality", "offline", "bitrate", "audio"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Embed Rich Metadata & Lyrics", "Embed full song tags, high-resolution artwork, and synchronized lyrics into downloaded files",
            listOf("embed", "metadata", "tags", "lyrics", "cover art", "id3", "download"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Music Storage Limit", "Maximum local storage allocated for downloaded YouTube music or Unlimited",
            listOf("storage", "limit", "cache", "quota", "max space", "gb", "mb", "download"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Pure YouTube Music", "Filter out non-music video content and show only audio songs",
            listOf("pure", "music", "filter", "video", "youtube", "audio only"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("WiFi Album Art Quality", "Resolution for downloading and displaying album covers on WiFi",
            listOf("album art", "quality", "wifi", "resolution", "cover", "visual"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),
        SearchableSetting("Mobile Album Art Quality", "Resolution for downloading and displaying album covers on metered networks",
            listOf("album art", "quality", "mobile", "cellular", "data", "resolution", "visual"),
            SettingsCategory.PLAYBACK, Screen.SettingsCategory.createRoute(SettingsCategory.PLAYBACK.id)),

        // ─── Audio FX (Equalizer, AutoEQ, 3D Sims, 8D Audio, Effects & Output) ────
        SearchableSetting("Audio FX", "Parametric EQ, AutoEQ, 3D Sims, 8D Audio, reverb, loudness, and output controls",
            listOf("audio fx", "audio lab", "equalizer", "eq", "autoeq", "reverb", "spatial", "3d", "8d", "bass", "treble", "dolby", "sound", "effects"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),
        SearchableSetting("Equalizer", "Interactive parametric graph editor, multi-band sliders, presets, bass, tilt, and auto preamp",
            listOf("equalizer", "eq", "parametric", "bands", "graph", "bass", "treble", "tilt", "preamp", "presets"),
            SettingsCategory.EQUALIZER, Screen.AudioLabEq.route),
        SearchableSetting("AutoEQ Headphone Correction", "Measured headphone frequency response correction profiles from the AutoEQ database",
            listOf("autoeq", "headphone", "correction", "harman", "earbuds", "iem", "profile", "equalizer"),
            SettingsCategory.EQUALIZER, Screen.AudioLabEq.route),
        SearchableSetting("3D Sims", "Place vocals, bass, and instruments around you in an interactive 3D acoustic room",
            listOf("3d sims", "stage", "soundstage", "room", "binaural", "kemar", "vocals", "spatial", "speakers"),
            SettingsCategory.EQUALIZER, Screen.AudioLabStage.route),
        SearchableSetting("Transition & Crossfade", "Auto, CrossMix, or Manual track transitions and crossfade curve editor",
            listOf("transition", "crossfade", "crossmix", "gapless", "overlap", "fade", "duration"),
            SettingsCategory.EQUALIZER, Screen.EditTransition.createRoute(null)),
        SearchableSetting("Reverb", "Acoustic room, hall, plate, cathedral, studio, stadium, and dreamy reverb",
            listOf("reverb", "room", "hall", "plate", "cathedral", "studio", "stadium", "dreamy", "echo", "damping"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("8D Audio", "Rotating spatial sound that moves around you with speed, depth, and room reverb",
            listOf("8d", "8d audio", "rotating", "surround", "orbit", "spatial", "chill", "balanced", "extreme"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Loudness & Compression", "Dynamic loudness booster, night mode compression, and song loudness leveling",
            listOf("loudness", "boost", "compression", "night mode", "punchy", "gentle", "limiter", "even out", "preamp"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Spatial Audio & Crossfeed", "Natural headphone crossfeed and virtual studio speaker angle simulation",
            listOf("spatial", "crossfeed", "speakers", "head shadow", "speaker angle", "stereo", "soundstage"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Mono Audio", "Combine left and right stereo channels into both ears for single-earbud listening",
            listOf("mono", "single earbud", "accessibility", "channels", "stereo"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Dolby Atmos & Surround", "Render 5.1, 7.1, and Atmos beds as 3D sound or pass to Android Spatializer",
            listOf("dolby", "atmos", "surround", "spatializer", "5.1", "7.1", "eac3", "joc"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Playback Speed & Pitch", "Speed multiplier, Slowed + Reverb, Nightcore, and semitone pitch shifting",
            listOf("speed", "pitch", "tempo", "slowed", "slowed + reverb", "nightcore", "semitones", "key"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Exponential Volume", "Logarithmic volume curve for fine low-volume control and maximum volume cap",
            listOf("exponential volume", "volume curve", "volume limit", "logarithmic", "quiet", "cap"),
            SettingsCategory.EQUALIZER, Screen.AudioLabFx.route),
        SearchableSetting("Resume when headphones reconnect", "Automatically resume playback when wired or Bluetooth headphones reconnect",
            listOf("headphone", "resume", "headset", "bluetooth", "jack", "reconnect"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),
        SearchableSetting("Auto-play on cast connect/disconnect", "Start playing immediately after switching Chromecast connections",
            listOf("cast", "chromecast", "autoplay", "queue", "tv", "stream"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),
        SearchableSetting("Enable ReplayGain", "Normalize volume levels using ReplayGain metadata from audio files",
            listOf("replaygain", "replay gain", "volume", "normalize", "loudness", "gain"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),
        SearchableSetting("Gain Mode", "Apply Track gain or Album gain normalization",
            listOf("replay gain", "album gain", "track gain", "loudness", "volume", "normalize"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),
        SearchableSetting("Hi-Fi Mode", "Float 32-bit PCM audio output for high-resolution playback",
            listOf("hifi", "hi-fi", "high quality", "lossless", "audio", "hi-res", "32-bit", "pcm float"),
            SettingsCategory.EQUALIZER, Screen.Equalizer.route),

        // ─── PixelJam & Profile ─────────────────────────────────────────────────────
        SearchableSetting("PixelJam", "Real-time synced listening rooms, shared queues, notifications, and sync server",
            listOf("pixeljam", "jam", "listen together", "sync", "room", "party", "group", "friends", "radio", "shared queue"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Profile & Avatar", "Edit display name, custom photo, Google photo, or illustrated avatar",
            listOf("profile", "avatar", "display name", "photo", "picture", "illustrated", "animal", "character", "pixeljam", "account"),
            null, Screen.Profile.route),
        SearchableSetting("Join & leave toasts", "Show a toast when someone joins or leaves your PixelJam room",
            listOf("pixeljam", "join", "leave", "toast", "notification", "alert", "room", "members"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Host & suggestion alerts", "Show toasts for co-host promotions and incoming song suggestions",
            listOf("pixeljam", "host", "co-host", "suggestion", "alert", "toast", "notification", "room"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Auto-continue queue on leave", "Automatically queue related tracks when you leave a PixelJam room",
            listOf("pixeljam", "auto continue", "auto-continue", "queue", "leave", "autoplay", "related"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Keep session alive in background", "Maintain WebSocket synchronization when PixelMusic is minimized",
            listOf("pixeljam", "keep alive", "background", "session", "websocket", "sync", "minimized"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Default Room Mode", "Choose the default room mode when hosting a PixelJam session",
            listOf("pixeljam", "default room mode", "room mode", "host", "collaborative", "listen along", "radio"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),
        SearchableSetting("Sync Server", "Configure or reset the PixelJam WebSocket sync server URL",
            listOf("pixeljam", "sync server", "server url", "websocket", "render", "host", "custom server"),
            SettingsCategory.PIXEL_JAM, Screen.PixelJamSettings.route),

        // ─── Library & Sources ──────────────────────────────────────────────────────
        SearchableSetting("Excluded Directories", "Folders to skip when scanning your local library",
            listOf("excluded", "folders", "directories", "ignore", "scan", "hide"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Artists", "Multi-artist parsing, delimiters, and organization options",
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
        SearchableSetting("Content Language", "Set preferred YouTube Music content language",
            listOf("language", "content", "youtube", "region"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Content Country", "Set preferred YouTube Music content country",
            listOf("country", "region", "content", "location"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Playlist Suggestion Source", "Use playlist title, content, or both for song suggestions",
            listOf("playlist", "suggestion", "recommend", "smart"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Hide Explicit Content", "Do not show songs marked as explicit in search and feeds",
            listOf("explicit", "hide", "clean", "censored"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Hide Videos", "Do not show video results in YouTube Music search and feeds",
            listOf("video", "hide", "audio only"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("My Top Size", "Number of songs to include in Top Stats list (20, 50, or 100)",
            listOf("my top", "top songs", "size", "count", "playlist", "limit"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Set Quick Picks", "Choose content source mode for Quick Picks (Discover, Last Listen, or Don't Show)",
            listOf("quick picks", "discover", "recommendations", "suggestions", "last listen"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Quick Picks Display Mode", "Card Carousel or List Grid layout for Quick Picks",
            listOf("quick picks", "display", "card", "list", "layout", "carousel", "grid"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Minimum Song Duration", "Skip tracks shorter than this duration during library scan",
            listOf("duration", "minimum", "short", "skip", "filter"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Minimum Tracks per Album", "Hide albums with fewer tracks",
            listOf("album", "tracks", "minimum", "hide", "filter"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Album Art Cache Limit", "Maximum disk space used for album artwork cache",
            listOf("album art", "cache", "limit", "storage", "size", "disk"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Clear Streaming Cache", "Delete all temporary streaming data from your device",
            listOf("cache", "clear", "storage", "space", "clean", "streaming"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Refresh Library", "Run a full rescan or rebuild the music library database from scratch",
            listOf("rescan", "refresh", "library", "sync", "scan", "index", "rebuild", "database"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),
        SearchableSetting("Auto-scan .lrc files", "Automatically scan and assign local .lrc lyrics files during library sync",
            listOf("lyrics", "lrc", "auto scan", "subtitles", "local"),
            SettingsCategory.LIBRARY, Screen.SettingsCategory.createRoute(SettingsCategory.LIBRARY.id)),

        // ─── AI Integration ─────────────────────────────────────────────────────────
        SearchableSetting("Provider", "Choose your AI provider (Gemini, DeepSeek, Groq, Mistral, NVIDIA, Kimi, GLM, OpenAI, OpenRouter)",
            listOf("ai", "provider", "gemini", "deepseek", "groq", "openai", "mistral", "nvidia", "kimi", "glm", "openrouter"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("Safe Token Mode", "Limit AI context size to save tokens (~1K vs ~8K deep listening profile)",
            listOf("ai", "token", "limit", "safe", "quota", "mode"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI API Key", "Configure credentials for your selected AI provider",
            listOf("ai", "api key", "credentials", "token", "key", "gemini", "openai", "groq"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("AI Model", "Choose which LLM model to use for your selected AI provider",
            listOf("ai model", "gemini", "deepseek", "groq", "gpt", "model", "llm"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("System Prompt", "Customize assistant personality and instructions or pick a curator preset",
            listOf("ai", "system prompt", "personality", "customize", "prompt", "curator"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),
        SearchableSetting("Total Consumption", "View AI token consumption statistics, activity log, and clear usage history",
            listOf("ai", "usage", "tokens", "statistics", "report", "consumption", "activity log"),
            SettingsCategory.AI_INTEGRATION, Screen.SettingsCategory.createRoute(SettingsCategory.AI_INTEGRATION.id)),

        // ─── Backup & Restore ───────────────────────────────────────────────────────
        SearchableSetting("Export Backup", "Save playlists, settings, and favorites to a .pxpl backup file",
            listOf("export", "backup", "save", "restore", "file", "pxpl"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),
        SearchableSetting("Import Backup", "Restore app data and playlists from a .pxpl backup file",
            listOf("import", "restore", "backup", "recover", "load", "pxpl"),
            SettingsCategory.BACKUP_RESTORE, Screen.SettingsCategory.createRoute(SettingsCategory.BACKUP_RESTORE.id)),

        // ─── Accounts, Cloud & Last.fm ──────────────────────────────────────────────
        SearchableSetting("Connected Accounts", "Manage linked streaming, cloud, and scrobbling services",
            listOf("accounts", "google", "youtube", "spotify", "discord", "telegram", "cloud", "login", "sync"),
            null, Screen.Accounts.route),
        SearchableSetting("YouTube Music Account", "Log in to access your YouTube Music library, playlists, and likes",
            listOf("youtube", "google", "account", "login", "sign in", "channel", "yt music", "cookie", "token"),
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
        SearchableSetting("Last.fm Login", "Connect your Last.fm account for scrobbling and personalized mixes",
            listOf("lastfm", "last.fm", "login", "account"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),
        SearchableSetting("Enable Scrobbling", "Scrobble played songs to your Last.fm profile",
            listOf("scrobble", "scrobbling", "lastfm", "last.fm", "track", "history"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),
        SearchableSetting("Update Now Playing", "Update your Last.fm status with the current track",
            listOf("lastfm", "last.fm", "now playing", "status", "scrobble"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),
        SearchableSetting("Generated Playlists Retention", "Choose how long to keep generated Last.fm mix playlists in your library",
            listOf("lastfm", "smart mix", "retention", "playlist", "delete", "keep"),
            SettingsCategory.LASTFM, Screen.SettingsCategory.createRoute(SettingsCategory.LASTFM.id)),

        // ─── Device Capabilities & Compatibility ────────────────────────────────────
        SearchableSetting("Device Capabilities", "Audio specs, hardware codecs, PCM float, offload formats, and decoder info",
            listOf("device", "capabilities", "hardware", "audio", "specs", "dsp", "dac", "codecs", "offload", "pcm float"),
            SettingsCategory.DEVELOPER, Screen.DeviceCapabilities.route),
        SearchableSetting("Feature Compatibility", "OS and platform feature support matrix (Progressive Blur, AGSL Shaders, Dolby Atmos, Dynamic Color)",
            listOf("compatibility", "features", "incompatible", "unsupported", "blur", "agsl", "android", "atmos", "spatializer", "hifi"),
            SettingsCategory.DEVELOPER, Screen.FeatureCompatibility.route),

        // ─── Developer & Experimental ───────────────────────────────────────────────
        SearchableSetting("Local Mode", "Offline & downloaded music only (Network & Local Mode)",
            listOf("local mode", "offline", "network", "downloaded", "no internet", "airplane", "local only", "developer"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Internet for lyrics", "Fetch online lyrics if not embedded while in Local Mode",
            listOf("internet for lyrics", "online lyrics", "local mode", "offline", "lyrics", "developer"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Rescan library", "Scan local storage for new or updated audio files",
            listOf("rescan", "rescan library", "scan", "local mode", "refresh", "storage", "audio files", "developer"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Test Setup Flow", "Re-launch the onboarding and initial setup flow",
            listOf("setup", "onboarding", "welcome", "test setup", "wizard", "developer"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Trigger Test Crash", "Intentionally crash the app to test crash reporting",
            listOf("crash", "test crash", "exception", "debug", "diagnostics", "developer"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Music Live Wallpaper", "Dynamic live wallpaper with album art, Material You shapes, blur, and fallback wallpaper",
            listOf("music live wallpaper", "live wallpaper", "wallpaper", "material you", "shapes", "cookie", "triangle", "blur", "crossfade", "experimental", "developer"),
            SettingsCategory.DEVELOPER, Screen.MusicLiveWallpaper.route),
        SearchableSetting("Experimental", "Player UI loading experiments, transition delays, and placeholder toggles",
            listOf("experimental", "developer", "beta", "tweak", "advanced", "player ui"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Delay everything", "Fine-tune transition delays for full player sheet components",
            listOf("delay", "smooth", "transition", "player open", "animation", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Use placeholders for delayed items", "Show lightweight placeholders while the full player sheet expands",
            listOf("placeholder", "skeleton", "transition", "player", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Make placeholders transparent", "Keep layout space for player placeholders while making them invisible",
            listOf("transparent", "placeholder", "player", "experimental"),
            SettingsCategory.DEVELOPER, Screen.Experimental.route),
        SearchableSetting("Force Daily Mix Regeneration", "Re-creates the Daily Mix playlist immediately",
            listOf("daily mix", "regenerate", "force", "maintenance", "refresh mix"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Force Stats Regeneration", "Clears cache and recalculates playback statistics",
            listOf("stats", "statistics", "recalculate", "force", "history"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Force Album Palette Regeneration", "Rebuild cached dynamic color palette variants for album art",
            listOf("palette", "colors", "extract", "regenerate", "artwork"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Universal App Logging", "Record all app activity to Logcat and internal log file (pixelmusic.log)",
            listOf("log", "logging", "debug", "verbose", "logcat", "diagnostics"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Export / Share App Logs", "Share or save the recorded app log file (pixelmusic.log)",
            listOf("export logs", "share logs", "bug report", "debug", "file"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),
        SearchableSetting("Clear App Logs", "Wipe the current log buffer and recorded log file",
            listOf("clear logs", "wipe", "reset logs", "clean", "debug"),
            SettingsCategory.DEVELOPER, Screen.SettingsCategory.createRoute(SettingsCategory.DEVELOPER.id)),

        // ─── About ──────────────────────────────────────────────────────────────────
        SearchableSetting("About PixelMusic", "App version, license information, credits, and changelog",
            listOf("about", "version", "changelog", "info", "update", "github", "developer", "credits"),
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
            "Motion Blur", "Blur Intensity" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsMotionBlur
            "Live Activity / Dynamic Island", "Island Visualizer Style" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsLiveNotificationOrIsland()
            "Wallpaper Blur", "Blur Strength" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsHardwareBlur
            "Music Live Wallpaper" -> com.saurav.pixelmusic.utils.AndroidVersionCompat.supportsMusicLiveWallpaper
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

        // Category matching (e.g. typing "developer", "dev", "appearance", "playback", "experiments")
        entry.category?.let { cat ->
            val catId = cat.id.lowercase()
            val catName = cat.name.lowercase()
            if (catId == q || catName == q) s += 50
            else if (catId.startsWith(q) || catName.startsWith(q)) s += 30
            else if (catId.contains(q) || catName.contains(q)) s += 15
        }

        // Multi-word query matching (e.g. "battery", "blur strength", "8d audio")
        val queryTokens = q.split(' ', '-', ',').filter { it.isNotBlank() }
        if (queryTokens.size > 1) {
            val allTokensMatch = queryTokens.all { token ->
                title.contains(token) || subtitle.contains(token) ||
                    (entry.category != null && entry.category.id.contains(token, ignoreCase = true)) ||
                    entry.keywords.any { it.contains(token, ignoreCase = true) }
            }
            if (allTokensMatch) {
                s += 90
            }
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
