package se.vadretapp.vadret.weatherforecast.model

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

data class ForecastHour(
    val key: String,
    val timeText: String,
    val temperature: Double,
    val feelsLikeTemperature: String?,
    val descriptionResource: StringResource,
    val icon: VadretIconGraphic
)