package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class HourFormatDto {
    @SerialName(value = "24h")
    TWENTY_FOUR_HOUR,

    @SerialName(value = "12h")
    TWELVE_HOUR
}