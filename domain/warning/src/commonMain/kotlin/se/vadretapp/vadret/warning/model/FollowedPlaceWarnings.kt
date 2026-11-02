package se.vadretapp.vadret.warning.model

import se.vadretapp.vadret.followedplace.model.FollowedPlace

data class FollowedPlaceWarnings(
    val followedPlaces: List<FollowedPlace>,
    val placeWarnings: List<PlaceWarning>
)