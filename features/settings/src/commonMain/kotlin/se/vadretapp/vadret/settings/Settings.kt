package se.vadretapp.vadret.settings

import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme

internal data class Settings(
    val theme: Theme,
    val language: AppLanguage,
    val hourFormatChoice: HourFormatChoice,
    val isHourlyForecastEnabled: Boolean,
    val isExpandOnceEnabled: Boolean,
    val isPollenEnabled: Boolean,
    val allergenPollenIds: Set<String>,
    val isEndOfSeasonSimulated: Boolean
)