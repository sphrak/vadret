package se.vadretapp.vadret.settings

import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.theme.model.Theme

internal data class Settings(
    val theme: Theme,
    val language: AppLanguage,
    val isHourlyForecastEnabled: Boolean,
    val isExpandOnceEnabled: Boolean,
    val isPollenEnabled: Boolean,
    val allergenPollenIds: Set<String>,
    val isEndOfSeasonSimulated: Boolean
)