package se.vadretapp.vadret.forecastpreference

import androidx.datastore.core.IOException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.local.DefaultForecastPreferenceLocalDataSource
import se.vadretapp.vadret.forecastpreference.local.ForecastPreferenceLocalDataSource
import se.vadretapp.vadret.forecastpreference.local.ForecastPreferencesEntity
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private val defaultForecastPreferences = ForecastPreferences(
    isHourlyForecastEnabled = false,
    isExpandOnceEnabled = true
)

private class FailingForecastPreferenceLocalDataSource(
    private val exception: Exception
) : ForecastPreferenceLocalDataSource {
    override suspend fun getForecastPreferences(): ForecastPreferencesEntity = throw exception
    override fun observeForecastPreferences(): Flow<ForecastPreferencesEntity> = flow { throw exception }
    override suspend fun setHourlyForecastEnabled(value: Boolean) = throw exception
    override suspend fun setExpandOnceEnabled(value: Boolean) = throw exception
}

class DefaultForecastPreferenceRepositoryTest {

    private fun TestScope.forecastPreferenceRepository(): ForecastPreferenceRepository =
        DefaultForecastPreferenceRepository(
            forecastPreferenceLocalDataSource = DefaultForecastPreferenceLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
        )

    @Test
    fun `nothing stored hides the hourly forecast and expands one day at a time`() =
        runTest {
            assertEquals(
                expected = defaultForecastPreferences.asRight(),
                actual = forecastPreferenceRepository().getForecastPreferences()
            )
        }

    @Test
    fun `each preference reads back as it was set without touching the other`() =
        runTest {
            val forecastPreferenceRepository: ForecastPreferenceRepository = forecastPreferenceRepository()

            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            val hourlyChanged: Either<Failure, ForecastPreferences> = forecastPreferenceRepository.getForecastPreferences()
            forecastPreferenceRepository.setExpandOnceEnabled(isEnabled = false)
            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = false)
            val expandOnceChanged: Either<Failure, ForecastPreferences> = forecastPreferenceRepository.getForecastPreferences()

            assertEquals(
                expected = listOf(
                    defaultForecastPreferences.copy(isHourlyForecastEnabled = true).asRight(),
                    defaultForecastPreferences.copy(isExpandOnceEnabled = false).asRight()
                ),
                actual = listOf(hourlyChanged, expandOnceChanged)
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored preferences and then each change`() =
        runTest {
            val forecastPreferenceRepository: ForecastPreferenceRepository = forecastPreferenceRepository()
            val emitted = mutableListOf<Either<Failure, ForecastPreferences>>()
            val job = launch {
                forecastPreferenceRepository.observeForecastPreferences().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            advanceUntilIdle()
            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            advanceUntilIdle()
            forecastPreferenceRepository.setExpandOnceEnabled(isEnabled = false)
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, ForecastPreferences>>>(
                expected = listOf(
                    defaultForecastPreferences.asRight(),
                    defaultForecastPreferences.copy(isHourlyForecastEnabled = true).asRight(),
                    ForecastPreferences(
                        isHourlyForecastEnabled = true,
                        isExpandOnceEnabled = false
                    ).asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val forecastPreferenceRepository: ForecastPreferenceRepository = DefaultForecastPreferenceRepository(
                forecastPreferenceLocalDataSource = FailingForecastPreferenceLocalDataSource(exception = IOException("disk full", null))
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    forecastPreferenceRepository.getForecastPreferences(),
                    forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true),
                    forecastPreferenceRepository.setExpandOnceEnabled(isEnabled = true),
                    forecastPreferenceRepository.observeForecastPreferences().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.forecastpreference" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val forecastPreferenceRepository: ForecastPreferenceRepository = DefaultForecastPreferenceRepository(
                forecastPreferenceLocalDataSource = FailingForecastPreferenceLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = forecastPreferenceRepository.getForecastPreferences()
                )
                assertEquals(
                    expected = LogLevel.ERROR,
                    actual = recordingLogSink.lines.single().level
                )
            }
        }
}