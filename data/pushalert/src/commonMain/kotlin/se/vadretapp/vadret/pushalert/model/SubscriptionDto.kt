package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionDto(
    @SerialName(value = "geohashes")
    val geohashes: List<String>? = null,
    @SerialName(value = "warning_lowest_level")
    val warningLowestLevel: String? = null,
    @SerialName(value = "pollen")
    val pollen: List<PollenSubscriptionDto>? = null,
    @SerialName(value = "temperature_trend")
    val temperatureTrend: Boolean? = null,
    @SerialName(value = "strong_wind")
    val strongWind: Boolean? = null
)