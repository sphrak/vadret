package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal interface GetPushAlertGeohashesTask {
    suspend operator fun invoke(
        currentGeohash: Geohash,
        pushAlertChoices: PushAlertChoices
    ): Either<Failure, List<Geohash>>
}