package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

// Finds the chosen place in the location fake the way the app does, so push tests steer it through that fake.
internal class FakeGetChosenPlaceTask(
    private val locationRepository: FakeLocationRepository
) : GetChosenPlaceTask {

    override suspend fun invoke(): Either<Failure, FollowedPlace> =
        locationRepository.isAutomatic().flatMap { isAutomatic: Boolean ->
            if (isAutomatic) {
                locationRepository.getDeviceLocation().flatMap { location: Location ->
                    chosenPlace(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ).asRight()
                }
            } else {
                locationRepository.observeLocationPreference().first().map { preference: LocationPreference -> preference.storedLocation }.flatMap { storedLocation: StoredLocation ->
                    chosenPlace(
                        latitude = storedLocation.latitude,
                        longitude = storedLocation.longitude
                    ).asRight()
                }
            }
        }

    private fun chosenPlace(
        latitude: Double,
        longitude: Double
    ): FollowedPlace =
        FollowedPlace(
            favoriteId = null,
            name = null,
            municipality = null,
            county = null,
            latitude = latitude,
            longitude = longitude,
            isChosen = true
        )
}