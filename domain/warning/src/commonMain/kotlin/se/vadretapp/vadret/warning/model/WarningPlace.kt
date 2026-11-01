package se.vadretapp.vadret.warning.model

data class WarningPlace(
    val latitude: Double,
    val longitude: Double,
    val county: String?,
    val municipality: String?
)