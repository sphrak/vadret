package se.vadretapp.vadret.forecastwarning

import se.vadretapp.vadret.setting.model.HourFormat

internal sealed interface WarningDetailEvent {
    data class OnStart(val warningIdentifier: String) : WarningDetailEvent
    data class OnLocaleChanged(
        val languageTag: String,
        val hourFormat: HourFormat
    ) : WarningDetailEvent
}