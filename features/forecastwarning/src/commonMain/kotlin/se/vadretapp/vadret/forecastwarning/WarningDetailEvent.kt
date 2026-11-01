package se.vadretapp.vadret.forecastwarning

internal sealed interface WarningDetailEvent {
    data class OnStart(val warningIdentifier: String) : WarningDetailEvent
    data class OnLocaleChanged(val languageTag: String) : WarningDetailEvent
}