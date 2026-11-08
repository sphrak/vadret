package se.vadretapp.vadret.aurora.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuroraHourDto(
    @SerialName(value = "time")
    val time: String? = null,
    @SerialName(value = "score")
    val score: Int? = null
)