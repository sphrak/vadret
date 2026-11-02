package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAddFavoriteTaskTest {

    private val favoriteRepository = FakeFavoriteRepository()
    private val addFavoriteTask = DefaultAddFavoriteTask(favoriteRepository = favoriteRepository)

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
    fun `a favorite that cannot be stored is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals(expected = failure.asLeft(), actual = addHarbour())
        }
}

private const val HARBOUR_LATITUDE = 57.7089
private const val HARBOUR_LONGITUDE = 11.9746