package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultObserveFollowedPlacesTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val observeFollowedPlacesTask: ObserveFollowedPlacesTask = DefaultObserveFollowedPlacesTask(
        locationRepository = locationRepository,
        favoriteRepository = favoriteRepository,
        chosenPlaceLookup = ChosenPlaceLookup(
            locationRepository = locationRepository,
            nominatimRepository = nominatimRepository
        )
    )

    private fun manyFavorites(count: Int): List<Favorite> =
        (1..count).map { number: Int ->
            umea.copy(
                favoriteId = "favorite-$number",
                locality = "Plats $number",
                latitude = umea.latitude + number
            )
        }

    private suspend fun shownNames(): List<String?> =
        when (val followedPlaces: Either<Failure, List<FollowedPlace>> = observeFollowedPlacesTask().first()) {
            is Either.Left -> error("Expected followed places but got ${followedPlaces.a}")
            is Either.Right -> followedPlaces.b.map { followedPlace: FollowedPlace -> followedPlace.name }
        }

    @Test
    fun `only the chosen place is followed while nothing is saved`() =
        runTest {
            favoriteRepository.favorites.value = emptyList()

            assertEquals(expected = listOf("Göteborg"), actual = shownNames())
        }

    @Test
    fun `the chosen place comes first and every saved place follows in its order`() =
        runTest {
            assertEquals(expected = listOf("Göteborg", "Umeå", "Visby", "Stugan"), actual = shownNames())
        }

    @Test
    fun `a chosen favorite is there once as the chosen place`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, visby)
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = umea.toStoredLocation()
            )

            val followedPlaces: List<FollowedPlace> = (observeFollowedPlacesTask().first() as Either.Right).b

            assertEquals(expected = listOf("Umeå", "Visby"), actual = followedPlaces.map { followedPlace: FollowedPlace -> followedPlace.name })
            assertEquals(expected = listOf(true, false), actual = followedPlaces.map { followedPlace: FollowedPlace -> followedPlace.isChosen })
        }

    @Test
    fun `a removed favorite is no longer followed`() =
        runTest {
            favoriteRepository.removeFavorite(favoriteId = cabin.favoriteId)

            assertEquals(expected = listOf("Göteborg", "Umeå", "Visby"), actual = shownNames())
        }

    @Test
    fun `a chosen place that is no saved place takes the room of one`() =
        runTest {
            favoriteRepository.favorites.value = manyFavorites(count = 5)
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = cabin.toStoredLocation()
            )

            assertEquals(expected = listOf("Stugan", "Plats 1", "Plats 2", "Plats 3", "Plats 4"), actual = shownNames())
        }

    @Test
    fun `a chosen favorite is followed with the four saved before the others`() =
        runTest {
            val favorites: List<Favorite> = manyFavorites(count = 6)
            favoriteRepository.favorites.value = favorites
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = favorites.last().toStoredLocation()
            )

            assertEquals(expected = listOf("Plats 6", "Plats 1", "Plats 2", "Plats 3", "Plats 4"), actual = shownNames())
        }

    @Test
    fun `saved places that cannot be read are the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals<Either<Failure, List<FollowedPlace>>>(expected = failure.asLeft(), actual = observeFollowedPlacesTask().first())
        }

    @Test
    fun `a chosen place that cannot be found leaves the saved places`() =
        runTest {
            favoriteRepository.favorites.value = listOf(visby)
            locationRepository.deviceLocation = Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE).asLeft()

            assertEquals(expected = listOf("Visby"), actual = shownNames())
        }

    @Test
    fun `at most five places are followed`() =
        runTest {
            favoriteRepository.favorites.value = manyFavorites(count = 6)

            assertEquals(expected = listOf("Göteborg", "Plats 1", "Plats 2", "Plats 3", "Plats 4"), actual = shownNames())
        }

    @Test
    fun `a preference that cannot be read is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            locationRepository.failure = failure

            assertEquals<Either<Failure, List<FollowedPlace>>>(expected = failure.asLeft(), actual = observeFollowedPlacesTask().first())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `saving another place gives the places again`() =
        runTest {
            favoriteRepository.favorites.value = emptyList()
            val emissions: MutableList<Either<Failure, List<FollowedPlace>>> = mutableListOf()
            val collection = launch { observeFollowedPlacesTask().toList(destination = emissions) }
            advanceUntilIdle()

            favoriteRepository.favorites.value = listOf(visby)
            advanceUntilIdle()
            collection.cancel()

            assertEquals(
                expected = listOf(listOf("Göteborg"), listOf("Göteborg", "Visby")),
                actual = emissions.map { followedPlaces: Either<Failure, List<FollowedPlace>> ->
                    (followedPlaces as Either.Right).b.map { followedPlace: FollowedPlace -> followedPlace.name }
                }
            )
        }
}