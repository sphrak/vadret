package se.vadretapp.vadret.nominatim.extension

import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.nominatim.remote.NominatimDto

internal fun NominatimDto.toLocality(): Locality =
    Locality(
        name = address?.city ?: address?.hamlet ?: address?.village,
        municipality = address?.municipality,
        county = address?.county
    )