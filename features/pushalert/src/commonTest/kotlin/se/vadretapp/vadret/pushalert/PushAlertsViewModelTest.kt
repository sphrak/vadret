package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
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
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.model.FavoriteRow
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
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
    fun startingShowsTheSavedChoicesAndThePollenTypes() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(choices = WARNINGS_FROM_YELLOW)
        advanceUntilIdle()

        assertEquals(
            expected = WARNINGS_FROM_YELLOW to listOf(BIRCH, GRASS),
            actual = viewModel.state.value.choices to viewModel.state.value.pollenTypes
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
    fun aSyncThatFailsLeavesTheNoticeToSaySoInsteadOfAFailure() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            syncPushAlertsTask = FakeSyncPushAlertsTask(result = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft())
        )

        viewModel.onEvent(event = PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = true))
        advanceUntilIdle()

        assertEquals(
            expected = null to false,
            actual = viewModel.state.value.failure to viewModel.state.value.isAttestationRefusedDialogVisible
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
            expected = Triple(null, true, false),
            actual = Triple(
                viewModel.state.value.failure,
                viewModel.state.value.isOutsideSwedenDialogVisible,
                viewModel.state.value.isAttestationRefusedDialogVisible
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
        val isExplained = viewModel.state.value.isAttestationRefusedDialogVisible
        viewModel.onEvent(event = PushAlertsEvent.OnAttestationRefusedDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = true to false,
            actual = isExplained to viewModel.state.value.isAttestationRefusedDialogVisible
        )
    }

    @Test
    fun startingShowsASwitchForEverySavedPlaceInTheOrderTheyWereSaved() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(favoriteIds = setOf(GOTEBORG_FAVORITE.favoriteId)),
            observeFavoritesTask = FakeObserveFavoritesTask(
                favorites = listOf(
                    MALMO_FAVORITE,
                    GOTEBORG_FAVORITE,
                    BERLIN_FAVORITE
                )
            )
        )

        assertEquals(
            expected = listOf(
                FavoriteRow(
                    favoriteId = MALMO_FAVORITE.favoriteId,
                    locality = "Malmö",
                    isChecked = false,
                    isInSweden = true
                ),
                FavoriteRow(
                    favoriteId = GOTEBORG_FAVORITE.favoriteId,
                    locality = "Göteborg",
                    isChecked = true,
                    isInSweden = true
                ),
                FavoriteRow(
                    favoriteId = BERLIN_FAVORITE.favoriteId,
                    locality = "Berlin",
                    isChecked = false,
                    isInSweden = false
                )
            ),
            actual = viewModel.state.value.favoriteRows
        )
    }

    @Test
    fun aSavedPlaceTurnedOnAndOffIsSavedAndSyncedAtOnce() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val syncPushAlertsTask = FakeSyncPushAlertsTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask,
            observeFavoritesTask = FakeObserveFavoritesTask(
                favorites = listOf(
                    MALMO_FAVORITE,
                    GOTEBORG_FAVORITE
                )
            )
        )

        toggle(
            viewModel = viewModel,
            favorite = MALMO_FAVORITE,
            isEnabled = true
        )
        val shown = viewModel.state.value.checkedFavoriteIds()
        toggle(
            viewModel = viewModel,
            favorite = MALMO_FAVORITE,
            isEnabled = false
        )

        assertEquals(
            expected = listOf(
                WARNINGS_FROM_YELLOW.copy(favoriteIds = setOf(MALMO_FAVORITE.favoriteId)),
                WARNINGS_FROM_YELLOW
            ),
            actual = savePushAlertChoicesTask.saved
        )
        assertEquals(
            expected = Triple(
                first = setOf(MALMO_FAVORITE.favoriteId),
                second = emptySet<String>(),
                third = 2
            ),
            actual = Triple(
                first = shown,
                second = viewModel.state.value.checkedFavoriteIds(),
                third = syncPushAlertsTask.syncs
            )
        )
    }

    @Test
    fun aSavedPlaceAloneIsSavedWithoutAskingToNotifySinceItChoosesNoAlert() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = FakeObserveFavoritesTask(favorites = listOf(MALMO_FAVORITE))
        )

        toggle(
            viewModel = viewModel,
            favorite = MALMO_FAVORITE,
            isEnabled = true
        )

        assertEquals(
            expected = null,
            actual = viewModel.state.value.pendingChoices
        )
        assertEquals(
            expected = listOf(PushAlertChoices.NOTHING_CHOSEN.copy(favoriteIds = setOf(MALMO_FAVORITE.favoriteId))),
            actual = savePushAlertChoicesTask.saved
        )
    }

    @Test
    fun aFifthSavedPlaceIsNotTurnedOnAndADialogSaysWhyUntilAnotherIsTurnedOff() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val fourPlaces = setOf(
            MALMO_FAVORITE.favoriteId,
            GOTEBORG_FAVORITE.favoriteId,
            UPPSALA_FAVORITE.favoriteId,
            UMEA_FAVORITE.favoriteId
        )
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(favoriteIds = fourPlaces),
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = FakeObserveFavoritesTask(favorites = FIVE_PLACES)
        )

        toggle(
            viewModel = viewModel,
            favorite = KIRUNA_FAVORITE,
            isEnabled = true
        )
        val refused = viewModel.state.value
        viewModel.onEvent(event = PushAlertsEvent.OnFavoriteLimitDialogDismissed)
        advanceUntilIdle()
        val isDialogVisibleAfterDismissal = viewModel.state.value.isFavoriteLimitDialogVisible
        toggle(
            viewModel = viewModel,
            favorite = UMEA_FAVORITE,
            isEnabled = false
        )
        toggle(
            viewModel = viewModel,
            favorite = KIRUNA_FAVORITE,
            isEnabled = true
        )

        assertEquals(
            expected = Triple(
                first = true,
                second = fourPlaces,
                third = false
            ),
            actual = Triple(
                first = refused.isFavoriteLimitDialogVisible,
                second = refused.checkedFavoriteIds(),
                third = isDialogVisibleAfterDismissal
            )
        )
        assertEquals(
            expected = listOf(
                fourPlaces - UMEA_FAVORITE.favoriteId,
                fourPlaces - UMEA_FAVORITE.favoriteId + KIRUNA_FAVORITE.favoriteId
            ),
            actual = savePushAlertChoicesTask.saved.map { pushAlertChoices: PushAlertChoices -> pushAlertChoices.favoriteIds }
        )
    }

    @Test
    fun aSavedPlaceRemovedElsewhereDisappearsAndNoLongerCountsTowardsTheLimit() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val observeFavoritesTask = FakeObserveFavoritesTask(favorites = FIVE_PLACES)
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(
                favoriteIds = setOf(
                    MALMO_FAVORITE.favoriteId,
                    GOTEBORG_FAVORITE.favoriteId,
                    UPPSALA_FAVORITE.favoriteId,
                    UMEA_FAVORITE.favoriteId
                )
            ),
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = observeFavoritesTask
        )

        observeFavoritesTask.favorites.value = FIVE_PLACES - UMEA_FAVORITE
        advanceUntilIdle()
        val shownPlaces = viewModel.state.value.favoriteRows.map { favoriteRow: FavoriteRow -> favoriteRow.favoriteId }
        toggle(
            viewModel = viewModel,
            favorite = KIRUNA_FAVORITE,
            isEnabled = true
        )

        assertEquals(
            expected = (FIVE_PLACES - UMEA_FAVORITE).map { favorite: Favorite -> favorite.favoriteId },
            actual = shownPlaces
        )
        assertEquals(
            expected = listOf(
                setOf(
                    MALMO_FAVORITE.favoriteId,
                    GOTEBORG_FAVORITE.favoriteId,
                    UPPSALA_FAVORITE.favoriteId,
                    KIRUNA_FAVORITE.favoriteId
                )
            ),
            actual = savePushAlertChoicesTask.saved.map { pushAlertChoices: PushAlertChoices -> pushAlertChoices.favoriteIds }
        )
    }

    @Test
    fun aSavedPlaceAbroadIsNotTurnedOnAndADialogSaysWhy() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = FakeObserveFavoritesTask(favorites = listOf(BERLIN_FAVORITE))
        )

        toggle(
            viewModel = viewModel,
            favorite = BERLIN_FAVORITE,
            isEnabled = true
        )
        val refused = viewModel.state.value
        viewModel.onEvent(event = PushAlertsEvent.OnOutsideSwedenDialogDismissed)
        advanceUntilIdle()

        assertEquals(
            expected = Triple(
                first = emptyList<PushAlertChoices>(),
                second = emptySet<String>(),
                third = true
            ),
            actual = Triple(
                first = savePushAlertChoicesTask.saved,
                second = refused.checkedFavoriteIds(),
                third = refused.isOutsideSwedenDialogVisible
            )
        )
        assertEquals(
            expected = false,
            actual = viewModel.state.value.isOutsideSwedenDialogVisible
        )
    }

    @Test
    fun outsideSwedenASavedPlaceInSwedenIsNotTurnedOnButCanBeTurnedOffWithoutAskingWhereThePhoneIs() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val isOutsideSwedenTask = FakeIsOutsideSwedenTask(isOutsideSweden = true)
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW.copy(favoriteIds = setOf(GOTEBORG_FAVORITE.favoriteId)),
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = FakeObserveFavoritesTask(
                favorites = listOf(
                    MALMO_FAVORITE,
                    GOTEBORG_FAVORITE
                )
            ),
            isOutsideSwedenTask = isOutsideSwedenTask
        )

        toggle(
            viewModel = viewModel,
            favorite = MALMO_FAVORITE,
            isEnabled = true
        )
        val refused = viewModel.state.value
        val checksWhenTurningOn = isOutsideSwedenTask.checks
        toggle(
            viewModel = viewModel,
            favorite = GOTEBORG_FAVORITE,
            isEnabled = false
        )

        assertEquals(
            expected = Triple(
                first = true,
                second = setOf(GOTEBORG_FAVORITE.favoriteId),
                third = 1
            ),
            actual = Triple(
                first = refused.isOutsideSwedenDialogVisible,
                second = refused.checkedFavoriteIds(),
                third = checksWhenTurningOn
            )
        )
        assertEquals(
            expected = listOf(WARNINGS_FROM_YELLOW) to 1,
            actual = savePushAlertChoicesTask.saved to isOutsideSwedenTask.checks
        )
    }

    @Test
    fun aSavedPlaceThatIsNoLongerShownCannotBeTurnedOn() = runTest(context = testDispatcher) {
        val savePushAlertChoicesTask = FakeSavePushAlertChoicesTask()
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            observeFavoritesTask = FakeObserveFavoritesTask(favorites = listOf(MALMO_FAVORITE))
        )

        toggle(
            viewModel = viewModel,
            favorite = GOTEBORG_FAVORITE,
            isEnabled = true
        )

        assertEquals(
            expected = emptyList<PushAlertChoices>(),
            actual = savePushAlertChoicesTask.saved
        )
    }

    @Test
    fun savedPlacesThatCannotBeReadAreShownAsAFailure() = runTest(context = testDispatcher) {
        val viewModel = startedViewModel(
            choices = WARNINGS_FROM_YELLOW,
            observeFavoritesTask = FakeObserveFavoritesTask(failure = Failure(errorCode = ErrorCode.STORAGE_FAILED))
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED),
            actual = viewModel.state.value.failure
        )
        assertEquals(
            expected = emptyList(),
            actual = viewModel.state.value.favoriteRows
        )
    }

    private fun TestScope.toggle(
        viewModel: PushAlertsViewModel,
        favorite: Favorite,
        isEnabled: Boolean
    ) {
        viewModel.onEvent(
            event = PushAlertsEvent.OnFavoriteToggled(
                favoriteId = favorite.favoriteId,
                isEnabled = isEnabled
            )
        )
        advanceUntilIdle()
    }

    private fun TestScope.startedViewModel(
        choices: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN,
        savePushAlertChoicesTask: FakeSavePushAlertChoicesTask = FakeSavePushAlertChoicesTask(),
        syncPushAlertsTask: FakeSyncPushAlertsTask = FakeSyncPushAlertsTask(),
        observePushAlertSyncPendingTask: FakeObservePushAlertSyncPendingTask = FakeObservePushAlertSyncPendingTask(),
        observeFavoritesTask: FakeObserveFavoritesTask = FakeObserveFavoritesTask(),
        isOutsideSwedenTask: FakeIsOutsideSwedenTask = FakeIsOutsideSwedenTask()
    ): PushAlertsViewModel {
        val viewModel = PushAlertsViewModel(
            observePushAlertChoicesTask = FakeObservePushAlertChoicesTask(choices = choices),
            observePushAlertSyncPendingTask = observePushAlertSyncPendingTask,
            observeFavoritesTask = observeFavoritesTask,
            savePushAlertChoicesTask = savePushAlertChoicesTask,
            syncPushAlertsTask = syncPushAlertsTask,
            getPollenTypesTask = FakeGetPollenTypesTask(pollenTypes = listOf(BIRCH, GRASS)),
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
    favoriteIds = emptySet()
)
private val STRONG_WIND_ONLY = PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true)
private val FIVE_PLACES = listOf(
    MALMO_FAVORITE,
    GOTEBORG_FAVORITE,
    UPPSALA_FAVORITE,
    UMEA_FAVORITE,
    KIRUNA_FAVORITE
)

private fun PushAlertsState.checkedFavoriteIds(): Set<String> =
    favoriteRows
        .filter { favoriteRow: FavoriteRow -> favoriteRow.isChecked }
        .map { favoriteRow: FavoriteRow -> favoriteRow.favoriteId }
        .toSet()