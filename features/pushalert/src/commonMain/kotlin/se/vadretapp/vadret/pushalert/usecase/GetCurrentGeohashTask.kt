package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash

internal interface GetCurrentGeohashTask {
    suspend operator fun invoke(): Either<Failure, Geohash>
}