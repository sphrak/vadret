package se.vadretapp.vadret.pollenpreference

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
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
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.pollenpreference.local.DefaultPollenPreferenceLocalDataSource
import se.vadretapp.vadret.pollenpreference.local.PollenPreferenceLocalDataSource
import se.vadretapp.vadret.pollenpreference.local.PollenPreferencesEntity
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private val defaultPollenPreferences = PollenPreferences(
    isPollenEnabled = true,
    allergenPollenIds = emptySet(),
    isHibernationNoticeShown = false,
    isEndOfSeasonSimulated = false
)

private class FailingPollenPreferenceLocalDataSource(
    private val exception: Exception
) : PollenPreferenceLocalDataSource {
    override suspend fun getPollenPreferences(): PollenPreferencesEntity = throw exception
    override fun observePollenPreferences(): Flow<PollenPreferencesEntity> = flow { throw exception }
    override suspend fun setPollenEnabled(value: Boolean) = throw exception
    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ) = throw exception
    override suspend fun setHibernationNoticeShown(value: Boolean) = throw exception
    override suspend fun setEndOfSeasonSimulated(value: Boolean) = throw exception
}

class DefaultPollenPreferenceRepositoryTest {

    private fun TestScope.pollenPreferenceRepository(dataStore: DataStore<Preferences> = testPreferencesDataStore(scope = backgroundScope)): PollenPreferenceRepository =
        DefaultPollenPreferenceRepository(
            pollenPreferenceLocalDataSource = DefaultPollenPreferenceLocalDataSource(dataStore = dataStore)
        )

    @Test
    fun `nothing stored shows pollen without allergens or the hibernation notice`() =
        runTest {
            assertEquals(
                expected = defaultPollenPreferences.asRight(),
                actual = pollenPreferenceRepository().getPollenPreferences()
            )
        }

    @Test
    fun `each preference reads back as it was set without touching the others`() =
        runTest {
            val pollenPreferenceRepository: PollenPreferenceRepository = pollenPreferenceRepository()

            pollenPreferenceRepository.setPollenEnabled(isEnabled = false)
            val pollenChanged: Either<Failure, PollenPreferences> = pollenPreferenceRepository.getPollenPreferences()
            pollenPreferenceRepository.setPollenEnabled(isEnabled = true)
            listOf("birch", "grass").forEach { pollenId: String ->
                pollenPreferenceRepository.setAllergen(
                    pollenId = pollenId,
                    isAllergen = true
                )
            }
            val allergensChanged: Either<Failure, PollenPreferences> = pollenPreferenceRepository.getPollenPreferences()
            listOf("birch", "grass").forEach { pollenId: String ->
                pollenPreferenceRepository.setAllergen(
                    pollenId = pollenId,
                    isAllergen = false
                )
            }
            pollenPreferenceRepository.setHibernationNoticeShown(isShown = true)
            val noticeChanged: Either<Failure, PollenPreferences> = pollenPreferenceRepository.getPollenPreferences()
            pollenPreferenceRepository.setHibernationNoticeShown(isShown = false)
            pollenPreferenceRepository.setEndOfSeasonSimulated(isSimulated = true)
            val simulationChanged: Either<Failure, PollenPreferences> = pollenPreferenceRepository.getPollenPreferences()

            assertEquals(
                expected = listOf(
                    defaultPollenPreferences.copy(isPollenEnabled = false).asRight(),
                    defaultPollenPreferences.copy(allergenPollenIds = setOf("birch", "grass")).asRight(),
                    defaultPollenPreferences.copy(isHibernationNoticeShown = true).asRight(),
                    defaultPollenPreferences.copy(isEndOfSeasonSimulated = true).asRight()
                ),
                actual = listOf(
                    pollenChanged,
                    allergensChanged,
                    noticeChanged,
                    simulationChanged
                )
            )
        }

    @Test
    fun `marking an allergen twice keeps it once and unmarking one that is not marked changes nothing`() =
        runTest {
            val pollenPreferenceRepository: PollenPreferenceRepository = pollenPreferenceRepository()

            List(size = 2) {
                pollenPreferenceRepository.setAllergen(
                    pollenId = "birch",
                    isAllergen = true
                )
            }
            pollenPreferenceRepository.setAllergen(
                pollenId = "grass",
                isAllergen = false
            )

            assertEquals(
                expected = defaultPollenPreferences.copy(allergenPollenIds = setOf("birch")).asRight(),
                actual = pollenPreferenceRepository.getPollenPreferences()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored preferences and then each change`() =
        runTest {
            val pollenPreferenceRepository: PollenPreferenceRepository = pollenPreferenceRepository()
            val emitted = mutableListOf<Either<Failure, PollenPreferences>>()
            val job = launch {
                pollenPreferenceRepository.observePollenPreferences().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            pollenPreferenceRepository.setPollenEnabled(isEnabled = false)
            advanceUntilIdle()
            pollenPreferenceRepository.setPollenEnabled(isEnabled = false)
            advanceUntilIdle()
            pollenPreferenceRepository.setHibernationNoticeShown(isShown = true)
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, PollenPreferences>>>(
                expected = listOf(
                    defaultPollenPreferences.asRight(),
                    defaultPollenPreferences.copy(isPollenEnabled = false).asRight(),
                    defaultPollenPreferences.copy(
                        isPollenEnabled = false,
                        isHibernationNoticeShown = true
                    ).asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val pollenPreferenceRepository: PollenPreferenceRepository = DefaultPollenPreferenceRepository(
                pollenPreferenceLocalDataSource = FailingPollenPreferenceLocalDataSource(
                    exception = IOException(
                        "disk full",
                        null
                    )
                )
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    pollenPreferenceRepository.getPollenPreferences(),
                    pollenPreferenceRepository.observePollenPreferences().toList().single(),
                    pollenPreferenceRepository.setPollenEnabled(isEnabled = true),
                    pollenPreferenceRepository.setAllergen(
                        pollenId = "birch",
                        isAllergen = true
                    ),
                    pollenPreferenceRepository.setHibernationNoticeShown(isShown = true),
                    pollenPreferenceRepository.setEndOfSeasonSimulated(isSimulated = true)
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.pollenpreference" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val pollenPreferenceRepository: PollenPreferenceRepository = DefaultPollenPreferenceRepository(
                pollenPreferenceLocalDataSource = FailingPollenPreferenceLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = pollenPreferenceRepository.getPollenPreferences()
                )
                assertEquals(
                    expected = LogLevel.ERROR,
                    actual = recordingLogSink.lines.single().level
                )
            }
        }
}