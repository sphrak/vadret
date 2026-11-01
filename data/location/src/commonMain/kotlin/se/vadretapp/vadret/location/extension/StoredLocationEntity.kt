package se.vadretapp.vadret.location.extension

import se.vadretapp.vadret.location.local.StoredLocationEntity
import se.vadretapp.vadret.location.model.StoredLocation

internal fun StoredLocationEntity.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )