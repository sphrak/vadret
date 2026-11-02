package se.vadretapp.vadret.nominatim.local

import se.vadretapp.vadret.nominatim.model.Locality

internal interface LocalityInMemoryDataSource {
    fun locality(
        latitude: Double,
        longitude: Double
    ): Locality?

    fun remember(
        latitude: Double,
        longitude: Double,
        locality: Locality
    )
}