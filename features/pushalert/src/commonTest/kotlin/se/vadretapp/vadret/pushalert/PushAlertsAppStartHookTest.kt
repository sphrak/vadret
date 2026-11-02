package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.usecase.DefaultAdoptPushAlertFavoritesTask
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertsAppStartHookTest {
    @Test
    fun aSyncThatFailsAtStartIsLoggedAsAWarning() = runTest {
        withRecordedLogs { recordingLogSink ->
            pushAlertsAppStartHook(syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft())).startInBackground(scope = this@runTest)

            assertEquals(
                expected = LogLevel.WARN to "Push alerts could not be brought up to date: NETWORK_UNAVAILABLE",
                actual = recordingLogSink.lines.map { line -> line.level to line.message }.last()
            )
        }
    }

    @Test
    fun aSyncThatWorksAtStartIsOnlyTraced() = runTest {
        withRecordedLogs { recordingLogSink ->
            pushAlertsAppStartHook(syncPushAlertsTask = FakeSyncPushAlertsTask(result = Unit.asRight())).startInBackground(scope = this@runTest)

            assertEquals(
                expected = listOf(LogLevel.DEBUG, LogLevel.DEBUG),
                actual = recordingLogSink.lines.map { line -> line.level }
            )
        }
    }

    @Test
    fun theFavoritesChosenForPushAlertsAreFollowedBeforeTheFirstSync() = runTest {
        val locationRepository = FakeLocationRepository()

        pushAlertsAppStartHook(
            pushAlertRepository = FakePushAlertRepository(choices = PushAlertChoices.NOTHING_CHOSEN.copy(favoriteIds = setOf("favorite-1"))),
            locationRepository = locationRepository
        ).startInBackground(scope = this)

        assertEquals(expected = listOf(setOf("favorite-1")), actual = locationRepository.adoptedFavoriteIds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun followingAnotherPlaceAnywhereInTheAppSyncsAgain() = runTest {
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val observeFollowedPlacesTask = FakeObserveFollowedPlacesTask()
        val hook = launch {
            pushAlertsAppStartHook(
                syncPushAlertsTask = syncPushAlertsTask,
                observeFollowedPlacesTask = observeFollowedPlacesTask
            ).onAppStart()
        }
        advanceUntilIdle()
        val syncsAtStart = syncPushAlertsTask.syncs

        observeFollowedPlacesTask.followedPlaces.value = listOf(STOCKHOLM_PLACE, MALMO_PLACE).asRight()
        advanceUntilIdle()
        observeFollowedPlacesTask.followedPlaces.value = listOf(STOCKHOLM_PLACE, MALMO_PLACE.copy(name = "Malmö C")).asRight()
        advanceUntilIdle()
        hook.cancel()

        assertEquals(expected = 1 to 2, actual = syncsAtStart to syncPushAlertsTask.syncs)
    }
}

// The hook keeps following the followed places, so a test starts it in the background and reads what it did so far.
@OptIn(ExperimentalCoroutinesApi::class)
private fun PushAlertsAppStartHook.startInBackground(scope: TestScope) {
    scope.backgroundScope.launch { onAppStart() }
    scope.runCurrent()
}

private fun pushAlertsAppStartHook(
    syncPushAlertsTask: FakeSyncPushAlertsTask = FakeSyncPushAlertsTask(),
    pushAlertRepository: FakePushAlertRepository = FakePushAlertRepository(),
    locationRepository: FakeLocationRepository = FakeLocationRepository(),
    observeFollowedPlacesTask: FakeObserveFollowedPlacesTask = FakeObserveFollowedPlacesTask()
): PushAlertsAppStartHook =
    PushAlertsAppStartHook(
        adoptPushAlertFavoritesTask = DefaultAdoptPushAlertFavoritesTask(
            pushAlertRepository = pushAlertRepository,
            locationRepository = locationRepository
        ),
        syncPushAlertsTask = syncPushAlertsTask,
        observeFollowedPlacesTask = observeFollowedPlacesTask
    )

private val STOCKHOLM_PLACE = FollowedPlace(
    favoriteId = null,
    name = "Stockholm",
    municipality = "Stockholm",
    county = "Stockholm",
    latitude = 59.3293,
    longitude = 18.0686,
    isChosen = true
)

private val MALMO_PLACE = FollowedPlace(
    favoriteId = "favorite-malmo",
    name = "Malmö",
    municipality = "Malmö",
    county = "Skåne",
    latitude = 55.605,
    longitude = 13.0038,
    isChosen = false
)