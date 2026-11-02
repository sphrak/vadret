package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

internal class DefaultGetCurrentGeohashTask(
    private val getChosenPlaceTask: GetChosenPlaceTask
) : GetCurrentGeohashTask {
    override suspend fun invoke(): Either<Failure, Geohash> =
        getChosenPlaceTask().flatMap { chosenPlace: FollowedPlace ->
            geohashAt(
                latitude = chosenPlace.latitude,
                longitude = chosenPlace.longitude
            )
        }

    private fun geohashAt(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Geohash> {
        val geohash = geohashOf(
            latitude = latitude,
            longitude = longitude
        )
        if (geohash == null) {
            logger.warn { "${ErrorCode.LOCATION_NOT_AVAILABLE}: the position is not a place on Earth" }
            return Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE).asLeft()
        }
        return geohash.asRight()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")