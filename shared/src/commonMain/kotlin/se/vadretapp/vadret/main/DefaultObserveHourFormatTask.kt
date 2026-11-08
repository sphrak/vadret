package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.HourFormat

internal class DefaultObserveHourFormatTask(
    private val settingRepository: SettingRepository
) : ObserveHourFormatTask {

    override fun invoke(): Flow<Either<Failure, HourFormat>> = settingRepository.observeHourFormat()
}