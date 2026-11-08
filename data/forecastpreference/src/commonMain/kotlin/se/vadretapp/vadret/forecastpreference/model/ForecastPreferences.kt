package se.vadretapp.vadret.forecastpreference.model

data class ForecastPreferences(
    val isHourlyForecastEnabled: Boolean,
    val isExpandOnceEnabled: Boolean,
    val isAuroraEnabled: Boolean
)