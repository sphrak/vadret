package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.settings.model.FailureDialog

internal data class PlacesState(
    val isFollowingPosition: Boolean = false,
    val isLocationPermissionRequested: Boolean = false,
    val favoriteChoices: ImmutableList<FavoriteChoice> = persistentListOf(),
    val searchQuery: String = "",
    val suggestions: ImmutableList<Place> = persistentListOf(),
    val failureDialog: FailureDialog? = null
)