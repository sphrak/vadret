package se.vadretapp.vadret.settings.model

internal data class FollowedPlaceRow(
    val favoriteId: String,
    val name: String,
    val isFollowed: Boolean,
    val isInSweden: Boolean
)