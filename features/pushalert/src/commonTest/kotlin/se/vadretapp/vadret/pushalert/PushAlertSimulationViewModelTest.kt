package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import se.vadretapp.vadret.testing.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PushAlertSimulationViewModelTest {
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
    fun everyAlertTypeIsSimulatedOnceOnItsOwnChannel() {
        val viewModel = viewModel(showSimulatedPushAlertTask = FakeShowSimulatedPushAlertTask(isShown = true))

        assertEquals(
            expected = listOf(
                PushAlertType.WARNING to PushAlertKind.WARNING,
                PushAlertType.POLLEN to PushAlertKind.POLLEN,
                PushAlertType.WARMER to PushAlertKind.TEMPERATURE,
                PushAlertType.COLDER to PushAlertKind.TEMPERATURE,
                PushAlertType.HARD_WIND to PushAlertKind.WIND,
                PushAlertType.STORM to PushAlertKind.WIND,
                PushAlertType.HURRICANE to PushAlertKind.WIND,
                PushAlertType.SLIPPERINESS to PushAlertKind.SLIPPERINESS,
                PushAlertType.AURORA to PushAlertKind.AURORA
            ),
            actual = viewModel.state.value.pushAlertMessages.map { pushAlertMessage: PushAlertMessage -> pushAlertMessage.pushAlertType to pushAlertMessage.kind }
        )
    }

    @Test
    fun everySimulatedAlertReplacesOnlyItsOwnEarlierNotification() {
        val viewModel = viewModel(showSimulatedPushAlertTask = FakeShowSimulatedPushAlertTask(isShown = true))
        val notificationIds: List<String?> = viewModel.state.value.pushAlertMessages.map { pushAlertMessage: PushAlertMessage -> pushAlertMessage.notificationId }

        assertEquals(
            expected = PushAlertType.entries.size,
            actual = notificationIds.filterNotNull().toSet().size
        )
    }

    @Test
    fun theSimulatedSlipperinessRefersToTheSlipperinessOfTheNightAheadOrJustPassed() {
        val evening = Instant.parse(input = "2026-11-08T18:00:00Z")
        val morning = Instant.parse(input = "2026-11-09T05:00:00Z")

        assertEquals(
            expected = List(size = 2) {
                WarningReference.Slipperiness(
                    geohash = Geohash(value = "u6sce"),
                    slipperinessDay = "2026-11-08"
                )
            },
            actual = listOf(evening, morning).map { now: Instant ->
                simulatedPushAlertMessages(now = now).simulatedWarningReference(pushAlertKind = PushAlertKind.SLIPPERINESS)
            }
        )
    }

    @Test
    fun theSimulatedAuroraRefersToTheNightAheadOrJustPassed() {
        val evening = Instant.parse(input = "2026-11-08T18:00:00Z")
        val morning = Instant.parse(input = "2026-11-09T05:00:00Z")

        assertEquals(
            expected = List(size = 2) {
                WarningReference.Aurora(
                    geohash = Geohash(value = "u6sce"),
                    auroraNight = "2026-11-08"
                )
            },
            actual = listOf(evening, morning).map { now: Instant ->
                simulatedPushAlertMessages(now = now).simulatedWarningReference(pushAlertKind = PushAlertKind.AURORA)
            }
        )
    }

    @Test
    fun theNightChangesAtSwedishNoonEvenOnTheDayTheClocksGoBack() {
        val beforeNoon = Instant.parse(input = "2026-10-25T10:30:00Z")
        val afterNoon = Instant.parse(input = "2026-10-25T11:30:00Z")

        assertEquals(
            expected = listOf(
                WarningReference.Aurora(
                    geohash = Geohash(value = "u6sce"),
                    auroraNight = "2026-10-24"
                ),
                WarningReference.Aurora(
                    geohash = Geohash(value = "u6sce"),
                    auroraNight = "2026-10-25"
                )
            ),
            actual = listOf(beforeNoon, afterNoon).map { now: Instant ->
                simulatedPushAlertMessages(now = now).simulatedWarningReference(pushAlertKind = PushAlertKind.AURORA)
            }
        )
    }

    @Test
    fun clickingAnAlertShowsIt() = runTest(context = testDispatcher) {
        val showSimulatedPushAlertTask = FakeShowSimulatedPushAlertTask(isShown = true)
        val viewModel = viewModel(showSimulatedPushAlertTask = showSimulatedPushAlertTask)
        val pushAlertMessage: PushAlertMessage = viewModel.state.value.pushAlertMessages.last()

        viewModel.onEvent(event = PushAlertSimulationEvent.OnPushAlertClicked(pushAlertMessage = pushAlertMessage))
        advanceUntilIdle()

        assertEquals(
            expected = listOf(pushAlertMessage) to false,
            actual = showSimulatedPushAlertTask.shown to viewModel.state.value.isNotificationsOffDialogVisible
        )
    }

    @Test
    fun clickingAnAlertWhileNotificationsAreOffExplainsWhyNothingShows() = runTest(context = testDispatcher) {
        val viewModel = viewModel(showSimulatedPushAlertTask = FakeShowSimulatedPushAlertTask(isShown = false))

        viewModel.onEvent(event = PushAlertSimulationEvent.OnPushAlertClicked(pushAlertMessage = viewModel.state.value.pushAlertMessages.first()))
        advanceUntilIdle()

        assertEquals(
            expected = true,
            actual = viewModel.state.value.isNotificationsOffDialogVisible
        )
    }

    @Test
    fun dismissingTheExplanationHidesIt() = runTest(context = testDispatcher) {
        val viewModel = viewModel(showSimulatedPushAlertTask = FakeShowSimulatedPushAlertTask(isShown = false))

        viewModel.onEvent(event = PushAlertSimulationEvent.OnPushAlertClicked(pushAlertMessage = viewModel.state.value.pushAlertMessages.first()))
        advanceUntilIdle()
        viewModel.onEvent(event = PushAlertSimulationEvent.OnNotificationsOffDismissed)

        assertEquals(
            expected = false,
            actual = viewModel.state.value.isNotificationsOffDialogVisible
        )
    }

    private fun viewModel(showSimulatedPushAlertTask: FakeShowSimulatedPushAlertTask): PushAlertSimulationViewModel = PushAlertSimulationViewModel(
        showSimulatedPushAlertTask = showSimulatedPushAlertTask,
        clock = FixedClock(now = SIMULATED_AT),
        coroutineDispatcher = testDispatcher
    )
}

private val SIMULATED_AT: Instant = Instant.parse(input = "2026-11-08T18:00:00Z")

private fun List<PushAlertMessage>.simulatedWarningReference(pushAlertKind: PushAlertKind): WarningReference? =
    single { pushAlertMessage: PushAlertMessage -> pushAlertMessage.kind == pushAlertKind }.warningReference