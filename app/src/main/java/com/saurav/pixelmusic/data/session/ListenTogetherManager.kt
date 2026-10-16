@file:Suppress("UNCHECKED_CAST")

package com.saurav.pixelmusic.data.session

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
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
 * - `meta`: hostName + hostKey + createdAt. On unexpected host disconnect
 *   the host stamps `hostGoneAt` instead of deleting the room; after a
 *   grace period a guest claims it (host transfer) via transaction.
 *   Vanishes only when the host intentionally ends the session.
 * - `members`: one child per participant, auto-removed on disconnect.
 * - `state`: the host's latest [SessionTrack] snapshot, with a monotonic
 *   `revision` (guests ignore snapshots that don't advance it).
 * - `ready`: per-guest buffering-ready flags, used during coordinated
 *   track starts.
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

    private val _connectionState = MutableStateFlow(ListenTogetherConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ListenTogetherConnectionState> = _connectionState.asStateFlow()

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
    private var publishRevision = 0L
    private var lastPublishedVideoId: String? = null
    private var lastSeenRevision = -1L
    private var lastHostPositionMs = 0L
    private var lastHostTitle = ""
    private var lastHostArtist = ""
    private var lastHostArtworkUrl = ""
    private var lastHostIsPlaying = false
    private var bufferRoundActive = false
    private var bufferVideoId = ""
    private var bufferDeadlineMs = 0L
    private var bufferTimeoutJob: Job? = null
    private var serverTimeOffsetMs = 0L
    private var serverTimeOffsetAttached = false
    private var connectionListener: ValueEventListener? = null
    private var wasConnected = false
    private val readyKeys = mutableSetOf<String>()
    private var readyListener: ChildEventListener? = null
    private var hostMetaListener: ValueEventListener? = null
    private var lastMetaHostGoneAt = 0L
    private var lastMetaHostName = ""
    private var lastClaimAttemptMs = 0L
    private var lastReadyVideoId: String? = null
    private val memberMap = LinkedHashMap<String, SessionMember>()
    private var localSyncVideoId: String? = null
    private var localSyncRevision: Long = 0L
    private var heartbeatJob: Job? = null
    private var livenessJob: Job? = null
    private var reactionsListener: ChildEventListener? = null
    private var messagesListener: ChildEventListener? = null
    private val _reactionEvents = MutableStateFlow<List<ReactionEvent>>(emptyList())
    val reactionEvents: StateFlow<List<ReactionEvent>> = _reactionEvents.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _songRequests = MutableStateFlow<List<SongRequest>>(emptyList())
    val songRequests: StateFlow<List<SongRequest>> = _songRequests.asStateFlow()
    private var myName: String = ""
    private var requestsListener: ChildEventListener? = null
    private val myRequestVotes = mutableSetOf<String>()
    private var lastReactionTs: Long = 0L
    private var lastMessageTs: Long = 0L
    private val lovedVideoIds = mutableSetOf<String>()

    companion object {
        /** Maximum participants in a single Listen Together session (Host + Guests). */
        const val MAX_SESSION_MEMBERS = 4

        /** How often each member refreshes its presence heartbeat. */
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        /** A member counts as live while its heartbeat is fresher than this. */
        private const val MEMBER_LIVE_WINDOW_MS = 12_000L
        /** Minimal debounce between emoji reactions (prevents network socket floods). */
        private const val REACTION_DEBOUNCE_MS = 250L
        /** Minimal debounce between preset messages. */
        private const val MESSAGE_DEBOUNCE_MS = 250L
        /** Reactions are intentionally short-lived so the overlay stays ephemeral. */
        private const val REACTION_TTL_MS = 60_000L
        /** Messages remain visible longer than reactions, but are still ephemeral. */
        private const val MESSAGE_TTL_MS = 5 * 60_000L
        /** Sync protocol version written into member entries (buffering needs >= 2). */
        private const val SYNC_PROTO_VERSION = 2
        /** Grace period after the host drops before a guest may claim the room. */
        private const val HOST_GRACE_MS = 20_000L
        /** Minimum gap between host-claim attempts. */
        private const val CLAIM_RETRY_MS = 10_000L
        /** Give up on a room when the host has been gone this long with no takeover. */
        private const val HOST_ABANDON_MS = 300_000L
        /** Max wait for guests to report buffered before starting anyway. */
        private const val BUFFER_TIMEOUT_MS = 12_000L
    }

    fun isHostActive(): Boolean = _uiState.value is ListenTogetherUiState.Hosting

    /** Publishes this device's latest local sync point for the member-status UI. */
    fun updateLocalSyncStatus(videoId: String?, revision: Long) {
        localSyncVideoId = videoId?.ifBlank { null }
        localSyncRevision = revision.coerceAtLeast(0L)
    }
    fun isGuestActive(): Boolean = _uiState.value is ListenTogetherUiState.Guest
    fun currentCode(): String? = sessionCode
    fun estimatedServerNowMs(): Long = System.currentTimeMillis() + serverTimeOffsetMs

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
        _connectionState.value = ListenTogetherConnectionState.CONNECTING
        _uiState.value = ListenTogetherUiState.Creating
        if (!ListenTogetherFirebase.ensureSignedIn()) {
            _uiState.value = ListenTogetherUiState.Error("Couldn't sign in. Check your connection and try again.")
            return false
        }
        return try {
            attachServerTimeOffsetListener()
            attachConnectionListener()
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
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Missing Firebase user")
            memberRef = sessionRef!!.child("members/$uid").also { ref ->
                ref.setValue(memberPayload(cleanName, photoUrl)).await()
                ref.onDisconnect().removeValue()
            }
            sessionRef!!.child("meta").setValue(
                mapOf(
                    "hostName" to cleanName,
                    "hostKey" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
                    "maxMembers" to MAX_SESSION_MEMBERS,
                    "createdAt" to ServerValue.TIMESTAMP
                )
            ).await()
            startHeartbeat()
            // On unexpected disconnect the host marks itself gone instead of
            // deleting the room: after a grace period a guest claims it (host
            // transfer) so the party survives a crash. Intentional leaves
            // cancel this first (see leaveSession).
            sessionRef!!.child("meta").onDisconnect().updateChildren(
                mapOf("hostGoneAt" to ServerValue.TIMESTAMP)
            )
            attachHostMetaListener()

            sessionCode = code
            isHost = true
            myName = cleanName
            lastPublishedSignature = null
            publishRevision = 0L
            lastPublishedVideoId = null
            lastSeenRevision = -1L
            lastMetaHostGoneAt = 0L
            lastMetaHostName = ""
            lastClaimAttemptMs = 0L
            lastReadyVideoId = null
            _remoteState.value = null
            attachMembersListener()
            attachSocialListeners()
            attachRequestsListener()
            _uiState.value = ListenTogetherUiState.Hosting(
                code,
                cleanName,
                listOf(SessionMember(cleanName, System.currentTimeMillis(), isLive = true))
            )
            Timber.d("ListenTogether: hosting session %s", code)
            true
        } catch (t: Throwable) {
            Timber.w(t, "ListenTogether: startHosting failed")
            runCatching { sessionRef?.removeValue()?.await() }
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
        if (cleanCode.length != 7) {
            _uiState.value = ListenTogetherUiState.Error("That code doesn't look right — it should be 7 letters.")
            return false
        }
        val cleanName = guestName.trim().ifBlank { "Guest" }.take(24)
        if (!ListenTogetherFirebase.isReady()) {
            _uiState.value = ListenTogetherUiState.Error("Listen Together isn't available right now.")
            return false
        }
        cleanupRefs()
        _connectionState.value = ListenTogetherConnectionState.CONNECTING
        _uiState.value = ListenTogetherUiState.Joining
        if (!ListenTogetherFirebase.ensureSignedIn()) {
            _uiState.value = ListenTogetherUiState.Error("Couldn't sign in. Check your connection and try again.")
            return false
        }
        return try {
            attachServerTimeOffsetListener()
            attachConnectionListener()
            val db = FirebaseDatabase.getInstance()
            val meta = db.getReference("sessions/$cleanCode/meta").get().await()
            if (!meta.exists()) {
                _uiState.value = ListenTogetherUiState.Error("Couldn't find that session. Check the code and try again.")
                return false
            }
            val metaMap = meta.value as? Map<String, Any?>
            val hostName = metaMap?.get("hostName") as? String ?: "Host"
            val maxAllowed = (metaMap?.get("maxMembers") as? Number)?.toInt() ?: MAX_SESSION_MEMBERS

            sessionRef = db.getReference("sessions/$cleanCode")
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Missing Firebase user")

            val membersSnapshot = sessionRef!!.child("members").get().await()
            val isAlreadyMember = membersSnapshot.hasChild(uid)
            if (!isAlreadyMember && membersSnapshot.childrenCount >= maxAllowed) {
                _uiState.value = ListenTogetherUiState.Error("This session is full (maximum $maxAllowed people).")
                cleanupRefs()
                return false
            }

            memberRef = sessionRef!!.child("members/$uid").also { ref ->
                ref.setValue(memberPayload(cleanName, photoUrl)).await()
                ref.onDisconnect().removeValue()
            }

            val postCheck = sessionRef!!.child("members").get().await()
            if (postCheck.childrenCount > maxAllowed) {
                val allowedKeys = postCheck.children.take(maxAllowed).mapNotNull { it.key }
                if (uid !in allowedKeys) {
                    memberRef?.removeValue()?.await()
                    cleanupRefs()
                    _uiState.value = ListenTogetherUiState.Error("This session is full (maximum $maxAllowed people).")
                    return false
                }
            }

            startHeartbeat()

            sessionCode = cleanCode
            isHost = false
            myName = cleanName
            attachGuestListeners()
            attachMembersListener()
            attachSocialListeners()
            attachRequestsListener()
            _uiState.value = ListenTogetherUiState.Guest(hostName, cleanCode)
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

    /** Guests leave without ending the shared room. */
    fun leaveSession() {
        // Stop the heartbeat first so it can't recreate our member node
        // after we've removed it.
        heartbeatJob?.cancel()
        heartbeatJob = null
        appScope.launch {
            runCatching {
                memberRef?.onDisconnect()?.cancel()
                memberRef?.removeValue()?.await()
                // Drop our buffering-ready flag too (its onDisconnect only
                // fires on connection loss, not on this explicit leave).
                memberRef?.key?.let { key ->
                    sessionRef?.child("ready")?.child(key)?.removeValue()?.await()
                }
                // Hosted rooms remain available for host takeover; explicit ending uses endSession().
            }
            cleanupRefs()
            _uiState.value = ListenTogetherUiState.Idle
            _remoteState.value = null
        }
    }

    /** Explicitly ends a hosted room and releases all guests. */
    fun endSession() {
        if (!isHost) return
        heartbeatJob?.cancel()
        heartbeatJob = null
        appScope.launch {
            runCatching {
                sessionRef?.child("meta")?.onDisconnect()?.cancel()
                sessionRef?.removeValue()?.await()
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
     *
     * On a track change with an audience, a coordinated-buffering round
     * runs instead: guests preload the track and report ready, then the
     * go-signal starts everyone together.
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
        lastHostPositionMs = positionMs
        lastHostTitle = title
        lastHostArtist = artist
        lastHostArtworkUrl = artworkUrl
        lastHostIsPlaying = isPlaying
        if (bufferRoundActive && videoId != bufferVideoId) {
            // Skipped again mid-round: restart the round for the new track,
            // or drop back to plain publishing when nobody can buffer.
            if (hasBufferCapableGuests()) {
                startBufferRound(ref, videoId, title, artist, artworkUrl, isPlaying, positionMs)
                return
            }
            bufferRoundActive = false
            detachReadyListener()
        }
        if (!bufferRoundActive && videoId != lastPublishedVideoId && hasBufferCapableGuests()) {
            startBufferRound(ref, videoId, title, artist, artworkUrl, isPlaying, positionMs)
            return
        }
        val pubIsPlaying = !bufferRoundActive && isPlaying
        val roundedPosition = (positionMs / 2000) * 2000
        val signature = "$videoId|$pubIsPlaying|$roundedPosition|$bufferRoundActive"
        if (signature == lastPublishedSignature) return
        lastPublishedSignature = signature
        writeState(
            ref,
            SessionTrack(
                videoId = videoId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                isPlaying = pubIsPlaying,
                positionMs = if (bufferRoundActive) 0L else positionMs,
                updatedAtMs = System.currentTimeMillis(),
                buffering = bufferRoundActive
            )
        )
    }

    /** Writes one snapshot, bumping the revision so guests can order them. */
    private fun writeState(ref: DatabaseReference, track: SessionTrack) {
        publishRevision++
        lastPublishedVideoId = track.videoId
        ref.child("state").setValue(track.copy(revision = publishRevision).toMap())
    }

    /** Guests call this once they've preloaded the buffering track. */
    fun markBufferReady(videoId: String, revision: Long) {
        if (!isGuestActive()) return
        if (videoId.isBlank() || videoId != _remoteState.value?.videoId) return
        if (revision != _remoteState.value?.revision) return
        if (videoId == lastReadyVideoId) return
        val key = memberRef?.key ?: return
        lastReadyVideoId = videoId
        val flag = sessionRef?.child("ready")?.child(key) ?: return
        flag.setValue(mapOf("videoId" to videoId, "revision" to revision, "ready" to true))
        flag.onDisconnect().removeValue()
    }

    // ------------------------------------------ coordinated buffering

    private fun hasBufferCapableGuests(): Boolean {
        val myKey = memberRef?.key
        return memberMap.any { (key, member) ->
            key != myKey && member.proto >= SYNC_PROTO_VERSION && isMemberLive(member)
        }
    }

    private fun startBufferRound(
        ref: DatabaseReference,
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        isPlaying: Boolean,
        positionMs: Long
    ) {
        stopBufferRound()
        bufferRoundActive = true
        bufferVideoId = videoId
        bufferDeadlineMs = System.currentTimeMillis() + BUFFER_TIMEOUT_MS
        lastHostIsPlaying = isPlaying
        lastHostPositionMs = positionMs
        readyKeys.clear()
        bufferTimeoutJob?.cancel()
        bufferTimeoutJob = appScope.launch {
            delay(BUFFER_TIMEOUT_MS)
            if (bufferRoundActive && bufferVideoId == videoId) finishBufferRound()
        }
        runCatching { ref.child("ready").removeValue() }
        attachReadyListener(ref)
        // Force the buffering snapshot through the throttle.
        lastPublishedSignature = null
        writeState(
            ref,
            SessionTrack(
                videoId = videoId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                isPlaying = false,
                positionMs = 0L,
                updatedAtMs = System.currentTimeMillis(),
                buffering = true
            )
        )
        Timber.d("ListenTogether: buffering round started for %s", videoId)
    }

    /** The go-signal: everyone starts from the host's live position at once. */
    private fun finishBufferRound() {
        if (!bufferRoundActive) return
        bufferRoundActive = false
        detachReadyListener()
        val ref = sessionRef ?: return
        runCatching { ref.child("ready").removeValue() }
        lastPublishedSignature = null
        writeState(
            ref,
            SessionTrack(
                videoId = bufferVideoId,
                title = lastHostTitle,
                artist = lastHostArtist,
                artworkUrl = lastHostArtworkUrl,
                isPlaying = lastHostIsPlaying,
                positionMs = lastHostPositionMs,
                updatedAtMs = System.currentTimeMillis(),
                buffering = false
            )
        )
        Timber.d("ListenTogether: buffering round done, go at %d", lastHostPositionMs)
    }

    private fun stopBufferRound() {
        bufferRoundActive = false
        detachReadyListener()
    }

    private fun attachReadyListener(ref: DatabaseReference) {
        detachReadyListener()
        readyListener = object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) {
                val map = s.value as? Map<*, *>
                val videoId = map?.get("videoId") as? String
                val revision = (map?.get("revision") as? Number)?.toLong()
                if (videoId == bufferVideoId && revision == publishRevision) s.key?.let { readyKeys.add(it) }
                checkBufferReady()
            }

            override fun onChildChanged(s: DataSnapshot, p: String?) = Unit
            override fun onChildRemoved(s: DataSnapshot) {
                s.key?.let { readyKeys.remove(it) }
            }

            override fun onChildMoved(s: DataSnapshot, p: String?) = Unit
            override fun onCancelled(e: DatabaseError) =
                Timber.w("ListenTogether: ready listener cancelled: %s", e.message)
        }.also { ref.child("ready").addChildEventListener(it) }
    }

    private fun detachReadyListener() {
        val ref = sessionRef?.child("ready")
        readyListener?.let { ref?.removeEventListener(it) }
        readyListener = null
    }

    /** Proceed when every live, buffering-capable guest is ready — or on timeout. */
    private fun checkBufferReady() {
        if (!bufferRoundActive) return
        if (System.currentTimeMillis() >= bufferDeadlineMs) {
            Timber.d("ListenTogether: buffering timed out, starting anyway")
            finishBufferRound()
            return
        }
        val myKey = memberRef?.key
        val waitingFor = memberMap.any { (key, member) ->
            key != myKey && member.proto >= SYNC_PROTO_VERSION &&
                isMemberLive(member) && key !in readyKeys
        }
        if (!waitingFor) finishBufferRound()
    }

    // ----------------------------------------------------------- listeners

    /** Guest listeners: host snapshots + session-existence watchdog. */
    private fun attachGuestListeners() {
        val ref = sessionRef ?: return
        detachGuestListeners()
        hasReceivedState = false
        lastSeenRevision = -1L
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
                val track = SessionTrack.fromMap(map)
                // Stale or reordered snapshot: the revision didn't advance.
                if (hasReceivedState && track.revision <= lastSeenRevision) return
                hasReceivedState = true
                lastSeenRevision = track.revision
                _remoteState.value = track
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("ListenTogether: state listener cancelled: %s", error.message)
            }
        }.also { ref.child("state").addValueEventListener(it) }

        // Fires even before the first publish, so a guest never sits in a
        // dead session when the host leaves immediately.
        metaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    if (isGuestActive()) {
                        _uiState.value = ListenTogetherUiState.Error("The host ended the session.")
                    }
                    return
                }
                val map = snapshot.value as? Map<String, Any?>
                lastMetaHostName = map?.get("hostName") as? String ?: lastMetaHostName
                lastMetaHostGoneAt = (map?.get("hostGoneAt") as? Number)?.toLong() ?: 0L
                // The host dropped and nobody claimed the room for a long
                // while (e.g. no guest runs the takeover logic): end it
                // instead of leaving a zombie session.
                if (isGuestActive() && lastMetaHostGoneAt > 0L &&
                    System.currentTimeMillis() - lastMetaHostGoneAt > HOST_ABANDON_MS
                ) {
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

    // ------------------------------------------------------ host transfer

    /**
     * Watches `meta/hostKey` while hosting. If another device claimed the
     * room (we dropped and the grace period passed), step down to guest
     * instead of fighting it.
     */
    private fun attachHostMetaListener() {
        val ref = sessionRef?.child("meta") ?: return
        detachHostMetaListener()
        hostMetaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isHost) return
                val map = snapshot.value as? Map<String, Any?> ?: return
                val currentKey = map["hostKey"] as? String
                val myKey = memberRef?.key
                if (!currentKey.isNullOrBlank() && !myKey.isNullOrBlank() && currentKey != myKey) {
                    demoteToGuest(map["hostName"] as? String ?: "Host")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("ListenTogether: host meta listener cancelled: %s", error.message)
            }
        }.also { ref.addValueEventListener(it) }
    }

    private fun detachHostMetaListener() {
        val ref = sessionRef?.child("meta")
        hostMetaListener?.let { ref?.removeEventListener(it) }
        hostMetaListener = null
    }

    /** We lost the room to another device; become its guest. */
    private fun demoteToGuest(newHostName: String) {
        if (!isHost) return
        Timber.d("ListenTogether: demoted, new host is %s", newHostName)
        isHost = false
        stopBufferRound()
        detachHostMetaListener()
        lastReadyVideoId = null
        attachGuestListeners()
        _uiState.value = ListenTogetherUiState.Guest(
            newHostName,
            sessionCode ?: "",
            currentMembers()
        )
    }

    /**
     * Called from the liveness tick: once the host has been gone past the
     * grace period, try to claim the room atomically. Exactly one guest wins
     * the transaction; losers see the new host and stand down.
     */
    private fun maybeClaimHost() {
        if (!isGuestActive()) return
        val goneAt = lastMetaHostGoneAt
        if (goneAt <= 0L) return
        val now = System.currentTimeMillis()
        if (now - goneAt < HOST_GRACE_MS) return
        if (now - lastClaimAttemptMs < CLAIM_RETRY_MS) return
        val ref = sessionRef?.child("meta") ?: return
        val myKey = memberRef?.key ?: return
        lastClaimAttemptMs = now
        val expectedHostName = lastMetaHostName
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                val curGoneAt = (current.child("hostGoneAt").value as? Number)?.toLong() ?: 0L
                val curHostName = current.child("hostName").value as? String
                if (curGoneAt <= 0L || curHostName != expectedHostName) return Transaction.abort()
                if (System.currentTimeMillis() - curGoneAt < HOST_GRACE_MS) return Transaction.abort()
                current.child("hostName").value = myName
                current.child("hostKey").value = myKey
                current.child("hostGoneAt").value = null
                return Transaction.success(current)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) onHostTakeoverWon()
                else Timber.d("ListenTogether: host claim not committed")
            }
        })
    }

    /** We won the election: seed the revision counter and take over publishing. */
    private fun onHostTakeoverWon() {
        val ref = sessionRef ?: return
        val code = sessionCode ?: return
        Timber.d("ListenTogether: won host election for %s", code)
        appScope.launch {
            // Seed from the freshest snapshot so the revision never goes
            // backwards for the other guests.
            val stateRev = runCatching {
                val snap = ref.child("state").get().await()
                ((snap.value as? Map<String, Any?>)?.get("revision") as? Number)?.toLong()
            }.getOrNull() ?: -1L
            publishRevision = maxOf(stateRev, _remoteState.value?.revision ?: -1L, lastSeenRevision)
            val remote = _remoteState.value
            lastHostPositionMs = remote?.positionMs ?: lastHostPositionMs
            lastHostTitle = remote?.title ?: lastHostTitle
            lastHostArtist = remote?.artist ?: lastHostArtist
            lastHostArtworkUrl = remote?.artworkUrl ?: lastHostArtworkUrl
            lastHostIsPlaying = remote?.isPlaying ?: lastHostIsPlaying
            lastPublishedVideoId = remote?.videoId
            lastPublishedSignature = null
            lastReadyVideoId = null
            stopBufferRound()
            runCatching { ref.child("ready").removeValue() }
            isHost = true
            detachGuestListeners()
            attachHostMetaListener()
            attachRequestsListener()
            _uiState.value = ListenTogetherUiState.Hosting(code, myName, currentMembers())
            refreshMemberList()
        }
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
                val now = System.currentTimeMillis()
                val reactionCutoff = now - REACTION_TTL_MS
                val messageCutoff = now - MESSAGE_TTL_MS
                _reactionEvents.value = _reactionEvents.value.filter { it.ts == 0L || it.ts >= reactionCutoff }
                _chatMessages.value = _chatMessages.value.filter { it.ts == 0L || it.ts >= messageCutoff }
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

    /** Member payload: name, photo, presence heartbeat and sync protocol version. */
    private fun memberPayload(name: String, photoUrl: String?): Map<String, Any> =
        buildMap {
            put("name", name)
            put("lastSeen", ServerValue.TIMESTAMP)
            put("proto", SYNC_PROTO_VERSION)
            put("uid", FirebaseAuth.getInstance().currentUser?.uid ?: "")
            put("syncRevision", localSyncRevision)
            localSyncVideoId?.takeIf { it.isNotBlank() }?.let { put("syncVideoId", it) }
            if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl)
        }

    /** Keeps our own member entry fresh so the host sees us as live. */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = appScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                runCatching {
                    val updates = hashMapOf<String, Any>("lastSeen" to ServerValue.TIMESTAMP)
                    val heartbeatVideoId = if (isHost) lastPublishedVideoId else localSyncVideoId
                    val heartbeatRevision = if (isHost) publishRevision else localSyncRevision
                    updates["syncVideoId"] = heartbeatVideoId.orEmpty()
                    updates["syncRevision"] = heartbeatRevision
                    memberRef?.updateChildren(updates)?.await()
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
                val proto = (raw["proto"] as? Number)?.toInt() ?: 0
                val syncRevision = (raw["syncRevision"] as? Number)?.toLong() ?: 0L
                val syncVideoId = raw["syncVideoId"] as? String
                SessionMember(
                    name = name,
                    lastSeenMs = lastSeen,
                    photoUrl = photoUrl,
                    proto = proto,
                    syncRevision = syncRevision,
                    syncVideoId = syncVideoId
                )
            }
            // Older app versions wrote a plain name string with no heartbeat.
            is String -> SessionMember(name = raw, lastSeenMs = 0L, isLegacy = true)
            else -> null
        }
    }

    /** A member counts as live while its heartbeat is fresh (legacy: by presence). */
    private fun isMemberLive(member: SessionMember): Boolean {
        val now = System.currentTimeMillis()
        // Legacy entries have no heartbeat; their presence alone means
        // they're connected (onDisconnect removes them when they drop).
        return member.isLegacy || now - member.lastSeenMs < MEMBER_LIVE_WINDOW_MS
    }

    private fun currentMembers(): List<SessionMember> =
        memberMap.values.map { it.copy(isLive = isMemberLive(it)) }.take(MAX_SESSION_MEMBERS)

    /** Re-emits the session state with fresh member liveness when anything changed. */
    private fun refreshMemberList() {
        val code = sessionCode ?: return
        val members = currentMembers()
        maybeClaimHost()
        checkBufferReady()
        when (val current = _uiState.value) {
            is ListenTogetherUiState.Hosting ->
                if (members != current.members) {
                    _uiState.value = ListenTogetherUiState.Hosting(code, myName, members)
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

    /** Submit a free-text song request for the host to review. */
    fun sendSongRequest(text: String): Boolean {
        val clean = text.trim().take(80)
        if (clean.isEmpty() || sessionRef == null) return false
        val key = sessionRef?.child("requests")?.push()?.key ?: return false
        val payload = mapOf(
            "text" to clean,
            "from" to myName.ifBlank { "?" },
            "ts" to ServerValue.TIMESTAMP,
            "votes" to emptyMap<String, Any>()
        )
        sessionRef?.child("requests")?.child(key)?.setValue(payload)
        return true
    }

    /** Toggle this user's vote on a request. Firebase UID makes the vote unique per user. */
    fun voteSongRequest(key: String): Boolean {
        if (key.isBlank() || sessionRef == null) return false
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val voteRef = sessionRef?.child("requests")?.child(key)?.child("votes")?.child(uid) ?: return false
        val currentlyVoted = key in myRequestVotes
        if (currentlyVoted) {
            myRequestVotes.remove(key)
            voteRef.removeValue()
        } else {
            myRequestVotes.add(key)
            voteRef.setValue(true)
        }
        refreshRequestVote(key)
        return true
    }

    /** The host can clear a request after acting on it. */
    fun dismissSongRequest(key: String) {
        if (!isHostActive() || key.isBlank()) return
        sessionRef?.child("requests")?.child(key)?.removeValue()
    }

    private fun refreshRequestVote(key: String) {
        sessionRef?.child("requests")?.child(key)?.get()?.addOnSuccessListener { snapshot ->
            updateSongRequestFromSnapshot(snapshot)
        }
    }

    private fun attachRequestsListener() {
        val ref = sessionRef ?: return
        detachRequestsListener()
        requestsListener = object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) = updateSongRequestFromSnapshot(s)
            override fun onChildChanged(s: DataSnapshot, p: String?) = updateSongRequestFromSnapshot(s)
            override fun onChildRemoved(s: DataSnapshot) {
                val key = s.key ?: return
                _songRequests.value = _songRequests.value.filterNot { it.key == key }
                myRequestVotes.remove(key)
            }
            override fun onChildMoved(s: DataSnapshot, p: String?) = Unit
            override fun onCancelled(e: DatabaseError) = Timber.w("ListenTogether: requests listener cancelled: %s", e.message)
        }.also { ref.child("requests").addChildEventListener(it) }
    }

    private fun detachRequestsListener() {
        val ref = sessionRef
        requestsListener?.let { ref?.child("requests")?.removeEventListener(it) }
        requestsListener = null
    }

    private fun updateSongRequestFromSnapshot(snapshot: DataSnapshot) {
        val key = snapshot.key ?: return
        val map = snapshot.value as? Map<*, *> ?: return
        val text = map["text"] as? String ?: return
        val from = map["from"] as? String ?: "?"
        val ts = (map["ts"] as? Number)?.toLong() ?: 0L
        val votesMap = map["votes"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val request = SongRequest(
            key = key,
            text = text,
            from = from,
            ts = ts,
            votes = votesMap.size,
            votedByMe = myRequestVotes.contains(key) || FirebaseAuth.getInstance().currentUser?.uid?.let { votesMap.containsKey(it) } == true
        )
        if (request.votedByMe) myRequestVotes.add(key) else myRequestVotes.remove(key)
        _songRequests.value = (_songRequests.value.filterNot { it.key == key } + request)
            .sortedWith(compareByDescending<SongRequest> { it.votes }.thenBy { it.ts })
            .take(20)
    }

    private fun handleSocialChild(s: DataSnapshot, isReaction: Boolean) {
        val key = s.key ?: return
        val map = s.value as? Map<String, Any?> ?: return
        val ts = (map["ts"] as? Number)?.toLong() ?: 0L
        val ttlMs = if (isReaction) REACTION_TTL_MS else MESSAGE_TTL_MS
        if (ts > 0L && System.currentTimeMillis() - ts > ttlMs) {
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
        detachHostMetaListener()
        detachReadyListener()
        detachMembersListener()
        detachSocialListeners()
        detachRequestsListener()
        stopBufferRound()
        publishRevision = 0L
        lastPublishedVideoId = null
        lastPublishedSignature = null
        lastSeenRevision = -1L
        lastMetaHostGoneAt = 0L
        lastMetaHostName = ""
        lastClaimAttemptMs = 0L
        lastReadyVideoId = null
        bufferVideoId = ""
        bufferDeadlineMs = 0L
        localSyncVideoId = null
        localSyncRevision = 0L
        bufferTimeoutJob?.cancel()
        bufferTimeoutJob = null
        lastHostPositionMs = 0L
        lastHostIsPlaying = false
        readyKeys.clear()
        myName = ""
        lovedVideoIds.clear()
        lastReactionTs = 0L
        lastMessageTs = 0L
        _reactionEvents.value = emptyList()
        _chatMessages.value = emptyList()
        _songRequests.value = emptyList()
        myRequestVotes.clear()
        heartbeatJob?.cancel()
        heartbeatJob = null
        memberMap.clear()
        _connectionState.value = ListenTogetherConnectionState.DISCONNECTED
        sessionRef = null
        memberRef = null
        sessionCode = null
        isHost = false
        hasReceivedState = false
        lastPublishedSignature = null
    }

    private fun attachConnectionListener() {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        if (connectionListener != null) {
            // The listener is intentionally kept alive across sessions, so a
            // new room must still refresh the current Firebase connection state.
            appScope.launch {
                runCatching {
                    val connected = connectedRef.get().await().getValue(Boolean::class.java) == true
                    if (connected) {
                        _connectionState.value = ListenTogetherConnectionState.CONNECTED
                        wasConnected = true
                    } else {
                        _connectionState.value = if (wasConnected) {
                            ListenTogetherConnectionState.RECONNECTING
                        } else {
                            ListenTogetherConnectionState.CONNECTING
                        }
                    }
                }
            }
            return
        }
        runCatching {
            connectionListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) == true
                    if (connected) {
                        _connectionState.value = ListenTogetherConnectionState.CONNECTED
                        wasConnected = true
                    } else {
                        _connectionState.value = if (wasConnected) {
                            ListenTogetherConnectionState.RECONNECTING
                        } else {
                            ListenTogetherConnectionState.CONNECTING
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _connectionState.value = ListenTogetherConnectionState.RECONNECTING
                    Timber.w("ListenTogether: connection listener cancelled: %s", error.message)
                }
            }.also { connectedRef.addValueEventListener(it) }
        }.onFailure {
            connectionListener = null
            _connectionState.value = ListenTogetherConnectionState.RECONNECTING
        }
    }

    private fun attachServerTimeOffsetListener() {
        if (serverTimeOffsetAttached) return
        serverTimeOffsetAttached = true
        runCatching {
            FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        serverTimeOffsetMs = (snapshot.value as? Number)?.toLong() ?: 0L
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Timber.w("ListenTogether: server time offset listener cancelled: %s", error.message)
                    }
                })
        }.onFailure { serverTimeOffsetAttached = false }
    }

    /** 7 letters, minus the confusable I/L/O. */
    private fun generateSessionCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ"
        return buildString {
            repeat(7) { append(alphabet.random()) }
        }
    }
}
