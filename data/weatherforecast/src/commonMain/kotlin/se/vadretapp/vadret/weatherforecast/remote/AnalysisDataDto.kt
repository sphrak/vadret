package se.vadretapp.vadret.weatherforecast.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AnalysisDataDto(
    @SerialName(value = "air_temperature")
    val airTemperature: Double,
    @SerialName(value = "wind_from_direction")
    val windFromDirection: Double,
    @SerialName(value = "wind_speed")
    val windSpeed: Double,
    @SerialName(value = "relative_humidity")
    val relativeHumidity: Int,
    @SerialName(value = "predominant_precipitation_type_at_surface")
    val precipitationType: Int,
    @SerialName(value = "symbol_code")
    val symbolCode: Int,
    @SerialName(value = "precipitation_amount_last_1_hours")
    val precipitationAmountLastHour: Double? = null
)