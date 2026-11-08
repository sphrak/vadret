package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForecastWarningsDto(
    @SerialName(value = "warnings")
    val warnings: List<ForecastWarningDto>? = null
)