package se.vadretapp.vadret.wind.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MultiPointDto(
    @SerialName(value = "type")
    val type: String,
    @SerialName(value = "coordinates")
    val coordinates: List<List<Double>>
)