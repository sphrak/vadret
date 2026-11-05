package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.StoredLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAddFavoriteTaskTest {

    private val favoriteRepository = FakeFavoriteRepository(initialFavorites = emptyList())
    private val locationRepository = FakeLocationRepository(isAutomatic = false)
    private val addFavoriteTask = DefaultAddFavoriteTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )

    private suspend fun addHarbour(): Either<Failure, Favorite> =
        addFavoriteTask(
            locality = "Göteborg",
            municipality = "Göteborgs kommun",
            county = "Västra Götalands län",
            latitude = HARBOUR_LATITUDE,
            longitude = HARBOUR_LONGITUDE
        )

    @Test
    fun `a new place is saved where it lies`() =
        runTest {
            val favorite: Either<Failure, Favorite> = addHarbour()

            assertEquals(
                expected = Favorite(
                    favoriteId = "favorite-1",
                    locality = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = HARBOUR_LATITUDE,
                    longitude = HARBOUR_LONGITUDE
                ).asRight(),
                actual = favorite
            )
        }

    @Test
    fun `a place saved again on the same spot is the favorite it already was`() =
        runTest {
            val first: Either<Failure, Favorite> = addHarbour()

            val second: Either<Failure, Favorite> = addHarbour()

            assertEquals(expected = first, actual = second)
            assertEquals(expected = 1, actual = favoriteRepository.favorites.value.size)
        }

    @Test
    fun `a place carrying the name of a favorite elsewhere is not saved`() =
        runTest {
            val gothenburgCenter = Favorite(
                favoriteId = "favorite-gothenburg",
                locality = "Göteborg",
                municipality = "Göteborgs kommun",
                county = "Västra Götalands län",
                latitude = 57.7000,
                longitude = 11.9700
            )
            favoriteRepository.favorites.value = listOf(gothenburgCenter)

            val favorite: Either<Failure, Favorite> = addHarbour()

            assertEquals(expected = Failure(errorCode = ErrorCode.FAVORITE_NAME_TAKEN).asLeft(), actual = favorite)
            assertEquals(expected = listOf(gothenburgCenter), actual = favoriteRepository.favorites.value)
        }

    @Test
    fun `five saved places are as many as can be followed`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, visby, cabin, kirunaFavorite, umea.copy(favoriteId = "favorite-north", locality = "Norr"))

            assertEquals(expected = Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft(), actual = addHarbour())
            assertEquals(expected = listOf(5), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `the device position takes the room of one saved place`() =
        runTest {
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(isAutomatic = true)

            addHarbour()

            assertEquals(expected = listOf(4), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `a chosen place that is no saved place takes the room of one`() =
        runTest {
            favoriteRepository.favorites.value = listOf(visby)

            addHarbour()

            assertEquals(expected = listOf(4), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `a location mode that cannot be read saves nothing`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            locationRepository.failure = failure

            assertEquals(expected = failure.asLeft(), actual = addHarbour())
            assertEquals(expected = emptyList(), actual = favoriteRepository.favorites.value)
        }

    @Test
    fun `saving the chosen place that is no saved place fits next to four others`() =
        runTest {
            favoriteRepository.favorites.value = listOf(umea, visby, cabin, kirunaFavorite)
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(
                storedLocation = StoredLocation(
                    locality = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = HARBOUR_LATITUDE,
                    longitude = HARBOUR_LONGITUDE
                )
            )

            assertEquals(expected = "Göteborg", actual = (addHarbour() as Either.Right).b.locality)
            assertEquals(expected = listOf(5), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `saved places that cannot be read save nothing`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals(expected = failure.asLeft(), actual = addHarbour())
            assertEquals(expected = emptyList(), actual = favoriteRepository.maximumFavoritesAsked)
        }

    @Test
    fun `a favorite that cannot be stored is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.addFailure = failure

            assertEquals(expected = failure.asLeft(), actual = addHarbour())
            assertEquals(expected = listOf(4), actual = favoriteRepository.maximumFavoritesAsked)
        }
}

private const val HARBOUR_LATITUDE = 57.7089
private const val HARBOUR_LONGITUDE = 11.9746