package se.vadretapp.vadret.weatherforecast.local

import kotlinx.coroutines.flow.Flow

internal interface WeatherForecastLocalDataSource {

    fun observeWeather(placeKey: String): Flow<SavedWeatherEntity?>
    suspend fun setWeather(savedWeatherEntity: SavedWeatherEntity)
}