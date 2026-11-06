package se.vadretapp.vadret.weatherforecast.local

import kotlinx.coroutines.flow.Flow

internal interface WeatherForecastLocalDataSource {

    fun observeWeather(placeKey: String): Flow<SavedWeatherEntity?>
    suspend fun setForecasts(
        placeKey: String,
        isChosenPlace: Boolean,
        forecastEntities: List<ForecastEntity>,
        fetchedAtEpochMillis: Long
    )
    suspend fun setAnalyses(
        placeKey: String,
        isChosenPlace: Boolean,
        analysisEntities: List<AnalysisEntity>
    )
}