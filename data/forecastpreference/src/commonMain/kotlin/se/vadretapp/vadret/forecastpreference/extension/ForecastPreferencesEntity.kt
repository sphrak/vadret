package se.vadretapp.vadret.forecastpreference.extension

import se.vadretapp.vadret.forecastpreference.local.ForecastPreferencesEntity
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

private const val IS_HOURLY_FORECAST_ENABLED_BY_DEFAULT = false
private const val IS_EXPAND_ONCE_ENABLED_BY_DEFAULT = true
private const val IS_AURORA_ENABLED_BY_DEFAULT = false

internal fun ForecastPreferencesEntity.toForecastPreferences(): ForecastPreferences =
    ForecastPreferences(
        isHourlyForecastEnabled = isHourlyForecastEnabled ?: IS_HOURLY_FORECAST_ENABLED_BY_DEFAULT,
        isExpandOnceEnabled = isExpandOnceEnabled ?: IS_EXPAND_ONCE_ENABLED_BY_DEFAULT,
        isAuroraEnabled = isAuroraEnabled ?: IS_AURORA_ENABLED_BY_DEFAULT
    )