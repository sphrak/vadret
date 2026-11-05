package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
                PushAlertType.HURRICANE to PushAlertKind.WIND
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
        coroutineDispatcher = testDispatcher
    )
}