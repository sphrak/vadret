package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList

data class ForecastDay(
    val key: String,
    val weekday: String,
    val dateText: String,
    val hours: ImmutableList<ForecastHour>
)