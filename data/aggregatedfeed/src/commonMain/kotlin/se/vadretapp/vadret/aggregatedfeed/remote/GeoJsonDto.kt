package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeoJsonDto(
    @SerialName(value = "type")
    val type: String,
    @SerialName(value = "geometry")
    val geometry: GeoJsonGeometryDto? = null,
    @SerialName(value = "features")
    val features: List<GeoJsonDto>? = null
)