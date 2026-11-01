package se.vadretapp.vadret.location.extension

import se.vadretapp.vadret.location.local.LocationEntity
import se.vadretapp.vadret.location.model.Location

internal fun LocationEntity.toLocation(): Location =
    Location(
        latitude = latitude,
        longitude = longitude
    )