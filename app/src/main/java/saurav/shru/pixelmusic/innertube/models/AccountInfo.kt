package saurav.shru.pixelmusic.innertube.models

data class AccountInfo(
    val name: String,
    val email: String?,
    val channelHandle: String?,
    val thumbnailUrl: String?,
    val isPro: Boolean = false
)
