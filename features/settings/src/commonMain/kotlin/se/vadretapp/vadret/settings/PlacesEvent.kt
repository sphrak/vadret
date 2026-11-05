package se.vadretapp.vadret.settings

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.place.model.Place

internal sealed interface PlacesEvent {
    data object OnStart : PlacesEvent
    data class OnFollowPositionToggled(val isFollowingPosition: Boolean) : PlacesEvent
    data class OnLocationPermissionResult(val isGranted: Boolean) : PlacesEvent
    data class OnSearchQueryChanged(val query: String) : PlacesEvent
    data class OnSearchSubmitted(val query: String) : PlacesEvent
    data object OnSearchCleared : PlacesEvent
    data class OnSuggestionClicked(val place: Place) : PlacesEvent
    data class OnPlaceChosen(val favorite: Favorite) : PlacesEvent
    data class OnPlaceRemoved(val favoriteId: String) : PlacesEvent
    data object OnFailureDismissed : PlacesEvent
}