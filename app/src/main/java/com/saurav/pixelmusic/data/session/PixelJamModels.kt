package com.saurav.pixelmusic.data.session

import androidx.compose.runtime.Immutable

object PixelJamConfig {
    const val DEFAULT_SERVER_URL = "https://pixelmusic-jam.onrender.com"

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_SERVER_URL
        return when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("wss://", ignoreCase = true) -> "https://" + trimmed.removePrefix("wss://")
            trimmed.startsWith("ws://", ignoreCase = true) -> "http://" + trimmed.removePrefix("ws://")
            else -> "https://$trimmed"
        }
    }

    fun toWebSocketUrl(baseUrl: String): String {
        val norm = normalizeBaseUrl(baseUrl)
        return when {
            norm.startsWith("https://", ignoreCase = true) -> "wss://" + norm.substring(8) + "/ws"
            norm.startsWith("http://", ignoreCase = true) -> "ws://" + norm.substring(7) + "/ws"
            else -> "wss://$norm/ws"
        }
    }

    fun toRoomsUrl(baseUrl: String): String = "${normalizeBaseUrl(baseUrl)}/rooms"

    fun toHealthUrl(baseUrl: String): String = "${normalizeBaseUrl(baseUrl)}/health"

    const val INVITE_PAGE_URL = "https://pixelmusic.pages.dev/jam"

    fun toInviteUrl(baseUrl: String, roomCode: String): String =
        "$INVITE_PAGE_URL?code=${roomCode.trim().uppercase()}"
}

enum class PixelJamConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

enum class PixelJamRoomMode(val wireValue: String, val label: String) {
    HOST_ONLY("host_only", "Host Only"),
    COLLABORATIVE("collaborative", "Collaborative");

    companion object {
        fun fromWire(raw: String?): PixelJamRoomMode =
            if (raw.equals("collaborative", ignoreCase = true)) COLLABORATIVE else HOST_ONLY
    }
}

@Immutable
data class PixelJamTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val source: String = "youtube",
    val suggestedBy: String = "",
    val isAutoDj: Boolean = false
)

@Immutable
data class PixelJamParticipant(
    val userId: String,
    val name: String,
    val avatarUrl: String = "",
    val isHost: Boolean,
    val isCoHost: Boolean,
    val joinedAt: Long = 0L
)

@Immutable
data class PixelJamChatReply(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String
)

@Immutable
data class PixelJamChatMessage(
    val id: String,
    val userId: String,
    val userName: String,
    val avatarUrl: String = "",
    val isHost: Boolean,
    val text: String,
    val timestamp: Long,
    val replyTo: PixelJamChatReply? = null
)

@Immutable
data class PixelJamSuggestion(
    val suggestionId: String,
    val fromUserId: String,
    val fromUserName: String,
    val track: PixelJamTrack
)

@Immutable
data class PixelJamPublicRoom(
    val code: String,
    val name: String,
    val participants: Int
)

@Immutable
data class PixelJamPlaybackState(
    val action: String = "",
    val fromUserId: String = "",
    val currentTrack: PixelJamTrack? = null,
    val positionMs: Long = 0L,
    val serverTimeMs: Long = 0L,
    val isPlaying: Boolean = false,
    val queue: List<PixelJamTrack> = emptyList(),
    val revision: Long = 0L
)

@Immutable
sealed interface PixelJamUiState {
    data object Idle : PixelJamUiState
    data object Creating : PixelJamUiState
    data class Joining(val code: String) : PixelJamUiState

    @Immutable
    data class InRoom(
        val roomCode: String,
        val roomName: String = "",
        val userId: String,
        val displayName: String,
        val isHost: Boolean,
        val isMainHost: Boolean,
        val isCoHost: Boolean,
        val hostId: String,
        val coHostId: String,
        val permanent: Boolean = false,
        val isRadio: Boolean = false,
        val roomMode: PixelJamRoomMode = PixelJamRoomMode.HOST_ONLY,
        val participants: List<PixelJamParticipant> = emptyList()
    ) : PixelJamUiState

    data class Error(val message: String, val code: String? = null) : PixelJamUiState
}

@Immutable
data class PixelJamServerRestartEvent(
    val reason: String = "server_restart",
    val message: String = DEFAULT_MESSAGE
) {
    companion object {
        const val DEFAULT_MESSAGE =
            "PixelJam server needed a quick break — switched to solo playback. Rejoin in a few seconds!"
    }
}
