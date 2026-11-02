package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.model.FollowedPlace

interface GetChosenPlaceTask {

    suspend operator fun invoke(): Either<Failure, FollowedPlace>
}