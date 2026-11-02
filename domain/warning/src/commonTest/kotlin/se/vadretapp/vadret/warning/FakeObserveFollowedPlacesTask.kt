package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace

internal val followedFunasdalen = FollowedPlace(
    favoriteId = null,
    name = "Funäsdalen",
    municipality = "Härjedalen",
    county = "Jämtland",
    latitude = 62.546,
    longitude = 12.542,
    isChosen = true
)

internal val followedVisby = FollowedPlace(
    favoriteId = "favorite-followedVisby",
    name = "Visby",
    municipality = "Gotland",
    county = "Gotland",
    latitude = 57.634,
    longitude = 18.294,
    isChosen = false
)

internal class FakeObserveFollowedPlacesTask : ObserveFollowedPlacesTask {

    val followedPlaces: MutableStateFlow<Either<Failure, List<FollowedPlace>>> = MutableStateFlow(value = listOf(followedFunasdalen).asRight())

    override fun invoke(): Flow<Either<Failure, List<FollowedPlace>>> = followedPlaces
}