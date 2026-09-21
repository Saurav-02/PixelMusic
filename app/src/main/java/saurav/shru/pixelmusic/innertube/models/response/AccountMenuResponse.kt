/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/ianshulyadav
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package saurav.shru.pixelmusic.innertube.models.response

import saurav.shru.pixelmusic.innertube.models.AccountInfo
import saurav.shru.pixelmusic.innertube.models.Runs
import saurav.shru.pixelmusic.innertube.models.Thumbnails
import saurav.shru.pixelmusic.innertube.models.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class AccountMenuResponse(
    val actions: List<Action>,
) {
    @Serializable
    data class Action(
        val openPopupAction: OpenPopupAction,
    ) {
        @Serializable
        data class OpenPopupAction(
            val popup: Popup,
        ) {
            @Serializable
            data class Popup(
                val multiPageMenuRenderer: MultiPageMenuRenderer,
            ) {
                @Serializable
                data class MultiPageMenuRenderer(
                    val header: Header?,
                ) {
                    @Serializable
                    data class Header(
                        val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer,
                    ) {
                        @Serializable
                        data class ActiveAccountHeaderRenderer(
                            val accountName: Runs,
                            val email: Runs?,
                            val channelHandle: Runs?,
                            val accountPhoto: Thumbnails,
                            val badges: List<Badge>? = null
                        ) {
                            @Serializable
                            data class Badge(
                                val metadataBadgeRenderer: MetadataBadgeRenderer? = null
                            ) {
                                @Serializable
                                data class MetadataBadgeRenderer(
                                    val label: String? = null
                                )
                            }
                            
                            fun toAccountInfo(): AccountInfo? {
                                val name = accountName.runs?.firstOrNull()?.text ?: return null
                                val hasProBadge = badges?.any { badge ->
                                    val label = badge.metadataBadgeRenderer?.label ?: ""
                                    label.contains("Pro", ignoreCase = true) || label.contains("Premium", ignoreCase = true)
                                } ?: false
                                
                                return AccountInfo(
                                    name = name,
                                    email = email?.runs?.firstOrNull()?.text,
                                    channelHandle = channelHandle?.runs?.firstOrNull()?.text,
                                    thumbnailUrl = accountPhoto.thumbnails.lastOrNull()?.normalizedUrl,
                                    isPro = hasProBadge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
