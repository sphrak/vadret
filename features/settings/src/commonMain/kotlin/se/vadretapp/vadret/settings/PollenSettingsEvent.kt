package se.vadretapp.vadret.settings

internal sealed interface PollenSettingsEvent {
    data object OnStart : PollenSettingsEvent
    data class OnPollenToggled(val isPollenEnabled: Boolean) : PollenSettingsEvent
    data class OnPollenMarked(
        val pollenId: String,
        val isMarked: Boolean
    ) : PollenSettingsEvent
}