package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForecastWarningDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "event")
    val event: String? = null,
    @SerialName(value = "warning_areas")
    val warningAreas: List<ForecastWarningAreaDto>? = null
)