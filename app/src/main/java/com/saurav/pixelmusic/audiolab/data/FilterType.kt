package com.saurav.pixelmusic.audiolab.data

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    PK,
    LSC,
    HSC,
    LPQ,
    HPQ
}
