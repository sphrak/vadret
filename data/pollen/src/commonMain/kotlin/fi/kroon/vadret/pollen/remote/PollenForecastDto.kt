package fi.kroon.vadret.pollen.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PollenForecastDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "regionId")
    val regionId: String,
    @SerialName(value = "startDate")
    val startDate: String,
    @SerialName(value = "endDate")
    val endDate: String,
    @SerialName(value = "text")
    val text: String? = null,
    @SerialName(value = "isEndOfSeason")
    val isEndOfSeason: Boolean? = null,
    @SerialName(value = "levelSeries")
    val levelSeries: List<PollenLevelDto>? = null
)