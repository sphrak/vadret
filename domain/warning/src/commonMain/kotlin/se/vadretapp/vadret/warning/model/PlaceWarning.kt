package se.vadretapp.vadret.warning.model

import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.followedplace.model.FollowedPlace

data class PlaceWarning(
    val aggregatedFeed: AggregatedFeed,
    val followedPlaces: List<FollowedPlace>
)