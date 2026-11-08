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
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositoryThemeTest {

    private fun TestScope.themeRepository(): Pair<SettingRepository, Unit> =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
        ) to Unit

    @Test
    fun `nothing stored follows the system`() =
        runTest {
            val (themeRepository: SettingRepository, _) = themeRepository()

            assertEquals(
                expected = Theme.SYSTEM.asRight(),
                actual = themeRepository.getTheme()
            )
        }

    @Test
    fun `every theme reads back as it was set`() =
        runTest {
            val (themeRepository: SettingRepository, _) = themeRepository()

            val themes: List<Either<Failure, Theme>> = Theme.entries.map { theme: Theme ->
                themeRepository.setTheme(theme = theme)
                themeRepository.getTheme()
            }

            assertEquals(
                expected = Theme.entries.map { theme: Theme -> theme.asRight() },
                actual = themes
            )
        }

    @Test
    fun `an unknown stored value follows the system`() =
        runTest {
            val dataStore = testPreferencesDataStore(scope = backgroundScope)
            dataStore.edit { preferences -> preferences[stringPreferencesKey(name = "theme")] = "Light (No background)" }
            val themeRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = dataStore),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            assertEquals(
                expected = Theme.SYSTEM.asRight(),
                actual = themeRepository.getTheme()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored theme and then each change`() =
        runTest {
            val (themeRepository: SettingRepository, _) = themeRepository()
            val emitted = mutableListOf<Either<Failure, Theme>>()
            val job = launch {
                themeRepository.observeTheme().take(count = 3).toList(destination = emitted)
            }
            advanceUntilIdle()

            themeRepository.setTheme(theme = Theme.DARK)
            advanceUntilIdle()
            themeRepository.setTheme(theme = Theme.DARK)
            advanceUntilIdle()
            themeRepository.setTheme(theme = Theme.AMOLED)
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, Theme>>>(
                expected = listOf(
                    Theme.SYSTEM.asRight(),
                    Theme.DARK.asRight(),
                    Theme.AMOLED.asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val themeRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IOException("disk full", null)),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    themeRepository.getTheme(),
                    themeRepository.setTheme(theme = Theme.DARK),
                    themeRepository.observeTheme().toList().single()
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
            val themeRepository: SettingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IllegalStateException("bug")),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = themeRepository.getTheme()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }
}