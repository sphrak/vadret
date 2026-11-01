package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

data class ComingDay(
    val key: String,
    val isToday: Boolean,
    val weekdayText: String,
    val dateText: String,
    val icon: VadretIconGraphic,
    val descriptionResource: StringResource,
    val precipitationText: String?,
    val lowTemperatureText: String,
    val highTemperatureText: String,
    val temperatureRangeStart: Float,
    val temperatureRangeEnd: Float,
    val hours: ImmutableList<ComingHour>,
    val firstShownHourIndex: Int,
    val isExpanded: Boolean
)