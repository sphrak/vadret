package se.vadretapp.vadret.place.extension

import se.vadretapp.vadret.place.local.PlaceEntity
import se.vadretapp.vadret.place.model.Place

internal fun PlaceEntity.toPlace(): Place =
    Place(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )