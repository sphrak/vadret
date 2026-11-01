package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeoLineEntity(
    @SerialName(value = "positions")
    val positions: List<GeoPositionEntity>
)