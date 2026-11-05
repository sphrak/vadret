package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.model.StoredLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetChosenPlaceTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val getChosenPlaceTask: GetChosenPlaceTask = DefaultGetChosenPlaceTask(
        locationRepository = locationRepository,
        favoriteRepository = favoriteRepository,
        chosenPlaceLookup = ChosenPlaceLookup(
            locationRepository = locationRepository,
            nominatimRepository = nominatimRepository
        )
    )

    private fun choose(storedLocation: StoredLocation) {
        locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
            isAutomatic = false,
            storedLocation = storedLocation
        )
    }

    @Test
    fun `the device position is the chosen place and Nominatim names it`() =
        runTest {
            assertEquals<Either<Failure, FollowedPlace>>(
                expected = FollowedPlace(
                    favoriteId = null,
                    name = "Göteborg",
                    municipality = "Göteborg",
                    county = "Västra Götaland",
                    latitude = 57.7089,
                    longitude = 11.9746,
                    isChosen = true
                ).asRight(),
                actual = getChosenPlaceTask()
            )
        }

    @Test
    fun `a device position Nominatim cannot name is known by its coordinates`() =
        runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, FollowedPlace>>(
                expected = FollowedPlace(
                    favoriteId = null,
                    name = null,
                    municipality = null,
                    county = null,
                    latitude = 57.7089,
                    longitude = 11.9746,
                    isChosen = true
                ).asRight(),
                actual = getChosenPlaceTask()
            )
        }

    @Test
    fun `a device position that cannot be found is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING)
            locationRepository.deviceLocation = failure.asLeft()

            assertEquals<Either<Failure, FollowedPlace>>(expected = failure.asLeft(), actual = getChosenPlaceTask())
        }

    @Test
    fun `a chosen favorite is known by its favorite`() =
        runTest {
            choose(storedLocation = visby.toStoredLocation())

            assertEquals<Either<Failure, FollowedPlace>>(
                expected = FollowedPlace(
                    favoriteId = "favorite-visby",
                    name = "Visby",
                    municipality = "Gotland",
                    county = "Gotland",
                    latitude = 57.634,
                    longitude = 18.294,
                    isChosen = true
                ).asRight(),
                actual = getChosenPlaceTask()
            )
        }

    @Test
    fun `a chosen place named by the person has no municipality or county`() =
        runTest {
            choose(storedLocation = cabin.toStoredLocation())

            assertEquals<Either<Failure, FollowedPlace>>(
                expected = FollowedPlace(
                    favoriteId = "favorite-cabin",
                    name = "Stugan",
                    municipality = null,
                    county = null,
                    latitude = 61.8286,
                    longitude = 16.0912,
                    isChosen = true
                ).asRight(),
                actual = getChosenPlaceTask()
            )
        }

    @Test
    fun `a chosen place whose favorite is gone is still the chosen place`() =
        runTest {
            choose(storedLocation = visby.toStoredLocation())
            favoriteRepository.favorites.value = listOf(umea)

            assertEquals(expected = null, actual = getChosenPlaceTask().successOrNullPlace()?.favoriteId)
            assertEquals(expected = "Visby", actual = getChosenPlaceTask().successOrNullPlace()?.name)
        }

    private fun Either<Failure, FollowedPlace>.successOrNullPlace(): FollowedPlace? =
        when (this) {
            is Either.Left -> null
            is Either.Right -> b
        }
}