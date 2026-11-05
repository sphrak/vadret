package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.usecase.DefaultGetPushAlertGeohashesTask
import kotlin.test.Test
import kotlin.test.assertEquals

private val whereThePhoneIs: Geohash = Geohash(value = "u6sce")

private val swedishFavorites: List<Favorite> = listOf(
    SOLNA_FAVORITE,
    GOTEBORG_FAVORITE,
    UPPSALA_FAVORITE,
    UMEA_FAVORITE,
    KIRUNA_FAVORITE,
    VISBY_FAVORITE
)

class DefaultGetPushAlertGeohashesTaskTest {

    private suspend fun geohashes(
        locationRepository: FakeLocationRepository,
        favorites: List<Favorite>,
        currentGeohash: Geohash = whereThePhoneIs
    ): Either<Failure, List<Geohash>> =
        DefaultGetPushAlertGeohashesTask(
            favoriteRepository = FakeFavoriteRepository(favorites = favorites),
            locationRepository = locationRepository
        )(currentGeohash = currentGeohash)

    private fun List<Favorite>.toGeohashes(): List<Geohash> = mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }

    @Test
    fun `where the phone is comes first and four saved places follow`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (listOf(whereThePhoneIs) + swedishFavorites.take(n = 4).toGeohashes()).asRight(),
                actual = geohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites
                )
            )
        }

    @Test
    fun `a chosen saved place counts once even when it was saved last`() =
        runTest {
            val chosenGeohash: List<Geohash> = listOf(VISBY_FAVORITE).toGeohashes()

            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (chosenGeohash + swedishFavorites.take(n = 4).toGeohashes()).asRight(),
                actual = geohashes(
                    locationRepository = FakeLocationRepository(
                        isAutomatic = false,
                        storedLocation = StoredLocation(
                            locality = VISBY_FAVORITE.locality,
                            municipality = VISBY_FAVORITE.municipality,
                            county = VISBY_FAVORITE.county,
                            latitude = VISBY_FAVORITE.latitude,
                            longitude = VISBY_FAVORITE.longitude
                        )
                    ),
                    favorites = swedishFavorites,
                    currentGeohash = chosenGeohash.single()
                )
            )
        }

    @Test
    fun `saved places outside Sweden get no push`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (listOf(whereThePhoneIs) + listOf(SOLNA_FAVORITE).toGeohashes()).asRight(),
                actual = geohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = listOf(BERLIN_FAVORITE, SOLNA_FAVORITE)
                )
            )
        }

    @Test
    fun `a location preference that cannot be read is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            assertEquals<Either<Failure, List<Geohash>>>(
                expected = failure.asLeft(),
                actual = geohashes(
                    locationRepository = FakeLocationRepository(failure = failure),
                    favorites = swedishFavorites
                )
            )
        }
}