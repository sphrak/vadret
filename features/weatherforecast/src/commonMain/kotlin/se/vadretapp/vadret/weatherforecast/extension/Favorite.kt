package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.StoredLocation

internal fun Favorite.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )