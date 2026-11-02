package se.vadretapp.vadret.settings.extension

import se.vadretapp.vadret.followedplace.model.FollowableFavorite
import se.vadretapp.vadret.settings.model.FollowedPlaceRow

internal fun FollowableFavorite.toFollowedPlaceRow(): FollowedPlaceRow =
    FollowedPlaceRow(
        favoriteId = favoriteId,
        name = name,
        isFollowed = isFollowed,
        isInSweden = isInSweden
    )