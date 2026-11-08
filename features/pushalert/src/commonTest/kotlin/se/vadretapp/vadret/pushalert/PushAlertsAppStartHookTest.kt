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
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePollenAllergensTask
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language
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
                expected = listOf(LogLevel.DEBUG),
                actual = recordingLogSink.lines.map { line -> line.level }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun savingAnotherPlaceAnywhereInTheAppSyncsAgain() = runTest {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pickingAnAllergenOrTurningPollenOffAnywhereInTheAppSyncsAgainButHidingANoticeDoesNot() = runTest {
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val pollenPreferenceRepository = FakePollenPreferenceRepository()
        val hook = launch {
            pushAlertsAppStartHook(
                syncPushAlertsTask = syncPushAlertsTask,
                pollenPreferenceRepository = pollenPreferenceRepository
            ).onAppStart()
        }
        advanceUntilIdle()
        val syncsAtStart = syncPushAlertsTask.syncs

        pollenPreferenceRepository.pollenPreferences.value = NO_ALLERGENS.copy(allergenPollenIds = setOf(BIRCH_ID)).asRight()
        advanceUntilIdle()
        pollenPreferenceRepository.pollenPreferences.value = NO_ALLERGENS.copy(
            allergenPollenIds = setOf(BIRCH_ID),
            isHibernationNoticeShown = false
        ).asRight()
        advanceUntilIdle()
        pollenPreferenceRepository.pollenPreferences.value = NO_ALLERGENS.copy(
            isPollenEnabled = false,
            allergenPollenIds = setOf(BIRCH_ID)
        ).asRight()
        advanceUntilIdle()
        hook.cancel()

        assertEquals(expected = 1 to 3, actual = syncsAtStart to syncPushAlertsTask.syncs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anotherLanguageOrClockSyncsAgainButTheSameOneAgainDoesNot() = runTest {
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val observeShownLanguageTask = FakeObserveShownLanguageTask()
        val observeHourFormatTask = FakeObserveHourFormatTask()
        val hook = launch {
            pushAlertsAppStartHook(
                syncPushAlertsTask = syncPushAlertsTask,
                observeShownLanguageTask = observeShownLanguageTask,
                observeHourFormatTask = observeHourFormatTask
            ).onAppStart()
        }
        advanceUntilIdle()
        val syncsAtStart = syncPushAlertsTask.syncs

        observeShownLanguageTask.languages.emit(value = Language.POLISH.asRight())
        advanceUntilIdle()
        observeShownLanguageTask.languages.emit(value = Language.POLISH.asRight())
        advanceUntilIdle()
        observeHourFormatTask.hourFormats.emit(value = HourFormat.TWELVE_HOUR.asRight())
        advanceUntilIdle()
        observeHourFormatTask.hourFormats.emit(value = HourFormat.TWELVE_HOUR.asRight())
        advanceUntilIdle()
        hook.cancel()

        assertEquals(expected = 1 to 3, actual = syncsAtStart to syncPushAlertsTask.syncs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aPlaceSavedWhileTheFirstSyncRunsSyncsAgain() = runTest {
        val observeFollowedPlacesTask = FakeObserveFollowedPlacesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask(
            whileSyncing = { sync: Int ->
                if (sync == 1) {
                    observeFollowedPlacesTask.followedPlaces.value = listOf(STOCKHOLM_PLACE).asRight()
                }
            }
        )
        val hook = launch {
            pushAlertsAppStartHook(
                syncPushAlertsTask = syncPushAlertsTask,
                observeFollowedPlacesTask = observeFollowedPlacesTask
            ).onAppStart()
        }
        advanceUntilIdle()
        hook.cancel()

        assertEquals(expected = 2, actual = syncPushAlertsTask.syncs)
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
    observeFollowedPlacesTask: FakeObserveFollowedPlacesTask = FakeObserveFollowedPlacesTask(),
    pollenPreferenceRepository: FakePollenPreferenceRepository = FakePollenPreferenceRepository(),
    observeShownLanguageTask: FakeObserveShownLanguageTask = FakeObserveShownLanguageTask(),
    observeHourFormatTask: FakeObserveHourFormatTask = FakeObserveHourFormatTask()
): PushAlertsAppStartHook =
    PushAlertsAppStartHook(
        syncPushAlertsTask = syncPushAlertsTask,
        observeFollowedPlacesTask = observeFollowedPlacesTask,
        observePollenAllergensTask = DefaultObservePollenAllergensTask(pollenPreferenceRepository = pollenPreferenceRepository),
        observeShownLanguageTask = observeShownLanguageTask,
        observeHourFormatTask = observeHourFormatTask
    )

private const val BIRCH_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"

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