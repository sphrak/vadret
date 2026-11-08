package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.HourFormatChoice

internal class DefaultSetHourFormatChoiceTask(
    private val settingRepository: SettingRepository
) : SetHourFormatChoiceTask {

    override suspend fun invoke(hourFormatChoice: HourFormatChoice): Either<Failure, Unit> = settingRepository.setHourFormatChoice(hourFormatChoice = hourFormatChoice)
}