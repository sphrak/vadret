package se.vadretapp.vadret.language

import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
import se.vadretapp.vadret.language.local.DefaultLanguageLocalDataSource
import se.vadretapp.vadret.language.local.LanguageLocalDataSource
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private class FailingLanguageLocalDataSource(
    private val exception: Exception
) : LanguageLocalDataSource {
    override suspend fun getLanguage(): String? = throw exception
    override fun observeLanguage(): Flow<String?> = flow { throw exception }
    override suspend fun setLanguage(value: String) = throw exception
}

class DefaultLanguageRepositoryTest {

    private fun TestScope.languageRepository(): Pair<LanguageRepository, LanguageLocalDataSource> {
        val languageLocalDataSource = DefaultLanguageLocalDataSource(
            dataStore = testPreferencesDataStore(scope = backgroundScope)
        )
        return DefaultLanguageRepository(languageLocalDataSource = languageLocalDataSource) to languageLocalDataSource
    }

    @Test
    fun `nothing stored follows the system language`() =
        runTest {
            val (languageRepository: LanguageRepository, _) = languageRepository()

            assertEquals(
                expected = AppLanguage.SYSTEM.asRight(),
                actual = languageRepository.getLanguage()
            )
        }

    @Test
    fun `every language reads back as it was set`() =
        runTest {
            val (languageRepository: LanguageRepository, _) = languageRepository()

            val languages: List<Either<Failure, AppLanguage>> = AppLanguage.entries.map { language: AppLanguage ->
                languageRepository.setLanguage(language = language)
                languageRepository.getLanguage()
            }

            assertEquals(
                expected = AppLanguage.entries.map { language: AppLanguage -> language.asRight() },
                actual = languages
            )
        }

    @Test
    fun `an unknown stored value follows the system language`() =
        runTest {
            val dataStore = testPreferencesDataStore(scope = backgroundScope)
            dataStore.edit { preferences -> preferences[stringPreferencesKey(name = "language")] = "Klingon" }
            val languageRepository: LanguageRepository = DefaultLanguageRepository(
                languageLocalDataSource = DefaultLanguageLocalDataSource(dataStore = dataStore)
            )

            assertEquals(
                expected = AppLanguage.SYSTEM.asRight(),
                actual = languageRepository.getLanguage()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored language and then each change`() =
        runTest {
            val (languageRepository: LanguageRepository, _) = languageRepository()
            val emitted = mutableListOf<Either<Failure, AppLanguage>>()
            val job = launch {
                languageRepository.observeLanguage().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            languageRepository.setLanguage(language = AppLanguage.SWEDISH)
            advanceUntilIdle()
            languageRepository.setLanguage(language = AppLanguage.SWEDISH)
            advanceUntilIdle()
            languageRepository.setLanguage(language = AppLanguage.POLISH)
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, AppLanguage>>>(
                expected = listOf(
                    AppLanguage.SYSTEM.asRight(),
                    AppLanguage.SWEDISH.asRight(),
                    AppLanguage.POLISH.asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val languageRepository: LanguageRepository = DefaultLanguageRepository(
                languageLocalDataSource = FailingLanguageLocalDataSource(exception = IOException("disk full", null))
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    languageRepository.getLanguage(),
                    languageRepository.setLanguage(language = AppLanguage.SWEDISH),
                    languageRepository.observeLanguage().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.language" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val languageRepository: LanguageRepository = DefaultLanguageRepository(
                languageLocalDataSource = FailingLanguageLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = languageRepository.getLanguage()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }
}