package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForecastWarningAreaDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "geohash")
    val geohash: String? = null,
    @SerialName(value = "published")
    val published: String? = null,
    @SerialName(value = "approximate_start")
    val approximateStart: String? = null,
    @SerialName(value = "approximate_end")
    val approximateEnd: String? = null,
    @SerialName(value = "level")
    val level: String? = null,
    @SerialName(value = "causes")
    val causes: List<ForecastWarningCauseDto>? = null,
    @SerialName(value = "score")
    val score: Int? = null,
    @SerialName(value = "peak")
    val peak: String? = null,
    @SerialName(value = "area")
    val area: GeoJsonDto? = null
)