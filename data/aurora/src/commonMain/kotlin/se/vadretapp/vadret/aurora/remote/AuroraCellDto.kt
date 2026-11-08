package se.vadretapp.vadret.aurora.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuroraCellDto(
    @SerialName(value = "geohash")
    val geohash: String? = null,
    @SerialName(value = "hours")
    val hours: List<AuroraHourDto>? = null
)