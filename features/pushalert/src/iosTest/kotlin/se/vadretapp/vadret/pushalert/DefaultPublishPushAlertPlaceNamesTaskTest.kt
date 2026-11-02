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
    fun onlyTheChosenSavedPlacesAreNamedByTheirCell() = runTest {
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()

        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = listOf(mapOf(MALMO_FAVORITE.cell() to "Malmö")),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun choosingOrForgettingASavedPlaceSavesTheNamesAgain() = runTest {
        val locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository()
        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            locationRepository = locationRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        locationRepository.followedFavoriteIds.value = following(MALMO_FAVORITE, GOTEBORG_FAVORITE)
        runCurrent()
        locationRepository.followedFavoriteIds.value = emptySet()
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
            locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE)),
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
            locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE, MALMO_CENTRAL_FAVORITE)),
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
            locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE)),
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        assertEquals(
            expected = emptyList(),
            actual = pushAlertPlaceNameRepository.savedPlaceNames
        )
    }

    @Test
    fun aFailedSaveIsTriedAgainOnTheNextChange() = runTest {
        val locationRepository = FakeLocationRepository(followedFavoriteIds = following(MALMO_FAVORITE))
        val pushAlertPlaceNameRepository = FakePushAlertPlaceNameRepository(failure = Failure(errorCode = ErrorCode.STORAGE_FAILED))
        publish(
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE, GOTEBORG_FAVORITE)),
            locationRepository = locationRepository,
            pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
        )

        locationRepository.followedFavoriteIds.value = following(GOTEBORG_FAVORITE)
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
    locationRepository: FakeLocationRepository,
    pushAlertPlaceNameRepository: FakePushAlertPlaceNameRepository
) {
    val publishPushAlertPlaceNamesTask = DefaultPublishPushAlertPlaceNamesTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository,
        pushAlertPlaceNameRepository = pushAlertPlaceNameRepository
    )
    backgroundScope.launch { publishPushAlertPlaceNamesTask() }
    runCurrent()
}

private fun following(vararg favorites: Favorite): Set<String> = favorites.map { favorite: Favorite -> favorite.favoriteId }.toSet()

private fun Favorite.cell(): String = checkNotNull(value = toGeohashOrNull()).toCell()