package com.saurav.pixelmusic.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PixelJamManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_SERVER_URL = stringPreferencesKey("pixeljam_server_url")
        val KEY_DISPLAY_NAME = stringPreferencesKey("pixeljam_display_name")
        val KEY_DEFAULT_MODE = stringPreferencesKey("pixeljam_default_mode")
        val KEY_NOTIFY_JOIN_LEAVE = booleanPreferencesKey("pixeljam_notify_join_leave")
        val KEY_NOTIFY_ROOM_ALERTS = booleanPreferencesKey("pixeljam_notify_room_alerts")
        val KEY_AUTO_QUEUE_ON_LEAVE = booleanPreferencesKey("pixeljam_auto_queue_on_leave")
        private const val PING_INTERVAL_MS = 15_000L
        private const val MAX_RECONNECT_ATTEMPTS = 2
        private const val WS_CLOSE_GOING_AWAY = 1001

        @Volatile
        private var activeInstance: PixelJamManager? = null

        /** Called when the user explicitly closes/unloads the player notification. */
        fun leaveIfActive() {
            val inst = activeInstance ?: return
            if (inst.isActive()) {
                inst.leaveRoom()
            }
        }
    }

    init {
        activeInstance = this
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val healthHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    val serverUrl: StateFlow<String> = dataStore.data
        .map { prefs -> PixelJamConfig.normalizeBaseUrl(prefs[KEY_SERVER_URL].orEmpty()) }
        .stateIn(scope, SharingStarted.Eagerly, PixelJamConfig.DEFAULT_SERVER_URL)

    val savedDisplayName: StateFlow<String> = dataStore.data
        .map { prefs -> prefs[KEY_DISPLAY_NAME].orEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val notifyJoinLeave: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_NOTIFY_JOIN_LEAVE] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val notifyRoomAlerts: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_NOTIFY_ROOM_ALERTS] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val autoQueueOnLeave: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_AUTO_QUEUE_ON_LEAVE] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val defaultRoomMode: StateFlow<PixelJamRoomMode> = dataStore.data
        .map { prefs -> PixelJamRoomMode.fromWire(prefs[KEY_DEFAULT_MODE]) }
        .stateIn(scope, SharingStarted.Eagerly, PixelJamRoomMode.HOST_ONLY)

    fun saveDisplayName(name: String) {
        val trimmed = name.trim().take(32)
        if (trimmed.isNotBlank()) {
            scope.launch {
                dataStore.edit { it[KEY_DISPLAY_NAME] = trimmed }
            }
        }
    }

    private val _uiState = MutableStateFlow<PixelJamUiState>(PixelJamUiState.Idle)
    val uiState: StateFlow<PixelJamUiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow(PixelJamConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PixelJamConnectionState> = _connectionState.asStateFlow()

    private val _playbackState = MutableStateFlow<PixelJamPlaybackState?>(null)
    val playbackState: StateFlow<PixelJamPlaybackState?> = _playbackState.asStateFlow()

    private val _sharedQueue = MutableStateFlow<List<PixelJamTrack>>(emptyList())
    val sharedQueue: StateFlow<List<PixelJamTrack>> = _sharedQueue.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<PixelJamChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<PixelJamChatMessage>> = _chatMessages.asStateFlow()

    private val _suggestions = MutableStateFlow<List<PixelJamSuggestion>>(emptyList())
    val suggestions: StateFlow<List<PixelJamSuggestion>> = _suggestions.asStateFlow()

    private val _publicRooms = MutableStateFlow<List<PixelJamPublicRoom>>(
        listOf(
            PixelJamPublicRoom("INDIA", "India", 0),
            PixelJamPublicRoom("WORLD", "International", 0)
        )
    )
    val publicRooms: StateFlow<List<PixelJamPublicRoom>> = _publicRooms.asStateFlow()

    private val _cooldownUntilMs = MutableStateFlow(0L)
    val cooldownUntilMs: StateFlow<Long> = _cooldownUntilMs.asStateFlow()

    private val _rttMs = MutableStateFlow(0L)
    val rttMs: StateFlow<Long> = _rttMs.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _serverRestartEvents = MutableSharedFlow<PixelJamServerRestartEvent>(extraBufferCapacity = 8)
    val serverRestartEvents: SharedFlow<PixelJamServerRestartEvent> = _serverRestartEvents.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null
    @Volatile
    private var serverClockOffsetMs: Long = 0L
    @Volatile
    private var intentionalClose: Boolean = false
    @Volatile
    private var reconnectingActiveRoom: Boolean = false
    @Volatile
    private var sessionBootId: String? = null

    private var pendingConnectAction: (() -> Unit)? = null
    private var activeRoomCode: String? = null
    private var activeDisplayName: String? = null
    private var activeAvatarUrl: String = ""
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt: Int = 0

    private fun isInOrReconnectingRoom(): Boolean =
        _uiState.value is PixelJamUiState.InRoom || reconnectingActiveRoom

    fun isActive(): Boolean = _uiState.value is PixelJamUiState.InRoom

    fun isMainHost(): Boolean = (_uiState.value as? PixelJamUiState.InRoom)?.isMainHost == true

    fun isRadioRoom(): Boolean = (_uiState.value as? PixelJamUiState.InRoom)?.isRadio == true

    fun canControlPlayback(): Boolean {
        val room = _uiState.value as? PixelJamUiState.InRoom ?: return false
        if (room.isRadio) return false
        return room.isHost || room.roomMode == PixelJamRoomMode.COLLABORATIVE
    }

    fun myUserId(): String? = (_uiState.value as? PixelJamUiState.InRoom)?.userId

    fun estimatedServerNowMs(): Long = System.currentTimeMillis() + serverClockOffsetMs

    fun inviteUrlForCurrentRoom(): String? {
        val code = (_uiState.value as? PixelJamUiState.InRoom)?.roomCode ?: return null
        return PixelJamConfig.toInviteUrl(serverUrl.value, code)
    }

    suspend fun setServerUrl(url: String) {
        val normalized = PixelJamConfig.normalizeBaseUrl(url)
        dataStore.edit { it[KEY_SERVER_URL] = normalized }
    }

    fun dismissError() {
        if (_uiState.value is PixelJamUiState.Error) {
            _uiState.value = PixelJamUiState.Idle
        }
    }

    fun refreshPublicRooms() {
        scope.launch {
            try {
                val base = serverUrl.value
                val req = Request.Builder()
                    .url(PixelJamConfig.toRoomsUrl(base))
                    .get()
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body.string().orEmpty()
                    val root = JSONObject(body)
                    val arr = root.optJSONArray("rooms") ?: return@use
                    val list = buildList {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            add(
                                PixelJamPublicRoom(
                                    code = obj.optString("code"),
                                    name = obj.optString("name"),
                                    participants = obj.optInt("participants", 0)
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        _publicRooms.value = list
                    }
                }
            } catch (t: Throwable) {
                Timber.d(t, "PixelJam: refreshPublicRooms failed")
            }
        }
    }

    fun createRoom(name: String, mode: PixelJamRoomMode, avatarUrl: String = "") {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.value = PixelJamUiState.Error("Enter a display name first.", "invalid_name")
            return
        }
        activeDisplayName = cleanName
        activeAvatarUrl = avatarUrl.trim()
        activeRoomCode = null
        sessionBootId = null
        _uiState.value = PixelJamUiState.Creating
        connectAndSend {
            val payload = JSONObject().apply {
                put("name", cleanName)
                put("room_mode", mode.wireValue)
                if (activeAvatarUrl.isNotBlank()) put("avatar_url", activeAvatarUrl)
            }
            sendEnvelope("create_room", payload)
        }
    }

    fun joinRoom(roomCode: String, name: String, avatarUrl: String = "") {
        val cleanCode = roomCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        val cleanName = name.trim()
        if (cleanCode.length < 3) {
            _uiState.value = PixelJamUiState.Error("Enter a valid room code.", "missing_room_code")
            return
        }
        if (cleanName.isBlank()) {
            _uiState.value = PixelJamUiState.Error("Enter a display name before joining.", "invalid_name")
            return
        }
        activeDisplayName = cleanName
        activeAvatarUrl = avatarUrl.trim()
        activeRoomCode = cleanCode
        sessionBootId = null
        _uiState.value = PixelJamUiState.Joining(cleanCode)
        connectAndSend {
            val payload = JSONObject().apply {
                put("room_code", cleanCode)
                put("name", cleanName)
                if (activeAvatarUrl.isNotBlank()) put("avatar_url", activeAvatarUrl)
            }
            sendEnvelope("join_room", payload)
        }
    }

    fun leaveRoom() {
        intentionalClose = true
        reconnectingActiveRoom = false
        reconnectAttempt = 0
        pendingConnectAction = null
        reconnectJob?.cancel()
        reconnectJob = null
        pingJob?.cancel()
        pingJob = null
        try {
            sendEnvelope("leave_room", JSONObject())
        } catch (_: Throwable) {
        }
        webSocket?.close(1000, "Leaving room")
        webSocket = null
        activeRoomCode = null
        sessionBootId = null
        resetRoomData()
        _connectionState.value = PixelJamConnectionState.DISCONNECTED
        _uiState.value = PixelJamUiState.Idle
    }

    @Synchronized
    private fun handleServerRestartKick(reason: String = "server_restart") {
        if (intentionalClose && !isInOrReconnectingRoom() && activeRoomCode == null) return
        intentionalClose = true
        reconnectingActiveRoom = false
        reconnectAttempt = 0
        pendingConnectAction = null
        reconnectJob?.cancel()
        reconnectJob = null
        pingJob?.cancel()
        pingJob = null
        try {
            webSocket?.close(1000, "Server restarting")
        } catch (_: Throwable) {
        }
        webSocket = null
        activeRoomCode = null
        sessionBootId = null
        resetRoomData()
        _connectionState.value = PixelJamConnectionState.DISCONNECTED
        _uiState.value = PixelJamUiState.Idle
        _serverRestartEvents.tryEmit(
            PixelJamServerRestartEvent(
                reason = reason.ifBlank { "server_restart" },
                message = PixelJamServerRestartEvent.DEFAULT_MESSAGE
            )
        )
    }

    private fun fetchServerBootId(baseUrl: String = serverUrl.value): String? {
        return try {
            val req = Request.Builder()
                .url(PixelJamConfig.toHealthUrl(baseUrl))
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            healthHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body.string().orEmpty()
                if (body.isBlank()) return null
                val root = JSONObject(body)
                root.optString("boot_id")
                    .ifBlank { root.optString("server_boot_id") }
                    .trim()
                    .ifBlank { null }
            }
        } catch (t: Throwable) {
            Timber.d(t, "PixelJam: fetchServerBootId failed")
            null
        }
    }

    fun sendPlaybackAction(
        action: String,
        track: PixelJamTrack? = null,
        positionMs: Long = 0L,
        isPlaying: Boolean = false
    ) {
        if (!canControlPlayback()) return
        val payload = JSONObject().apply {
            put("action", action)
            put("position_ms", positionMs.coerceAtLeast(0L))
            put("client_time_ms", estimatedServerNowMs())
            put("is_playing", isPlaying)
            if (track != null) {
                put("track", trackToJson(track))
            }
        }
        sendEnvelope("playback_action", payload)
    }

    fun publishQueue(tracks: List<PixelJamTrack>) {
        if (!isMainHost()) return
        val arr = JSONArray()
        tracks.take(5).forEach { arr.put(trackToJson(it)) }
        val payload = JSONObject().apply { put("queue", arr) }
        sendEnvelope("queue_update", payload)
        _sharedQueue.value = tracks.take(5)
    }

    fun sendChat(text: String, replyToId: String? = null) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val payload = JSONObject().apply {
            put("text", clean)
            if (!replyToId.isNullOrBlank()) {
                put("reply_to_id", replyToId)
            }
        }
        sendEnvelope("send_chat", payload)
    }

    fun suggestTrack(track: PixelJamTrack) {
        val payload = JSONObject().apply {
            put("track", trackToJson(track))
        }
        sendEnvelope("suggest_track", payload)
    }

    fun approveSuggestion(suggestionId: String) {
        sendEnvelope("approve_suggestion", JSONObject().put("suggestion_id", suggestionId))
        _suggestions.value = _suggestions.value.filterNot { it.suggestionId == suggestionId }
    }

    fun rejectSuggestion(suggestionId: String) {
        sendEnvelope("reject_suggestion", JSONObject().put("suggestion_id", suggestionId))
        _suggestions.value = _suggestions.value.filterNot { it.suggestionId == suggestionId }
    }

    fun changeRoomMode(mode: PixelJamRoomMode) {
        sendEnvelope("change_room_mode", JSONObject().put("room_mode", mode.wireValue))
    }

    fun grantCoHost(userId: String) {
        sendEnvelope("grant_host", JSONObject().put("user_id", userId))
    }

    fun revokeCoHost(userId: String) {
        sendEnvelope("revoke_host", JSONObject().put("user_id", userId))
    }

    private fun resetRoomData() {
        _playbackState.value = null
        _sharedQueue.value = emptyList()
        _chatMessages.value = emptyList()
        _suggestions.value = emptyList()
        _cooldownUntilMs.value = 0L
        _rttMs.value = 0L
    }

    private fun connectAndSend(onOpenAction: () -> Unit) {
        intentionalClose = true
        reconnectingActiveRoom = false
        reconnectJob?.cancel()
        pingJob?.cancel()
        webSocket?.close(1000, "Reconnecting")
        webSocket = null

        intentionalClose = false
        reconnectAttempt = 0
        pendingConnectAction = onOpenAction
        _connectionState.value = PixelJamConnectionState.CONNECTING

        scope.launch {
            val base = dataStore.data.first()[KEY_SERVER_URL].orEmpty().ifBlank { PixelJamConfig.DEFAULT_SERVER_URL }
            val wsUrl = PixelJamConfig.toWebSocketUrl(base)
            Timber.d("PixelJam: connecting to %s", wsUrl)
            val request = Request.Builder().url(wsUrl).build()
            webSocket = okHttpClient.newWebSocket(request, socketListener)
        }
    }

    private fun scheduleReconnect() {
        if (intentionalClose) return
        val code = activeRoomCode ?: return
        val name = activeDisplayName ?: return
        val expectedBootId = sessionBootId
        reconnectingActiveRoom = true
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            handleServerRestartKick(reason = "connection_lost")
            return
        }
        reconnectAttempt++
        _connectionState.value = PixelJamConnectionState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (1_200L * reconnectAttempt).coerceAtMost(4_000L)
            delay(backoffMs)
            if (intentionalClose) return@launch

            if (expectedBootId != null) {
                val liveBootId = fetchServerBootId(serverUrl.value)
                if (intentionalClose) return@launch
                if (liveBootId != null && liveBootId != expectedBootId) {
                    Timber.i(
                        "PixelJam: server boot_id changed on /health (%s -> %s); kicking to solo playback",
                        expectedBootId,
                        liveBootId
                    )
                    handleServerRestartKick(reason = "server_restart")
                    return@launch
                }
            }

            pendingConnectAction = {
                sendEnvelope(
                    "join_room",
                    JSONObject().apply {
                        put("room_code", code)
                        put("name", name)
                        if (activeAvatarUrl.isNotBlank()) put("avatar_url", activeAvatarUrl)
                    }
                )
            }
            val wsUrl = PixelJamConfig.toWebSocketUrl(serverUrl.value)
            webSocket = okHttpClient.newWebSocket(Request.Builder().url(wsUrl).build(), socketListener)
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                sendRawEnvelope("""{"type":"ping","payload":${System.currentTimeMillis()}}""")
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun sendEnvelope(type: String, payload: JSONObject) {
        val env = JSONObject().apply {
            put("type", type)
            put("payload", payload)
        }
        sendRawEnvelope(env.toString())
    }

    private fun sendRawEnvelope(raw: String) {
        try {
            webSocket?.send(raw)
        } catch (t: Throwable) {
            Timber.w(t, "PixelJam: send failed")
        }
    }

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (this@PixelJamManager.webSocket !== webSocket) return
            _connectionState.value = PixelJamConnectionState.CONNECTED
            startPingLoop()
            pendingConnectAction?.invoke()
            pendingConnectAction = null
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (this@PixelJamManager.webSocket !== webSocket) return
            try {
                handleMessage(text)
            } catch (t: Throwable) {
                Timber.w(t, "PixelJam: failed to parse message")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (this@PixelJamManager.webSocket !== webSocket) return
            if (!intentionalClose && code == WS_CLOSE_GOING_AWAY && isInOrReconnectingRoom()) {
                handleServerRestartKick(reason = reason.ifBlank { "server_restart" })
                return
            }
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (this@PixelJamManager.webSocket !== webSocket) return
            pingJob?.cancel()
            if (intentionalClose) return
            if (code == WS_CLOSE_GOING_AWAY && isInOrReconnectingRoom()) {
                handleServerRestartKick(reason = reason.ifBlank { "server_restart" })
                return
            }
            if (isInOrReconnectingRoom() && activeRoomCode != null) {
                scheduleReconnect()
            } else {
                _connectionState.value = PixelJamConnectionState.DISCONNECTED
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (this@PixelJamManager.webSocket !== webSocket) return
            Timber.w(t, "PixelJam: websocket failure")
            pingJob?.cancel()
            if (intentionalClose) return
            if (isInOrReconnectingRoom() && activeRoomCode != null) {
                scheduleReconnect()
            } else {
                _connectionState.value = PixelJamConnectionState.DISCONNECTED
                _uiState.value = PixelJamUiState.Error(
                    "Couldn't reach PixelJam server. If it's waking up on Render, wait a few seconds and try again."
                )
            }
        }
    }

    private fun handleMessage(raw: String) {
        val env = JSONObject(raw)
        val type = env.optString("type")
        val payload = env.optJSONObject("payload")

        when (type) {
            "pong" -> {
                if (payload != null) {
                    val clientTs = payload.optLong("client_time_ms", 0L)
                    val serverTs = payload.optLong("server_time_ms", 0L)
                    val now = System.currentTimeMillis()
                    if (clientTs > 0L && serverTs > 0L) {
                        val rtt = (now - clientTs).coerceAtLeast(0L)
                        _rttMs.value = rtt
                        serverClockOffsetMs = serverTs - (clientTs + rtt / 2L)
                    }
                }
            }

            "room_created" -> {
                if (payload == null) return
                val code = payload.optString("room_code")
                val userId = payload.optString("user_id")
                val name = payload.optString("name")
                val mode = PixelJamRoomMode.fromWire(payload.optString("room_mode"))
                val incomingBootId = payload.optString("server_boot_id")
                    .ifBlank { payload.optString("boot_id") }
                    .trim()
                    .ifBlank { null }
                if (incomingBootId != null) {
                    sessionBootId = incomingBootId
                } else if (sessionBootId == null) {
                    scope.launch {
                        fetchServerBootId()?.let { fetched ->
                            if (sessionBootId == null && isInOrReconnectingRoom()) {
                                sessionBootId = fetched
                            }
                        }
                    }
                }
                reconnectAttempt = 0
                reconnectingActiveRoom = false
                activeRoomCode = code
                activeDisplayName = name
                resetRoomData()
                _uiState.value = PixelJamUiState.InRoom(
                    roomCode = code,
                    userId = userId,
                    displayName = name,
                    isHost = true,
                    isMainHost = true,
                    isCoHost = false,
                    hostId = userId,
                    coHostId = "",
                    permanent = false,
                    roomMode = mode,
                    participants = listOf(
                        PixelJamParticipant(
                            userId = userId,
                            name = name,
                            avatarUrl = payload.optString("avatar_url", activeAvatarUrl),
                            isHost = true,
                            isCoHost = false,
                            joinedAt = System.currentTimeMillis()
                        )
                    )
                )
            }

            "sync_state" -> {
                if (payload == null) return
                val incomingBootId = payload.optString("server_boot_id")
                    .ifBlank { payload.optString("boot_id") }
                    .trim()
                    .ifBlank { null }
                val previousBootId = sessionBootId
                if (reconnectingActiveRoom && previousBootId != null && incomingBootId != null && incomingBootId != previousBootId) {
                    Timber.i(
                        "PixelJam: server boot_id changed on sync_state (%s -> %s); kicking to solo playback",
                        previousBootId,
                        incomingBootId
                    )
                    handleServerRestartKick(reason = "server_restart")
                    return
                }
                if (incomingBootId != null) {
                    sessionBootId = incomingBootId
                } else if (sessionBootId == null) {
                    scope.launch {
                        fetchServerBootId()?.let { fetched ->
                            if (sessionBootId == null && isInOrReconnectingRoom()) {
                                sessionBootId = fetched
                            }
                        }
                    }
                }

                val code = payload.optString("room_code")
                val userId = payload.optString("user_id")
                val name = payload.optString("name")
                val hostId = payload.optString("host_id")
                val coHostId = payload.optString("cohost_id")
                val isHost = payload.optBoolean("is_host", false)
                val permanent = payload.optBoolean("permanent", false)
                val isRadio = payload.optBoolean("radio", false)
                val roomName = payload.optString("room_name", "")
                val mode = PixelJamRoomMode.fromWire(payload.optString("room_mode"))

                reconnectAttempt = 0
                reconnectingActiveRoom = false
                activeRoomCode = code
                activeDisplayName = name

                val participants = parseParticipants(payload.optJSONArray("participants"))
                val chat = parseChatHistory(payload.optJSONArray("chat_history"))
                val playbackObj = payload.optJSONObject("playback")
                val playback = playbackObj?.let { parsePlaybackState(it, action = "sync_state") }

                _chatMessages.value = chat
                _sharedQueue.value = playback?.queue.orEmpty()
                _playbackState.value = playback

                _uiState.value = PixelJamUiState.InRoom(
                    roomCode = code,
                    roomName = roomName,
                    userId = userId,
                    displayName = name,
                    isHost = isHost,
                    isMainHost = hostId == userId,
                    isCoHost = coHostId.isNotBlank() && coHostId == userId,
                    hostId = hostId,
                    coHostId = coHostId,
                    permanent = permanent,
                    isRadio = isRadio,
                    roomMode = mode,
                    participants = participants
                )
            }

            "user_joined" -> {
                val room = _uiState.value as? PixelJamUiState.InRoom ?: return
                if (payload == null) return
                val uid = payload.optString("user_id")
                val name = payload.optString("name")
                val isHost = payload.optBoolean("is_host", false)
                if (uid.isBlank()) return
                val updated = room.participants.filterNot { it.userId == uid } + PixelJamParticipant(
                    userId = uid,
                    name = name,
                    avatarUrl = payload.optString("avatar_url", ""),
                    isHost = isHost,
                    isCoHost = room.coHostId == uid,
                    joinedAt = System.currentTimeMillis()
                )
                _uiState.value = room.copy(participants = updated)
                if (uid != room.userId && notifyJoinLeave.value) {
                    _events.tryEmit("$name joined PixelJam")
                }
            }

            "user_left" -> {
                val room = _uiState.value as? PixelJamUiState.InRoom ?: return
                if (payload == null) return
                val uid = payload.optString("user_id")
                val name = payload.optString("name")
                val updated = room.participants.filterNot { it.userId == uid }
                _uiState.value = room.copy(participants = updated)
                if (uid != room.userId && name.isNotBlank() && notifyJoinLeave.value) {
                    _events.tryEmit("$name left the room")
                }
            }

            "host_changed" -> {
                val room = _uiState.value as? PixelJamUiState.InRoom ?: return
                if (payload == null) return
                val hostId = payload.optString("host_id").ifBlank { payload.optString("new_host_id") }
                val coHostId = payload.optString("cohost_id")
                val newHostName = payload.optString("new_host_name")
                val updatedParticipants = room.participants.map { p ->
                    p.copy(
                        isHost = p.userId == hostId || (coHostId.isNotBlank() && p.userId == coHostId),
                        isCoHost = coHostId.isNotBlank() && p.userId == coHostId
                    )
                }
                _uiState.value = room.copy(
                    hostId = hostId,
                    coHostId = coHostId,
                    isMainHost = room.userId == hostId,
                    isCoHost = coHostId.isNotBlank() && room.userId == coHostId,
                    isHost = room.userId == hostId || (coHostId.isNotBlank() && room.userId == coHostId),
                    participants = updatedParticipants
                )
                if (newHostName.isNotBlank() && notifyRoomAlerts.value) {
                    _events.tryEmit("$newHostName is now the host")
                }
            }

            "cohost_changed" -> {
                val room = _uiState.value as? PixelJamUiState.InRoom ?: return
                if (payload == null) return
                val hostId = payload.optString("host_id").ifBlank { room.hostId }
                val coHostId = payload.optString("cohost_id")
                val coHostName = payload.optString("cohost_name")
                val updatedParticipants = room.participants.map { p ->
                    p.copy(
                        isHost = p.userId == hostId || (coHostId.isNotBlank() && p.userId == coHostId),
                        isCoHost = coHostId.isNotBlank() && p.userId == coHostId
                    )
                }
                _uiState.value = room.copy(
                    hostId = hostId,
                    coHostId = coHostId,
                    isMainHost = room.userId == hostId,
                    isCoHost = coHostId.isNotBlank() && room.userId == coHostId,
                    isHost = room.userId == hostId || (coHostId.isNotBlank() && room.userId == coHostId),
                    participants = updatedParticipants
                )
                if (coHostId.isNotBlank() && coHostName.isNotBlank() && notifyRoomAlerts.value) {
                    _events.tryEmit("$coHostName is now a co-host")
                }
            }

            "room_mode_changed" -> {
                val room = _uiState.value as? PixelJamUiState.InRoom ?: return
                if (payload == null) return
                val mode = PixelJamRoomMode.fromWire(payload.optString("room_mode"))
                _uiState.value = room.copy(roomMode = mode)
                _events.tryEmit("Room mode: ${mode.label}")
            }

            "chat_message" -> {
                if (payload == null) return
                val msg = parseChatMessage(payload) ?: return
                _chatMessages.value = (_chatMessages.value + msg).takeLast(100)
            }

            "sync_playback" -> {
                if (payload == null) return
                val parsed = parsePlaybackState(payload, action = payload.optString("action"))
                _playbackState.value = parsed
                if (parsed.queue.isNotEmpty() || parsed.action == "queue_clear") {
                    _sharedQueue.value = parsed.queue
                }
            }

            "queue_updated" -> {
                if (payload == null) return
                _sharedQueue.value = parseTrackList(payload.optJSONArray("queue"))
            }

            "suggestion_received" -> {
                if (payload == null) return
                val trackObj = payload.optJSONObject("track") ?: return
                val track = parseTrack(trackObj) ?: return
                val suggestion = PixelJamSuggestion(
                    suggestionId = payload.optString("suggestion_id"),
                    fromUserId = payload.optString("from_user_id"),
                    fromUserName = payload.optString("from_user_name"),
                    track = track
                )
                _suggestions.value = (_suggestions.value.filterNot { it.suggestionId == suggestion.suggestionId } + suggestion)
                if (notifyRoomAlerts.value) {
                    _events.tryEmit("${suggestion.fromUserName} suggested \"${track.title}\"")
                }
            }

            "cooldown" -> {
                if (payload != null) {
                    val retryAt = payload.optLong("retry_at_ms", 0L)
                    if (retryAt > 0L) _cooldownUntilMs.value = retryAt
                }
            }

            "server_shutdown" -> {
                if (isInOrReconnectingRoom() || activeRoomCode != null) {
                    val reason = payload?.optString("reason").orEmpty().ifBlank { "server_restart" }
                    handleServerRestartKick(reason = reason)
                }
            }

            "error" -> {
                if (payload == null) return
                val errCode = payload.optString("code")
                val errMsg = payload.optString("message").ifBlank { "PixelJam error" }
                val retryAt = payload.optLong("retry_at_ms", 0L)
                if (retryAt > 0L) _cooldownUntilMs.value = retryAt
                if ((errCode == "room_not_found" || errCode == "not_in_room") && isInOrReconnectingRoom()) {
                    handleServerRestartKick(reason = "room_expired")
                    return
                }
                if (_uiState.value is PixelJamUiState.InRoom) {
                    _events.tryEmit(errMsg)
                } else {
                    _uiState.value = PixelJamUiState.Error(errMsg, errCode)
                }
            }
        }
    }

    private fun trackToJson(track: PixelJamTrack): JSONObject = JSONObject().apply {
        put("id", track.id)
        put("title", track.title)
        put("artist", track.artist)
        if (track.album.isNotBlank()) put("album", track.album)
        if (track.artworkUrl.isNotBlank()) put("artwork_url", track.artworkUrl)
        if (track.durationMs > 0L) put("duration_ms", track.durationMs)
        if (track.source.isNotBlank()) put("source", track.source)
        if (track.suggestedBy.isNotBlank()) put("suggested_by", track.suggestedBy)
        if (track.isAutoDj) put("is_autodj", true)
    }

    private fun parseTrack(obj: JSONObject?): PixelJamTrack? {
        if (obj == null) return null
        val id = obj.optString("id")
        val title = obj.optString("title")
        if (id.isBlank() && title.isBlank()) return null
        return PixelJamTrack(
            id = id.ifBlank { title },
            title = title.ifBlank { "Unknown track" },
            artist = obj.optString("artist", ""),
            album = obj.optString("album", ""),
            artworkUrl = obj.optString("artwork_url", ""),
            durationMs = obj.optLong("duration_ms", 0L),
            source = obj.optString("source", "youtube"),
            suggestedBy = obj.optString("suggested_by", ""),
            isAutoDj = obj.optBoolean("is_autodj", false)
        )
    }

    private fun parseTrackList(arr: JSONArray?): List<PixelJamTrack> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                parseTrack(arr.optJSONObject(i))?.let { add(it) }
            }
        }
    }

    private fun parsePlaybackState(obj: JSONObject, action: String): PixelJamPlaybackState {
        val trackObj = obj.optJSONObject("track") ?: obj.optJSONObject("current_track")
        return PixelJamPlaybackState(
            action = action,
            fromUserId = obj.optString("from_user_id", ""),
            currentTrack = parseTrack(trackObj),
            positionMs = obj.optLong("position_ms", 0L),
            serverTimeMs = obj.optLong("server_time_ms", System.currentTimeMillis()),
            isPlaying = obj.optBoolean("is_playing", false),
            queue = parseTrackList(obj.optJSONArray("queue")),
            revision = obj.optLong("revision", 0L)
        )
    }

    private fun parseParticipants(arr: JSONArray?): List<PixelJamParticipant> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    PixelJamParticipant(
                        userId = obj.optString("user_id"),
                        name = obj.optString("name"),
                        avatarUrl = obj.optString("avatar_url", ""),
                        isHost = obj.optBoolean("is_host", false),
                        isCoHost = obj.optBoolean("is_cohost", false),
                        joinedAt = obj.optLong("joined_at", 0L)
                    )
                )
            }
        }
    }

    private fun parseChatMessage(obj: JSONObject): PixelJamChatMessage? {
        val id = obj.optString("id")
        val text = obj.optString("text")
        if (text.isBlank()) return null
        val replyObj = obj.optJSONObject("reply_to")
        val reply = replyObj?.let {
            PixelJamChatReply(
                id = it.optString("id"),
                userId = it.optString("user_id"),
                userName = it.optString("user_name"),
                text = it.optString("text")
            )
        }
        return PixelJamChatMessage(
            id = id.ifBlank { "${obj.optLong("timestamp")}_${obj.optString("user_id")}" },
            userId = obj.optString("user_id"),
            userName = obj.optString("user_name"),
            avatarUrl = obj.optString("avatar_url", ""),
            isHost = obj.optBoolean("is_host", false),
            text = text,
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            replyTo = reply
        )
    }

    private fun parseChatHistory(arr: JSONArray?): List<PixelJamChatMessage> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parseChatMessage(obj)?.let { add(it) }
            }
        }
    }
}
