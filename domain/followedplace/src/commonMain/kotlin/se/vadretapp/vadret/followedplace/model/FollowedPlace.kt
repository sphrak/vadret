package se.vadretapp.vadret.followedplace.model

data class FollowedPlace(
    val favoriteId: String?,
    val name: String?,
    val municipality: String?,
    val county: String?,
    val latitude: Double,
    val longitude: Double,
    val isChosen: Boolean
)