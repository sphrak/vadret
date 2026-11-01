package se.vadretapp.vadret.forecastwarning

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
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.no_alert_available
import se.vadretapp.vadret.core.resources.no_network_available
import se.vadretapp.vadret.testing.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WarningDetailViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val getWarningsTask = FakeGetWarningsTask()
    private val warningDetailViewModel = WarningDetailViewModel(
        getWarningsTask = getWarningsTask,
        clock = FixedClock(now = Instant.parse(input = "2026-10-24T08:30:00Z")),
        coroutineDispatcher = testDispatcher
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the warning is found in the cached feed and loaded once`() =
        testScope.runTest {
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "smhi-1-1"))
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "smhi-1-1"))
            advanceUntilIdle()

            assertEquals(expected = "smhi-1-1", actual = warningDetailViewModel.state.value.warning?.identifier)
            assertEquals(expected = false, actual = warningDetailViewModel.state.value.isLoading)
            assertEquals(expected = null, actual = warningDetailViewModel.state.value.messageResource)
            assertEquals(expected = listOf(false), actual = getWarningsTask.refreshes)
        }

    @Test
    fun `a warning that has expired says there is nothing to show`() =
        testScope.runTest {
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "smhi-9-9"))
            advanceUntilIdle()

            assertEquals(expected = null, actual = warningDetailViewModel.state.value.warning)
            assertEquals(expected = Res.string.no_alert_available, actual = warningDetailViewModel.state.value.messageResource)
        }

    @Test
    fun `a failed load shows why`() =
        testScope.runTest {
            getWarningsTask.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "smhi-1-1"))
            advanceUntilIdle()

            assertEquals(expected = Res.string.no_network_available, actual = warningDetailViewModel.state.value.messageResource)
            assertEquals(expected = false, actual = warningDetailViewModel.state.value.isLoading)
        }
}