package se.vadretapp.vadret.weatherforecast.remote

internal interface WeatherForecastRemoteDataSource {
    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double
    ): WeatherForecastDto

    suspend fun getAnalysis(
        latitude: Double,
        longitude: Double
    ): AnalysisDto
}