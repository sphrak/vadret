package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash

internal interface GetPushAlertGeohashesTask {
    suspend operator fun invoke(currentGeohash: Geohash): Either<Failure, List<Geohash>>
}