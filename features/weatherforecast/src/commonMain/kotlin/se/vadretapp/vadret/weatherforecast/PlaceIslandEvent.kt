package se.vadretapp.vadret.weatherforecast

internal sealed interface PlaceIslandEvent {
    data class OnPlaceDropped(
        val latitude: Double,
        val longitude: Double
    ) : PlaceIslandEvent
    data object OnRetryClicked : PlaceIslandEvent
    data class OnLabelChanged(val label: String) : PlaceIslandEvent
    data object OnSaveClicked : PlaceIslandEvent
    data object OnLabelDismissed : PlaceIslandEvent
    data object OnOpenForecastClicked : PlaceIslandEvent
    data object OnOpeningShown : PlaceIslandEvent
}