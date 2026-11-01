package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOf
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

internal class DefaultGetCurrentGeohashTask(
    private val locationRepository: LocationRepository
) : GetCurrentGeohashTask {
    override suspend fun invoke(): Either<Failure, Geohash> =
        locationRepository.isAutomatic().flatMap { isAutomatic: Boolean ->
            if (isAutomatic) {
                locationRepository.getDeviceLocation().flatMap { location: Location ->
                    geohashAt(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            } else {
                locationRepository.getStoredLocation().flatMap { storedLocation: StoredLocation ->
                    geohashAt(
                        latitude = storedLocation.latitude,
                        longitude = storedLocation.longitude
                    )
                }
            }
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