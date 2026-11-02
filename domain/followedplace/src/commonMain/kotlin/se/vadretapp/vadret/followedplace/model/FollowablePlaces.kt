package se.vadretapp.vadret.followedplace.model

data class FollowablePlaces(
    val chosenPlace: FollowedPlace?,
    val favorites: List<FollowableFavorite>
)