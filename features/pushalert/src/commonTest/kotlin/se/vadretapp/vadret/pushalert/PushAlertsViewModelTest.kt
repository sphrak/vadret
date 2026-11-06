package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
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
    fun aPollenTypeIsFollowedAtTheChosenLevelAndTheLevelMovesThemAll() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask
        )

        viewModel.onEvent(

            event = PushAlertsEvent.OnPollenTypeToggled(

                pollenTypeId = BIRCH.pollenId,

                isEnabled = true

            )

        )
        advanceUntilIdle()
        viewModel.onEvent(
            event = PushAlertsEvent.OnPollenTypeToggled(
                pollenTypeId = GRASS.pollenId,
                isEnabled = true
            )
        )
        advanceUntilIdle()
        viewModel.onEvent(event = PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = PollenAlertLevel.HIGH_TO_VERY_HIGH))
        advanceUntilIdle()
        viewModel.onEvent(
            event = PushAlertsEvent.OnPollenTypeToggled(
                pollenTypeId = BIRCH.pollenId,
                isEnabled = false
            )
        )
        advanceUntilIdle()

        assertEquals(
            expected = listOf(
                listOf(
                    PollenChoice(
                        pollenTypeId = BIRCH.pollenId,
                        lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH
                    )
                ),
                listOf(
                    PollenChoice(
                        pollenTypeId = BIRCH.pollenId,
                        lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH
                    ),
                    PollenChoice(
                        pollenTypeId = GRASS.pollenId,
                        lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH
                    )
                ),
                listOf(
                    PollenChoice(
                        pollenTypeId = BIRCH.pollenId,
                        lowestLevel = PollenAlertLevel.HIGH_TO_VERY_HIGH
                    ),
                    PollenChoice(
                        pollenTypeId = GRASS.pollenId,
                        lowestLevel = PollenAlertLevel.HIGH_TO_VERY_HIGH
                    )
                ),
                listOf(
                    PollenChoice(
                        pollenTypeId = GRASS.pollenId,
                        lowestLevel = PollenAlertLevel.HIGH_TO_VERY_HIGH
                    )
                )
            ),
            actual = savePushAlertChoicesTask.saved.map { pushAlertChoices -> pushAlertChoices.pollenChoices }
        )
        assertEquals(
            expected = setOf(GRASS.pollenId) to PollenAlertLevel.HIGH_TO_VERY_HIGH,
            actual = viewModel.state.value.chosenPollenTypeIds to viewModel.state.value.pollenLevel
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
        val pollenFromModerate = WARNINGS_FROM_YELLOW.copy(
            pollenChoices = listOf(
                PollenChoice(
                    pollenTypeId = BIRCH.pollenId,
                    lowestLevel = PollenAlertLevel.MODERATE
                )
            )
        )
        val viewModel = startedViewModel(
            choices = pollenFromModerate,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        )

        viewModel.onEvent(event = PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = PollenAlertLevel.HIGH))
        advanceUntilIdle()

        assertEquals(
            expected = Triple(emptyList<PushAlertChoices>(), PollenAlertLevel.MODERATE, true),
            actual = Triple(
                savePushAlertChoicesTask.saved,
                viewModel.state.value.pollenLevel,
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
    fun nothingShowsUntilThePollenTypesAreInPlaceSoEverySectionShowsAtOnce() = runTest(context = testDispatcher) {
        val observePollenTypesTask = FakeObservePollenTypesTask(pollenTypes = CompletableDeferred())
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            observePollenTypesTask = observePollenTypesTask
        )
        val isLoadingBefore = viewModel.state.value.isLoading
        observePollenTypesTask.pollenTypes.complete(value = listOf(BIRCH, GRASS).asRight())
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = true,
                second = false,
                third = WARNINGS_FROM_YELLOW to listOf(BIRCH, GRASS)
            ),
            actual = Triple(
                first = isLoadingBefore,
                second = viewModel.state.value.isLoading,
                third = viewModel.state.value.choices to viewModel.state.value.pollenTypes
            )
        )
    }

    @Test
    fun pollenTypesThatCannotBeLoadedAreSaidToBeMissingWithoutADialog() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            observePollenTypesTask = FakeObservePollenTypesTask(pollenTypes = CompletableDeferred(value = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()))
        )

        assertEquals(
            expected = Triple(
                first = false,
                second = true,
                third = null
            ),
            actual = Triple(
                first = viewModel.state.value.isLoading,
                second = viewModel.state.value.arePollenTypesUnavailable,
                third = viewModel.state.value.failure
            )
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
        observePollenTypesTask: FakeObservePollenTypesTask = FakeObservePollenTypesTask(pollenTypes = CompletableDeferred(value = listOf(BIRCH, GRASS).asRight())),
        observePushAlertPlacesTask: FakeObservePushAlertPlacesTask = FakeObservePushAlertPlacesTask(places = PLACES)
    ): PushAlertsViewModel {
        val viewModel = PushAlertsViewModel(
            observePushAlertChoicesTask = FakeObservePushAlertChoicesTask(choices = choices),
            observePushAlertPlacesTask = observePushAlertPlacesTask,
            observePushAlertSyncPendingTask = observePushAlertSyncPendingTask,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask,
            observePollenTypesTask = observePollenTypesTask,
            isOutsideSwedenTask = isOutsideSwedenTask,
            coroutineDispatcher = testDispatcher
        )
        viewModel.onEvent(event = PushAlertsEvent.OnStart)
        advanceUntilIdle()
        return viewModel
    }
}

private val BIRCH = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236",
    name = "Björk",
    hasForecasts = true
)
private val GRASS = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323237",
    name = "Gräs",
    hasForecasts = true
)
private val WARNINGS_FROM_YELLOW = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenChoices = emptyList(),
    temperatureTrend = false,
    strongWind = false,
    isChosenPlaceNotified = true,
    notifiedFavoriteIds = emptySet()
)
private val UPPSALA = PushAlertPlace.SavedPlace(
    favoriteId = "favorite-uppsala",
    name = "Uppsala"
)
private val PLACES: List<PushAlertPlace> = listOf(
    PushAlertPlace.ChosenPlace(name = null),
    UPPSALA
)
private val STRONG_WIND_ONLY = PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true)