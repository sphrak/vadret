package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubscriptionDto(
    @SerialName(value = "geohashes")
    val geohashes: List<String>? = null,
    @SerialName(value = "place_names")
    val placeNames: Map<String, String?>? = null,
    @SerialName(value = "daily_geohash")
    val dailyGeohash: String? = null,
    @SerialName(value = "warning_lowest_level")
    val warningLowestLevel: String? = null,
    @SerialName(value = "pollen")
    val pollen: List<PollenSubscriptionDto>? = null,
    @SerialName(value = "temperature_trend")
    val temperatureTrend: Boolean? = null,
    @SerialName(value = "strong_wind")
    val strongWind: Boolean? = null,
    @SerialName(value = "slipperiness")
    val slipperiness: Boolean? = null,
    @SerialName(value = "aurora")
    val aurora: Boolean? = null,
    @SerialName(value = "hour_format")
    val hourFormat: HourFormatDto? = null,
    @SerialName(value = "language")
    val language: LanguageDto? = null
)