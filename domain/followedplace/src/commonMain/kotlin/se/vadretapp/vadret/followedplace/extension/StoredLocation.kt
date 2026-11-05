package se.vadretapp.vadret.followedplace.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.model.StoredLocation

internal fun StoredLocation.toChosenPlace(chosenFavorite: Favorite?): FollowedPlace =
    FollowedPlace(
        favoriteId = chosenFavorite?.favoriteId,
        name = locality,
        municipality = municipality.ifEmpty { null },
        county = county.ifEmpty { null },
        latitude = latitude,
        longitude = longitude,
        isChosen = true
    )