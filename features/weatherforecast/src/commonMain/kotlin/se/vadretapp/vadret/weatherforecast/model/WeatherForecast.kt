package se.vadretapp.vadret.weatherforecast.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class WeatherForecast(
    val current: CurrentWeather? = null,
    val comingDays: ImmutableList<ComingDay> = persistentListOf(),
    val days: ImmutableList<ForecastDay> = persistentListOf()
)