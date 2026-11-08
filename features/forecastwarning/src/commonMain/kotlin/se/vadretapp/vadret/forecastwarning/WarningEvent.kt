package se.vadretapp.vadret.forecastwarning

import se.vadretapp.vadret.setting.model.HourFormat

internal sealed interface WarningEvent {
    data object OnStart : WarningEvent
    data object OnRefresh : WarningEvent
    data object OnErrorShown : WarningEvent
    data class OnLocaleChanged(
        val languageTag: String,
        val hourFormat: HourFormat
    ) : WarningEvent
}