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
import se.vadretapp.vadret.pushalert.usecase.DefaultPublishPushAlertPlaceNamesTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPublishPushAlertPlaceNamesTaskTest {
    @Test
    fun everySavedPlaceIsNamedByItsCell() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = listOf(
                mapOf(
                    MALMO_FAVORITE.cell() to "Malmö",
                    GOTEBORG_FAVORITE.cell() to "Göteborg"
                )
            ),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun savingOrRemovingAPlaceSavesTheNamesAgain() = runTest {
        val favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()
        publish(
            favoriteRepository = favoriteRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        favoriteRepository.favorites.value = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)
        runCurrent()
        favoriteRepository.favorites.value = emptyList()
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
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        favoriteRepository.favorites.value = listOf(MALMO_FAVORITE, MALMO_CENTRAL_FAVORITE)
        runCurrent()

        assertEquals(
            expected = listOf(mapOf(MALMO_FAVORITE.cell() to "Malmö")),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun twoSavedPlacesInOneCellAreNamedAfterTheFirst() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, MALMO_CENTRAL_FAVORITE)),
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
    fun nothingIsSavedWhileTheSavedPlacesCannotBeRead() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(
                favorites = listOf(MALMO_FAVORITE),
                failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            ),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = emptyList(),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun aFailedSaveIsTriedAgainOnTheNextChange() = runTest {
        val favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository(failure = Failure(errorCode = ErrorCode.STORAGE_FAILED))
        publish(
            favoriteRepository = favoriteRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        favoriteRepository.favorites.value = listOf(GOTEBORG_FAVORITE)
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
    pushAlertPlaceNameRepository: FakePushAlertPlaceNameRepository
) {
    val publishPushAlertPlaceNamesTask = DefaultPublishPushAlertPlaceNamesTask(
        favoriteRepository = favoriteRepository,
        pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
    )
    backgroundScope.launch { publishPushAlertPlaceNamesTask() }
    runCurrent()
}

private fun Favorite.cell(): String = checkNotNull(value = toGeohashOrNull()).toCell()