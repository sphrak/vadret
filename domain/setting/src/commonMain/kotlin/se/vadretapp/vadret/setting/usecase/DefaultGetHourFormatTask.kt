package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings

internal class DefaultGetHourFormatTask(
    private val settingRepository: SettingRepository
) : GetHourFormatTask {

    override suspend fun invoke(): Either<Failure, HourFormat> =
        settingRepository.getHourFormatChoice().flatMap { hourFormatChoice: HourFormatChoice ->
            settingRepository.getSystemSettings().map { systemSettings: SystemSettings ->
                hourFormatChoice.toHourFormat(isSystemTwentyFourHour = systemSettings.isTwentyFourHour)
            }
        }
}