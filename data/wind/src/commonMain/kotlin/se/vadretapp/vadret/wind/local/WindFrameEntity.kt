package se.vadretapp.vadret.wind.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WindFrameEntity(
    @SerialName(value = "valid_at_epoch_millis")
    val validAtEpochMillis: Long,
    @SerialName(value = "wind_speeds")
    val windSpeeds: List<Float>,
    @SerialName(value = "wind_from_directions")
    val windFromDirections: List<Float>
)