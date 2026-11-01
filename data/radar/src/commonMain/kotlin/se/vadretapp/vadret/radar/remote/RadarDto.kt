package se.vadretapp.vadret.radar.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RadarDto(
    @SerialName(value = "files")
    val files: List<RadarFileDto>
)