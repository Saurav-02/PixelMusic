/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.pages

import saurav.shru.pixelmusic.innertube.models.Album
import saurav.shru.pixelmusic.innertube.models.AlbumItem
import saurav.shru.pixelmusic.innertube.models.Artist
import saurav.shru.pixelmusic.innertube.models.ArtistItem
import saurav.shru.pixelmusic.innertube.models.MusicResponsiveListItemRenderer
import saurav.shru.pixelmusic.innertube.models.MusicTwoRowItemRenderer
import saurav.shru.pixelmusic.innertube.models.PlaylistItem
import saurav.shru.pixelmusic.innertube.models.SongItem
import saurav.shru.pixelmusic.innertube.models.YTItem
import saurav.shru.pixelmusic.innertube.models.oddElements
import saurav.shru.pixelmusic.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchPlaylistEndpoint?.playlistId
                ?: renderer.menu?.menuRenderer?.items?.firstOrNull()
                    ?.menuNavigationItemRenderer?.navigationEndpoint
                    ?.watchPlaylistEndpoint?.playlistId
                ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                artists = null,
                year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }
    }
}
