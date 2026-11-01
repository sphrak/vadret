package se.vadretapp.vadret.forecastwarning

internal sealed interface WarningStoryEvent {
    data class OnStart(val warningIdentifiers: List<String>) : WarningStoryEvent
    data class OnStoryShown(val index: Int) : WarningStoryEvent
    data object OnNext : WarningStoryEvent
    data object OnPrevious : WarningStoryEvent
    data class OnLocaleChanged(val languageTag: String) : WarningStoryEvent
}