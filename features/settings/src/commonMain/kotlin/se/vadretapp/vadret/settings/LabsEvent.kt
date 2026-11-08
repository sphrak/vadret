package se.vadretapp.vadret.settings

internal sealed interface LabsEvent {
    data object OnStart : LabsEvent
    data class OnHourlyForecastToggled(val isHourlyForecastEnabled: Boolean) : LabsEvent
    data class OnAuroraToggled(val isAuroraEnabled: Boolean) : LabsEvent
    data object OnFailureDismissed : LabsEvent
}