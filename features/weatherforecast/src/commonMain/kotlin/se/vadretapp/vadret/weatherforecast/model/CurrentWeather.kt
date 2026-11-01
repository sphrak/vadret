package se.vadretapp.vadret.weatherforecast.model

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

data class CurrentWeather(
    val temperatureText: String,
    val feelsLikeTemperatureText: String,
    val icon: VadretIconGraphic,
    val descriptionResource: StringResource,
    val windDirectionResource: StringResource,
    val windSpeed: Double,
    val windArrowRotation: Float,
    val humidityPercent: Int
)