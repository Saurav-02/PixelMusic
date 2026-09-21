package com.saurav.pixelmusic.data.remote.youtube.cipher

interface PlayerConfigRepository {
    val enabled: Boolean
    val sourceUrl: String
    val defaultSourceUrl: String
    var cachedJson: String
    var cachedAtMs: Long
    var cachedSourceUrl: String
    var cachedEtag: String

    companion object {
        fun disabled(): PlayerConfigRepository =
            object : PlayerConfigRepository {
                override val enabled: Boolean = false
                override val sourceUrl: String = ""
                override val defaultSourceUrl: String = ""
                override var cachedJson: String = ""
                override var cachedAtMs: Long = 0L
                override var cachedSourceUrl: String = ""
                override var cachedEtag: String = ""
            }
    }
}

