@file:Suppress("UNCHECKED_CAST")

package com.saurav.pixelmusic.data.session

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.saurav.pixelmusic.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend for Listen Together group-listening sessions.
 *
 * One session = one node under `sessions/<CODE>` in Firebase Realtime
 * Database:
 * - `meta`: host name + creation time (removed when the host leaves, which
 *   also deletes the whole session).
 * - `members`: one child per participant, auto-removed on disconnect.
 * - `state`: the host's latest [SessionTrack] snapshot.
 *
 * Each device streams its own audio; only the "what / playing / where" is
 * synced. Anonymous Firebase Auth keeps the database rules simple.
 */
@Singleton
class ListenTogetherManager @Inject constructor(
    @AppScope private val appScope: CoroutineScope
) {

    private val _uiState = MutableStateFlow<ListenTogetherUiState>(ListenTogetherUiState.Idle)
    val uiState: StateFlow<ListenTogetherUiState> = _uiState.asStateFlow()

    private val _remoteState = MutableStateFlow<SessionTrack?>(null)
    val remoteState: StateFlow<SessionTrack?> = _remoteState.asStateFlow()

    private var sessionRef: DatabaseReference? = null
    private var memberRef: DatabaseReference? = null
    private var stateListener: ValueEventListener? = null
    private var metaListener: ValueEventListener? = null
    private var membersListener: ChildEventListener? = null
    private var sessionCode: String? = null
    private var isHost = false
    private var hasReceivedState = false
    private var lastPublishedSignature: String? = null
    private val memberMap = LinkedHashMap<String, SessionMember>()
    private var heartbeatJob: Job? = null
    private var livenessJob: Job? = null
    private var reactionsListener: ChildEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private val _reactionEvents = MutableStateFlow<List<ReactionEvent>>(emptyList())
    val reactionEvents: StateFlow<List<ReactionEvent>> = _reactionEvents.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private var myName: String = ""
    private var lastReactionTs: Long = 0L
    private var lastMessageTs: Long = 0L
    private val lovedVideoIds = mutableSetOf<String>()

    companion object {
        /** How often each member refreshes its presence heartbeat. */
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        /** A member counts as live while its heartbeat is fresher than this. */
        private const val MEMBER_LIVE_WINDOW_MS = 12_000L
        /** Minimal debounce between emoji reactions (prevents network socket floods). */
        private const val REACTION_DEBOUNCE_MS = 250L
        /** Minimal debounce between preset messages. */
        private const val MESSAGE_DEBOUNCE_MS = 250L
        /** Reactions/messages older than this are pruned everywhere. */
        private const val SOCIAL_TTL_MS = 60_000L
    }

    fun isHostActive(): Boolean = _uiState.value is ListenTogetherUiState.Hosting
    fun isGuestActive(): Boolean = _uiState.value is ListenTogetherUiState.Guest
    fun currentCode(): String? = sessionCode

    // ---------------------------------------------------------- hosting

    /**
     * Creates a session and registers the host as its first member.
     * Returns true on success (state becomes [ListenTogetherUiState.Hosting]).
     */
    suspend fun startHosting(hostName: String, photoUrl: String? = null): Boolean {
        val cleanName = hostName.trim().ifBlank { "Host" }.take(24)
        if (!ListenTogetherFirebase.isReady()) {
            _uiState.value = ListenTogetherUiState.Error("Listen Together isn't available right now.")
            return false
        }
        cleanupRefs()
        _uiState.value = ListenTogetherUiState.Creating
        if (!ListenTogetherFirebase.ensureSignedIn()) {
            _uiState.value = ListenTogetherUiState.Error("Couldn't sign in. Check your connection and try again.")
            return false
        }
        return try {
            val db = FirebaseDatabase.getInstance()
            // Pick a room code that isn't already taken (6 attempts, then accept).
            var code = generateSessionCode()
            run {
                repeat(6) {
                    val exists = runCatching {
                        db.getReference("sessions/$code/meta").get().await().exists()
                    }.getOrDefault(false)
                    if (!exists) return@run
                    code = generateSessionCode()
                }
            }

            sessionRef = db.getReference("sessions/$code")
            sessionRef!!.child("meta").setValue(
                mapOf(
                    "hostName" to cleanName,
                    "createdAt" to ServerValue.TIMESTAMP
                )
            ).await()
            memberRef = sessionRef!!.child("members").push().also { ref ->
                ref.setValue(memberPayload(cleanName, photoUrl)).await()
                ref.onDisconnect().removeValue()
            }
            startHeartbeat()
            // Guests watch `meta`: when it vanishes (host disconnect),
            // their watchdog ends the session for them.
            sessionRef!!.child("meta").onDisconnect().removeValue()

            sessionCode = code
            isHost = true
            myName = cleanName
            lastPublishedSignature = null
            _remoteState.value = null
            attachMembersListener()
            attachSocialListeners()
            _uiState.value = ListenTogetherUiState.Hosting(
                code,
                listOf(SessionMember(cleanName, System.currentTimeMillis(), isLive = true))
            )
            Timber.d("ListenTogether: hosting session %s", code)
            true
        } catch (t: Throwable) {
            Timber.w(t, "ListenTogether: startHosting failed")
            cleanupRefs()
            _uiState.value = ListenTogetherUiState.Error("Couldn't start the session. Try again.")
            false
        }
    }

    // ----------------------------------------------------------- joining

    /**
     * Joins the session with the given room code.
     * Returns true on success (state becomes [ListenTogetherUiState.Guest]).
     */
    suspend fun joinSession(code: String, guestName: String, photoUrl: String? = null): Boolean {
        val cleanCode = code.trim().uppercase().filter { it in 'A'..'Z' }
        if (cleanCode.length != 6) {
            _uiState.value = ListenTogetherUiState.Error("That code doesn't look right — it should be 6 letters.")
            return false
        }
        val cleanName = guestName.trim().ifBlank { "Guest" }.take(24)
        if (!ListenTogetherFirebase.isReady()) {
            _uiState.value = ListenTogetherUiState.Error("Listen Together isn't available right now.")
            return false
        }
        cleanupRefs()
        _uiState.value = ListenTogetherUiState.Joining
        if (!ListenTogetherFirebase.ensureSignedIn()) {
            _uiState.value = ListenTogetherUiState.Error("Couldn't sign in. Check your connection and try again.")
            return false
        }
        return try {
            val db = FirebaseDatabase.getInstance()
            val meta = db.getReference("sessions/$cleanCode/meta").get().await()
            if (!meta.exists()) {
                _uiState.value = ListenTogetherUiState.Error("Couldn't find that session. Check the code and try again.")
                return false
            }
            val hostName = (meta.value as? Map<String, Any?>)?.get("hostName") as? String ?: "Host"

            sessionRef = db.getReference("sessions/$cleanCode")
            memberRef = sessionRef!!.child("members").push().also { ref ->
                ref.setValue(memberPayload(cleanName, photoUrl)).await()
                ref.onDisconnect().removeValue()
            }
            startHeartbeat()

            sessionCode = cleanCode
            isHost = false
            myName = cleanName
            attachGuestListeners()
            attachMembersListener()
            attachSocialListeners()
            _uiState.value = ListenTogetherUiState.Guest(hostName)
            Timber.d("ListenTogether: joined session %s", cleanCode)
            true
        } catch (t: Throwable) {
            Timber.w(t, "ListenTogether: joinSession failed")
            cleanupRefs()
            _uiState.value = ListenTogetherUiState.Error("Couldn't join. Check your connection and try again.")
            false
        }
    }

    // ------------------------------------------------------------ leaving

    /** Leaves the session. Hosts also delete the room so guests are released. */
    fun leaveSession() {
        // Stop the heartbeat first so it can't recreate our member node
        // after we've removed it.
        heartbeatJob?.cancel()
        heartbeatJob = null
        appScope.launch {
            runCatching {
                memberRef?.onDisconnect()?.cancel()
                memberRef?.removeValue()?.await()
                if (isHost) {
                    sessionRef?.removeValue()?.await()
                }
            }
            cleanupRefs()
            _uiState.value = ListenTogetherUiState.Idle
            _remoteState.value = null
        }
    }

    /** Clears a shown error back to idle (e.g. when the sheet is dismissed). */
    fun clearError() {
        if (_uiState.value is ListenTogetherUiState.Error) {
            _uiState.value = ListenTogetherUiState.Idle
        }
    }

    // ---------------------------------------------------------- publishing

    /**
     * Publishes the host's playback snapshot. Called ~1/sec; writes are
     * throttled to real changes (new track, play/pause flip, or >= 2s of
     * position movement) to stay far under the free-tier limits.
     */
    fun publishHostState(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        isPlaying: Boolean,
        positionMs: Long
    ) {
        val ref = sessionRef ?: return
        if (!isHostActive()) return
        val roundedPosition = (positionMs / 2000) * 2000
        val signature = "$videoId|$isPlaying|$roundedPosition"
        if (signature == lastPublishedSignature) return
        lastPublishedSignature = signature
        val track = SessionTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isPlaying = isPlaying,
            positionMs = positionMs,
            updatedAtMs = System.currentTimeMillis()
        )
        ref.child("state").setValue(track.toMap())
    }

    // ----------------------------------------------------------- listeners

    /** Guest listeners: host snapshots + session-existence watchdog. */
    private fun attachGuestListeners() {
        val ref = sessionRef ?: return
        detachGuestListeners()
        hasReceivedState = false
        stateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = snapshot.value as? Map<String, Any?>
                if (map == null) {
                    // Null before the first snapshot just means the host hasn't
                    // published yet; null afterwards means they ended it.
                    if (hasReceivedState && isGuestActive()) {
                        _uiState.value = ListenTogetherUiState.Error("The host ended the session.")
                    }
                    return
                }
                hasReceivedState = true
                _remoteState.value = SessionTrack.fromMap(map)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("ListenTogether: state listener cancelled: %s", error.message)
            }
        }.also { ref.child("state").addValueEventListener(it) }

        // Fires even before the first publish, so a guest never sits in a
        // dead session when the host leaves immediately.
        metaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() && isGuestActive()) {
                    _uiState.value = ListenTogetherUiState.Error("The host ended the session.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("ListenTogether: meta listener cancelled: %s", error.message)
            }
        }.also { ref.child("meta").addValueEventListener(it) }
    }

    private fun detachGuestListeners() {
        val ref = sessionRef
        stateListener?.let { ref?.child("state")?.removeEventListener(it) }
        stateListener = null
        metaListener?.let { ref?.child("meta")?.removeEventListener(it) }
        metaListener = null
    }

    /** Host listener: keeps the member list live, with liveness from heartbeats. */
    private fun attachMembersListener() {
        val ref = sessionRef?.child("members") ?: return
        detachMembersListener()
        memberMap.clear()
        membersListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                parseMember(snapshot)?.let { memberMap[snapshot.key ?: return] = it }
                refreshMemberList()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                parseMember(snapshot)?.let { memberMap[snapshot.key ?: return] = it }
                refreshMemberList()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                memberMap.remove(snapshot.key)
                refreshMemberList()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) {
                Timber.w("ListenTogether: members listener cancelled: %s", error.message)
            }
        }.also { ref.addChildEventListener(it) }
        // Recompute liveness periodically so a missed heartbeat flips a
        // member to "reconnecting" even when no child event fires.
        livenessJob?.cancel()
        livenessJob = appScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                refreshMemberList()
                val cutoff = System.currentTimeMillis() - SOCIAL_TTL_MS
                _reactionEvents.value = _reactionEvents.value.filter { it.ts == 0L || it.ts >= cutoff }
                _chatMessages.value = _chatMessages.value.filter { it.ts == 0L || it.ts >= cutoff }
            }
        }
    }

    private fun detachMembersListener() {
        val ref = sessionRef?.child("members")
        membersListener?.let { ref?.removeEventListener(it) }
        membersListener = null
        livenessJob?.cancel()
        livenessJob = null
    }

    /** Member payload: name, photo and a presence heartbeat timestamp. */
    private fun memberPayload(name: String, photoUrl: String?): Map<String, Any> =
        buildMap {
            put("name", name)
            put("lastSeen", ServerValue.TIMESTAMP)
            if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl)
        }

    /** Keeps our own member entry fresh so the host sees us as live. */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = appScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                runCatching {
                    memberRef?.child("lastSeen")?.setValue(ServerValue.TIMESTAMP)?.await()
                }
            }
        }
    }

    private fun parseMember(snapshot: DataSnapshot): SessionMember? {
        return when (val raw = snapshot.value) {
            is Map<*, *> -> {
                val name = raw["name"] as? String ?: return null
                val lastSeen = (raw["lastSeen"] as? Number)?.toLong() ?: 0L
                val photoUrl = raw["photoUrl"] as? String
                SessionMember(name = name, lastSeenMs = lastSeen, photoUrl = photoUrl)
            }
            // Older app versions wrote a plain name string with no heartbeat.
            is String -> SessionMember(name = raw, lastSeenMs = 0L, isLegacy = true)
            else -> null
        }
    }

    /** Re-emits the session state with fresh member liveness when anything changed. */
    private fun refreshMemberList() {
        val code = sessionCode ?: return
        val now = System.currentTimeMillis()
        val members = memberMap.values.map {
            // Legacy entries have no heartbeat; their presence alone means
            // they're connected (onDisconnect removes them when they drop).
            it.copy(isLive = it.isLegacy || now - it.lastSeenMs < MEMBER_LIVE_WINDOW_MS)
        }
        when (val current = _uiState.value) {
            is ListenTogetherUiState.Hosting ->
                if (members != current.members) {
                    _uiState.value = ListenTogetherUiState.Hosting(code, members)
                }
            is ListenTogetherUiState.Guest ->
                if (members != current.members) {
                    _uiState.value = current.copy(members = members)
                }
            else -> Unit
        }
    }

    // ------------------------------------------------------------ social

    /**
     * Sends an emoji reaction to the room.
     * Returns false when the per-user cooldown blocks it.
     */
    fun sendReaction(emoji: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastReactionTs < REACTION_DEBOUNCE_MS) return false
        lastReactionTs = now
        pushSocial("reactions", mapOf("emoji" to emoji))
        return true
    }

    /**
     * Sends the "loved this" reaction, once per song.
     * Returns false when already sent for this song or the debounce blocks it.
     */
    fun sendLovedReaction(videoId: String): Boolean {
        if (videoId.isBlank() || !lovedVideoIds.add(videoId)) return false
        val now = System.currentTimeMillis()
        if (now - lastReactionTs < REACTION_DEBOUNCE_MS) {
            lovedVideoIds.remove(videoId)
            return false
        }
        lastReactionTs = now
        pushSocial("reactions", mapOf("emoji" to "\uD83D\uDC9C", "loved" to true))
        return true
    }

    /**
     * Sends a preset message to the room.
     * Returns false when the debounce blocks it.
     */
    fun sendPresetMessage(text: String): Boolean {
        val clean = text.trim().take(48)
        if (clean.isEmpty()) return false
        val now = System.currentTimeMillis()
        if (now - lastMessageTs < MESSAGE_DEBOUNCE_MS) return false
        lastMessageTs = now
        pushSocial("messages", mapOf("text" to clean))
        return true
    }

    private fun pushSocial(node: String, fields: Map<String, Any>) {
        val ref = sessionRef?.child(node)?.push() ?: return
        val payload = HashMap<String, Any>(fields).apply {
            put("from", myName.ifBlank { "?" })
            put("ts", ServerValue.TIMESTAMP)
        }
        ref.setValue(payload)
        // Best-effort expiry so these nodes don't grow while nobody watches.
        ref.onDisconnect().removeValue()
    }

    /** Subscribes to reactions + preset messages (host and guests alike). */
    private fun attachSocialListeners() {
        val ref = sessionRef ?: return
        detachSocialListeners()
        reactionsListener = object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) =
                handleSocialChild(s, isReaction = true)
            override fun onChildChanged(s: DataSnapshot, p: String?) = Unit
            override fun onChildRemoved(s: DataSnapshot) {
                val key = s.key ?: return
                _reactionEvents.value = _reactionEvents.value.filterNot { it.key == key }
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) = Unit
            override fun onCancelled(e: DatabaseError) =
                Timber.w("ListenTogether: reactions listener cancelled: %s", e.message)
        }.also { ref.child("reactions").addChildEventListener(it) }
        messagesListener = object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) =
                handleSocialChild(s, isReaction = false)
            override fun onChildChanged(s: DataSnapshot, p: String?) = Unit
            override fun onChildRemoved(s: DataSnapshot) {
                val key = s.key ?: return
                _chatMessages.value = _chatMessages.value.filterNot { it.key == key }
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) = Unit
            override fun onCancelled(e: DatabaseError) =
                Timber.w("ListenTogether: messages listener cancelled: %s", e.message)
        }.also { ref.child("messages").addChildEventListener(it) }
    }

    private fun detachSocialListeners() {
        val ref = sessionRef
        reactionsListener?.let { ref?.child("reactions")?.removeEventListener(it) }
        reactionsListener = null
        messagesListener?.let { ref?.child("messages")?.removeEventListener(it) }
        messagesListener = null
    }

    private fun handleSocialChild(s: DataSnapshot, isReaction: Boolean) {
        val key = s.key ?: return
        val map = s.value as? Map<String, Any?> ?: return
        val ts = (map["ts"] as? Number)?.toLong() ?: 0L
        if (ts > 0L && System.currentTimeMillis() - ts > SOCIAL_TTL_MS) {
            // Anyone who sees a stale entry prunes it; keeps the nodes small.
            s.ref.removeValue()
            return
        }
        val from = map["from"] as? String ?: "?"
        if (isReaction) {
            val emoji = map["emoji"] as? String ?: return
            val loved = map["loved"] as? Boolean ?: false
            val cur = _reactionEvents.value
            if (cur.none { it.key == key }) {
                _reactionEvents.value = (cur + ReactionEvent(key, emoji, from, ts, loved)).takeLast(20)
            }
        } else {
            val text = map["text"] as? String ?: return
            val cur = _chatMessages.value
            if (cur.none { it.key == key }) {
                _chatMessages.value = (cur + ChatMessage(key, text, from, ts)).takeLast(20)
            }
        }
    }

    private fun cleanupRefs() {
        detachGuestListeners()
        detachMembersListener()
        detachSocialListeners()
        myName = ""
        lovedVideoIds.clear()
        lastReactionTs = 0L
        lastMessageTs = 0L
        _reactionEvents.value = emptyList()
        _chatMessages.value = emptyList()
        heartbeatJob?.cancel()
        heartbeatJob = null
        memberMap.clear()
        sessionRef = null
        memberRef = null
        sessionCode = null
        isHost = false
        hasReceivedState = false
        lastPublishedSignature = null
    }

    /** 6 letters, minus the confusable I/L/O. */
    private fun generateSessionCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ"
        return buildString {
            repeat(6) { append(alphabet.random()) }
        }
    }
}
