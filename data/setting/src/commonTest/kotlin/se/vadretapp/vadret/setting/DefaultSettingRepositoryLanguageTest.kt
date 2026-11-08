package se.vadretapp.vadret.setting

import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositoryLanguageTest {

    private fun TestScope.settingRepository(): SettingRepository =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
        )

    @Test
    fun `nothing stored follows the system language`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            assertEquals(
                expected = AppLanguage.SYSTEM.asRight(),
                actual = settingRepository.getLanguage()
            )
        }

    @Test
    fun `every language reads back as it was set`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            val languages: List<Either<Failure, AppLanguage>> = AppLanguage.entries.map { language: AppLanguage ->
                settingRepository.setLanguage(language = language)
                settingRepository.getLanguage()
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
            val settingRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = dataStore),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            assertEquals(
                expected = AppLanguage.SYSTEM.asRight(),
                actual = settingRepository.getLanguage()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored language and then each change`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()
            val emitted = mutableListOf<Either<Failure, AppLanguage>>()
            val job = launch {
                settingRepository.observeLanguage().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            settingRepository.setLanguage(language = AppLanguage.SWEDISH)
            advanceUntilIdle()
            settingRepository.setLanguage(language = AppLanguage.SWEDISH)
            advanceUntilIdle()
            settingRepository.setLanguage(language = AppLanguage.POLISH)
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
            val settingRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IOException("disk full", null)),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    settingRepository.getLanguage(),
                    settingRepository.setLanguage(language = AppLanguage.SWEDISH),
                    settingRepository.observeLanguage().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.setting" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val settingRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IllegalStateException("bug")),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = settingRepository.getLanguage()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }
}