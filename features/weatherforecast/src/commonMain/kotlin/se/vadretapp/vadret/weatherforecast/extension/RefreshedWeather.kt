package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather

internal fun RefreshedWeather.toPlaceForecast(
    placeName: String?,
    municipality: String?,
    county: String?,
    latitude: Double,
    longitude: Double
): PlaceForecast =
    PlaceForecast(
        placeName = placeName,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude,
        forecasts = placeWeather.forecasts,
        analyses = placeWeather.analyses,
        fetchedAt = placeWeather.fetchedAt,
        isRefreshFailed = isRefreshFailed
    )