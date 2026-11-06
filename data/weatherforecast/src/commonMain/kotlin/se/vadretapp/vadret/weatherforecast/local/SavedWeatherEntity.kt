package se.vadretapp.vadret.weatherforecast.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedWeatherEntity(
    @SerialName(value = "place_key")
    val placeKey: String,
    @SerialName(value = "is_chosen_place")
    val isChosenPlace: Boolean,
    @SerialName(value = "fetched_at_epoch_millis")
    val fetchedAtEpochMillis: Long?,
    @SerialName(value = "forecasts")
    val forecasts: List<ForecastEntity>,
    @SerialName(value = "analyses")
    val analyses: List<AnalysisEntity>
)