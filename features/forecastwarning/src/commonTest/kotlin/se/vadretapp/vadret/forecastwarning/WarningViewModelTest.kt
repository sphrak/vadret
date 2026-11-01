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
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.no_network_available
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.testing.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WarningViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val getWarningsTask = FakeGetWarningsTask()
    private val warningViewModel = WarningViewModel(
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

    private fun shownIdentifiers(): List<String> = warningViewModel.state.value.warnings.map { warningItem: WarningItem -> warningItem.identifier }

    @Test
    fun `nothing loads until the screen starts the view model and then only once`() =
        testScope.runTest {
            advanceUntilIdle()
            assertEquals(expected = emptyList(), actual = getWarningsTask.refreshes)

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = listOf(false), actual = getWarningsTask.refreshes)
        }

    @Test
    fun `every warning is shown in one list newest first`() =
        testScope.runTest {
            getWarningsTask.result = listOf(
                aggregatedFeed(
                    identifier = "smhi-1",
                    publishedAt = Instant.parse(input = "2026-10-24T05:00:00Z")
                ),
                aggregatedFeed(
                    identifier = "krisinformation-1",
                    publishedAt = Instant.parse(input = "2026-10-24T07:00:00Z"),
                    source = FeedSource.KRISINFORMATION
                ),
                aggregatedFeed(
                    identifier = "smhi-2",
                    publishedAt = Instant.parse(input = "2026-10-24T06:00:00Z")
                )
            ).asRight()

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = listOf("krisinformation-1", "smhi-2", "smhi-1"), actual = shownIdentifiers())
            assertEquals(expected = false, actual = warningViewModel.state.value.isLoading)
            assertEquals(expected = false, actual = warningViewModel.state.value.isEmpty)
        }

    @Test
    fun `no warnings is told apart from a failed load`() =
        testScope.runTest {
            getWarningsTask.result = emptyList<AggregatedFeed>().asRight()
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            assertTrue(actual = warningViewModel.state.value.isEmpty)

            getWarningsTask.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            warningViewModel.onEvent(event = WarningEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = Res.string.no_network_available, actual = warningViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = warningViewModel.state.value.isRefreshing)

            warningViewModel.onEvent(event = WarningEvent.OnErrorShown)
            assertEquals(expected = null, actual = warningViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `a refresh skips the cache`() =
        testScope.runTest {
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            warningViewModel.onEvent(event = WarningEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = listOf(false, true), actual = getWarningsTask.refreshes)
        }

    @Test
    fun `a new language tells the times again without loading`() =
        testScope.runTest {
            getWarningsTask.result = listOf(
                aggregatedFeed(
                    identifier = "smhi-1",
                    endsAt = Instant.parse(input = "2026-10-24T08:30:00Z") + 48.hours
                )
            ).asRight()
            warningViewModel.onEvent(event = WarningEvent.OnLocaleChanged(languageTag = "en"))
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            val englishText: String? = warningViewModel.state.value.warnings.single().endsAtText

            warningViewModel.onEvent(event = WarningEvent.OnLocaleChanged(languageTag = "sv-SE"))
            advanceUntilIdle()
            val swedishText: String? = warningViewModel.state.value.warnings.single().endsAtText

            assertEquals(expected = true, actual = englishText?.startsWith(prefix = "Mon"))
            assertEquals(expected = true, actual = swedishText?.startsWith(prefix = "mån"))
            assertEquals(expected = listOf(false), actual = getWarningsTask.refreshes)
        }
}