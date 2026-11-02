package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.nominatim.NominatimRepository

interface ObserveChosenPlaceWarningsTask {

    operator fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>>
}

fun ObserveChosenPlaceWarningsTask(
    feedRepository: FeedRepository,
    locationRepository: LocationRepository,
    nominatimRepository: NominatimRepository
): ObserveChosenPlaceWarningsTask =
    DefaultObserveChosenPlaceWarningsTask(
        feedRepository = feedRepository,
        locationRepository = locationRepository,
        nominatimRepository = nominatimRepository
    )