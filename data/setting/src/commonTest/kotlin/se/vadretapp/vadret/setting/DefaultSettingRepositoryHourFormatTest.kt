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
import se.vadretapp.vadret.setting.local.DefaultSettingLocalDataSource
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.testing.testPreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingRepositoryHourFormatTest {

    private val systemSettingLocalDataSource = FakeSystemSettingLocalDataSource()

    private fun TestScope.settingRepository(): SettingRepository =
        DefaultSettingRepository(
            settingLocalDataSource = DefaultSettingLocalDataSource(dataStore = testPreferencesDataStore(scope = backgroundScope)),
            systemSettingLocalDataSource = systemSettingLocalDataSource
        )

    @Test
    fun `nothing stored follows the clock of the phone`() =
        runTest {
            val settingRepository: SettingRepository = settingRepository()
            val onATwentyFourHourPhone: Either<Failure, HourFormat> = settingRepository.getHourFormat()
            systemSettingLocalDataSource.isTwentyFourHourClock = false

            assertEquals(
                expected = listOf(
                    HourFormatChoice.SYSTEM.asRight(),
                    HourFormat.TWENTY_FOUR_HOUR.asRight(),
                    HourFormat.TWELVE_HOUR.asRight()
                ),
                actual = listOf(
                    settingRepository.getHourFormatChoice(),
                    onATwentyFourHourPhone,
                    settingRepository.getHourFormat()
                )
            )
        }

    @Test
    fun `a chosen clock reads back and wins over the clock of the phone`() =
        runTest {
            systemSettingLocalDataSource.isTwentyFourHourClock = false
            val settingRepository: SettingRepository = settingRepository()

            val readBack: List<List<Either<Failure, Any>>> = HourFormatChoice.entries.map { hourFormatChoice: HourFormatChoice ->
                settingRepository.setHourFormatChoice(hourFormatChoice = hourFormatChoice)
                listOf(
                    settingRepository.getHourFormatChoice(),
                    settingRepository.getHourFormat(),
                    settingRepository.observeHourFormat().first()
                )
            }

            assertEquals(
                expected = listOf(
                    listOf(HourFormatChoice.SYSTEM.asRight(), HourFormat.TWELVE_HOUR.asRight(), HourFormat.TWELVE_HOUR.asRight()),
                    listOf(HourFormatChoice.TWENTY_FOUR_HOUR.asRight(), HourFormat.TWENTY_FOUR_HOUR.asRight(), HourFormat.TWENTY_FOUR_HOUR.asRight()),
                    listOf(HourFormatChoice.TWELVE_HOUR.asRight(), HourFormat.TWELVE_HOUR.asRight(), HourFormat.TWELVE_HOUR.asRight())
                ),
                actual = readBack
            )
        }

    @Test
    fun `storage that fails says so instead of guessing a clock`() =
        runTest {
            val settingRepository = DefaultSettingRepository(
                settingLocalDataSource = FailingSettingLocalDataSource(exception = IOException("disk full", null)),
                systemSettingLocalDataSource = systemSettingLocalDataSource
            )

            assertEquals(
                expected = List(size = 4) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                actual = listOf(
                    settingRepository.getHourFormatChoice(),
                    settingRepository.getHourFormat(),
                    settingRepository.observeHourFormat().first(),
                    settingRepository.setHourFormatChoice(hourFormatChoice = HourFormatChoice.TWELVE_HOUR)
                )
            )
        }
}