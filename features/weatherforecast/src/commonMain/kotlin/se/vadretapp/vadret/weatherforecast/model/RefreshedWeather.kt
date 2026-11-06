package se.vadretapp.vadret.weatherforecast.model

internal data class RefreshedWeather(
    val placeWeather: PlaceWeather,
    val isRefreshFailed: Boolean
)