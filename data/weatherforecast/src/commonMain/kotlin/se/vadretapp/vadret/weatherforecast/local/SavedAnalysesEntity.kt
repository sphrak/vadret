package se.vadretapp.vadret.weatherforecast.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedAnalysesEntity(
    @SerialName(value = "place_key")
    val placeKey: String,
    @SerialName(value = "analyses")
    val analyses: List<AnalysisEntity>
)