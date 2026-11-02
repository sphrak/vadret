package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf

internal fun Favorite.toGeohashOrNull(): Geohash? =
    geohashOf(
        latitude = latitude,
        longitude = longitude
    )