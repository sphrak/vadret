package se.vadretapp.vadret.wind.local

import kotlinx.coroutines.flow.Flow

internal interface WindLocalDataSource {
    suspend fun getWindForecast(): WindForecastEntity?
    fun observeWindForecast(): Flow<WindForecastEntity?>
    suspend fun setWindForecast(windForecastEntity: WindForecastEntity)
}