package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.model.PlaceNaming

// The dialog only opens when the name Nominatim found cannot be used, so a written name replaces it.
internal fun PlaceNaming?.localityFor(placeName: String?): String? =
    if (this == null) {
        placeName
    } else {
        label.trim().ifEmpty { null }
    }

internal fun PlaceNaming?.withoutName(): PlaceNaming =
    if (this == null) {
        PlaceNaming()
    } else {
        copy(isLabelMissing = true)
    }

internal fun PlaceNaming?.withNameTaken(placeName: String): PlaceNaming =
    (this ?: PlaceNaming()).copy(
        isLabelMissing = false,
        takenPlaceName = placeName
    )