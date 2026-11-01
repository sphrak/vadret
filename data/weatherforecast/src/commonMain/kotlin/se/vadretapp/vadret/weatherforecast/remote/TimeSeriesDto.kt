package se.vadretapp.vadret.weatherforecast.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TimeSeriesDto(
    @SerialName(value = "time")
    val time: String,
    @SerialName(value = "intervalParametersStartTime")
    val intervalParametersStartTime: String,
    @SerialName(value = "data")
    val data: ForecastDataDto
)