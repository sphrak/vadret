package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import se.vadretapp.vadret.pushalert.usecase.DefaultAddPlaceNameTask
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
    fun anAlertWithoutACellOrWithoutAnySavedPlaceIsLeftAsItIs() = runTest {
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
                    favoriteRepository = FakeFavoriteRepository()
                )
            )
        )
    }

    @Test
    fun aCellWithNoSavedPlaceIsLeftAsItIs() = runTest {
        assertEquals(
            expected = listOf(
                WIND_IN_MALMO,
                WIND_IN_MALMO.copy(cell = "u628")
            ),
            actual = listOf(
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    favoriteRepository = FakeFavoriteRepository(favorites = listOf(GOTEBORG_FAVORITE))
                ),
                addPlaceName(pushAlertMessage = WIND_IN_MALMO.copy(cell = "u628"))
            )
        )
    }

    @Test
    fun aSavedPlaceThatWasRemovedNamesNothing() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO,
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
                favoriteRepository = FakeFavoriteRepository(favorites = listOf(GOTEBORG_FAVORITE))
            )
        )
    }

    @Test
    fun aCellWhereThePhoneWasAtTheLastSyncIsLeftAsItIsEvenWhenASavedPlaceIsInIt() = runTest {
        val windInStockholm = WIND_IN_MALMO.copy(cell = "u6sc")

        assertEquals(
            expected = listOf(
                windInStockholm,
                WIND_IN_MALMO,
                WIND_IN_MALMO
            ),
            actual = listOf(
                addPlaceName(
                    pushAlertMessage = windInStockholm,
                    favoriteRepository = FakeFavoriteRepository(favorites = listOf(SOLNA_FAVORITE))
                ),
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    pushAlertRepository = FakePushAlertRepository(syncedCurrentGeohash = Geohash(value = "u3chr"))
                ),
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    pushAlertRepository = FakePushAlertRepository(syncedCurrentGeohash = Geohash(value = "u3chn"))
                )
            )
        )
    }

    @Test
    fun withoutAKeptSyncOrWithOneThatCannotBeReadTheSavedPlaceIsStillNamed() = runTest {
        assertEquals(
            expected = List(size = 2) { WIND_IN_MALMO.copy(title = "Storm i dag - Malmö") },
            actual = listOf(
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    pushAlertRepository = FakePushAlertRepository(syncedCurrentGeohash = null)
                ),
                addPlaceName(
                    pushAlertMessage = WIND_IN_MALMO,
                    pushAlertRepository = FakePushAlertRepository(
                        syncedCurrentGeohash = SYNCED_IN_STOCKHOLM,
                        syncedCurrentGeohashFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
                    )
                )
            )
        )
    }

    @Test
    fun twoSavedPlacesInOneCellAreNamedAfterTheFirstSaved() = runTest {
        assertEquals(
            expected = WIND_IN_MALMO.copy(title = "Storm i dag - Limhamn"),
            actual = addPlaceName(
                pushAlertMessage = WIND_IN_MALMO,
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
    fun aSlipperinessAlertIsNamedAfterTheSavedPlaceInItsSmallerCellOnly() = runTest {
        val slipperinessInLimhamn = WIND_IN_MALMO.copy(
            title = "Möjlig halka i natt",
            kind = PushAlertKind.SLIPPERINESS,
            pushAlertType = PushAlertType.SLIPPERINESS,
            cell = "u3chn"
        )

        assertEquals(
            expected = listOf(
                slipperinessInLimhamn.copy(title = "Möjlig halka i natt - Limhamn"),
                slipperinessInLimhamn
            ),
            actual = listOf(
                FakeFavoriteRepository(
                    favorites = listOf(
                        MALMO_FAVORITE,
                        LIMHAMN_FAVORITE
                    )
                ),
                FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
            ).map { favoriteRepository: FakeFavoriteRepository ->
                addPlaceName(
                    pushAlertMessage = slipperinessInLimhamn,
                    favoriteRepository = favoriteRepository
                )
            }
        )
    }

    @Test
    fun anAuroraAlertIsNamedAfterTheSavedPlaceInItsSmallerCellOnly() = runTest {
        val auroraInLimhamn = WIND_IN_MALMO.copy(
            title = "Chans för norrsken i kväll",
            kind = PushAlertKind.AURORA,
            pushAlertType = PushAlertType.AURORA,
            cell = "u3chn"
        )

        assertEquals(
            expected = listOf(
                auroraInLimhamn.copy(title = "Chans för norrsken i kväll - Limhamn"),
                auroraInLimhamn
            ),
            actual = listOf(
                FakeFavoriteRepository(
                    favorites = listOf(
                        MALMO_FAVORITE,
                        LIMHAMN_FAVORITE
                    )
                ),
                FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
            ).map { favoriteRepository: FakeFavoriteRepository ->
                addPlaceName(
                    pushAlertMessage = auroraInLimhamn,
                    favoriteRepository = favoriteRepository
                )
            }
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
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE)),
    pushAlertRepository: FakePushAlertRepository = FakePushAlertRepository(syncedCurrentGeohash = SYNCED_IN_STOCKHOLM)
): PushAlertMessage =
    DefaultAddPlaceNameTask(
        favoriteRepository = favoriteRepository,
        pushAlertRepository = pushAlertRepository
    )(pushAlertMessage = pushAlertMessage)

private val SYNCED_IN_STOCKHOLM = Geohash(value = "u6sce")
private val WIND_IN_MALMO = PushAlertMessage(
    title = "Storm i dag",
    body = "Upp till 25 m/s i medelvind.",
    kind = PushAlertKind.WIND,
    pushAlertType = PushAlertType.STORM,
    notificationId = "n-1",
    cell = "u3ch",
    warningReference = null
)
private val TEMPERATURE_IN_MALMO = WIND_IN_MALMO.copy(
    title = "Varmare",
    kind = PushAlertKind.TEMPERATURE
)