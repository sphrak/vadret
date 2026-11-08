package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.model.SystemSettings

internal class DefaultObserveShownLanguageTask(
    private val settingRepository: SettingRepository
) : ObserveShownLanguageTask {

    // The phone's language is followed too, so a language switched on the phone while the app runs reaches it at once.
    override fun invoke(): Flow<Either<Failure, Language>> =
        combine(
            flow = settingRepository.observeLanguage(),
            flow2 = settingRepository.observeSystemSettings()
        ) { appLanguage: Either<Failure, AppLanguage>, systemSettings: Either<Failure, SystemSettings> ->
            appLanguage.flatMap { chosen: AppLanguage ->
                systemSettings.map { phone: SystemSettings -> chosen.toShownLanguage(systemLanguageTag = phone.languageTag) }
            }
        }.distinctUntilChanged()
}