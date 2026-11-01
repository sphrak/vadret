package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeoPositionEntity(
    @SerialName(value = "latitude")
    val latitude: Double,
    @SerialName(value = "longitude")
    val longitude: Double
)