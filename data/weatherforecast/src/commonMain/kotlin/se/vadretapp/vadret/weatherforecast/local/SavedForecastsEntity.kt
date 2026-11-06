package se.vadretapp.vadret.weatherforecast.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedForecastsEntity(
    @SerialName(value = "place_key")
    val placeKey: String,
    @SerialName(value = "forecasts")
    val forecasts: List<ForecastEntity>
)