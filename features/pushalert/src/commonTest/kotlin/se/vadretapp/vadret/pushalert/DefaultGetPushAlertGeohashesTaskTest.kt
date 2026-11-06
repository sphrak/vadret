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
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
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

private val WARNINGS_FROM_YELLOW: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN.copy(warningLowestLevel = WarningAlertLevel.YELLOW)

class DefaultGetPushAlertGeohashesTaskTest {

    private suspend fun pushAlertGeohashes(
        locationRepository: FakeLocationRepository,
        favorites: List<Favorite>,
        pushAlertChoices: PushAlertChoices,
        currentGeohash: Geohash = whereThePhoneIs
    ): Either<Failure, List<Geohash>> =
        DefaultGetPushAlertGeohashesTask(
            favoriteRepository = FakeFavoriteRepository(favorites = favorites),
            locationRepository = locationRepository
        )(
            currentGeohash = currentGeohash,
            pushAlertChoices = pushAlertChoices
        )

    private fun List<Favorite>.toGeohashes(): List<Geohash> = mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }

    private fun List<Favorite>.favoriteIds(): Set<String> = map { favorite: Favorite -> favorite.favoriteId }.toSet()

    @Test
    fun `where the phone is comes first and the marked saved places follow up to five places`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (listOf(whereThePhoneIs) + swedishFavorites.take(n = 4).toGeohashes()).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(notifiedFavoriteIds = swedishFavorites.favoriteIds())
                )
            )
        }

    @Test
    fun `saved places that are not marked are left out`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (listOf(whereThePhoneIs) + listOf(UPPSALA_FAVORITE).toGeohashes()).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(notifiedFavoriteIds = setOf(UPPSALA_FAVORITE.favoriteId))
                )
            )
        }

    @Test
    fun `the chosen place is left out when it is not marked`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = listOf(SOLNA_FAVORITE).toGeohashes().asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(
                        isChosenPlaceNotified = false,
                        notifiedFavoriteIds = setOf(SOLNA_FAVORITE.favoriteId)
                    )
                )
            )
        }

    @Test
    fun `a chosen saved place counts once even when it was saved last`() =
        runTest {
            val chosenGeohash: List<Geohash> = listOf(VISBY_FAVORITE).toGeohashes()

            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (chosenGeohash + swedishFavorites.take(n = 4).toGeohashes()).asRight(),
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
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(notifiedFavoriteIds = swedishFavorites.favoriteIds()),
                    currentGeohash = chosenGeohash.single()
                )
            )
        }

    @Test
    fun `marked saved places outside Sweden get no push`() =
        runTest {
            val favorites: List<Favorite> = listOf(BERLIN_FAVORITE, SOLNA_FAVORITE)

            assertEquals<Either<Failure, List<Geohash>>>(
                expected = (listOf(whereThePhoneIs) + listOf(SOLNA_FAVORITE).toGeohashes()).asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = favorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(notifiedFavoriteIds = favorites.favoriteIds())
                )
            )
        }

    @Test
    fun `no marked place sends no place`() =
        runTest {
            assertEquals<Either<Failure, List<Geohash>>>(
                expected = emptyList<Geohash>().asRight(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = swedishFavorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW.copy(isChosenPlaceNotified = false)
                )
            )
        }

    @Test
    fun `a location preference that cannot be read is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            assertEquals<Either<Failure, List<Geohash>>>(
                expected = failure.asLeft(),
                actual = pushAlertGeohashes(
                    locationRepository = FakeLocationRepository(failure = failure),
                    favorites = swedishFavorites,
                    pushAlertChoices = WARNINGS_FROM_YELLOW
                )
            )
        }
}