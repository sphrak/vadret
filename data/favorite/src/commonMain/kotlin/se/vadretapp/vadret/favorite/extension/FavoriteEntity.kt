package se.vadretapp.vadret.favorite.extension

import se.vadretapp.vadret.favorite.local.FavoriteEntity
import se.vadretapp.vadret.favorite.model.Favorite

internal fun FavoriteEntity.toFavorite(): Favorite =
    Favorite(
        favoriteId = favoriteId,
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )