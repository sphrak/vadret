package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.model.FollowableFavorite
import se.vadretapp.vadret.followedplace.model.FollowablePlaces
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultObserveFollowablePlacesTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val observeFollowablePlacesTask: ObserveFollowablePlacesTask = DefaultObserveFollowablePlacesTask(
        locationRepository = locationRepository,
        favoriteRepository = favoriteRepository,
        chosenPlaceLookup = ChosenPlaceLookup(
            locationRepository = locationRepository,
            nominatimRepository = FakeNominatimRepository()
        )
    )

    private suspend fun followablePlaces(): FollowablePlaces = (observeFollowablePlacesTask().first() as Either.Right).b

    @Test
    fun `every favorite can be followed and says whether it is`() =
        runTest {
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(followedFavoriteIds = setOf("favorite-visby"))

            assertEquals(
                expected = listOf(
                    FollowableFavorite(
                        favoriteId = "favorite-umea",
                        name = "Umeå",
                        isFollowed = false,
                        isChosen = false,
                        isInSweden = true
                    ),
                    FollowableFavorite(
                        favoriteId = "favorite-visby",
                        name = "Visby",
                        isFollowed = true,
                        isChosen = false,
                        isInSweden = true
                    ),
                    FollowableFavorite(
                        favoriteId = "favorite-cabin",
                        name = "Stugan",
                        isFollowed = false,
                        isChosen = false,
                        isInSweden = true
                    )
                ),
                actual = followablePlaces().favorites
            )
            assertEquals(expected = "Göteborg", actual = followablePlaces().chosenPlace?.name)
        }

    @Test
    fun `the chosen favorite is told apart and a favorite abroad says so`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, umea.copy(favoriteId = "favorite-berlin", locality = "Berlin", latitude = 52.52, longitude = 13.405))
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                isAutomatic = false,
                storedLocation = umea.toStoredLocation()
            )

            assertEquals(
                expected = listOf(true to true, false to false),
                actual = followablePlaces().favorites.map { followableFavorite: FollowableFavorite -> followableFavorite.isChosen to followableFavorite.isInSweden }
            )
        }

    @Test
    fun `a chosen place that cannot be found leaves the favorites followable`() =
        runTest {
            locationRepository.deviceLocation = Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE).asLeft()

            assertEquals(expected = null, actual = followablePlaces().chosenPlace)
            assertEquals(expected = 3, actual = followablePlaces().favorites.size)
        }
}