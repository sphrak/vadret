package se.vadretapp.vadret.location.local

internal data class StoredLocationEntity(
    val locality: String,
    val municipality: String,
    val county: String,
    val latitude: Double,
    val longitude: Double
)