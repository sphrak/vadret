package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather

internal fun PlaceWeather.toRefreshedWeather(): RefreshedWeather =
    RefreshedWeather(
        placeWeather = this,
        isRefreshFailed = false
    )