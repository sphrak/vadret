package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.warning.extension.toWarningPlace
import kotlin.time.Clock

internal class DefaultObserveUnseenFollowedPlaceWarningsTask(
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask,
    private val feedRepository: FeedRepository,
    private val getForecastWarningRequestTask: GetForecastWarningRequestTask,
    private val clock: Clock
) : ObserveUnseenFollowedPlaceWarningsTask {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>> =
        observeFollowedPlacesTask().flatMapLatest { followedPlaces: Either<Failure, List<FollowedPlace>> ->
            followedPlaces.onResult(
                onError = { failure: Failure -> flowOf(value = failure.asLeft()) },
                onSuccess = { places: List<FollowedPlace> ->
                    feedRepository.observeUnseenWarnings(
                        warningPlaces = places.map { followedPlace: FollowedPlace -> followedPlace.toWarningPlace() },
                        isRefresh = isRefresh,
                        getForecastWarningRequestTask = getForecastWarningRequestTask,
                        clock = clock
                    )
                }
            )
        }
}