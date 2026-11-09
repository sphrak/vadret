package se.vadretapp.vadret.weatherforecast.model

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

data class WeekDay(
    val dayKey: String,
    val isToday: Boolean,
    val weekdayText: String,
    val icon: VadretIconGraphic,
    val descriptionResource: StringResource,
    val precipitationText: String?,
    val lowTemperatureText: String,
    val highTemperatureText: String,
    val lowTemperatureLevel: Float,
    val highTemperatureLevel: Float
)