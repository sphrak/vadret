package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.NotificationTextSettings
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat

internal class DefaultObserveNotificationTextSettingsTask(
    private val settingRepository: SettingRepository
) : ObserveNotificationTextSettingsTask {

    override fun invoke(): Flow<Either<Failure, NotificationTextSettings>> =
        combine(
            flow = settingRepository.observeLanguage(),
            flow2 = settingRepository.observeHourFormat()
        ) { language: Either<Failure, AppLanguage>, hourFormat: Either<Failure, HourFormat> ->
            language.flatMap { chosenLanguage: AppLanguage ->
                hourFormat.map { chosenHourFormat: HourFormat ->
                    NotificationTextSettings(
                        language = chosenLanguage,
                        hourFormat = chosenHourFormat
                    )
                }
            }
        }
}