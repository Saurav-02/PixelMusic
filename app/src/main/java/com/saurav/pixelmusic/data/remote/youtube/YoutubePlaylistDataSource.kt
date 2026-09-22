package com.saurav.pixelmusic.data.remote.youtube

import com.saurav.pixelmusic.data.model.youtube.Playlist
import com.saurav.pixelmusic.data.model.youtube.PlaylistInfo
import com.saurav.pixelmusic.data.model.youtube.UmihiSettings
import com.saurav.pixelmusic.data.model.youtube.PlaylistSongCrossRef

/**
 * Remote data source for fetching YouTube playlist metadata and their songs
 * directly from the YouTube Music API via [YoutubeRequestHelper] + [YoutubeHelper].
 *
 * - [retrieveAll] fetches the list of playlists visible in the authenticated user's library.
 * - [retrieveOne] fetches the full song list for a single playlist.
 *
 * Requires a valid authenticated [UmihiSettings] (cookies) passed from [DatastoreRepository].
 */
class YoutubePlaylistDataSource {

    /**
     * Returns all playlist info entries visible in the user's YouTube Music library.
     */
    fun retrieveAll(settings: UmihiSettings): List<PlaylistInfo> {
        val directPlaylists = try {
            val json = YoutubeRequestHelper.browse(
                Constants.YoutubeApi.Browse.PLAYLIST_BROWSE_ID,
                settings
            )
            YoutubeHelper.extractPlaylists(json, settings)
        } catch (e: Exception) {
            UmihiHelper.printe("retrieveAll via browse failed: ${e.message}")
            emptyList()
        }

        if (directPlaylists.isNotEmpty()) {
            return directPlaylists
        }

        // Fallback: Fetch via InnerTube library endpoint
        return try {
            kotlinx.coroutines.runBlocking {
                saurav.shru.pixelmusic.innertube.YouTube.library(Constants.YoutubeApi.Browse.PLAYLIST_BROWSE_ID)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<saurav.shru.pixelmusic.innertube.models.PlaylistItem>()
                    ?.map { item ->
                        PlaylistInfo(
                            id = item.id,
                            title = item.title,
                            coverHref = item.thumbnail?.let { com.saurav.pixelmusic.data.remote.youtube.upgradeThumbnailUrlToHighQuality(it) }
                        )
                    } ?: emptyList()
            }
        } catch (e: Exception) {
            UmihiHelper.printe("retrieveAll via InnerTube fallback failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches and returns a [Playlist] populated with its full song list from YouTube.
     * The "liked_songs" playlist id is translated to YouTube's internal "LM" browse id.
     */
    fun retrieveOne(playlist: Playlist, settings: UmihiSettings): Playlist {
        val remoteId = if (playlist.info.id == "liked_songs") "LM" else playlist.info.id
        val remoteSongs = YoutubeHelper.extractSongList(
            YoutubeRequestHelper.browse(
                remoteId,
                settings
            ),
            settings
        )
        return playlist.copy(
            unsortedSongs = remoteSongs,
            crossRefs = remoteSongs.mapIndexed { index, song ->
                PlaylistSongCrossRef(playlist.info.id, song.youtubeId, index)
            }
        )
    }
}
