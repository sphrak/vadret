package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pushalert.extension.toCell
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.usecase.DefaultPublishPushAlertPlaceNamesTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPublishPushAlertPlaceNamesTaskTest {
    @Test
    fun onlyTheChosenSavedPlacesAreNamedByTheirCell() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = listOf(mapOf(MALMO_FAVORITE.cell() to "Malmö")),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun choosingOrForgettingASavedPlaceSavesTheNamesAgain() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()
        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            pushAlertRepository = pushAlertRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        pushAlertRepository.savedChoices.value = choosing(MALMO_FAVORITE, GOTEBORG_FAVORITE)
        runCurrent()
        pushAlertRepository.savedChoices.value = PushAlertChoices.NOTHING_CHOSEN
        runCurrent()

        assertEquals(
            expected = listOf(
                mapOf(MALMO_FAVORITE.cell() to "Malmö"),
                mapOf(
                    MALMO_FAVORITE.cell() to "Malmö",
                    GOTEBORG_FAVORITE.cell() to "Göteborg"
                ),
                emptyMap()
            ),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun aChangeThatNamesNoOtherPlaceIsNotSavedAgain() = runTest {
        val favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()
        publish(
            favoriteRepository = favoriteRepository,
            pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        favoriteRepository.favorites.value = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)
        runCurrent()

        assertEquals(
            expected = listOf(mapOf(MALMO_FAVORITE.cell() to "Malmö")),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun twoChosenSavedPlacesInOneCellAreNamedAfterTheFirst() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, MALMO_CENTRAL_FAVORITE)),
            pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE, MALMO_CENTRAL_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = MALMO_FAVORITE.cell(),
            actual = MALMO_CENTRAL_FAVORITE.cell()
        )
        assertEquals(
            expected = listOf(mapOf(MALMO_FAVORITE.cell() to "Malmö")),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun nothingIsSavedWhileTheSavedPlacesOrTheChoicesCannotBeRead() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(
                favorites = listOf(MALMO_FAVORITE),
                failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            ),
            pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = emptyList(),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun aFailedSaveIsTriedAgainOnTheNextChange() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = choosing(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository(failure = Failure(errorCode = ErrorCode.STORAGE_FAILED))
        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            pushAlertRepository = pushAlertRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        pushAlertRepository.savedChoices.value = choosing(GOTEBORG_FAVORITE)
        runCurrent()

        assertEquals(
            expected = listOf(
                mapOf(MALMO_FAVORITE.cell() to "Malmö"),
                mapOf(GOTEBORG_FAVORITE.cell() to "Göteborg")
            ),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }
}

private fun TestScope.publish(
    favoriteRepository: FakeFavoriteRepository,
    pushAlertRepository: FakePushAlertRepository,
    pushAlertPlaceNameRepository: FakePushAlertPlaceNameRepository
) {
    val publishPushAlertPlaceNamesTask = DefaultPublishPushAlertPlaceNamesTask(
        favoriteRepository = favoriteRepository,
        pushAlertRepository = pushAlertRepository,
        pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
    )
    backgroundScope.launch { publishPushAlertPlaceNamesTask() }
    runCurrent()
}

private fun choosing(vararg favorites: Favorite): PushAlertChoices =
    PushAlertChoices.NOTHING_CHOSEN.copy(favoriteIds = favorites.map { favorite: Favorite -> favorite.favoriteId }.toSet())

private fun Favorite.cell(): String = checkNotNull(value = toGeohashOrNull()).toCell()