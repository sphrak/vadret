package se.vadretapp.vadret.weatherforecast.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AnalysisEntity(
    @SerialName(value = "analysed_at_epoch_millis")
    val analysedAtEpochMillis: Long,
    @SerialName(value = "air_temperature")
    val airTemperature: Double,
    @SerialName(value = "wind_from_direction")
    val windFromDirection: Double,
    @SerialName(value = "wind_speed")
    val windSpeed: Double,
    @SerialName(value = "relative_humidity")
    val relativeHumidity: Int,
    @SerialName(value = "precipitation_type")
    val precipitationType: Int,
    @SerialName(value = "symbol_code")
    val symbolCode: Int,
    @SerialName(value = "precipitation_amount_last_hour")
    val precipitationAmountLastHour: Double? = null
)