package se.vadretapp.vadret.setting

import androidx.datastore.core.IOException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.testPreferencesDataStore
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositoryHourFormatTest {

    private fun TestScope.settingRepository(): SettingRepository =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
        )

    @Test
    fun `nothing stored follows the clock of the phone`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            assertEquals(
                expected = HourFormatChoice.SYSTEM.asRight() to HourFormatChoice.SYSTEM.asRight(),
                actual = settingRepository.getHourFormatChoice() to settingRepository.observeHourFormatChoice().first()
            )
        }

    @Test
    fun `every clock reads back as it was chosen`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()

            val readBack: List<Pair<Either<Failure, HourFormatChoice>, Either<Failure, HourFormatChoice>>> =
                HourFormatChoice.entries.map { hourFormatChoice: HourFormatChoice ->
                    settingRepository.setHourFormatChoice(hourFormatChoice = hourFormatChoice)
                    settingRepository.getHourFormatChoice() to settingRepository.observeHourFormatChoice().first()
                }

            assertEquals(
                expected = HourFormatChoice.entries.map { hourFormatChoice: HourFormatChoice -> hourFormatChoice.asRight() to hourFormatChoice.asRight() },
                actual = readBack
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        runTest {
            val settingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IOException("disk full", null)),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    settingRepository.getHourFormatChoice(),
                    settingRepository.observeHourFormatChoice().first(),
                    settingRepository.setHourFormatChoice(hourFormatChoice = HourFormatChoice.TWELVE_HOUR)
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
            val settingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IllegalStateException("bug")),
                systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = settingRepository.getHourFormatChoice()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }
}