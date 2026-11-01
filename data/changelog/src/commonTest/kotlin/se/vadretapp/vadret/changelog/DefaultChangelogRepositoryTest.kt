package se.vadretapp.vadret.changelog

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultChangelogRepositoryTest {

    @Test
    fun `the changelog is read as it is written`() =
        runTest {
            val markdown = "# Changelog\n\n## [3.1.0] -- 2026-10-26\n\n- New 'Coming days' view ☀️\n"
            val changelogRepository: ChangelogRepository = ChangelogRepository(
                readChangelog = { markdown },
                coroutineDispatcher = StandardTestDispatcher(scheduler = testScheduler)
            )

            assertEquals<Either<Failure, Changelog>>(
                expected = Changelog(markdown = markdown).asRight(),
                actual = changelogRepository.getChangelog()
            )
        }

    @Test
    fun `a changelog that cannot be read is a known failure`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Changelog>> = listOf(
                    IOException("closed"),
                    IOException("missing")
                ).map { exception: Exception ->
                    ChangelogRepository(
                        readChangelog = { throw exception },
                        coroutineDispatcher = StandardTestDispatcher(scheduler = testScheduler)
                    ).getChangelog()
                }

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.FILE_READ_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `an unexpected failure is logged as an error`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val result: Either<Failure, Changelog> = ChangelogRepository(
                    readChangelog = { throw IllegalStateException("broken") },
                    coroutineDispatcher = StandardTestDispatcher(scheduler = testScheduler)
                ).getChangelog()

                assertEquals(
                    expected = Failure(errorCode = ErrorCode.UNKNOWN).asLeft(),
                    actual = result
                )
                assertEquals(
                    expected = listOf(LogLevel.ERROR),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }
}