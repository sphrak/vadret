package se.vadretapp.vadret.weatherforecast.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.icons.VadretIconGraphic

@Immutable
data class ComingDay(
    val key: String,
    val date: LocalDate,
    val isToday: Boolean,
    val weekdayText: String,
    val dateText: String,
    val icon: VadretIconGraphic,
    val descriptionResource: StringResource,
    val precipitationText: String?,
    val lowTemperatureText: String,
    val highTemperatureText: String,
    val lowTemperature: Double,
    val highTemperature: Double,
    val temperatureRangeStart: Float,
    val temperatureRangeEnd: Float,
    val hours: ImmutableList<ComingHour>,
    val firstShownHourIndex: Int,
    val isExpanded: Boolean
)