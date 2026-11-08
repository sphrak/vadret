package se.vadretapp.vadret.setting

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.setting.model.SystemSettings
import se.vadretapp.vadret.setting.model.SystemSettingsEntity
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositorySystemSettingsTest {

    private val systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()

    private fun TestScope.settingRepository(): SettingRepository =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = systemSettingLocalDataSource
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the clock and language of the phone are read and followed while they change`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()
            val followed = mutableListOf<Either<Failure, SystemSettings>>()
            val job = launch { settingRepository.observeSystemSettings().toList(destination = followed) }
            advanceUntilIdle()

            systemSettingLocalDataSource.systemSettingsEntity.value = SystemSettingsEntity(
                isTwentyFourHour = false,
                languageTag = "en-US"
            )
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(
                    SystemSettings(isTwentyFourHour = true, languageTag = "sv-SE").asRight(),
                    SystemSettings(isTwentyFourHour = false, languageTag = "en-US").asRight()
                ) to SystemSettings(isTwentyFourHour = false, languageTag = "en-US").asRight(),
                actual = followed.toList() to settingRepository.getSystemSettings()
            )
        }

    @Test
    fun `a phone that cannot be read is an unknown failure and logged as an error`() =
        runTest {
            val settingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IllegalStateException("bug")),
                systemSettingLocalDataSource = FailingSystemSettingLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft() to Failure.unknown().asLeft(),
                    actual = settingRepository.getSystemSettings() to settingRepository.observeSystemSettings().first()
                )
                assertEquals(
                    expected = listOf(LogLevel.ERROR, LogLevel.ERROR),
                    actual = recordingLogSink.lines.map { line -> line.level }
                )
            }
        }
}