package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.usecase.DefaultAddPlaceNameTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAddPlaceNameTaskTest {
    @Test
    fun aWindOrTemperatureAlertAboutAChosenSavedPlaceElsewhereIsNamedAfterIt() = runTest {
        assertEquals(
            expected = listOf(
                WIND_IN_MALMO.copy(title = "Storm i dag - Malmö"),
                TEMPERATURE_IN_MALMO.copy(title = "Varmare - Malmö")
            ),
            actual = listOf(
                WIND_IN_MALMO,
                TEMPERATURE_IN_MALMO
            ).map { pushAlertMessage: PushAlertMessage -> addPlaceName(pushAlertMessage = pushAlertMessage) }
        )
    }

    @Test
    fun warningsAndPollenAlreadyNameTheirAreaAndAreLeftAsTheyAre() = runTest {
        val warning = WIND_IN_MALMO.copy(
            title = "Gul varning: Skåne län",
            kind = PushAlertKind.WARNING
        )
        val pollen = WIND_IN_MALMO.copy(
            title = "Björkpollen i Malmö",
            kind = PushAlertKind.POLLEN
        )

        assertEquals(
            expected = listOf(
                warning,
                pollen
            ),
            actual = listOf(
                warning,
                pollen
            ).map { pushAlertMessage: PushAlertMessage -> addPlaceName(pushAlertMessage = pushAlertMessage) }
        )
    }

    @Test
    fun anAlertWithoutACellOrWithoutAnyChosenSavedPlaceIsLeftAsItIs() = runTest {
        val withoutCell = WIND_IN_MALMO.copy(cell = null)

        assertEquals(
            expected = listOf(
                withoutCell,
                WIND_IN_MALMO
            ),
            actual = listOf(
                addPlaceName(pushAlertMessage = withoutCell),
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    favoriteIds = emptySet()
                )
            )
        )
    }

    @Test
    fun aCellThatIsNoChosenSavedPlaceIsLeftAsItIs() = runTest {
        assertEquals(
            expected = listOf(
                WIND_IN_MALMO,
                WIND_IN_MALMO.copy(cell = "u628")
            ),
            actual = listOf(
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    favoriteIds = setOf(GOTEBORG_FAVORITE.favoriteId),
                    favoriteRepository = FakeFavoriteRepository(
                        favorites = listOf(
                            MALMO_FAVORITE,
                            GOTEBORG_FAVORITE
                        )
                    )
                ),
                addPlaceName(pushAlertMessage = WIND_IN_MALMO.copy(cell = "u628"))
            )
        )
    }

    @Test
    fun aChosenSavedPlaceThatWasRemovedNamesNothing() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO,
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
                favoriteRepository = FakeFavoriteRepository(favorites = listOf(GOTEBORG_FAVORITE))
            )
        )
    }

    @Test
    fun aCellWhereThePhoneIsNowIsLeftAsItIsEvenWhenASavedPlaceIsInIt() = runTest {
        val windInStockholm = WIND_IN_MALMO.copy(cell = "u6sc")

        assertEquals(
            expected = listOf(
                windInStockholm,
                WIND_IN_MALMO
            ),
            actual = listOf(
                addPlaceName(
                    pushAlertMessage = windInStockholm,
                    favoriteIds = setOf(SOLNA_FAVORITE.favoriteId),
                    favoriteRepository = FakeFavoriteRepository(favorites = listOf(SOLNA_FAVORITE))
                ),
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    locationRepository = FakeLocationRepository(isAutomatic = false)
                )
            )
        )
    }

    @Test
    fun whenWhereThePhoneIsCannotBeFoundTheChosenSavedPlaceIsStillNamed() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO.copy(title = "Storm i dag - Malmö"),
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
                locationRepository = FakeLocationRepository(deviceLocation = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft())
            )
        )
    }

    @Test
    fun twoChosenSavedPlacesInOneCellAreNamedAfterTheFirstSaved() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO.copy(title = "Storm i dag - Limhamn"),
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
                favoriteIds = setOf(
                    MALMO_FAVORITE.favoriteId,
                    LIMHAMN_FAVORITE.favoriteId
                ),
                favoriteRepository = FakeFavoriteRepository(
                    favorites = listOf(
                        LIMHAMN_FAVORITE,
                        MALMO_FAVORITE
                    )
                )
            )
        )
    }

    @Test
    fun savedPlacesThatCannotBeReadLeaveTheAlertAsItIs() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO,
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
                favoriteRepository = FakeFavoriteRepository(
                    favorites = listOf(MALMO_FAVORITE),
                    failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                )
            )
        )
    }
}

private suspend fun addPlaceName(
    pushAlertMessage: PushAlertMessage,
    favoriteIds: Set<String> = setOf(MALMO_FAVORITE.favoriteId),
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE)),
    locationRepository: FakeLocationRepository = FakeLocationRepository()
): PushAlertMessage =
    DefaultAddPlaceNameTask(
        favoriteRepository = favoriteRepository,
        getCurrentGeohashTask = DefaultGetCurrentGeohashTask(locationRepository = locationRepository)
    )(
        pushAlertMessage = pushAlertMessage,
        favoriteIds = favoriteIds
    )

private val WIND_IN_MALMO = PushAlertMessage(
    title = "Storm i dag",
    body = "Upp till 25 m/s i medelvind.",
    kind = PushAlertKind.WIND,
    notificationId = "n-1",
    cell = "u3ch"
)
private val TEMPERATURE_IN_MALMO = WIND_IN_MALMO.copy(
    title = "Varmare",
    kind = PushAlertKind.TEMPERATURE
)