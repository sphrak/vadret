package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeoPolygonEntity(
    @SerialName(value = "outline")
    val outline: List<GeoPositionEntity>,
    @SerialName(value = "holes")
    val holes: List<List<GeoPositionEntity>>
)