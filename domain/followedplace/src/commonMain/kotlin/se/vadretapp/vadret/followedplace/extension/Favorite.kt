package se.vadretapp.vadret.followedplace.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.model.StoredLocation

internal fun Favorite.toFollowedPlace(): FollowedPlace =
    FollowedPlace(
        favoriteId = favoriteId,
        name = locality,
        municipality = municipality.ifEmpty { null },
        county = county.ifEmpty { null },
        latitude = latitude,
        longitude = longitude,
        isChosen = false
    )

internal fun Favorite.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )