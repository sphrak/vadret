package se.vadretapp.vadret.weatherforecast.local

internal interface WeatherForecastLocalDataSource {

    suspend fun getForecasts(placeKey: String): List<ForecastEntity>?
    suspend fun setForecasts(
        placeKey: String,
        forecastEntities: List<ForecastEntity>
    )
    suspend fun getAnalyses(placeKey: String): List<AnalysisEntity>?
    suspend fun setAnalyses(
        placeKey: String,
        analysisEntities: List<AnalysisEntity>
    )
}