package com.saurav.pixelmusic.data.playlist

import android.content.Context
import android.net.Uri
import com.saurav.pixelmusic.data.model.Playlist
import com.saurav.pixelmusic.data.model.Song
import com.saurav.pixelmusic.data.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import saurav.shru.pixelmusic.innertube.YouTube
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.filterVideo
import com.saurav.pixelmusic.data.remote.youtube.YoutubeHelper
import com.saurav.pixelmusic.data.remote.youtube.YoutubeRequestHelper
import com.saurav.pixelmusic.data.remote.youtube.toNativeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Singleton
class M3uManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) {

    private fun getSongExportPath(song: Song): String {
        val youtubeId = song.youtubeId 
            ?: if (song.id.startsWith("youtube_")) song.id.substringAfter("youtube_")
               else if (song.contentUriString.startsWith("youtube://")) song.contentUriString.substringAfter("youtube://")
               else null
        return if (youtubeId != null) {
            "youtube://$youtubeId"
        } else {
            song.path
        }
    }

    private fun parseYoutubeId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("youtube://")) {
            return trimmed.substringAfter("youtube://").trim().takeIf { it.isNotEmpty() }
        }
        
        // Match YouTube URL patterns:
        // - y2u.be/ID
        // - youtu.be/ID
        // - youtube.com/watch?v=ID
        // - music.youtube.com/watch?v=ID
        // - youtube.com/embed/ID
        // - youtube.com/v/ID
        val regex = Regex(
            """(?:https?://)?(?:www\.|music\.)?(?:youtube\.com/(?:watch\?v=|embed/|v/)|youtu\.be/|y2u\.be/)([a-zA-Z0-9_-]{11})""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(trimmed)?.groupValues?.getOrNull(1)
    }

    private suspend fun fetchYoutubeSongDetails(
        youtubeId: String,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackDuration: Long = 0L,
        fallbackAlbum: String = "YouTube Music"
    ): Song {
        // 1. Try Next API to get full metadata (album, art, artists)
        try {
            val nextResult = YouTube.next(
                saurav.shru.pixelmusic.innertube.models.WatchEndpoint(videoId = youtubeId)
            ).getOrNull()
            val songItem = nextResult?.items?.firstOrNull { it.id == youtubeId }
            if (songItem != null) {
                val ySong = songItem.toNativeSong()
                return ySong.copy(
                    title = if (fallbackTitle != "YouTube Song" && fallbackTitle.isNotBlank()) fallbackTitle else ySong.title,
                    artist = if (fallbackArtist != "Unknown Artist" && fallbackArtist.isNotBlank()) fallbackArtist else ySong.artist,
                    duration = if (fallbackDuration > 0) fallbackDuration else ySong.duration,
                    album = if (fallbackAlbum != "YouTube Music" && fallbackAlbum.isNotBlank()) fallbackAlbum else ySong.album
                )
            }
        } catch (e: Exception) {
            // ignore and fallback
        }

        // 2. Try Player API
        return try {
            val jsonString = YoutubeRequestHelper.getPlayerInfo(youtubeId)
            val ySong = YoutubeHelper.extractSongInfo(jsonString).toNativeSong()
            ySong.copy(
                title = if (fallbackTitle != "YouTube Song" && fallbackTitle.isNotBlank()) fallbackTitle else ySong.title,
                artist = if (fallbackArtist != "Unknown Artist" && fallbackArtist.isNotBlank()) fallbackArtist else ySong.artist,
                duration = if (fallbackDuration > 0) fallbackDuration else ySong.duration,
                album = if (fallbackAlbum != "YouTube Music" && fallbackAlbum.isNotBlank()) fallbackAlbum else ySong.album
            )
        } catch (e: Exception) {
            val songId = "youtube_$youtubeId"
            val artistId = -(17_000_000_000_000L + kotlin.math.abs(fallbackArtist.lowercase().hashCode().toLong()))
            val albumId = -(16_000_000_000_000L + kotlin.math.abs(fallbackAlbum.lowercase().hashCode().toLong()))
            Song(
                id = songId,
                title = fallbackTitle,
                artist = fallbackArtist,
                artistId = artistId,
                album = fallbackAlbum,
                albumId = albumId,
                path = "",
                contentUriString = "youtube://$youtubeId",
                albumArtUriString = "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg",
                duration = fallbackDuration,
                genre = "YouTube",
                mimeType = "audio/webm",
                bitrate = 128,
                sampleRate = 44100,
                youtubeId = youtubeId
            )
        }
    }

    private suspend fun searchAndFetchYoutubeSong(
        title: String,
        artist: String,
        fallbackDuration: Long = 0L,
        fallbackAlbum: String = "YouTube Music"
    ): Song? {
        val songItem = try {
            val query = "$title $artist"
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val songsList = result?.items?.filterIsInstance<SongItem>()?.filterVideo(true).orEmpty()
            // Prefer ATV (official audio tracks)
            songsList.firstOrNull {
                val musicVideoType = it.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                musicVideoType == "MUSIC_VIDEO_TYPE_ATV"
            } ?: songsList.firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return null

        return songItem.toNativeSong().copy(
            title = if (title.isNotBlank()) title else songItem.toNativeSong().title,
            artist = if (artist.isNotBlank()) artist else songItem.toNativeSong().artist,
            duration = if (fallbackDuration > 0) fallbackDuration else songItem.toNativeSong().duration,
            album = if (fallbackAlbum != "YouTube Music" && fallbackAlbum.isNotBlank()) fallbackAlbum else songItem.toNativeSong().album
        )
    }

    suspend fun parseM3u(
        uri: Uri,
        onProgress: (current: Int, total: Int, title: String, artist: String) -> Unit = { _, _, _, _ -> }
    ): Pair<String, List<String>> {
        var playlistName = "Imported Playlist"

        // Try to get the filename as playlist name first
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                playlistName = cursor.getString(nameIndex).removeSuffix(".m3u").removeSuffix(".m3u8")
            }
        }

        val entries = mutableListOf<M3uEntry>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                var lastExtInf: String? = null
                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line?.trim() ?: continue
                    if (trimmedLine.isEmpty()) continue
                    if (trimmedLine.startsWith("#EXTINF:")) {
                        lastExtInf = trimmedLine
                        continue
                    }
                    if (trimmedLine.startsWith("#")) continue

                    // Decode URL-encoded chars (%20 etc.) and normalize file:// URIs
                    val rawDecoded = try {
                        java.net.URLDecoder.decode(trimmedLine, "UTF-8")
                    } catch (e: Exception) {
                        trimmedLine
                    }
                    // Strip file:// / file:/// scheme to get a plain filesystem path
                    val decodedLine = when {
                        rawDecoded.startsWith("file:///") -> rawDecoded.removePrefix("file://")
                        rawDecoded.startsWith("file://") -> rawDecoded.removePrefix("file://")
                        else -> rawDecoded
                    }

                    var extTitle = ""
                    var extArtist = ""
                    var extDurationMs = 0L
                    lastExtInf?.let { extInf ->
                        // #EXTINF: <duration> [optional attrs],<display title>
                        // Duration is the first space-separated token after ':'
                        val afterColon = extInf.substringAfter(":", "")
                        val durationStr = afterColon.substringBefore(",", "").trim().split(" ").firstOrNull()?.trimEnd() ?: ""
                        val secs = durationStr.toLongOrNull() ?: 0L
                        extDurationMs = secs * 1000L

                        // Everything after the first comma is the display name
                        val metaPart = extInf.substringAfter(",", "").trim()
                        if (metaPart.isNotBlank()) {
                            // Split only on " - " (with spaces) to avoid breaking hyphenated titles
                            val dashIdx = metaPart.indexOf(" - ")
                            if (dashIdx > 0) {
                                extArtist = metaPart.substring(0, dashIdx).trim()
                                extTitle = metaPart.substring(dashIdx + 3).trim()
                            } else {
                                extTitle = metaPart.trim()
                            }
                        }
                    }
                    entries.add(M3uEntry(decodedLine, extTitle, extArtist, extDurationMs))
                    lastExtInf = null
                }
            }
        }

        // Load a filtered one-shot snapshot so import respects the current library visibility rules.
        val allSongs = musicRepository.getAllSongsOnce()

        // Build lookup maps for fast matching
        val songsByPath = allSongs.associateBy { it.path }
        val songsByFileName = allSongs.groupBy { it.path.substringAfterLast("/").substringBeforeLast(".") }
        val songsByContentUriFileName = allSongs.groupBy { it.contentUriString.substringAfterLast("/") }
        val songsByYoutubeId = allSongs.filter { it.youtubeId != null }.associateBy { it.youtubeId!! }
        val songsByTitleAndArtist = allSongs.groupBy { "${it.title.lowercase().trim()} - ${it.artist.lowercase().trim()}" }
        val songsByTitle = allSongs.groupBy { it.title.lowercase().trim() }

        val totalSongs = entries.size
        val resolvedIds = Array<String?>(totalSongs) { null }
        val entriesNeedingNetwork = mutableListOf<Pair<Int, M3uEntry>>()

        for ((index, entry) in entries.withIndex()) {
            val titleFallback = if (entry.extTitle.isNotBlank()) entry.extTitle else entry.path.substringAfterLast("/").substringBeforeLast(".")
            val artistFallback = if (entry.extArtist.isNotBlank()) entry.extArtist else "Unknown Artist"

            val youtubeId = parseYoutubeId(entry.path)
            if (youtubeId != null) {
                val songId = "youtube_$youtubeId"
                val existingSong = songsByYoutubeId[youtubeId]
                    ?: allSongs.find { it.id == songId || it.contentUriString == "youtube://$youtubeId" }
                if (existingSong != null) {
                    resolvedIds[index] = existingSong.id
                } else {
                    entriesNeedingNetwork.add(index to entry)
                }
                continue
            }

            // 1) Try exact path match
            val songByPath = songsByPath[entry.path]
            if (songByPath != null) {
                resolvedIds[index] = songByPath.id
                continue
            }

            // 2) Try filename match from path column
            val fileName = entry.path.substringAfterLast("/").substringBeforeLast(".")
            val matchedSong = songsByFileName[fileName]?.firstOrNull()
                ?: songsByContentUriFileName[fileName]?.firstOrNull()
            if (matchedSong != null) {
                resolvedIds[index] = matchedSong.id
                continue
            }

            // 3) Try Title & Artist match
            if (entry.extTitle.isNotBlank()) {
                val key = "${entry.extTitle.lowercase().trim()} - ${entry.extArtist.lowercase().trim()}"
                val matchByTitleAndArtist = songsByTitleAndArtist[key]?.firstOrNull()
                if (matchByTitleAndArtist != null) {
                    resolvedIds[index] = matchByTitleAndArtist.id
                    continue
                }

                val matchByTitle = songsByTitle[entry.extTitle.lowercase().trim()]?.firstOrNull()
                if (matchByTitle != null) {
                    resolvedIds[index] = matchByTitle.id
                    continue
                }

                // No local match, needs YouTube lookup
                entriesNeedingNetwork.add(index to entry)
            } else if (entry.path.isNotBlank() && youtubeId == null) {
                // If it is a local path with no title info and didn't match, fallback to local filename resolution search
                val fileNameSearch = entry.path.substringAfterLast("/").substringBeforeLast(".")
                if (fileNameSearch.isNotBlank()) {
                    entriesNeedingNetwork.add(index to entry)
                }
            }
        }

        val processedCount = java.util.concurrent.atomic.AtomicInteger(totalSongs - entriesNeedingNetwork.size)
        // Send initial progress count from local matching
        onProgress(processedCount.get(), totalSongs, "Resolving local library matches...", "")

        val newSongsToInsert = java.util.concurrent.CopyOnWriteArrayList<Song>()
        val semaphore = Semaphore(5)

        withContext(Dispatchers.IO) {
            val deferreds = entriesNeedingNetwork.map { (index, entry) ->
                async {
                    semaphore.withPermit {
                        val youtubeId = parseYoutubeId(entry.path)
                        val titleFallback = if (entry.extTitle.isNotBlank()) entry.extTitle else entry.path.substringAfterLast("/").substringBeforeLast(".")
                        val artistFallback = if (entry.extArtist.isNotBlank()) entry.extArtist else "Unknown Artist"

                        val song = if (youtubeId != null) {
                            fetchYoutubeSongDetails(
                                youtubeId = youtubeId,
                                fallbackTitle = titleFallback,
                                fallbackArtist = artistFallback,
                                fallbackDuration = entry.extDurationMs
                            )
                        } else {
                            searchAndFetchYoutubeSong(
                                title = titleFallback,
                                artist = artistFallback,
                                fallbackDuration = entry.extDurationMs
                            )
                        }

                        if (song != null) {
                            newSongsToInsert.add(song)
                            resolvedIds[index] = song.id
                        }

                        val current = processedCount.incrementAndGet()
                        onProgress(
                            if (current > totalSongs) totalSongs else current,
                            totalSongs,
                            song?.title ?: titleFallback,
                            song?.artist ?: artistFallback
                        )
                    }
                }
            }
            deferreds.awaitAll()
        }

        if (newSongsToInsert.isNotEmpty()) {
            musicRepository.insertYoutubeSongs(newSongsToInsert.toList())
        }

        val songIds = resolvedIds.filterNotNull()
        return Pair(playlistName, songIds)
    }

    fun generateM3u(playlist: Playlist, songs: List<Song>): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        for (song in songs) {
            sb.append("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n")
            sb.append("${getSongExportPath(song)}\n")
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------------------
    // CSV support
    // ---------------------------------------------------------------------------

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            '"' + value.replace("\"", "\"\"") + '"'
        } else value
    }

    fun generateCsv(songs: List<Song>): String {
        val sb = StringBuilder()
        sb.append("Title,Artist,Album,Duration (ms),Path\n")
        for (song in songs) {
            sb.append(escapeCsv(song.title)).append(',')
            sb.append(escapeCsv(song.artist)).append(',')
            sb.append(escapeCsv(song.album)).append(',')
            sb.append(song.duration).append(',')
            sb.append(escapeCsv(getSongExportPath(song))).append('\n')
        }
        return sb.toString()
    }

    suspend fun parseCsv(
        uri: Uri,
        onProgress: (current: Int, total: Int, title: String, artist: String) -> Unit = { _, _, _, _ -> }
    ): Pair<String, List<String>> {
        var playlistName = "Imported Playlist"

        // Derive playlist name from file name
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                playlistName = cursor.getString(nameIndex).removeSuffix(".csv")
            }
        }

        val entries = mutableListOf<CsvEntry>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var isFirstLine = true
                var line: String?
                
                var titleIdx = 0
                var artistIdx = 1
                var albumIdx = 2
                var durationIdx = 3
                var pathIdx = 4

                while (reader.readLine().also { line = it } != null) {
                    val trimmedLine = line?.trim() ?: continue
                    if (trimmedLine.isEmpty()) continue
                    
                    // Parse CSV row
                    val cols = splitCsvRow(trimmedLine)
                    
                    if (isFirstLine) {
                        isFirstLine = false
                        val lowercaseCols = cols.map { it.lowercase().trim() }
                        val t = lowercaseCols.indexOfFirst { it in listOf("title", "track", "track name", "name", "song", "song name", "trackname") }
                        val a = lowercaseCols.indexOfFirst { it in listOf("artist", "artist name", "artists", "artistname") }
                        val al = lowercaseCols.indexOfFirst { it in listOf("album", "album name", "album title", "albumname") }
                        val d = lowercaseCols.indexOfFirst { it in listOf("duration", "duration (ms)", "length", "time", "durationms") }
                        val p = lowercaseCols.indexOfFirst { it in listOf("path", "file", "filepath", "uri", "url", "location") }

                        if (t != -1 || a != -1) {
                            if (t != -1) titleIdx = t
                            if (a != -1) artistIdx = a
                            if (al != -1) albumIdx = al
                            if (d != -1) durationIdx = d
                            if (p != -1) pathIdx = p
                            continue
                        }
                    }
                    
                    val title = cols.getOrNull(titleIdx)?.trim() ?: ""
                    val artist = cols.getOrNull(artistIdx)?.trim() ?: ""
                    val album = cols.getOrNull(albumIdx)?.trim() ?: ""
                    val durationStr = if (durationIdx != -1) cols.getOrNull(durationIdx)?.trim() ?: "" else ""
                    val rawPath = if (pathIdx != -1) cols.getOrNull(pathIdx)?.trim() ?: "" else ""
                    val path = try {
                        java.net.URLDecoder.decode(rawPath, "UTF-8")
                    } catch (e: Exception) {
                        rawPath
                    }

                    if (title.isBlank() && path.isBlank()) continue
                    entries.add(CsvEntry(title, artist, album, durationStr, path))
                }
            }
        }

        val allSongs = musicRepository.getAllSongsOnce()
        val songsByPath = allSongs.associateBy { it.path }
        val songsByTitle = allSongs.groupBy { it.title.lowercase().trim() }
        val songsByFileName = allSongs.groupBy { it.path.substringAfterLast("/").substringBeforeLast(".") }
        val songsByYoutubeId = allSongs.filter { it.youtubeId != null }.associateBy { it.youtubeId!! }
        val songsByTitleAndArtist = allSongs.groupBy { "${it.title.lowercase().trim()} - ${it.artist.lowercase().trim()}" }

        val totalSongs = entries.size
        val resolvedIds = Array<String?>(totalSongs) { null }
        val entriesNeedingNetwork = mutableListOf<Pair<Int, CsvEntry>>()

        for ((index, entry) in entries.withIndex()) {
            val youtubeId = if (entry.path.isNotBlank()) parseYoutubeId(entry.path) else null
            if (youtubeId != null) {
                val songId = "youtube_$youtubeId"
                val existingSong = songsByYoutubeId[youtubeId]
                    ?: allSongs.find { it.id == songId || it.contentUriString == "youtube://$youtubeId" }
                if (existingSong != null) {
                    resolvedIds[index] = existingSong.id
                } else {
                    entriesNeedingNetwork.add(index to entry)
                }
                continue
            }

            // 1) Try exact path match
            if (entry.path.isNotBlank()) {
                val songByPath = songsByPath[entry.path]
                if (songByPath != null) {
                    resolvedIds[index] = songByPath.id
                    continue
                }

                // Try filename match from path column
                val fileName = entry.path.substringAfterLast("/").substringBeforeLast(".")
                val matchedSong = songsByFileName[fileName]?.firstOrNull()
                if (matchedSong != null) {
                    resolvedIds[index] = matchedSong.id
                    continue
                }
            }

            // 2) Try Title & Artist match
            if (entry.title.isNotBlank()) {
                val key = "${entry.title.lowercase().trim()} - ${entry.artist.lowercase().trim()}"
                val matchByTitleAndArtist = songsByTitleAndArtist[key]?.firstOrNull()
                if (matchByTitleAndArtist != null) {
                    resolvedIds[index] = matchByTitleAndArtist.id
                    continue
                }

                val matchByTitle = songsByTitle[entry.title.lowercase().trim()]?.firstOrNull()
                if (matchByTitle != null) {
                    resolvedIds[index] = matchByTitle.id
                    continue
                }

                // No local match, needs YouTube lookup
                entriesNeedingNetwork.add(index to entry)
            } else if (entry.path.isNotBlank() && youtubeId == null) {
                // Local path with no title info and didn't match, fallback to local filename resolution search
                val fileNameSearch = entry.path.substringAfterLast("/").substringBeforeLast(".")
                if (fileNameSearch.isNotBlank()) {
                    entriesNeedingNetwork.add(index to entry)
                }
            }
        }

        val processedCount = java.util.concurrent.atomic.AtomicInteger(totalSongs - entriesNeedingNetwork.size)
        // Send initial progress count from local matching
        onProgress(processedCount.get(), totalSongs, "Resolving local library matches...", "")

        val newSongsToInsert = java.util.concurrent.CopyOnWriteArrayList<Song>()
        val semaphore = Semaphore(5)

        withContext(Dispatchers.IO) {
            val deferreds = entriesNeedingNetwork.map { (index, entry) ->
                async {
                    semaphore.withPermit {
                        val youtubeId = if (entry.path.isNotBlank()) parseYoutubeId(entry.path) else null
                        val titleFallback = if (entry.title.isNotBlank()) entry.title else entry.path.substringAfterLast("/").substringBeforeLast(".")
                        val artistFallback = if (entry.artist.isNotBlank()) entry.artist else "Unknown Artist"
                        val durationMs = entry.durationStr.toLongOrNull() ?: 0L

                        val song = if (youtubeId != null) {
                            fetchYoutubeSongDetails(
                                youtubeId = youtubeId,
                                fallbackTitle = titleFallback,
                                fallbackArtist = artistFallback,
                                fallbackDuration = durationMs,
                                fallbackAlbum = entry.album.ifBlank { "YouTube Music" }
                            )
                        } else {
                            searchAndFetchYoutubeSong(
                                title = titleFallback,
                                artist = artistFallback,
                                fallbackDuration = durationMs,
                                fallbackAlbum = entry.album.ifBlank { "YouTube Music" }
                            )
                        }

                        if (song != null) {
                            newSongsToInsert.add(song)
                            resolvedIds[index] = song.id
                        }

                        val current = processedCount.incrementAndGet()
                        onProgress(
                            if (current > totalSongs) totalSongs else current,
                            totalSongs,
                            song?.title ?: titleFallback,
                            song?.artist ?: artistFallback
                        )
                    }
                }
            }
            deferreds.awaitAll()
        }

        if (newSongsToInsert.isNotEmpty()) {
            musicRepository.insertYoutubeSongs(newSongsToInsert.toList())
        }

        val songIds = resolvedIds.filterNotNull()
        return Pair(playlistName, songIds)
    }

    /** Minimal CSV row splitter that handles double-quoted fields. */
    private fun splitCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < row.length && row[i + 1] == '"' -> {
                    current.append('"'); i++ // escaped quote
                }
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}

private data class M3uEntry(
    val path: String,
    val extTitle: String = "",
    val extArtist: String = "",
    val extDurationMs: Long = 0L
)

private data class CsvEntry(
    val title: String,
    val artist: String,
    val album: String,
    val durationStr: String,
    val path: String
)
