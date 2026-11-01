package se.vadretapp.vadret.favorite.model

data class Favorite(
    val favoriteId: String,
    val locality: String,
    val municipality: String,
    val county: String,
    val latitude: Double,
    val longitude: Double
)