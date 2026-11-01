package se.vadretapp.vadret.wind.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WindTimeSeriesDto(
    @SerialName(value = "time")
    val time: String,
    @SerialName(value = "data")
    val data: Map<String, List<Double>>
)