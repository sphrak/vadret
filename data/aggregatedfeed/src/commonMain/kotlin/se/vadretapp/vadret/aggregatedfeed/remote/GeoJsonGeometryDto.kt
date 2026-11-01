package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class GeoJsonGeometryDto(
    @SerialName(value = "type")
    val type: String,
    @SerialName(value = "coordinates")
    val coordinates: JsonElement? = null
)