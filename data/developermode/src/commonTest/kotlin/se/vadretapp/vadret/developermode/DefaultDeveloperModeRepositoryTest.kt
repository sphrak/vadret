package se.vadretapp.vadret.developermode

import androidx.datastore.core.IOException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.local.DefaultDeveloperModeLocalDataSource
import se.vadretapp.vadret.developermode.local.DeveloperModeLocalDataSource
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.IOException as FileIOException

private class FailingDeveloperModeLocalDataSource(
    private val exception: Exception
) : DeveloperModeLocalDataSource {
    override fun observeIsDeveloperModeUnlocked(): Flow<Boolean> = flow { throw exception }
    override suspend fun unlockDeveloperMode() = throw exception
}

class DefaultDeveloperModeRepositoryTest {

    @Test
    fun `developer mode is locked until it is unlocked`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DeveloperModeRepository(dataStore = testPreferencesDataStore(scope = backgroundScope))

            val before: Either<Failure, Boolean> = developerModeRepository.observeIsDeveloperModeUnlocked().first()
            developerModeRepository.unlockDeveloperMode()

            assertEquals(
                expected = listOf(false.asRight(), true.asRight()),
                actual = listOf(before, developerModeRepository.observeIsDeveloperModeUnlocked().first())
            )
        }

    @Test
    fun `an unlock is kept by the next repository on the same store`() =
        runTest {
            val dataStore = testPreferencesDataStore(scope = backgroundScope)
            DefaultDeveloperModeRepository(developerModeLocalDataSource = DefaultDeveloperModeLocalDataSource(dataStore = dataStore)).unlockDeveloperMode()

            assertEquals(
                expected = true.asRight(),
                actual = DeveloperModeRepository(dataStore = dataStore).observeIsDeveloperModeUnlocked().first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `unlocking again emits nothing new`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DeveloperModeRepository(dataStore = testPreferencesDataStore(scope = backgroundScope))
            val emitted = mutableListOf<Either<Failure, Boolean>>()
            val job = launch {
                developerModeRepository.observeIsDeveloperModeUnlocked().take(count = 2).toList(destination = emitted)
            }
            advanceUntilIdle()

            developerModeRepository.unlockDeveloperMode()
            advanceUntilIdle()
            developerModeRepository.unlockDeveloperMode()
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, Boolean>>>(
                expected = listOf(false.asRight(), true.asRight()),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DefaultDeveloperModeRepository(
                developerModeLocalDataSource = FailingDeveloperModeLocalDataSource(exception = IOException("disk full", null))
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    developerModeRepository.unlockDeveloperMode(),
                    developerModeRepository.observeIsDeveloperModeUnlocked().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.developermode" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DefaultDeveloperModeRepository(
                developerModeLocalDataSource = FailingDeveloperModeLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = developerModeRepository.unlockDeveloperMode()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `a file that cannot be read is a storage failure`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DefaultDeveloperModeRepository(
                developerModeLocalDataSource = FailingDeveloperModeLocalDataSource(exception = FileIOException("disk full"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
                    actual = developerModeRepository.unlockDeveloperMode()
                )
                assertEquals(expected = LogLevel.WARN, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `cancellation is rethrown`() =
        runTest {
            val developerModeRepository: DeveloperModeRepository = DefaultDeveloperModeRepository(
                developerModeLocalDataSource = FailingDeveloperModeLocalDataSource(exception = CancellationException("cancelled"))
            )

            assertFailsWith<CancellationException> {
                developerModeRepository.unlockDeveloperMode()
            }
        }
}