package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
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
import se.vadretapp.vadret.forecastwarning.model.AuroraHourItem
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.no_alert_available
import se.vadretapp.vadret.ui.resources.no_network_available
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
    private val getAuroraHoursTask = FakeGetAuroraHoursTask()
    private val warningDetailViewModel = WarningDetailViewModel(
        getWarningsTask = getWarningsTask,
        getAuroraHoursTask = getAuroraHoursTask,
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
            assertEquals(expected = 1, actual = getWarningsTask.calls)
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

    @Test
    fun `an aurora warning shows its hours first once they are got and other warnings ask for none`() =
        testScope.runTest {
            getWarningsTask.result = listOf(auroraFeed(identifier = "vadret-aurora-u6sce-2026-10-24-POSSIBLE")).asRight()
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "vadret-aurora-u6sce-2026-10-24-POSSIBLE"))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(40, 55),
                actual = (warningDetailViewModel.state.value.warning?.details?.first() as WarningDetail.Hourly).auroraHourItems.map { auroraHourItem: AuroraHourItem -> auroraHourItem.score }
            )
            assertEquals(expected = 1, actual = getAuroraHoursTask.auroras.size)
        }

    @Test
    fun `an aurora warning whose hours cannot be got is shown without them`() =
        testScope.runTest {
            getWarningsTask.result = listOf(auroraFeed(identifier = "vadret-aurora-u6sce-2026-10-24-POSSIBLE")).asRight()
            getAuroraHoursTask.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "vadret-aurora-u6sce-2026-10-24-POSSIBLE"))
            advanceUntilIdle()

            assertEquals(
                expected = "vadret-aurora-u6sce-2026-10-24-POSSIBLE" to false,
                actual = warningDetailViewModel.state.value.warning?.identifier to (warningDetailViewModel.state.value.warning?.details?.any { warningDetail: WarningDetail -> warningDetail is WarningDetail.Hourly } == true)
            )
        }

    @Test
    fun `a warning that is no aurora asks for no hours`() =
        testScope.runTest {
            warningDetailViewModel.onEvent(event = WarningDetailEvent.OnStart(warningIdentifier = "smhi-1-1"))
            advanceUntilIdle()

            assertEquals(expected = emptyList(), actual = getAuroraHoursTask.auroras)
        }
}