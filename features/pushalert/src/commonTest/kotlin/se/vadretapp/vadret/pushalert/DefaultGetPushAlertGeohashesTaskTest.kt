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
import se.vadretapp.vadret.pushalert.model.PushAlertGeohashes
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

    private suspend fun pushAlertGeohashes(
        locationRepository: FakeLocationRepository,
        favorites: List<Favorite>,
        currentGeohash: Geohash = whereThePhoneIs,
        forecastForAllPlaces: Boolean = false
    ): Either<Failure, PushAlertGeohashes> =
        DefaultGetPushAlertGeohashesTask(
            favoriteRepository = FakeFavoriteRepository(favorites = favorites),
            locationRepository = locationRepository
        )(
            currentGeohash = currentGeohash,
            forecastForAllPlaces = forecastForAllPlaces
        )

    private fun List<Favorite>.toGeohashes(): List<Geohash> = mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }

    @Test
    fun `where the phone is comes first and four saved places follow while temperature and wind follow the first saved place`() =
        runTest {
            assertEquals<Either<Failure, PushAlertGeohashes>>(
                expected = PushAlertGeohashes(
                    geohashes = listOf(whereThePhoneIs) + swedishFavorites.take(n = 4).toGeohashes(),
                    forecastGeohashes = listOf(SOLNA_FAVORITE).toGeohashes()
                ).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites
                )
            )
        }

    @Test
    fun `a chosen saved place counts once even when it was saved last and is the place for temperature and wind`() =
        runTest {
            val chosenGeohash: List<Geohash> = listOf(VISBY_FAVORITE).toGeohashes()

            assertEquals<Either<Failure, PushAlertGeohashes>>(
                expected = PushAlertGeohashes(
                    geohashes = chosenGeohash + swedishFavorites.take(n = 4).toGeohashes(),
                    forecastGeohashes = chosenGeohash
                ).asRight(),
                actual = pushAlertGeohashes(
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
    fun `saved places outside Sweden get no push and are skipped for temperature and wind`() =
        runTest {
            assertEquals<Either<Failure, PushAlertGeohashes>>(
                expected = PushAlertGeohashes(
                    geohashes = listOf(whereThePhoneIs) + listOf(SOLNA_FAVORITE).toGeohashes(),
                    forecastGeohashes = listOf(SOLNA_FAVORITE).toGeohashes()
                ).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = listOf(BERLIN_FAVORITE, SOLNA_FAVORITE)
                )
            )
        }

    @Test
    fun `without saved places in Sweden temperature and wind follow where the phone is`() =
        runTest {
            assertEquals<List<Either<Failure, PushAlertGeohashes>>>(
                expected = List(size = 2) {
                    PushAlertGeohashes(
                        geohashes = listOf(whereThePhoneIs),
                        forecastGeohashes = listOf(whereThePhoneIs)
                    ).asRight()
                },
                actual = listOf(emptyList(), listOf(BERLIN_FAVORITE)).map { favorites: List<Favorite> ->
                    pushAlertGeohashes(
                        locationRepository = FakeLocationRepository(isAutomatic = true),
                        favorites = favorites
                    )
                }
            )
        }

    @Test
    fun `temperature and wind for all places follow every place that is sent`() =
        runTest {
            val geohashes: List<Geohash> = listOf(whereThePhoneIs) + swedishFavorites.take(n = 4).toGeohashes()

            assertEquals<Either<Failure, PushAlertGeohashes>>(
                expected = PushAlertGeohashes(
                    geohashes = geohashes,
                    forecastGeohashes = geohashes
                ).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites,
                    forecastForAllPlaces = true
                )
            )
        }

    @Test
    fun `the first saved place is sent even when more places are saved than can be followed`() =
        runTest {
            val sent = pushAlertGeohashes(
                locationRepository = FakeLocationRepository(isAutomatic = true),
                favorites = swedishFavorites + swedishFavorites.reversed()
            )

            assertEquals(
                expected = listOf(SOLNA_FAVORITE).toGeohashes() to true,
                actual = when (sent) {
                    is Either.Left -> null to false
                    is Either.Right -> sent.b.forecastGeohashes to sent.b.geohashes.containsAll(elements = sent.b.forecastGeohashes)
                }
            )
        }

    @Test
    fun `a location preference that cannot be read is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            assertEquals<Either<Failure, PushAlertGeohashes>>(
                expected = failure.asLeft(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(failure = failure),
                    favorites = swedishFavorites
                )
            )
        }
}