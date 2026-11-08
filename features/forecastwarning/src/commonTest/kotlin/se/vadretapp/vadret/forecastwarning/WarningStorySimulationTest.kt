package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.extension.successOrNull
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
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedGetAuroraHoursTask
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedGetWarningsTask
import se.vadretapp.vadret.forecastwarning.simulation.SimulatedMarkWarningSeenTask
import se.vadretapp.vadret.forecastwarning.simulation.simulatedWarnings
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.testing.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WarningStorySimulationTest {

    private val now: Instant = Instant.parse(input = "2026-11-08T19:00:00Z")
    private val clock = FixedClock(now = now)
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `there is a simulated warning of every kind the app shows and each is told apart`() {
        val warnings: List<AggregatedFeed> = simulatedWarnings(now = now)

        assertEquals(
            expected = listOf(
                FeedSource.VADRET_API to AuroraChance.HIGH,
                FeedSource.VADRET_API to AuroraChance.POSSIBLE,
                FeedSource.VADRET_API to null,
                FeedSource.SMHI to null,
                FeedSource.SMHI to null,
                FeedSource.SMHI to null,
                FeedSource.KRISINFORMATION to null
            ),
            actual = warnings.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.source to aggregatedFeed.aurora?.chance }
        )
        assertEquals(
            expected = listOf(WarningLevel.YELLOW, WarningLevel.ORANGE, WarningLevel.RED),
            actual = warnings.mapNotNull { aggregatedFeed: AggregatedFeed -> aggregatedFeed.warningLevel }
        )
        assertEquals(expected = warnings.size, actual = warnings.map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier }.distinct().size)
        assertTrue(actual = warnings.single { aggregatedFeed: AggregatedFeed -> aggregatedFeed.source == FeedSource.KRISINFORMATION }.isPublicAlert)
        assertTrue(actual = warnings.single { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier == "simulated-slipperiness" }.slipperiness != null)
    }

    @Test
    fun `the simulated warnings are timed around now so they read as current`() {
        val warnings: List<AggregatedFeed> = simulatedWarnings(now = now)

        assertTrue(actual = warnings.all { aggregatedFeed: AggregatedFeed -> aggregatedFeed.publishedAt < now })
        assertTrue(actual = warnings.all { aggregatedFeed: AggregatedFeed -> aggregatedFeed.isGoingOnAfter(now = now) })
    }

    @Test
    fun `a simulated aurora night rises to its score at its peak and falls away on both sides`() =
        testScope.runTest {
            val aurora: Aurora = assertNotNull(actual = simulatedWarnings(now = now).first().aurora)

            val auroraHours: List<AuroraHour>? = SimulatedGetAuroraHoursTask()(aurora = aurora).successOrNull

            assertEquals(
                expected = listOf(42, 54, 66, 78, 66, 54, 42) to (now + 3.hours),
                actual = auroraHours?.map { auroraHour: AuroraHour -> auroraHour.score } to auroraHours?.maxBy { auroraHour: AuroraHour -> auroraHour.score }?.time
            )
        }

    @Test
    fun `a simulated aurora night never falls below no chance and has no hours without a peak`() =
        testScope.runTest {
            val possibleAurora: Aurora = assertNotNull(actual = simulatedWarnings(now = now)[1].aurora)

            val scores: List<Int>? = SimulatedGetAuroraHoursTask()(aurora = possibleAurora).successOrNull?.map { auroraHour: AuroraHour -> auroraHour.score }
            val withoutPeak: List<AuroraHour>? = SimulatedGetAuroraHoursTask()(aurora = possibleAurora.copy(peakAt = null)).successOrNull

            assertEquals(
                expected = listOf(0, 10, 22, 34, 22, 10, 0) to emptyList(),
                actual = scores to withoutPeak
            )
        }

    @Test
    fun `seeing a simulated story stores nothing`() =
        testScope.runTest {
            assertEquals(
                expected = Unit,
                actual = SimulatedMarkWarningSeenTask()(warningIdentifier = "simulated-aurora-high").successOrNull
            )
        }

    @Test
    fun `the developer list shows every simulated warning once it knows the language and plays them all in order`() =
        testScope.runTest {
            val warningStorySimulationViewModel = WarningStorySimulationViewModel(
                getWarningsTask = SimulatedGetWarningsTask(clock = clock),
                clock = clock,
                coroutineDispatcher = testDispatcher
            )
            advanceUntilIdle()
            val beforeLanguage: List<String> = warningStorySimulationViewModel.state.value.warningIdentifiers

            warningStorySimulationViewModel.onEvent(
                event = WarningStorySimulationEvent.OnLocaleChanged(
                    languageTag = "sv-SE",
                    hourFormat = HourFormat.TWENTY_FOUR_HOUR
                )
            )
            advanceUntilIdle()

            assertEquals(
                expected = emptyList<String>() to simulatedWarnings(now = now).map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier },
                actual = beforeLanguage to warningStorySimulationViewModel.state.value.warningIdentifiers
            )
        }

    @Test
    fun `a simulated story is the real story of the simulated warnings with their aurora hours`() =
        testScope.runTest {
            val warningStoryViewModel = WarningStoryViewModel(
                getWarningsTask = SimulatedGetWarningsTask(clock = clock),
                getAuroraHoursTask = SimulatedGetAuroraHoursTask(),
                markWarningSeenTask = SimulatedMarkWarningSeenTask(),
                clock = clock,
                coroutineDispatcher = testDispatcher
            )

            warningStoryViewModel.onEvent(event = WarningStoryEvent.OnStart(warningIdentifiers = listOf("simulated-aurora-high", "simulated-smhi-red")))
            advanceUntilIdle()

            assertEquals(
                expected = listOf<Pair<AuroraChance?, WarningLevel?>>(AuroraChance.HIGH to null, null to WarningLevel.RED),
                actual = warningStoryViewModel.state.value.warnings.map { warningItem: WarningItem -> warningItem.auroraChance to warningItem.warningLevel }
            )
        }
}

private fun AggregatedFeed.isGoingOnAfter(now: Instant): Boolean {
    val shownEndsAt: Instant = endsAt ?: return true
    return shownEndsAt > now
}