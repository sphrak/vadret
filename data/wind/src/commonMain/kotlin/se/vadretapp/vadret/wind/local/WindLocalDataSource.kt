package se.vadretapp.vadret.wind.local

internal interface WindLocalDataSource {
    fun getWindForecast(): WindForecastEntity?
    fun setWindForecast(windForecastEntity: WindForecastEntity)
}