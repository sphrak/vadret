package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.model.FollowablePlaces

interface ObserveFollowablePlacesTask {

    operator fun invoke(): Flow<Either<Failure, FollowablePlaces>>
}