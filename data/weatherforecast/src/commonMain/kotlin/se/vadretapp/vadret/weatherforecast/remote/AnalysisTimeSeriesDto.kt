package se.vadretapp.vadret.weatherforecast.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AnalysisTimeSeriesDto(
    @SerialName(value = "time")
    val time: String,
    @SerialName(value = "data")
    val data: AnalysisDataDto
)