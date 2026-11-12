package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PlaceName
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal interface GetPushAlertPlaceNamesTask {
    suspend operator fun invoke(
        currentGeohash: Geohash,
        pushAlertChoices: PushAlertChoices
    ): Either<Failure, Map<Geohash, PlaceName?>>
}