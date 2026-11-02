package se.vadretapp.vadret.followedplace.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowedPlace

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