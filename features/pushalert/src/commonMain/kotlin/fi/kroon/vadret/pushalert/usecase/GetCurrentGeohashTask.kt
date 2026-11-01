package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.geohash.model.Geohash
import io.github.sphrak.either.Either

internal interface GetCurrentGeohashTask {
    suspend operator fun invoke(): Either<Failure, Geohash>
}