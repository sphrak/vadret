package se.vadretapp.vadret.weatherforecast

import se.vadretapp.vadret.place.model.Place

internal sealed interface PlaceSearchEvent {
    data class OnQueryChanged(val query: String) : PlaceSearchEvent
    data class OnSearchSubmitted(val query: String) : PlaceSearchEvent
    data class OnSuggestionClicked(val place: Place) : PlaceSearchEvent
    data object OnErrorShown : PlaceSearchEvent
}