package se.vadretapp.vadret.place.local

internal data class PlaceEntity(
    val locality: String,
    val municipality: String,
    val county: String,
    val latitude: Double,
    val longitude: Double
)