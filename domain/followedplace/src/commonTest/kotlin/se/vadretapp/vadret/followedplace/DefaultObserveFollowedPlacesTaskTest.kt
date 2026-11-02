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

    private fun follow(vararg favoriteIds: String) {
        locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(followedFavoriteIds = favoriteIds.toSet())
    }

    private suspend fun shownNames(): List<String?> =
        when (val followedPlaces: Either<Failure, List<FollowedPlace>> = observeFollowedPlacesTask().first()) {
            is Either.Left -> error("Expected followed places but got ${followedPlaces.a}")
            is Either.Right -> followedPlaces.b.map { followedPlace: FollowedPlace -> followedPlace.name }
        }

    @Test
    fun `only the chosen place is followed until a favorite is`() =
        runTest {
            assertEquals(expected = listOf("Göteborg"), actual = shownNames())
        }

    @Test
    fun `the chosen place comes first and the followed favorites keep their order`() =
        runTest {
            follow("favorite-cabin", "favorite-umea")

            assertEquals(expected = listOf("Göteborg", "Umeå", "Stugan"), actual = shownNames())
        }

    @Test
    fun `a chosen favorite that is also followed is there once as the chosen place`() =
        runTest {
            follow("favorite-umea", "favorite-visby")
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = umea.toStoredLocation()
            )

            val followedPlaces: List<FollowedPlace> = (observeFollowedPlacesTask().first() as Either.Right).b

            assertEquals(expected = listOf("Umeå", "Visby"), actual = followedPlaces.map { followedPlace: FollowedPlace -> followedPlace.name })
            assertEquals(expected = listOf(true, false), actual = followedPlaces.map { followedPlace: FollowedPlace -> followedPlace.isChosen })
        }

    @Test
    fun `a followed favorite that was removed is no longer followed`() =
        runTest {
            follow("favorite-visby", "favorite-gone")
            favoriteRepository.favorites.value = listOf(umea, visby)

            assertEquals(expected = listOf("Göteborg", "Visby"), actual = shownNames())
        }

    @Test
    fun `a chosen place that cannot be found leaves the followed favorites`() =
        runTest {
            follow("favorite-visby")
            locationRepository.deviceLocation = Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE).asLeft()

            assertEquals(expected = listOf("Visby"), actual = shownNames())
        }

    @Test
    fun `at most five places are followed`() =
        runTest {
            val manyFavorites: List<Favorite> = (1..6).map { number: Int ->
                umea.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number",
                    latitude = umea.latitude + number
                )
            }
            favoriteRepository.favorites.value = manyFavorites
            follow(*manyFavorites.map { favorite: Favorite -> favorite.favoriteId }.toTypedArray())

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
    fun `following another favorite gives the places again`() =
        runTest {
            val emissions: MutableList<Either<Failure, List<FollowedPlace>>> = mutableListOf()
            val collection = launch { observeFollowedPlacesTask().toList(destination = emissions) }
            advanceUntilIdle()

            follow("favorite-visby")
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