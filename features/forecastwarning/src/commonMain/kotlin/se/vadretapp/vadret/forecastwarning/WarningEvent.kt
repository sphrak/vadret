package se.vadretapp.vadret.forecastwarning

internal sealed interface WarningEvent {
    data object OnStart : WarningEvent
    data object OnRefresh : WarningEvent
    data object OnErrorShown : WarningEvent
    data class OnLocaleChanged(val languageTag: String) : WarningEvent
}