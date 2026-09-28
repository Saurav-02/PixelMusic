package com.saurav.pixelmusic.data.remote.youtube

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saurav.pixelmusic.data.model.youtube.Cookies
import com.saurav.pixelmusic.data.model.youtube.UmihiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.youtubeDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.Datastore.NAME)

/** Maximum number of YouTube accounts supported. */
const val MAX_YT_ACCOUNTS = 2

/**
 * One stored YouTube account. Slot 1 lives in the legacy preference keys,
 * slot 2 in the dedicated YT2_* keys. [isActive] marks the account whose
 * cookies/profile back every request in the app.
 */
data class YtAccount(
    val slot: Int,
    val username: String,
    val handle: String,
    val avatarUrl: String,
    val isActive: Boolean,
)

open class DatastoreRepository(private val context: Context) {
    object PreferenceKeys {
        val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
        val COOKIES = stringPreferencesKey(Constants.Datastore.COOKIES_KEY)
        val DATA_SYNC_ID = stringPreferencesKey(Constants.Datastore.DATA_SYNC_ID)
        val UPDATE_CHANNEL = stringPreferencesKey(Constants.Datastore.UPDATE_CHANNEL_KEY)
        val SHOW_PODCAST_PLAYLIST = booleanPreferencesKey(Constants.Datastore.SHOW_PODCAST_PLAYLIST)
        val USE_SPECIAL_LANGUAGE = booleanPreferencesKey(Constants.Datastore.USE_SPECIAL_LANGUAGE)
        val USE_AUDIO_OFFLOAD = booleanPreferencesKey(Constants.Datastore.USE_AUDIO_OFFLOAD)
        val KEEP_SCREEN_ON = booleanPreferencesKey(Constants.Datastore.KEEP_SCREEN_ON)
        val USE_ANIMATED_LYRICS = booleanPreferencesKey(Constants.Datastore.USE_ANIMATED_LYRICS)
        val ANIMATED_LYRICS_BLUR_ENABLED = booleanPreferencesKey(Constants.Datastore.ANIMATED_LYRICS_BLUR_ENABLED)
        val USE_IMMERSIVE_LYRICS = booleanPreferencesKey("use_immersive_lyrics")
        val LYRICS_AUTOHIDE_DELAY = intPreferencesKey("lyrics_autohide_delay")
        val SHOW_PLAYER_FILE_INFO = booleanPreferencesKey("show_player_file_info")
        val PLAYER_THEME_PREFERENCE = stringPreferencesKey("player_theme_preference")
        val COLOR_PALETTE_PREFERENCE = stringPreferencesKey("color_palette_preference")
        val LYRICS_MINIPLAYER_POSITION = stringPreferencesKey("lyrics_miniplayer_position")
        val LYRICS_MINIPLAYER_ALIGNMENT = stringPreferencesKey("lyrics_miniplayer_alignment")
        val USE_IMMERSIVE_LYRICS_STATUS_BAR = booleanPreferencesKey("use_immersive_lyrics_status_bar")
        val AUTO_QUEUE_ENABLED = booleanPreferencesKey("auto_queue_enabled")
        val AVOID_REPETITIVE_SONGS = booleanPreferencesKey("avoid_repetitive_songs")
        val PRELOAD_QUEUE_ENABLED = booleanPreferencesKey("preload_queue_enabled")
        val PRELOAD_QUEUE_SIZE = intPreferencesKey("preload_queue_size")
        val PERSISTENT_QUEUE = stringPreferencesKey("persistent_queue")
        val YT_USERNAME = stringPreferencesKey("yt_username")
        val YT_HANDLE = stringPreferencesKey("yt_handle")
        val YT_AVATAR_URL = stringPreferencesKey("yt_avatar_url")
        val ACTIVE_YT_ACCOUNT = intPreferencesKey("active_yt_account")
        val YT2_COOKIES = stringPreferencesKey("yt_account2_cookies")
        val YT2_DATA_SYNC_ID = stringPreferencesKey("yt_account2_data_sync_id")
        val YT2_USERNAME = stringPreferencesKey("yt_account2_username")
        val YT2_HANDLE = stringPreferencesKey("yt_account2_handle")
        val YT2_AVATAR_URL = stringPreferencesKey("yt_account2_avatar_url")
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.youtubeDataStore.edit {
            it[key] = value
        }
    }

    open val settings = context.youtubeDataStore.data.map {
        val updateChannel = it[PreferenceKeys.UPDATE_CHANNEL]?.let { value -> UmihiSettings.UpdateChannel.valueOf(value) }
            ?: UmihiSettings.UpdateChannel.Stable
        val showPodcastPlaylist = it[PreferenceKeys.SHOW_PODCAST_PLAYLIST] ?: true
        val useSpecialLanguage = it[PreferenceKeys.USE_SPECIAL_LANGUAGE] ?: false
        val useAudioOffload = it[PreferenceKeys.USE_AUDIO_OFFLOAD] ?: false
        val keepScreenOn = it[PreferenceKeys.KEEP_SCREEN_ON] ?: false
        val useAnimatedLyrics = it[PreferenceKeys.USE_ANIMATED_LYRICS] ?: true
        val animatedLyricsBlurEnabled = it[PreferenceKeys.ANIMATED_LYRICS_BLUR_ENABLED] ?: true
        val useImmersiveLyrics = it[PreferenceKeys.USE_IMMERSIVE_LYRICS] ?: true
        val lyricsAutoHideDelay = it[PreferenceKeys.LYRICS_AUTOHIDE_DELAY] ?: 4
        val showPlayerFileInfo = it[PreferenceKeys.SHOW_PLAYER_FILE_INFO] ?: false
        val playerThemePreference = it[PreferenceKeys.PLAYER_THEME_PREFERENCE] ?: "ALBUM_ART"
        val colorPalettePreference = it[PreferenceKeys.COLOR_PALETTE_PREFERENCE] ?: "SAGE"
        val lyricsMiniPlayerPosition = it[PreferenceKeys.LYRICS_MINIPLAYER_POSITION] ?: "TOP"
        val lyricsMiniPlayerAlignment = it[PreferenceKeys.LYRICS_MINIPLAYER_ALIGNMENT] ?: "LEFT"
        val useImmersiveLyricsStatusBar = it[PreferenceKeys.USE_IMMERSIVE_LYRICS_STATUS_BAR] ?: true
        val autoQueueEnabled = it[PreferenceKeys.AUTO_QUEUE_ENABLED] ?: true
        val avoidRepetitiveSongs = it[PreferenceKeys.AVOID_REPETITIVE_SONGS] ?: false
        val preloadQueueEnabled = it[PreferenceKeys.PRELOAD_QUEUE_ENABLED] ?: true
        val preloadQueueSize = it[PreferenceKeys.PRELOAD_QUEUE_SIZE] ?: 5
        val cookies = cookies.first()
        val dataSyncId = dataSyncId.first()

        UmihiSettings(
            updateChannel = updateChannel,
            showPodcastPlaylist = showPodcastPlaylist,
            cookies = cookies,
            dataSyncId = dataSyncId,
            useSpecialLanguage = useSpecialLanguage,
            useAudioOffload = useAudioOffload,
            keepScreenOn = keepScreenOn,
            useAnimatedLyrics = useAnimatedLyrics,
            animatedLyricsBlurEnabled = animatedLyricsBlurEnabled,
            useImmersiveLyrics = useImmersiveLyrics,
            lyricsAutoHideDelay = lyricsAutoHideDelay,
            showPlayerFileInfo = showPlayerFileInfo,
            playerThemePreference = playerThemePreference,
            colorPalettePreference = colorPalettePreference,
            lyricsMiniPlayerPosition = lyricsMiniPlayerPosition,
            lyricsMiniPlayerAlignment = lyricsMiniPlayerAlignment,
            useImmersiveLyricsStatusBar = useImmersiveLyricsStatusBar,
            autoQueueEnabled = autoQueueEnabled,
            avoidRepetitiveSongs = avoidRepetitiveSongs,
            preloadQueueEnabled = preloadQueueEnabled,
            preloadQueueSize = preloadQueueSize
        )
    }



    /**
     * Slot of the active YouTube account (1 or 2). Every account-backed flow
     * below resolves against this, so switching accounts is a single atomic
     * preference write and the whole app follows.
     */
    private val activeYtAccountSlot: Flow<Int> = context.youtubeDataStore.data.map {
        it[PreferenceKeys.ACTIVE_YT_ACCOUNT] ?: 1
    }

    val activeYtAccount: Flow<Int> = activeYtAccountSlot

    val cookies: Flow<Cookies> = combine(activeYtAccountSlot, context.youtubeDataStore.data) { active, prefs ->
        Cookies(if (active == 2) prefs[PreferenceKeys.YT2_COOKIES] ?: "" else prefs[PreferenceKeys.COOKIES] ?: "")
    }

    val dataSyncId: Flow<String> = combine(activeYtAccountSlot, context.youtubeDataStore.data) { active, prefs ->
        if (active == 2) prefs[PreferenceKeys.YT2_DATA_SYNC_ID] ?: "" else prefs[PreferenceKeys.DATA_SYNC_ID] ?: ""
    }

    val ytUsername: Flow<String> = combine(activeYtAccountSlot, context.youtubeDataStore.data) { active, prefs ->
        if (active == 2) prefs[PreferenceKeys.YT2_USERNAME] ?: "" else prefs[PreferenceKeys.YT_USERNAME] ?: ""
    }

    val ytHandle: Flow<String> = combine(activeYtAccountSlot, context.youtubeDataStore.data) { active, prefs ->
        if (active == 2) prefs[PreferenceKeys.YT2_HANDLE] ?: "" else prefs[PreferenceKeys.YT_HANDLE] ?: ""
    }

    val ytAvatarUrl: Flow<String> = combine(activeYtAccountSlot, context.youtubeDataStore.data) { active, prefs ->
        if (active == 2) prefs[PreferenceKeys.YT2_AVATAR_URL] ?: "" else prefs[PreferenceKeys.YT_AVATAR_URL] ?: ""
    }

    val ytIsProUser = context.youtubeDataStore.data.map {
        it[PreferenceKeys.IS_PRO_USER] ?: false
    }

    suspend fun saveYtProfile(name: String, handle: String, avatarUrl: String, isPro: Boolean = false) {
        context.youtubeDataStore.edit { prefs ->
            if ((prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] ?: 1) == 2) {
                prefs[PreferenceKeys.YT2_USERNAME] = name
                prefs[PreferenceKeys.YT2_HANDLE] = handle
                prefs[PreferenceKeys.YT2_AVATAR_URL] = avatarUrl
            } else {
                prefs[PreferenceKeys.YT_USERNAME] = name
                prefs[PreferenceKeys.YT_HANDLE] = handle
                prefs[PreferenceKeys.YT_AVATAR_URL] = avatarUrl
            }
            prefs[PreferenceKeys.IS_PRO_USER] = isPro
        }
    }

    suspend fun saveCookies(cookies: Cookies) {
        context.youtubeDataStore.edit { prefs ->
            val key = if ((prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] ?: 1) == 2) PreferenceKeys.YT2_COOKIES else PreferenceKeys.COOKIES
            prefs[key] = cookies.toRawCookie()
        }
    }

    suspend fun saveDataSyncId(newId: String) {
        context.youtubeDataStore.edit { prefs ->
            val key = if ((prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] ?: 1) == 2) PreferenceKeys.YT2_DATA_SYNC_ID else PreferenceKeys.DATA_SYNC_ID
            prefs[key] = newId
        }
    }

    /** All stored YouTube accounts, active account first. */
    val ytAccounts: Flow<List<YtAccount>> = context.youtubeDataStore.data.map { prefs ->
        val active = prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] ?: 1
        buildList {
            if ((prefs[PreferenceKeys.COOKIES] ?: "").isNotEmpty()) {
                add(
                    YtAccount(
                        slot = 1,
                        username = prefs[PreferenceKeys.YT_USERNAME] ?: "",
                        handle = prefs[PreferenceKeys.YT_HANDLE] ?: "",
                        avatarUrl = prefs[PreferenceKeys.YT_AVATAR_URL] ?: "",
                        isActive = active == 1,
                    )
                )
            }
            if ((prefs[PreferenceKeys.YT2_COOKIES] ?: "").isNotEmpty()) {
                add(
                    YtAccount(
                        slot = 2,
                        username = prefs[PreferenceKeys.YT2_USERNAME] ?: "",
                        handle = prefs[PreferenceKeys.YT2_HANDLE] ?: "",
                        avatarUrl = prefs[PreferenceKeys.YT2_AVATAR_URL] ?: "",
                        isActive = active == 2,
                    )
                )
            }
        }.sortedByDescending { it.isActive }
    }

    suspend fun ytAccountCount(): Int = ytAccounts.first().size

    /**
     * Switches the active account. This is a single atomic write; every
     * account-backed flow ([cookies], [ytUsername], ...) re-emits and the
     * whole app (UI, innertube singleton, request helpers) follows without
     * any further action.
     */
    suspend fun setActiveYtAccount(slot: Int) {
        require(slot == 1 || slot == 2) { "slot must be 1 or 2" }
        context.youtubeDataStore.edit { it[PreferenceKeys.ACTIVE_YT_ACCOUNT] = slot }
    }

    /**
     * Stores a freshly logged-in session into [slot] and makes it active,
     * atomically. Callers pick slot 1 for the first login and slot 2 when
     * adding another account.
     */
    suspend fun saveYtAccountSession(
        slot: Int,
        cookies: Cookies,
        dataSyncId: String?,
        name: String,
        handle: String,
        avatarUrl: String,
    ) {
        require(slot == 1 || slot == 2) { "slot must be 1 or 2" }
        context.youtubeDataStore.edit { prefs ->
            if (slot == 2) {
                prefs[PreferenceKeys.YT2_COOKIES] = cookies.toRawCookie()
                if (dataSyncId != null) prefs[PreferenceKeys.YT2_DATA_SYNC_ID] = dataSyncId
                prefs[PreferenceKeys.YT2_USERNAME] = name
                prefs[PreferenceKeys.YT2_HANDLE] = handle
                prefs[PreferenceKeys.YT2_AVATAR_URL] = avatarUrl
            } else {
                prefs[PreferenceKeys.COOKIES] = cookies.toRawCookie()
                if (dataSyncId != null) prefs[PreferenceKeys.DATA_SYNC_ID] = dataSyncId
                prefs[PreferenceKeys.YT_USERNAME] = name
                prefs[PreferenceKeys.YT_HANDLE] = handle
                prefs[PreferenceKeys.YT_AVATAR_URL] = avatarUrl
            }
            prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] = slot
        }
    }

    /**
     * Removes the account in [slot]. If the removed account was active and
     * another account exists, that account is promoted into slot 1 and
     * becomes active.
     * @return true if at least one account remains afterwards.
     */
    suspend fun removeYtAccount(slot: Int): Boolean {
        var remaining = false
        context.youtubeDataStore.edit { prefs ->
            val cookies1 = prefs[PreferenceKeys.COOKIES] ?: ""
            val cookies2 = prefs[PreferenceKeys.YT2_COOKIES] ?: ""
            when {
                slot == 2 -> {
                    prefs.remove(PreferenceKeys.YT2_COOKIES)
                    prefs.remove(PreferenceKeys.YT2_DATA_SYNC_ID)
                    prefs.remove(PreferenceKeys.YT2_USERNAME)
                    prefs.remove(PreferenceKeys.YT2_HANDLE)
                    prefs.remove(PreferenceKeys.YT2_AVATAR_URL)
                    prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] = 1
                    remaining = cookies1.isNotEmpty()
                }
                cookies2.isNotEmpty() -> {
                    prefs[PreferenceKeys.COOKIES] = cookies2
                    prefs[PreferenceKeys.DATA_SYNC_ID] = prefs[PreferenceKeys.YT2_DATA_SYNC_ID] ?: ""
                    prefs[PreferenceKeys.YT_USERNAME] = prefs[PreferenceKeys.YT2_USERNAME] ?: ""
                    prefs[PreferenceKeys.YT_HANDLE] = prefs[PreferenceKeys.YT2_HANDLE] ?: ""
                    prefs[PreferenceKeys.YT_AVATAR_URL] = prefs[PreferenceKeys.YT2_AVATAR_URL] ?: ""
                    prefs.remove(PreferenceKeys.YT2_COOKIES)
                    prefs.remove(PreferenceKeys.YT2_DATA_SYNC_ID)
                    prefs.remove(PreferenceKeys.YT2_USERNAME)
                    prefs.remove(PreferenceKeys.YT2_HANDLE)
                    prefs.remove(PreferenceKeys.YT2_AVATAR_URL)
                    prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] = 1
                    remaining = true
                }
                else -> {
                    prefs[PreferenceKeys.COOKIES] = ""
                    prefs[PreferenceKeys.DATA_SYNC_ID] = ""
                    prefs[PreferenceKeys.YT_USERNAME] = ""
                    prefs[PreferenceKeys.YT_HANDLE] = ""
                    prefs[PreferenceKeys.YT_AVATAR_URL] = ""
                    prefs[PreferenceKeys.ACTIVE_YT_ACCOUNT] = 1
                    remaining = false
                }
            }
        }
        return remaining
    }

    suspend fun getPersistentQueue(): String {
        return context.youtubeDataStore.data.first()[PreferenceKeys.PERSISTENT_QUEUE] ?: ""
    }

    suspend fun savePersistentQueue(json: String) {
        context.youtubeDataStore.edit {
            it[PreferenceKeys.PERSISTENT_QUEUE] = json
        }
    }
}
