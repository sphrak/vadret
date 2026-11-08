package se.vadretapp.vadret.aggregatedfeed

import androidx.sqlite.SQLiteException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import se.vadretapp.vadret.aggregatedfeed.local.DefaultSeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.local.inMemoryAggregatedFeedDatabase
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val now: Instant = Instant.parse(input = "2026-10-28T10:00:00Z")

class SeenWarningTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val clock = FixedClock(now = now)
    private val aggregatedFeedDatabase: AggregatedFeedDatabase = inMemoryAggregatedFeedDatabase(
        coroutineDispatcher = StandardTestDispatcher(scheduler = testScope.testScheduler)
    )
    private val seenWarningRepository: SeenWarningRepository = seenWarningRepository(
        seenWarningLocalDataSource = DefaultSeenWarningLocalDataSource(aggregatedFeedDatabase = aggregatedFeedDatabase)
    )

    private fun seenWarningRepository(seenWarningLocalDataSource: SeenWarningLocalDataSource): SeenWarningRepository =
        DefaultSeenWarningRepository(
            seenWarningLocalDataSource = seenWarningLocalDataSource,
            clock = clock
        )

    @AfterTest
    fun tearDown() {
        aggregatedFeedDatabase.close()
    }

    @Test
    fun `a warning is marked seen at the current time`() =
        testScope.runTest {
            assertEquals(expected = Unit.asRight(), actual = seenWarningRepository.markSeen(warningIdentifier = "smhi-1-2"))

            assertEquals(
                expected = listOf(
                    SeenWarning(
                        warningIdentifier = "smhi-1-2",
                        seenAt = now
                    )
                ).asRight(),
                actual = seenWarningRepository.observeSeenWarnings().first()
            )
        }

    @Test
    fun `seeing a warning again moves its seen time forward`() =
        testScope.runTest {
            seenWarningRepository.markSeen(warningIdentifier = "smhi-1-2")
            clock.now = now + 3.minutes
            seenWarningRepository.markSeen(warningIdentifier = "smhi-1-2")

            assertEquals(
                expected = listOf(
                    SeenWarning(
                        warningIdentifier = "smhi-1-2",
                        seenAt = now + 3.minutes
                    )
                ).asRight(),
                actual = seenWarningRepository.observeSeenWarnings().first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the seen warnings and then each change`() =
        testScope.runTest {
            val emitted = mutableListOf<Either<Failure, List<SeenWarning>>>()
            val job = launch {
                seenWarningRepository.observeSeenWarnings().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            seenWarningRepository.markSeen(warningIdentifier = "a")
            advanceUntilIdle()
            clock.now = now + 1.minutes
            seenWarningRepository.markSeen(warningIdentifier = "b")
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, List<SeenWarning>>>>(
                expected = listOf(
                    emptyList<SeenWarning>().asRight(),
                    listOf(
                        SeenWarning(
                            warningIdentifier = "a",
                            seenAt = now
                        )
                    ).asRight(),
                    listOf(
                        SeenWarning(
                            warningIdentifier = "b",
                            seenAt = now + 1.minutes
                        ),
                        SeenWarning(
                            warningIdentifier = "a",
                            seenAt = now
                        )
                    ).asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        testScope.runTest {
            val failingSeenWarningRepository: SeenWarningRepository = seenWarningRepository(
                seenWarningLocalDataSource = FailingSeenWarningLocalDataSource(exception = SQLiteException("disk full"))
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    failingSeenWarningRepository.markSeen(warningIdentifier = "a"),
                    failingSeenWarningRepository.observeSeenWarnings().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.aggregatedfeed" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        testScope.runTest {
            val failingSeenWarningRepository: SeenWarningRepository = seenWarningRepository(
                seenWarningLocalDataSource = FailingSeenWarningLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = failingSeenWarningRepository.markSeen(warningIdentifier = "a")
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `cancellation is rethrown`() =
        testScope.runTest {
            val failingSeenWarningRepository: SeenWarningRepository = seenWarningRepository(
                seenWarningLocalDataSource = FailingSeenWarningLocalDataSource(exception = CancellationException("cancelled"))
            )

            assertFailsWith<CancellationException> {
                failingSeenWarningRepository.markSeen(warningIdentifier = "a")
            }
        }
}