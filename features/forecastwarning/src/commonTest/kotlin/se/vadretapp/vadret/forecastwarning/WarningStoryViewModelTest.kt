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
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.warning_detail_description
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WarningStoryViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val getWarningsTask = FakeGetWarningsTask()
    private val markWarningSeenTask = FakeMarkWarningSeenTask()
    private val warningStoryViewModel = WarningStoryViewModel(
        getWarningsTask = getWarningsTask,
        markWarningSeenTask = markWarningSeenTask,
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

    private fun storyWarningIdentifiers(): List<String> = warningStoryViewModel.state.value.warnings.map { warningItem: WarningItem -> warningItem.identifier }

    private fun start(warningIdentifiers: List<String> = listOf("smhi-1-1", "smhi-1-2")) {
        warningStoryViewModel.onEvent(event = WarningStoryEvent.OnStart(warningIdentifiers = warningIdentifiers))
    }

    @Test
    fun `stories keep the order they were opened in and skip warnings that are gone`() =
        testScope.runTest {
            start(warningIdentifiers = listOf("smhi-1-1", "smhi-9-9", "smhi-1-2"))
            start(warningIdentifiers = listOf("smhi-1-2"))
            advanceUntilIdle()

            assertEquals(expected = listOf("smhi-1-1", "smhi-1-2"), actual = storyWarningIdentifiers())
            assertEquals(expected = 0, actual = warningStoryViewModel.state.value.currentIndex)
            assertEquals(expected = 1, actual = getWarningsTask.calls)
        }

    @Test
    fun `a story shows only the first detail of its warning`() =
        testScope.runTest {
            getWarningsTask.result = listOf(
                aggregatedFeed(
                    identifier = "smhi-1-1",
                    details = listOf(
                        FeedDetail(
                            kind = FeedDetailKind.DESCRIPTION,
                            title = "Händelsebeskrivning",
                            text = "Vindbyar på 30 m/s."
                        ),
                        FeedDetail(
                            kind = FeedDetailKind.ADVICE,
                            title = "Vad ska jag tänka på?",
                            text = "Undvik onödiga resor."
                        )
                    )
                )
            ).asRight()
            start(warningIdentifiers = listOf("smhi-1-1"))
            advanceUntilIdle()

            assertEquals(
                expected = listOf<WarningDetail>(
                    WarningDetail.Labelled(
                        labelResource = Res.string.warning_detail_description,
                        text = "Vindbyar på 30 m/s."
                    )
                ),
                actual = warningStoryViewModel.state.value.warnings
                    .single()
                    .details
            )
        }

    @Test
    fun `a shown story is marked seen`() =
        testScope.runTest {
            start()
            advanceUntilIdle()

            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnStoryShown(index = 1))
            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnStoryShown(index = 5))
            advanceUntilIdle()

            assertEquals(expected = listOf("smhi-1-2"), actual = markWarningSeenTask.warningIdentifiers)
        }

    @Test
    fun `next moves on and finishes after the last story`() =
        testScope.runTest {
            start()
            advanceUntilIdle()

            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnNext)
            advanceUntilIdle()
            assertEquals(expected = 1, actual = warningStoryViewModel.state.value.currentIndex)
            assertEquals(expected = false, actual = warningStoryViewModel.state.value.isFinished)

            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnNext)
            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnNext)
            advanceUntilIdle()
            assertEquals(expected = 1, actual = warningStoryViewModel.state.value.currentIndex)
            assertEquals(expected = true, actual = warningStoryViewModel.state.value.isFinished)
        }

    @Test
    fun `previous goes back but not before the first story`() =
        testScope.runTest {
            start()
            advanceUntilIdle()

            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnNext)
            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnPrevious)
            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnPrevious)
            advanceUntilIdle()

            assertEquals(expected = 0, actual = warningStoryViewModel.state.value.currentIndex)
        }

    @Test
    fun `stepping before the stories have loaded changes nothing`() =
        testScope.runTest {
            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnNext)
            advanceUntilIdle()

            assertEquals(expected = 0, actual = warningStoryViewModel.state.value.currentIndex)
            assertEquals(expected = false, actual = warningStoryViewModel.state.value.isFinished)
        }

    @Test
    fun `no stories left or a failed load finishes at once`() =
        testScope.runTest {
            start(warningIdentifiers = listOf("smhi-9-9"))
            advanceUntilIdle()
            assertEquals(expected = true, actual = warningStoryViewModel.state.value.isFinished)

            val failingViewModel = WarningStoryViewModel(
                getWarningsTask = FakeGetWarningsTask(result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()),
                markWarningSeenTask = markWarningSeenTask,
                clock = FixedClock(now = Instant.parse(input = "2026-10-24T08:30:00Z")),
                coroutineDispatcher = testDispatcher
            )
            failingViewModel.onEvent(event = WarningStoryEvent.OnStart(warningIdentifiers = listOf("smhi-1-1")))
            advanceUntilIdle()

            assertEquals(expected = true, actual = failingViewModel.state.value.isFinished)
            assertEquals(expected = false, actual = failingViewModel.state.value.isLoading)
        }
}