package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

internal class FakeLocationRepository(
    storedLocation: StoredLocation,
    var deviceLocation: Either<Failure, Location> = Location(
        latitude = 62.546,
        longitude = 12.542
    ).asRight()
) : LocationRepository {

    val locationPreference: MutableStateFlow<Either<Failure, LocationPreference>> = MutableStateFlow(
        value = LocationPreference(
            isAutomatic = false,
            storedLocation = storedLocation
        ).asRight()
    )

    override suspend fun getDeviceLocation(): Either<Failure, Location> = deviceLocation

    override suspend fun isAutomatic(): Either<Failure, Boolean> = error("The warnings follow the observed preference")

    override suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit> = error("The warnings never change the location mode")

    override suspend fun getStoredLocation(): Either<Failure, StoredLocation> = error("The warnings follow the observed preference")

    override fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>> = locationPreference

    override suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit> = error("The warnings never choose a place")
}