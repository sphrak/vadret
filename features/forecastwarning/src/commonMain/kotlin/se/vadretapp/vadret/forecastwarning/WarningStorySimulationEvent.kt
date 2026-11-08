package se.vadretapp.vadret.forecastwarning

import se.vadretapp.vadret.setting.model.HourFormat

internal sealed interface WarningStorySimulationEvent {
    data class OnLocaleChanged(
        val languageTag: String,
        val hourFormat: HourFormat
    ) : WarningStorySimulationEvent
}