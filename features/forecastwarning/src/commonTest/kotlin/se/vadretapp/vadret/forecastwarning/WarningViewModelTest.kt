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
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning
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
    private val observeFollowedPlaceWarningsTask = FakeObserveFollowedPlaceWarningsTask()
    private val warningViewModel = WarningViewModel(
        observeFollowedPlaceWarningsTask = observeFollowedPlaceWarningsTask,
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
            assertEquals(expected = emptyList(), actual = observeFollowedPlaceWarningsTask.refreshes)

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = listOf(false), actual = observeFollowedPlaceWarningsTask.refreshes)
        }

    @Test
    fun `every warning of the chosen place is shown in one list newest first`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = listOf(
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
            ).atOneFollowedPlace().asRight()

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = listOf("krisinformation-1", "smhi-2", "smhi-1"), actual = shownIdentifiers())
            assertEquals(expected = false, actual = warningViewModel.state.value.isLoading)
            assertEquals(expected = false, actual = warningViewModel.state.value.isEmpty)
        }

    @Test
    fun `no warnings is told apart from a failed load`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = emptyList<AggregatedFeed>().atOneFollowedPlace().asRight()
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            assertTrue(actual = warningViewModel.state.value.isEmpty)

            observeFollowedPlaceWarningsTask.results.value = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            warningViewModel.onEvent(event = WarningEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = Res.string.no_network_available, actual = warningViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = warningViewModel.state.value.isRefreshing)

            warningViewModel.onEvent(event = WarningEvent.OnErrorShown)
            assertEquals(expected = null, actual = warningViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `choosing another place shows its warnings without loading again`() =
        testScope.runTest {
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            observeFollowedPlaceWarningsTask.results.value = listOf(aggregatedFeed(identifier = "smhi-9-1")).atOneFollowedPlace().asRight()
            advanceUntilIdle()

            assertEquals(expected = listOf("smhi-9-1"), actual = shownIdentifiers())
            assertEquals(expected = listOf(false), actual = observeFollowedPlaceWarningsTask.refreshes)
        }

    @Test
    fun `with more than one followed place each warning names the places it affects`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = FollowedPlaceWarnings(
                followedPlaces = listOf(gothenburg, cabin),
                placeWarnings = listOf(
                    PlaceWarning(
                        aggregatedFeed = aggregatedFeed(identifier = "smhi-1-1"),
                        followedPlaces = listOf(gothenburg, cabin)
                    )
                )
            ).asRight()

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = "Göteborg, Stugan", actual = warningViewModel.state.value.warnings.single().followedPlaceNames)
        }

    @Test
    fun `with one followed place the warnings do not name it`() =
        testScope.runTest {
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = listOf(null, null), actual = warningViewModel.state.value.warnings.map { warningItem: WarningItem -> warningItem.followedPlaceNames })
        }

    @Test
    fun `a place without a name is left out of the named places`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = FollowedPlaceWarnings(
                followedPlaces = listOf(gothenburg, cabin),
                placeWarnings = listOf(
                    PlaceWarning(
                        aggregatedFeed = aggregatedFeed(identifier = "smhi-1-1"),
                        followedPlaces = listOf(gothenburg.copy(name = null))
                    )
                )
            ).asRight()

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = null, actual = warningViewModel.state.value.warnings.single().followedPlaceNames)
        }

    @Test
    fun `the followed places are named above their warnings`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = FollowedPlaceWarnings(
                followedPlaces = listOf(gothenburg, cabin),
                placeWarnings = emptyList()
            ).asRight()

            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()

            assertEquals(expected = "Göteborg, Stugan", actual = warningViewModel.state.value.followedPlaceNames)
        }

    @Test
    fun `a refresh skips the cache`() =
        testScope.runTest {
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            warningViewModel.onEvent(event = WarningEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = listOf(false, true), actual = observeFollowedPlaceWarningsTask.refreshes)
        }

    @Test
    fun `a new language tells the times again without loading`() =
        testScope.runTest {
            observeFollowedPlaceWarningsTask.results.value = listOf(
                aggregatedFeed(
                    identifier = "smhi-1",
                    endsAt = Instant.parse(input = "2026-10-24T08:30:00Z") + 48.hours
                )
            ).atOneFollowedPlace().asRight()
            warningViewModel.onEvent(event = WarningEvent.OnLocaleChanged(languageTag = "en"))
            warningViewModel.onEvent(event = WarningEvent.OnStart)
            advanceUntilIdle()
            val englishText: String? = warningViewModel.state.value.warnings.single().endsAtText

            warningViewModel.onEvent(event = WarningEvent.OnLocaleChanged(languageTag = "sv-SE"))
            advanceUntilIdle()
            val swedishText: String? = warningViewModel.state.value.warnings.single().endsAtText

            assertEquals(expected = true, actual = englishText?.startsWith(prefix = "Mon"))
            assertEquals(expected = true, actual = swedishText?.startsWith(prefix = "mån"))
            assertEquals(expected = listOf(false), actual = observeFollowedPlaceWarningsTask.refreshes)
        }
}