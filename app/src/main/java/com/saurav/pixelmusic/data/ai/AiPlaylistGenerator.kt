package com.saurav.pixelmusic.data.ai


import com.saurav.pixelmusic.data.DailyMixManager
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.preferences.AiPreferencesRepository
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import com.saurav.pixelmusic.data.repository.MusicRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

data class AiPlaylistGenerationResult(
    val songs: List<Song>,
    val title: String? = null,
    val description: String? = null
)

private sealed interface TrackSelectionItem {
    data class Local(val id: String) : TrackSelectionItem
    data class Online(val query: String) : TrackSelectionItem
}

private data class ParsedPlaylistResponse(
    val title: String?,
    val description: String?,
    val trackItems: List<TrackSelectionItem>
)

class AiPlaylistGenerator @Inject constructor(
    private val dailyMixManager: DailyMixManager,
    private val aiOrchestrator: AiOrchestrator,
    private val digestGenerator: UserProfileDigestGenerator,
    private val preferencesRepo: AiPreferencesRepository,
    private val json: Json,
    private val musicRepository: MusicRepository
) {

    suspend fun generate(
        userPrompt: String,
        allSongs: List<Song>,
        minLength: Int,
        maxLength: Int,
        candidateSongs: List<Song>? = null,
        type: AiSystemPromptType = AiSystemPromptType.PLAYLIST
    ): Result<AiPlaylistGenerationResult> {
        return try {

            // Get offline scored candidates to pass to LLM (much smaller context window than the whole library)
            val samplingPool = when {
                candidateSongs.isNullOrEmpty().not() -> candidateSongs
                else -> {
                    val rankedForPrompt = dailyMixManager.getTopCandidatesForAi(
                        allSongs = allSongs,
                        favoriteSongIds = emptySet(),
                        limit = 100
                    )
                    if (rankedForPrompt.isNotEmpty()) rankedForPrompt else allSongs
                }
            }

            // Token Optimization: Reduce sample size based on safe mode
            val isSafe = preferencesRepo.isSafeTokenLimitEnabled.first()
            val sampleCap = if (isSafe) 40 else 80
            val sampleSize = max(minLength, sampleCap).coerceAtMost(sampleCap)
            val songSample = samplingPool.take(sampleSize)
            
            // Token Optimization: Compact JSON format — only essential fields
            val availableSongsJson = buildString {
                songSample.forEachIndexed { index, song ->
                    val score = dailyMixManager.getScore(song.id)
                    val title = song.title.replace("\"", "'").take(40)
                    val artist = song.displayArtist.replace("\"", "'").take(25)
                    val genre = song.genre?.replace("\"", "'")?.take(15) ?: "?"
                    if (index > 0) append(",\n")
                    append("""{"id":"${song.id}","t":"$title","a":"$artist","g":"$genre","s":$score}""")
                }
            }

            // Bring in the telemetry digest
            val userDigest = digestGenerator.generateDigest(allSongs, isSafe)

            val sanitizedUserPrompt = userPrompt
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

            // Token Optimization: Compact prompt structure with XML data boundaries
            val fullPrompt = """
            $userDigest
            <request>
            <query>$sanitizedUserPrompt</query>
            <target_length>$minLength-$maxLength tracks</target_length>
            </request>
            <candidate_pool>
            [$availableSongsJson]
            </candidate_pool>
            """.trimIndent()

            val responseText = aiOrchestrator.generateContent(fullPrompt, type)

            val parsedResponse = extractPlaylistData(responseText)

            val songMap = allSongs.associateBy { it.id }
            val generatedPlaylist = mutableListOf<Song>()
            val seenSongKeys = mutableSetOf<String>()

            for (trackItem in parsedResponse.trackItems) {
                when (trackItem) {
                    is TrackSelectionItem.Local -> {
                        val localSong = songMap[trackItem.id]
                        if (localSong != null) {
                            val key = "${localSong.displayArtist.trim().lowercase()} - ${localSong.title.trim().lowercase()}"
                            if (seenSongKeys.add(key)) {
                                generatedPlaylist.add(localSong)
                            }
                        }
                    }
                    is TrackSelectionItem.Online -> {
                        val resolvedSong = resolveOnlineTrack(trackItem.query)
                        if (resolvedSong != null) {
                            val key = "${resolvedSong.displayArtist.trim().lowercase()} - ${resolvedSong.title.trim().lowercase()}"
                            if (seenSongKeys.add(key)) {
                                generatedPlaylist.add(resolvedSong)
                            }
                        }
                    }
                }
            }

            if (generatedPlaylist.isEmpty()) {
                Result.failure(IllegalArgumentException("AI returned song selections that don't match your library or online catalog. Try adjusting your prompt."))
            } else {
                Result.success(
                    AiPlaylistGenerationResult(
                        songs = generatedPlaylist,
                        title = parsedResponse.title,
                        description = parsedResponse.description
                    )
                )
            }

        } catch (e: IllegalArgumentException) {
            Result.failure(Exception(e.message ?: "AI response did not contain a valid playlist."))
        } catch (e: Exception) {
            val errorDetails = buildDetailedErrorMessage(e)
            Result.failure(Exception(errorDetails, e))
        }
    }

    private suspend fun resolveOnlineTrack(query: String): Song? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null
        return try {
            val searchResult = YouTube.search(
                query = trimmed,
                filter = YouTube.SearchFilter.FILTER_SONG
            ).getOrNull()

            val songItem = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()
            if (songItem != null) {
                val nativeSong = songItem.toNativeSong()
                musicRepository.insertYoutubeSongs(listOf(nativeSong))
                nativeSong
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("AiPlaylist").w(e, "Failed to resolve online track for query: $trimmed")
            null
        }
    }

    /**
     * Builds a user-friendly error message from the exception chain.
     * Walks the cause chain to find the most specific error detail.
     */
    private fun buildDetailedErrorMessage(e: Exception): String {
        val rootMessage = e.message?.takeIf { it.isNotBlank() }
        val causeMessage = e.cause?.message?.takeIf { it.isNotBlank() }
        val className = e::class.simpleName ?: "Unknown"

        // Check for common error patterns
        val combinedMessages = listOfNotNull(rootMessage, causeMessage).joinToString(" → ")
        
        return when {
            combinedMessages.contains("timeout", ignoreCase = true) ||
            combinedMessages.contains("timed out", ignoreCase = true) ->
                "Request timed out. The AI provider may be slow or overloaded. Try again."
            
            combinedMessages.contains("network", ignoreCase = true) ||
            combinedMessages.contains("connect", ignoreCase = true) ||
            combinedMessages.contains("SocketException", ignoreCase = true) ||
            combinedMessages.contains("no internet", ignoreCase = true) ||
            combinedMessages.contains("offline", ignoreCase = true) ||
            combinedMessages.contains("wifi", ignoreCase = true) ->
                "No Internet Connection. Check your WiFi or mobile data and try again."

            combinedMessages.contains("airplane", ignoreCase = true) ->
                "Airplane mode is active. Please turn it off to use AI."

            combinedMessages.contains("401", ignoreCase = true) ||
            combinedMessages.contains("403", ignoreCase = true) ||
            combinedMessages.contains("permission", ignoreCase = true) ||
            combinedMessages.contains("denied", ignoreCase = true) ||
            combinedMessages.contains("forbidden", ignoreCase = true) ||
            combinedMessages.contains("unauthorized", ignoreCase = true) ->
                "Permission Denied. Your API key might be invalid or restricted."
            
            combinedMessages.contains("safety", ignoreCase = true) ||
            combinedMessages.contains("blocked", ignoreCase = true) ->
                "Content was blocked by safety filters. Try rephrasing your prompt."
            
            combinedMessages.contains("model", ignoreCase = true) &&
            (combinedMessages.contains("not found", ignoreCase = true) ||
             combinedMessages.contains("unavailable", ignoreCase = true)) ->
                "The selected AI model is unavailable. Try selecting a different model in AI Settings."
            
            rootMessage != null -> "AI Error: $rootMessage"
            causeMessage != null -> "AI Error: $causeMessage"
            else -> "AI Error ($className): An unexpected error occurred. Try again."
        }
    }

    private fun extractPlaylistData(rawResponse: String): ParsedPlaylistResponse {
        val sanitized = rawResponse
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // 1. Try parsing JSON object: {"title": "...", "description": "...", "tracks": [...]}
        val firstBrace = sanitized.indexOf('{')
        val lastBrace = sanitized.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val candidate = sanitized.substring(firstBrace, lastBrace + 1)
            val parsed = runCatching {
                val obj = JSONObject(candidate)
                val title = obj.optString("title").takeIf { it.isNotBlank() }
                    ?: obj.optString("playlist_title").takeIf { it.isNotBlank() }
                val description = obj.optString("description").takeIf { it.isNotBlank() }
                    ?: obj.optString("playlist_description").takeIf { it.isNotBlank() }

                val tracksArr = obj.optJSONArray("tracks")
                    ?: obj.optJSONArray("songs")
                    ?: obj.optJSONArray("playlist")

                val items = mutableListOf<TrackSelectionItem>()
                if (tracksArr != null) {
                    for (i in 0 until tracksArr.length()) {
                        when (val elem = tracksArr.opt(i)) {
                            is JSONObject -> {
                                val id = elem.optString("id").takeIf { it.isNotBlank() }
                                val online = elem.optString("online").takeIf { it.isNotBlank() }
                                    ?: elem.optString("online_query").takeIf { it.isNotBlank() }
                                    ?: elem.optString("query").takeIf { it.isNotBlank() }
                                when {
                                    id != null -> items.add(TrackSelectionItem.Local(id))
                                    online != null -> items.add(TrackSelectionItem.Online(online))
                                }
                            }
                            is String -> {
                                if (elem.isNotBlank()) items.add(TrackSelectionItem.Local(elem))
                            }
                        }
                    }
                }
                ParsedPlaylistResponse(title, description, items)
            }.getOrNull()

            if (parsed != null && parsed.trackItems.isNotEmpty()) {
                return parsed
            }
        }

        // 2. Try parsing raw JSON array: ["id1", "id2"] or [{"id": "id1"}, {"online": "..."}]
        val firstBracket = sanitized.indexOf('[')
        val lastBracket = sanitized.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket > firstBracket) {
            val candidate = sanitized.substring(firstBracket, lastBracket + 1)
            val parsed = runCatching {
                val arr = JSONArray(candidate)
                val items = mutableListOf<TrackSelectionItem>()
                for (i in 0 until arr.length()) {
                    when (val elem = arr.opt(i)) {
                        is JSONObject -> {
                            val id = elem.optString("id").takeIf { it.isNotBlank() }
                            val online = elem.optString("online").takeIf { it.isNotBlank() }
                                ?: elem.optString("online_query").takeIf { it.isNotBlank() }
                            when {
                                id != null -> items.add(TrackSelectionItem.Local(id))
                                online != null -> items.add(TrackSelectionItem.Online(online))
                            }
                        }
                        is String -> {
                            if (elem.isNotBlank()) items.add(TrackSelectionItem.Local(elem))
                        }
                    }
                }
                ParsedPlaylistResponse(null, null, items)
            }.getOrNull()

            if (parsed != null && parsed.trackItems.isNotEmpty()) {
                return parsed
            }
        }

        throw IllegalArgumentException(
            "AI returned an invalid response format. Expected a playlist structure with song selections. " +
            "This usually happens with smaller models. Try selecting a more capable model in AI Settings."
        )
    }
}
