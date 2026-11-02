package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveFollowedPlacesTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace

internal class FakeObserveFollowedPlacesTask : ObserveFollowedPlacesTask {

    val followedPlaces: MutableStateFlow<Either<Failure, List<FollowedPlace>>> = MutableStateFlow(value = emptyList<FollowedPlace>().asRight())

    override fun invoke(): Flow<Either<Failure, List<FollowedPlace>>> = followedPlaces
}