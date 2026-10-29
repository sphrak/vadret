package fi.kroon.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenLevelDto(
    @SerialName(value = "pollenId")
    val pollenId: String,
    @SerialName(value = "level")
    val level: Int,
    @SerialName(value = "time")
    val time: String
)