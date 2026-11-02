package se.vadretapp.vadret.followedplace.model

data class FollowableFavorite(
    val favoriteId: String,
    val name: String,
    val isFollowed: Boolean,
    val isChosen: Boolean,
    val isInSweden: Boolean
)