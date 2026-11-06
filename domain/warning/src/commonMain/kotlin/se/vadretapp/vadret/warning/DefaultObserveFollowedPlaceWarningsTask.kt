package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.warning.extension.affects
import se.vadretapp.vadret.warning.extension.observeRefreshedFeed
import se.vadretapp.vadret.warning.extension.toWarningPlace
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning

internal class DefaultObserveFollowedPlaceWarningsTask(
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask,
    private val feedRepository: FeedRepository
) : ObserveFollowedPlaceWarningsTask {

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, FollowedPlaceWarnings>> =
        combine(
            flow = observeFollowedPlacesTask(),
            flow2 = feedRepository.observeRefreshedFeed(isRefresh = isRefresh)
        ) { followedPlaces: Either<Failure, List<FollowedPlace>>, aggregatedFeeds: Either<Failure, List<AggregatedFeed>> ->
            followedPlaces.flatMap { places: List<FollowedPlace> ->
                aggregatedFeeds.map { feeds: List<AggregatedFeed> ->
                    FollowedPlaceWarnings(
                        followedPlaces = places,
                        placeWarnings = feeds.mapNotNull { aggregatedFeed: AggregatedFeed -> aggregatedFeed.toPlaceWarning(followedPlaces = places) }
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