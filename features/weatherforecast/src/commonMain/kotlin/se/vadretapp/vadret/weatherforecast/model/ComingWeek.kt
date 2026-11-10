package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList

data class ComingWeek(
    val weekNumberText: String,
    val dateRangeText: String,
    val days: ImmutableList<WeekDay>,
    val shadeStops: ImmutableList<TemperatureShadeStop>
)