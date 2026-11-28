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
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.BrowseEndpoint
import saurav.shru.pixelmusic.innertube.models.MusicCarouselShelfRenderer
import saurav.shru.pixelmusic.innertube.models.MusicResponsiveListItemRenderer
import saurav.shru.pixelmusic.innertube.models.MusicShelfRenderer
import saurav.shru.pixelmusic.innertube.models.MusicTwoRowItemRenderer
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SectionListRenderer
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem
import saurav.shru.pixelmusic.innertube.models.filterExplicit
import saurav.shru.pixelmusic.innertube.models.getItems
import saurav.shru.pixelmusic.innertube.models.oddElements
import saurav.shru.pixelmusic.innertube.utils.parseTime

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
    val isSongsSection: Boolean = false,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
) {
    companion object {
        fun fromSectionListRendererContent(content: SectionListRenderer.Content): ArtistSection? {
            return when {
                content.musicShelfRenderer != null -> fromMusicShelfRenderer(content.musicShelfRenderer)
                content.musicCarouselShelfRenderer != null -> fromMusicCarouselShelfRenderer(content.musicCarouselShelfRenderer)
                else -> null
            }
        }

        private fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): ArtistSection? {
            return ArtistSection(
                title = renderer.title?.runs?.firstOrNull()?.text ?: "",
                items = renderer.contents?.getItems()?.mapNotNull {
                    fromMusicResponsiveListItemRenderer(it)
                }?.ifEmpty { null } ?: return null,
                moreEndpoint = renderer.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint
                    ?: renderer.bottomEndpoint?.browseEndpoint
                    ?: renderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                isSongsSection = true
            )
        }

        private fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): ArtistSection? {
            val hasResponsiveSongs = renderer.contents.any { it.musicResponsiveListItemRenderer != null }
            return ArtistSection(
                title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return null,
                items = renderer.contents.mapNotNull { content ->
                    content.musicTwoRowItemRenderer?.let { twoRow ->
                        fromMusicTwoRowItemRenderer(twoRow)
                    } ?: content.musicResponsiveListItemRenderer?.let { listItem ->
                        fromMusicResponsiveListItemRenderer(listItem)
                    }
                }.ifEmpty { null } ?: return null,
                moreEndpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint
                    ?: renderer.header.musicCarouselShelfBasicHeaderRenderer.title.runs.firstOrNull()?.navigationEndpoint?.browseEndpoint,
                isSongsSection = hasResponsiveSongs
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                    ?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint?.videoId
                ?: renderer.navigationEndpoint?.anyWatchEndpoint?.videoId
                ?: return null
            val extractedArtistRuns = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST")
            val artistRuns = if (extractedArtistRuns.isNotEmpty()) {
                extractedArtistRuns
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
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text ?: return null,
                artists = artistRuns.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                album = albumRuns.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text?.parseTime(),
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                    ?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint
                    ?: renderer.navigationEndpoint?.anyWatchEndpoint
            )
        }

        private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = listOfNotNull(renderer.subtitle?.runs?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }),
                        album = null,
                        duration = null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isAlbum -> {
                    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
                    val subtitleRuns = renderer.subtitle?.runs.orEmpty()
                    val parsedYear = subtitleRuns.asReversed().firstNotNullOfOrNull { it.text.trim().toIntOrNull() }
                    val typeLabel = subtitleRuns.firstOrNull { it.text.trim().toIntOrNull() == null && it.text.trim() != "•" }?.text
                    AlbumItem(
                        browseId = browseId,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
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

                renderer.isPlaylist -> {
                    // Playlist from YouTube Music
                    PlaylistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = Artist(
                            name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                            id = null
                        ),
                        songCountText = null,
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
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        channelId = renderer.menu?.menuRenderer?.items?.find {
                            it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE"
                        }?.toggleMenuServiceItemRenderer?.defaultServiceEndpoint?.subscribeEndpoint?.channelIds?.firstOrNull(),
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    )
                }

                else -> null
            }
        }
    }
}
