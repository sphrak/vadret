package se.vadretapp.vadret.settings

import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.theme.model.Theme

internal data class Settings(
    val isAutomaticLocation: Boolean,
    val theme: Theme,
    val language: AppLanguage,
    val isHourlyForecastEnabled: Boolean,
    val isExpandOnceEnabled: Boolean,
    val storedLocation: StoredLocation
)