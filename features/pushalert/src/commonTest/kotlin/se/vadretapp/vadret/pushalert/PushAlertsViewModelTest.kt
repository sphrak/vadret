package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.DefaultObserveAuroraEnabledTask
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePollenAllergensTask
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PushAlertsViewModelTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theFirstChoiceWaitsForPermissionToNotifyBeforeAnythingIsSaved() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(savePushAlertChoicesTask = savePushAlertChoicesTask)

        viewModel.onEvent(event = PushAlertsEvent.OnWarningAlertsToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = WARNINGS_FROM_YELLOW,
            actual = viewModel.state.value.pendingChoices
        )
        assertEquals(
            expected = emptyList(),
            actual = savePushAlertChoicesTask.saved
        )
    }

    @Test
    fun strongWindAsTheFirstChoiceWaitsForPermissionAndIsSavedOnceItIsGranted() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(savePushAlertChoicesTask = savePushAlertChoicesTask)

        viewModel.onEvent(event = PushAlertsEvent.OnStrongWindToggled(isEnabled = true))
        advanceUntilIdle()
        val waiting = viewModel.state.value.pendingChoices to savePushAlertChoicesTask.saved.toList()
        viewModel.onEvent(event = PushAlertsEvent.OnNotificationPermissionResult(isGranted = true))
        advanceUntilIdle()

        assertEquals(
            expected = STRONG_WIND_ONLY to emptyList<PushAlertChoices>(),
            actual = waiting
        )
        assertEquals(
            expected = listOf(STRONG_WIND_ONLY),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = null,
            actual = viewModel.state.value.pendingChoices
        )
    }

    @Test
    fun auroraAlertsAreOnlyOfferedWhileTheAuroraLabIsOn() = runTest(context = testDispatcher) {
        val forecastPreferenceRepository = FakeForecastPreferenceRepository(isAuroraEnabled = false)
        val viewModel = startedViewModel(forecastPreferenceRepository = forecastPreferenceRepository)
        val offeredWhileOff = viewModel.state.value.isAuroraShown

        forecastPreferenceRepository.forecastPreferences.update { stored: Either<Failure, ForecastPreferences> ->
            stored.map { forecastPreferences: ForecastPreferences -> forecastPreferences.copy(isAuroraEnabled = true) }
        }
        advanceUntilIdle()

        assertEquals(
            expected = false to true,
            actual = offeredWhileOff to viewModel.state.value.isAuroraShown
        )
    }

    @Test
    fun permissionGrantedSavesTheWaitingChoice() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(savePushAlertChoicesTask = savePushAlertChoicesTask)

        viewModel.onEvent(event = PushAlertsEvent.OnWarningAlertsToggled(isEnabled = true))
        viewModel.onEvent(event = PushAlertsEvent.OnNotificationPermissionResult(isGranted = true))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(WARNINGS_FROM_YELLOW),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = null,
            actual = viewModel.state.value.pendingChoices
        )
    }

    @Test
    fun permissionRefusedSavesNothingAndSaysWhy() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(savePushAlertChoicesTask = savePushAlertChoicesTask)

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        viewModel.onEvent(event = PushAlertsEvent.OnNotificationPermissionResult(isGranted = false))
        advanceUntilIdle()
        val isExplained = viewModel.state.value.isNotificationPermissionDialogVisible
        viewModel.onEvent(event = PushAlertsEvent.OnNotificationPermissionDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = emptyList(),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = true to false,
            actual = isExplained to viewModel.state.value.isNotificationPermissionDialogVisible
        )
    }

    @Test
    fun withAlertsAlreadyOnAChangeIsSavedAndSyncedAtOnce() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnWarningLevelSelected(warningAlertLevel = WarningAlertLevel.RED))
        advanceUntilIdle()
        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()
        viewModel.onEvent(event = PushAlertsEvent.OnWarningAlertsToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(
                WARNINGS_FROM_YELLOW.copy(warningLowestLevel = WarningAlertLevel.RED),
                WARNINGS_FROM_YELLOW.copy(
                    warningLowestLevel = WarningAlertLevel.RED,
                    temperatureTrend = true
                ),
                WARNINGS_FROM_YELLOW.copy(
                    warningLowestLevel = null,
                    temperatureTrend = true
                )
            ),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = 3,
            actual = syncPushAlertsTask.syncs
        )
    }

    @Test
    fun withAlertsAlreadyOnStrongWindIsSavedAndSyncedAtOnceBothOnAndOff() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnStrongWindToggled(isEnabled = true))
        advanceUntilIdle()
        val shown = viewModel.state.value.choices.strongWind
        viewModel.onEvent(event = PushAlertsEvent.OnStrongWindToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(
                WARNINGS_FROM_YELLOW.copy(strongWind = true),
                WARNINGS_FROM_YELLOW
            ),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = Triple(
                first = true,
                second = false,
                third = 2
            ),
            actual = Triple(
                first = shown,
                second = viewModel.state.value.choices.strongWind,
                third = syncPushAlertsTask.syncs
            )
        )
    }

    @Test
    fun withAlertsAlreadyOnAuroraIsSavedAndSyncedAtOnceBothOnAndOff() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnAuroraToggled(isEnabled = true))
        advanceUntilIdle()
        val shown = viewModel.state.value.choices.aurora
        viewModel.onEvent(event = PushAlertsEvent.OnAuroraToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(
                WARNINGS_FROM_YELLOW.copy(aurora = true),
                WARNINGS_FROM_YELLOW
            ),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = Triple(
                first = true,
                second = false,
                third = 2
            ),
            actual = Triple(
                first = shown,
                second = viewModel.state.value.choices.aurora,
                third = syncPushAlertsTask.syncs
            )
        )
    }

    @Test
    fun markingASavedPlaceIsSavedAndSyncedAtOnceBothOnAndOff() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val viewModel = startedViewModel(
            choices = STRONG_WIND_ONLY,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask
        )

        listOf(true, false).forEach { isEnabled: Boolean ->
            viewModel.onEvent(
                event = PushAlertsEvent.OnPlaceToggled(
                    pushAlertPlace = UPPSALA,
                    isEnabled = isEnabled
                )
            )
            advanceUntilIdle()
        }

        assertEquals(
            expected = listOf(
                STRONG_WIND_ONLY.copy(notifiedFavoriteIds = setOf(UPPSALA.favoriteId)),
                STRONG_WIND_ONLY
            ) to 2,
            actual = savePushAlertChoicesTask.saved to syncPushAlertsTask.syncs
        )
    }

    @Test
    fun theChosenPlaceCanBeTurnedOffWhileAnotherPlaceIsMarked() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val markedUppsala = STRONG_WIND_ONLY.copy(notifiedFavoriteIds = setOf(UPPSALA.favoriteId))
        val viewModel = startedViewModel(
            choices = markedUppsala,
            savePushAlertChoicesTask = savePushAlertChoicesTask
        )

        viewModel.onEvent(
            event = PushAlertsEvent.OnPlaceToggled(
                pushAlertPlace = PushAlertPlace.ChosenPlace(name = null),
                isEnabled = false
            )
        )
        advanceUntilIdle()

        assertEquals(
            expected = listOf(markedUppsala.copy(isChosenPlaceNotified = false)) to false,
            actual = savePushAlertChoicesTask.saved to viewModel.state.value.isLastPlaceDialogVisible
        )
    }

    @Test
    fun theLastMarkedPlaceStaysAndADialogSaysWhy() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = STRONG_WIND_ONLY,
            savePushAlertChoicesTask = savePushAlertChoicesTask
        )

        viewModel.onEvent(
            event = PushAlertsEvent.OnPlaceToggled(
                pushAlertPlace = PushAlertPlace.ChosenPlace(name = null),
                isEnabled = false
            )
        )
        advanceUntilIdle()
        val shown = viewModel.state.value.isLastPlaceDialogVisible
        viewModel.onEvent(event = PushAlertsEvent.OnLastPlaceDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = true,
                second = false,
                third = Pair(emptyList<PushAlertChoices>(), STRONG_WIND_ONLY)
            ),
            actual = Triple(
                first = shown,
                second = viewModel.state.value.isLastPlaceDialogVisible,
                third = Pair(savePushAlertChoicesTask.saved.toList(), viewModel.state.value.choices)
            )
        )
    }

    @Test
    fun thePlacesShowWithTheChoices() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(choices = STRONG_WIND_ONLY)

        assertEquals(
            expected = PLACES,
            actual = viewModel.state.value.places
        )
    }

    @Test
    fun changesMadeFasterThanTheyCanBeSavedEndInTheLastOne() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnWarningLevelSelected(warningAlertLevel = WarningAlertLevel.RED))
        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = WARNINGS_FROM_YELLOW.copy(
                warningLowestLevel = WarningAlertLevel.RED,
                temperatureTrend = true
            ),
            actual = savePushAlertChoicesTask.saved.last()
        )
    }

    @Test
    fun pollenIsTurnedOnFromModerateToHighAndItsLevelCanBeMovedBeforeItIsTurnedOff() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask
        )

        listOf(
            PushAlertsEvent.OnPollenAlertsToggled(isEnabled = true),
            PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = PollenAlertLevel.HIGH_TO_VERY_HIGH),
            PushAlertsEvent.OnPollenAlertsToggled(isEnabled = false)
        ).forEach { event: PushAlertsEvent ->
            viewModel.onEvent(event = event)
            advanceUntilIdle()
        }

        assertEquals(
            expected = listOf(PollenAlertLevel.MODERATE_TO_HIGH, PollenAlertLevel.HIGH_TO_VERY_HIGH, null),
            actual = savePushAlertChoicesTask.saved.map { pushAlertChoices -> pushAlertChoices.pollenLowestLevel }
        )
    }

    @Test
    fun withoutAllergensPickedForPollenTurningPollenOnSaysWhyAndSavesNothing() = runTest(context = testDispatcher) {
        val shown = listOf(
            NO_ALLERGENS,
            BIRCH_ALLERGY.copy(isPollenEnabled = false)
        ).map { pollenPreferences ->
            val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
            val viewModel = startedViewModel(
                choices = WARNINGS_FROM_YELLOW,
                savePushAlertChoicesTask = savePushAlertChoicesTask,
                pollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = pollenPreferences.asRight())
            )
            viewModel.onEvent(event = PushAlertsEvent.OnPollenAlertsToggled(isEnabled = true))
            advanceUntilIdle()
            val isExplained = viewModel.state.value.isNoAllergensDialogVisible
            viewModel.onEvent(event = PushAlertsEvent.OnNoAllergensDialogDismissed)
            advanceUntilIdle()
            Triple(
                first = isExplained,
                second = viewModel.state.value.isNoAllergensDialogVisible,
                third = savePushAlertChoicesTask.saved to viewModel.state.value.choices.pollenLowestLevel
            )
        }

        assertEquals(
            expected = List(size = 2) {
                Triple(
                    first = true,
                    second = false,
                    third = Pair<List<PushAlertChoices>, PollenAlertLevel?>(
                        first = emptyList(),
                        second = null
                    )
                )
            },
            actual = shown
        )
    }

    @Test
    fun pollenCanBeTurnedOffEvenAfterTheLastAllergenIsDropped() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.HIGH),
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            pollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = NO_ALLERGENS.asRight())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnPollenAlertsToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf<PollenAlertLevel?>(null) to false,
            actual = savePushAlertChoicesTask.saved.map { pushAlertChoices -> pushAlertChoices.pollenLowestLevel } to viewModel.state.value.isNoAllergensDialogVisible
        )
    }

    @Test
    fun allergensThatCannotBeReadAreShownAsTheFailure() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            pollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft())
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED) to false,
            actual = viewModel.state.value.failure to viewModel.state.value.hasPollenAllergens
        )
    }

    @Test
    fun levelDialogsOpenAndClose() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(choices = WARNINGS_FROM_YELLOW)
        val seen = mutableListOf<Pair<Boolean, Boolean>>()

        listOf(
            PushAlertsEvent.OnWarningLevelRowClicked,
            PushAlertsEvent.OnWarningLevelDialogDismissed,
            PushAlertsEvent.OnPollenLevelRowClicked,
            PushAlertsEvent.OnPollenLevelDialogDismissed
        ).forEach { event: PushAlertsEvent ->
            viewModel.onEvent(event = event)
            advanceUntilIdle()
            seen += viewModel.state.value.isWarningLevelDialogVisible to viewModel.state.value.isPollenLevelDialogVisible
        }

        assertEquals(
            expected = listOf(true to false, false to false, false to true, false to false),
            actual = seen
        )
    }

    @Test
    fun aChoiceThatCannotBeSavedIsShownAndCanBeDismissed() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = FakeSavePushAlertChoicesTask(result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()
        val shown = viewModel.state.value.failure
        viewModel.onEvent(event = PushAlertsEvent.OnFailureDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE) to null,
            actual = shown to viewModel.state.value.failure
        )
    }

    @Test
    fun outsideSwedenNothingIsAskedOrSavedAndADialogSaysWhy() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        )

        viewModel.onEvent(event = PushAlertsEvent.OnWarningAlertsToggled(isEnabled = true))
        advanceUntilIdle()
        val shown = viewModel.state.value
        viewModel.onEvent(event = PushAlertsEvent.OnOutsideSwedenDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = Triple(true, null, PushAlertChoices.NOTHING_CHOSEN),
            actual = Triple(shown.isOutsideSwedenDialogVisible, shown.pendingChoices, shown.choices)
        )
        assertEquals(
            expected = emptyList<PushAlertChoices>() to false,
            actual = savePushAlertChoicesTask.saved to viewModel.state.value.isOutsideSwedenDialogVisible
        )
    }

    @Test
    fun outsideSwedenAChangedLevelIsNotSavedAndTheShownLevelStays() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val pollenFromModerate = WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.MODERATE)
        val viewModel = startedViewModel(
            choices = pollenFromModerate,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        )

        viewModel.onEvent(event = PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = PollenAlertLevel.HIGH))
        advanceUntilIdle()

        assertEquals(
            expected = Triple<List<PushAlertChoices>, PollenAlertLevel?, Boolean>(
                first = emptyList(),
                second = PollenAlertLevel.MODERATE,
                third = true
            ),
            actual = Triple(
                savePushAlertChoicesTask.saved,
                viewModel.state.value.choices.pollenLowestLevel,
                viewModel.state.value.isOutsideSwedenDialogVisible
            )
        )
    }

    @Test
    fun outsideSwedenEverythingCanStillBeTurnedOffWithoutAskingWhereThePhoneIs() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = isOutsideSwedenTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnWarningAlertsToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(PushAlertChoices.NOTHING_CHOSEN) to 0,
            actual = savePushAlertChoicesTask.saved to isOutsideSwedenTask.checks
        )
    }

    @Test
    fun outsideSwedenOneOfSeveralAlertsCanBeTurnedOffWithoutAskingWhereThePhoneIs() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(temperatureTrend = true),
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = isOutsideSwedenTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(WARNINGS_FROM_YELLOW) to 0,
            actual = savePushAlertChoicesTask.saved to isOutsideSwedenTask.checks
        )
    }

    @Test
    fun outsideSwedenStrongWindIsNotTurnedOnAndADialogSaysWhy() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        )

        viewModel.onEvent(event = PushAlertsEvent.OnStrongWindToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = emptyList<PushAlertChoices>(),
                second = WARNINGS_FROM_YELLOW,
                third = true
            ),
            actual = Triple(
                first = savePushAlertChoicesTask.saved,
                second = viewModel.state.value.choices,
                third = viewModel.state.value.isOutsideSwedenDialogVisible
            )
        )
    }

    @Test
    fun outsideSwedenStrongWindAsTheOnlyAlertCanBeTurnedOffWithoutAskingWhereThePhoneIs() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        val viewModel = startedViewModel(
            choices = STRONG_WIND_ONLY,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = isOutsideSwedenTask
        )

        viewModel.onEvent(event = PushAlertsEvent.OnStrongWindToggled(isEnabled = false))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(PushAlertChoices.NOTHING_CHOSEN) to 0,
            actual = savePushAlertChoicesTask.saved to isOutsideSwedenTask.checks
        )
    }

    @Test
    fun choicesNotSentYetShowANoticeUntilASyncWorks() = runTest(context = testDispatcher) {
        val observePushAlertSyncPendingTask = FakeObservePushAlertSyncPendingTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            observePushAlertSyncPendingTask = observePushAlertSyncPendingTask
        )
        val seen = mutableListOf<Boolean>()

        listOf(true, false).forEach { isSyncPending: Boolean ->
            observePushAlertSyncPendingTask.isSyncPending.value = isSyncPending
            advanceUntilIdle()
            seen += viewModel.state.value.isSyncPending
        }

        assertEquals(
            expected = listOf(true, false),
            actual = seen
        )
    }

    @Test
    fun aChoiceThatCannotBeSentIsShownInADialogThatCanBeDismissed() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()
        val isShown = viewModel.state.value.isSendFailedDialogVisible
        viewModel.onEvent(event = PushAlertsEvent.OnSendFailedDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = true,
                second = false,
                third = null
            ),
            actual = Triple(
                first = isShown,
                second = viewModel.state.value.isSendFailedDialogVisible,
                third = viewModel.state.value.failure
            )
        )
    }

    @Test
    fun aChoiceThatIsSentShowsNoDialog() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(choices = WARNINGS_FROM_YELLOW)

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = false,
            actual = viewModel.state.value.isSendFailedDialogVisible
        )
    }

    @Test
    fun aPlaceTheServerDoesNotCoverIsExplainedAsOutsideSweden() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.UNSUPPORTED_AREA).asLeft())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = false,
                second = true,
                third = false
            ),
            actual = Triple(
                first = viewModel.state.value.isSendFailedDialogVisible,
                second = viewModel.state.value.isOutsideSwedenDialogVisible,
                third = viewModel.state.value.isAttestationRefusedDialogVisible
            )
        )
    }

    @Test
    fun aPhoneTheServerCannotConfirmIsExplainedAndTheExplanationCanBeDismissed() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.ATTESTATION_REFUSED).asLeft())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()
        val isExplained = viewModel.state.value.isAttestationRefusedDialogVisible to viewModel.state.value.isSendFailedDialogVisible
        viewModel.onEvent(event = PushAlertsEvent.OnAttestationRefusedDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = (true to false) to false,
            actual = isExplained to viewModel.state.value.isAttestationRefusedDialogVisible
        )
    }

    private fun TestScope.startedViewModel(
        choices: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN,
        savePushAlertChoicesTask: FakeSavePushAlertChoicesTask = FakeSavePushAlertChoicesTask(),
        syncPushAlertsTask: FakeSyncPushAlertsTask = FakeSyncPushAlertsTask(),
        observePushAlertSyncPendingTask: FakeObservePushAlertSyncPendingTask = FakeObservePushAlertSyncPendingTask(),
        isOutsideSwedenTask: FakeIsOutsideSwedenTask = FakeIsOutsideSwedenTask(),
        observePushAlertPlacesTask: FakeObservePushAlertPlacesTask = FakeObservePushAlertPlacesTask(places = PLACES),
        pollenPreferenceRepository: FakePollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = BIRCH_ALLERGY.asRight()),
        forecastPreferenceRepository: FakeForecastPreferenceRepository = FakeForecastPreferenceRepository()
    ): PushAlertsViewModel {
        val viewModel = PushAlertsViewModel(
            observePushAlertChoicesTask = FakeObservePushAlertChoicesTask(choices = choices),
            observePushAlertPlacesTask = observePushAlertPlacesTask,
            observePushAlertSyncPendingTask = observePushAlertSyncPendingTask,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask,
            observePollenAllergensTask = DefaultObservePollenAllergensTask(pollenPreferenceRepository = pollenPreferenceRepository),
            observeAuroraEnabledTask = DefaultObserveAuroraEnabledTask(forecastPreferenceRepository = forecastPreferenceRepository),
            isOutsideSwedenTask = isOutsideSwedenTask,
            coroutineDispatcher = testDispatcher
        )
        viewModel.onEvent(event = PushAlertsEvent.OnStart)
        advanceUntilIdle()
        return viewModel
    }
}

private val WARNINGS_FROM_YELLOW = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenLowestLevel = null,
    temperatureTrend = false,
    strongWind = false,
    slipperiness = false,
    aurora = false,
    isChosenPlaceNotified = true,
    notifiedFavoriteIds = emptySet()
)
private val BIRCH_ALLERGY = NO_ALLERGENS.copy(allergenPollenIds = setOf("2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"))
private val UPPSALA = PushAlertPlace.SavedPlace(
    favoriteId = "favorite-uppsala",
    name = "Uppsala"
)
private val PLACES: List<PushAlertPlace> = listOf(
    PushAlertPlace.ChosenPlace(name = null),
    UPPSALA
)
private val STRONG_WIND_ONLY = PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true)