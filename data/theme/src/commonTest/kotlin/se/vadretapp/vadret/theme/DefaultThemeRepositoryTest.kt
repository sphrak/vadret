package se.vadretapp.vadret.theme

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
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.theme.local.DefaultThemeLocalDataSource
import se.vadretapp.vadret.theme.local.ThemeLocalDataSource
import se.vadretapp.vadret.theme.model.Theme
import kotlin.test.Test
import kotlin.test.assertEquals

private class FailingThemeLocalDataSource(
    private val exception: Exception
) : ThemeLocalDataSource {
    override suspend fun getTheme(): String? = throw exception
    override fun observeTheme(): Flow<String?> = flow { throw exception }
    override suspend fun setTheme(value: String) = throw exception
}

class DefaultThemeRepositoryTest {

    private fun TestScope.themeRepository(): Pair<ThemeRepository, ThemeLocalDataSource> {
        val themeLocalDataSource = DefaultThemeLocalDataSource(
            dataStore = testPreferencesDataStore(scope = backgroundScope)
        )
        return DefaultThemeRepository(themeLocalDataSource = themeLocalDataSource) to themeLocalDataSource
    }

    @Test
    fun `nothing stored is the light theme`() =
        runTest {
            val (themeRepository: ThemeRepository, _) = themeRepository()

            assertEquals(
                expected = Theme.LIGHT.asRight(),
                actual = themeRepository.getTheme()
            )
        }

    @Test
    fun `every theme reads back as it was set`() =
        runTest {
            val (themeRepository: ThemeRepository, _) = themeRepository()

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
    fun `an unknown stored value is the light theme`() =
        runTest {
            val dataStore = testPreferencesDataStore(scope = backgroundScope)
            dataStore.edit { preferences -> preferences[stringPreferencesKey(name = "theme")] = "Light (No background)" }
            val themeRepository: ThemeRepository = DefaultThemeRepository(
                themeLocalDataSource = DefaultThemeLocalDataSource(dataStore = dataStore)
            )

            assertEquals(
                expected = Theme.LIGHT.asRight(),
                actual = themeRepository.getTheme()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored theme and then each change`() =
        runTest {
            val (themeRepository: ThemeRepository, _) = themeRepository()
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
                    Theme.LIGHT.asRight(),
                    Theme.DARK.asRight(),
                    Theme.AMOLED.asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val themeRepository: ThemeRepository = DefaultThemeRepository(
                themeLocalDataSource = FailingThemeLocalDataSource(exception = IOException("disk full", null))
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
                    expected = List(size = results.size) { "se.vadretapp.vadret.theme" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        runTest {
            val themeRepository: ThemeRepository = DefaultThemeRepository(
                themeLocalDataSource = FailingThemeLocalDataSource(exception = IllegalStateException("bug"))
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