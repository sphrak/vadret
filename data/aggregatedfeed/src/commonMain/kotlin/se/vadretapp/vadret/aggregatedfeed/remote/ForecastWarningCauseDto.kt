package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForecastWarningCauseDto(
    @SerialName(value = "cause")
    val cause: String? = null,
    @SerialName(value = "level")
    val level: String? = null
)