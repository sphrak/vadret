package se.vadretapp.vadret.radar.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RadarFormatDto(
    @SerialName(value = "link")
    val link: String
)