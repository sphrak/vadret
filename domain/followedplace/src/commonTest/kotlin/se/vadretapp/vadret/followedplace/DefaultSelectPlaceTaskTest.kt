package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import kotlin.test.Test
import kotlin.test.assertEquals

private val kirunaStoredLocation = StoredLocation(
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbotten",
    latitude = 67.8558,
    longitude = 20.2253
)

class DefaultSelectPlaceTaskTest {

    private val locationRepository = FakeLocationRepository(isAutomatic = true)
    private val favoriteRepository = FakeFavoriteRepository(initialFavorites = emptyList())
    private val selectPlaceTask: SelectPlaceTask = DefaultSelectPlaceTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )

    @Test
    fun `the place becomes a favorite and the selected location`() =
        runTest {
            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = selectPlaceTask(place = kiruna)
            )
            assertEquals(expected = listOf(kirunaFavorite), actual = favoriteRepository.favorites.value)
            assertEquals(expected = listOf("selected=Kiruna"), actual = locationRepository.writes)
            assertEquals(
                expected = LocationPreference(
                    isAutomatic = false,
                    storedLocation = kirunaStoredLocation
                ),
                actual = locationRepository.locationPreference.value
            )
        }

    @Test
    fun `picking a favorite place again keeps one favorite`() =
        runTest {
            selectPlaceTask(place = kiruna)
            selectPlaceTask(place = kiruna)

            assertEquals(expected = listOf(kirunaFavorite), actual = favoriteRepository.favorites.value)
        }

    @Test
    fun `a new place becomes the chosen one so a fifth fits next to the position`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, visby, cabin, umea.copy(favoriteId = "favorite-north", locality = "Norr"))

            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = selectPlaceTask(place = kiruna)
            )
            assertEquals(expected = listOf(5), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `a sixth place is refused and nothing is selected`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, visby, cabin, kirunaFavorite.copy(locality = "Abisko"), umea.copy(favoriteId = "favorite-north", locality = "Norr"))

            assertEquals<Either<Failure, Unit>>(
                expected = Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft(),
                actual = selectPlaceTask(place = kiruna)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
        }

    @Test
    fun `a place that cannot be saved is not selected`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = selectPlaceTask(place = kiruna)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
            assertEquals<Either<Failure, Boolean>>(expected = true.asRight(), actual = locationRepository.isAutomatic())
        }

    @Test
    fun `a place that cannot be selected keeps the mode`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            locationRepository.selectStoredLocationResult = failure.asLeft()

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = selectPlaceTask(place = kiruna)
            )
            assertEquals<Either<Failure, Boolean>>(expected = true.asRight(), actual = locationRepository.isAutomatic())
        }
}