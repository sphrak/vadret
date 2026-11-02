package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal fun Favorite.toStoredLocation(): StoredLocation =
    StoredLocation(
        locality = locality,
        municipality = municipality,
        county = county,
        latitude = latitude,
        longitude = longitude
    )

// Only a favorite on the very spot counts, since one of the same name elsewhere is another place.
internal fun Favorite.isAt(placeForecast: PlaceForecast): Boolean = latitude == placeForecast.latitude && longitude == placeForecast.longitude