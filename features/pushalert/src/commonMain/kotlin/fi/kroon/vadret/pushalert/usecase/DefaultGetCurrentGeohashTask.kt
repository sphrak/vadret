package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.geohash.model.Geohash
import fi.kroon.vadret.geohash.model.geohashOf
import fi.kroon.vadret.location.LocationRepository
import fi.kroon.vadret.location.model.Location
import fi.kroon.vadret.location.model.StoredLocation
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap

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

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")