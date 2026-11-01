package se.vadretapp.vadret.wind.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WindParameterDto(
    @SerialName(value = "referenceTime")
    val referenceTime: String,
    @SerialName(value = "geometry")
    val geometry: MultiPointDto? = null,
    @SerialName(value = "timeSeries")
    val timeSeries: List<WindTimeSeriesDto>
)