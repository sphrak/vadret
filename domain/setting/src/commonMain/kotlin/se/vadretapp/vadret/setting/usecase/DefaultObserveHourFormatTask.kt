package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.SystemSettings

internal class DefaultObserveHourFormatTask(
    private val settingRepository: SettingRepository
) : ObserveHourFormatTask {

    // The phone's clock is followed too, so a clock switched on the phone while the app runs reaches it at once.
    override fun invoke(): Flow<Either<Failure, HourFormat>> =
        combine(
            flow = settingRepository.observeHourFormatChoice(),
            flow2 = settingRepository.observeSystemSettings()
        ) { hourFormatChoice: Either<Failure, HourFormatChoice>, systemSettings: Either<Failure, SystemSettings> ->
            hourFormatChoice.flatMap { chosen: HourFormatChoice ->
                systemSettings.map { phone: SystemSettings -> chosen.toHourFormat(isSystemTwentyFourHour = phone.isTwentyFourHour) }
            }
        }.distinctUntilChanged()
}