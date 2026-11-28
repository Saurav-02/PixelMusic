/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.pages

import saurav.shru.pixelmusic.innertube.models.Album
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.AlbumReleaseType
import saurav.shru.pixelmusic.innertube.models.Artist
import saurav.shru.pixelmusic.innertube.models.MusicResponsiveListItemRenderer
import saurav.shru.pixelmusic.innertube.models.MusicTwoRowItemRenderer
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem
import saurav.shru.pixelmusic.innertube.models.oddElements
import saurav.shru.pixelmusic.innertube.models.splitBySeparator
import saurav.shru.pixelmusic.innertube.utils.parseTime

data class ArtistItemsPage(
    val title: String,
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint?.videoId
                ?: renderer.navigationEndpoint?.anyWatchEndpoint?.videoId
                ?: return null
            val extractedArtists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST")
            val artistRuns = if (extractedArtists.isNotEmpty()) {
                extractedArtists
            } else {
                renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.oddElements().orEmpty()
            }
            val albumRuns = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ALBUM")
                .ifEmpty { PageHelper.extractRuns(renderer.flexColumns, "ALBUM") }
                .ifEmpty {
                    renderer.flexColumns.getOrNull(3)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                        ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                        ?: emptyList()
                }
            return SongItem(
                id = videoId,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = artistRuns.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                album = albumRuns.firstOrNull()?.let { run ->
                    run.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                        Album(
                            name = run.text,
                            id = id
                        )
                    }
                },
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()
                    ?.text?.parseTime(),
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint
                    ?: renderer.navigationEndpoint?.anyWatchEndpoint
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isAlbum -> {
                    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
                    val subtitleRuns = renderer.subtitle?.runs.orEmpty()
                    val parsedYear = subtitleRuns.asReversed().firstNotNullOfOrNull { it.text.trim().toIntOrNull() }
                    val typeLabel = subtitleRuns.firstOrNull { it.text.trim().toIntOrNull() == null && it.text.trim() != "•" }?.text
                    AlbumItem(
                        browseId = browseId,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer
                            ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.anyWatchEndpoint?.playlistId
                            ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" },
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = parsedYear,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                        releaseType = AlbumReleaseType.fromLabel(typeLabel)
                    )
                }
                // Video
                renderer.isSong -> SongItem(
                    id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    artists = renderer.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.oddElements()?.map {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    } ?: return null,
                    album = null,
                    duration = null,
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    endpoint = renderer.navigationEndpoint.watchEndpoint
                )
                renderer.isPlaylist -> PlaylistItem(
                    id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    author = renderer.subtitle?.runs?.getOrNull(2)?.let {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    },
                    songCountText = renderer.subtitle?.runs?.getOrNull(4)?.text,
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint = renderer.thumbnailOverlay
                        ?.musicItemThumbnailOverlayRenderer?.content
                        ?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchPlaylistEndpoint,
                    shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                )
                else -> null
            }
        }
    }
}
