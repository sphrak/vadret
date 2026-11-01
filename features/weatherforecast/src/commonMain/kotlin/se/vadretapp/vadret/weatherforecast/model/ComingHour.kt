package se.vadretapp.vadret.weatherforecast.model

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

data class ComingHour(
    val key: String,
    val isNow: Boolean,
    val timeText: String,
    val icon: VadretIconGraphic,
    val descriptionResource: StringResource,
    val temperatureText: String,
    val temperatureLevel: Float,
    val precipitationText: String?,
    val windSpeedText: String,
    val windLevel: Float,
    val windArrowRotation: Float,
    val windDirectionResource: StringResource
)