package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf
import se.vadretapp.vadret.pushalert.model.FavoriteRow

internal fun Favorite.toGeohashOrNull(): Geohash? =
    geohashOf(
        latitude = latitude,
        longitude = longitude
    )

internal fun Favorite.toFavoriteRow(favoriteIds: Set<String>): FavoriteRow =
    FavoriteRow(
        favoriteId = favoriteId,
        locality = locality,
        isChecked = favoriteId in favoriteIds,
        isInSweden = toGeohashOrNull()?.isInSwedishWaters() == true
    )