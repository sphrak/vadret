package se.vadretapp.vadret.settings

internal sealed interface DeveloperEvent {
    data object OnStart : DeveloperEvent
    data class OnExpandOnceToggled(val isExpandOnceEnabled: Boolean) : DeveloperEvent
    data class OnEndOfSeasonSimulationToggled(val isEndOfSeasonSimulated: Boolean) : DeveloperEvent
}