package se.vadretapp.vadret.radar.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RadarFileDto(
    @SerialName(value = "key")
    val key: String,
    @SerialName(value = "valid")
    val valid: String,
    @SerialName(value = "formats")
    val formats: List<RadarFormatDto>
)