package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class HourFormatTaskTest {

    @Test
    fun followingThePhoneTakesItsClockAndAChosenClockWinsOverIt() =
        runTest {
            val hourFormats: List<Pair<Either<Failure, HourFormat>, Either<Failure, HourFormat>>> = HourFormatChoice.entries.flatMap { hourFormatChoice: HourFormatChoice ->
                listOf(true, false).map { isTwentyFourHour: Boolean ->
                    val settingRepository = FakeSettingRepository(
                        hourFormatChoice = hourFormatChoice.asRight(),
                        systemSettings = SystemSettings(
                            isTwentyFourHour = isTwentyFourHour,
                            languageTag = SWEDISH_TAG
                        ).asRight()
                    )
                    DefaultGetHourFormatTask(settingRepository = settingRepository)() to DefaultObserveHourFormatTask(settingRepository = settingRepository)().first()
                }
            }

            assertEquals(
                expected = listOf(
                    HourFormat.TWENTY_FOUR_HOUR,
                    HourFormat.TWELVE_HOUR,
                    HourFormat.TWENTY_FOUR_HOUR,
                    HourFormat.TWENTY_FOUR_HOUR,
                    HourFormat.TWELVE_HOUR,
                    HourFormat.TWELVE_HOUR
                ).map { hourFormat: HourFormat -> hourFormat.asRight() to hourFormat.asRight() },
                actual = hourFormats
            )
        }

    @Test
    fun aChoiceOrAPhoneThatCannotBeReadIsAFailure() =
        runTest {
            val failures: List<Pair<Either<Failure, HourFormat>, Either<Failure, HourFormat>>> = listOf(
                FakeSettingRepository(hourFormatChoice = STORAGE_FAILED.asLeft()),
                FakeSettingRepository(systemSettings = STORAGE_FAILED.asLeft())
            ).map { settingRepository: FakeSettingRepository ->
                DefaultGetHourFormatTask(settingRepository = settingRepository)() to DefaultObserveHourFormatTask(settingRepository = settingRepository)().first()
            }

            assertEquals(
                expected = List(size = 2) { STORAGE_FAILED.asLeft() to STORAGE_FAILED.asLeft() },
                actual = failures
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aClockSwitchedOnThePhoneIsFollowedOnceAndAChangeOfLanguageAloneIsNot() =
        runTest {
            val settingRepository = FakeSettingRepository()
            val followed = mutableListOf<Either<Failure, HourFormat>>()
            val job = launch { DefaultObserveHourFormatTask(settingRepository = settingRepository)().toList(destination = followed) }
            advanceUntilIdle()

            settingRepository.systemSettings.value = SystemSettings(isTwentyFourHour = true, languageTag = ENGLISH_TAG).asRight()
            advanceUntilIdle()
            settingRepository.systemSettings.value = SystemSettings(isTwentyFourHour = false, languageTag = ENGLISH_TAG).asRight()
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                expected = listOf(HourFormat.TWENTY_FOUR_HOUR.asRight(), HourFormat.TWELVE_HOUR.asRight()),
                actual = followed.toList()
            )
        }
}

private val STORAGE_FAILED = Failure(errorCode = ErrorCode.STORAGE_FAILED)
private const val SWEDISH_TAG = "sv-SE"
private const val ENGLISH_TAG = "en-US"