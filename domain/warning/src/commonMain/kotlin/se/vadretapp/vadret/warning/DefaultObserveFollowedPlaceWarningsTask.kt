package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.warning.extension.affects
import se.vadretapp.vadret.warning.extension.toWarningPlace
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning

internal class DefaultObserveFollowedPlaceWarningsTask(
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask,
    private val feedRepository: FeedRepository
) : ObserveFollowedPlaceWarningsTask {

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, FollowedPlaceWarnings>> =
        observeFollowedPlacesTask().map { followedPlaces: Either<Failure, List<FollowedPlace>> ->
            followedPlaces.flatMap { places: List<FollowedPlace> ->
                feedRepository
                    .getAggregatedFeed(isRefresh = isRefresh)
                    .map { aggregatedFeeds: List<AggregatedFeed> ->
                        FollowedPlaceWarnings(
                            followedPlaces = places,
                            placeWarnings = aggregatedFeeds.mapNotNull { aggregatedFeed: AggregatedFeed -> aggregatedFeed.toPlaceWarning(followedPlaces = places) }
                        )
                    }
            }
        }

    private fun AggregatedFeed.toPlaceWarning(followedPlaces: List<FollowedPlace>): PlaceWarning? {
        val affectedPlaces: List<FollowedPlace> = followedPlaces.filter { followedPlace: FollowedPlace -> affects(warningPlace = followedPlace.toWarningPlace()) }
        if (affectedPlaces.isEmpty()) {
            return null
        }
        return PlaceWarning(
            aggregatedFeed = this,
            followedPlaces = affectedPlaces
        )
    }
}