package se.vadretapp.vadret.followedplace.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.model.StoredLocation

// The chosen place is a copy of the favorite it was chosen from, so the favorite is found again by its coordinates.
internal fun StoredLocation.toChosenPlace(favorites: List<Favorite>): FollowedPlace =
    FollowedPlace(
        favoriteId = favorites
            .firstOrNull { favorite: Favorite -> favorite.latitude == latitude && favorite.longitude == longitude }
            ?.favoriteId,
        name = locality,
        municipality = municipality.ifEmpty { null },
        county = county.ifEmpty { null },
        latitude = latitude,
        longitude = longitude,
        isChosen = true
    )