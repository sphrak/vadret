package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

internal class FakeLocationRepository(
    private val isAutomatic: Boolean = true,
    private val deviceLocation: Either<Failure, Location> = Location(latitude = 59.3293, longitude = 18.0686).asRight()
) : LocationRepository {
    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = isAutomatic.asRight()

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> = Unit.asRight()

    override suspend fun getStoredLocation(): Either<Failure, StoredLocation> = MALMO.asRight()

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> =
        flowOf(LocationPreference(isAutomatic = isAutomatic, storedLocation = MALMO).asRight())

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> = Unit.asRight()
}

private val MALMO = StoredLocation(
    locality = "Malmö",
    municipality = "Malmö",
    county = "Skåne län",
    latitude = 55.6050,
    longitude = 13.0038
)