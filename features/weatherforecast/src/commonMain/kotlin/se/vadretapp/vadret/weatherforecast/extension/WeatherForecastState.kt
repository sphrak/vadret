package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast

internal fun WeatherForecastState.withComingDays(comingDays: ImmutableList<ComingDay>): WeatherForecastState = copy(forecast = forecast.copy(comingDays = comingDays))

internal fun WeatherForecastState.withForecast(
    forecast: WeatherForecast,
    isSamePlace: Boolean
): WeatherForecastState {
    val comingDays: ImmutableList<ComingDay> = if (isSamePlace) {
        forecast.comingDays.withExpansionOf(previousComingDays = this.forecast.comingDays)
    } else {
        forecast.comingDays
    }
    return copy(forecast = forecast).withComingDays(comingDays = comingDays)
}