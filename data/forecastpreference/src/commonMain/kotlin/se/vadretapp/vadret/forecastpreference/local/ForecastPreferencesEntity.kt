package se.vadretapp.vadret.forecastpreference.local

internal data class ForecastPreferencesEntity(
    val isHourlyForecastEnabled: Boolean?,
    val isExpandOnceEnabled: Boolean?,
    val isAuroraEnabled: Boolean?
)