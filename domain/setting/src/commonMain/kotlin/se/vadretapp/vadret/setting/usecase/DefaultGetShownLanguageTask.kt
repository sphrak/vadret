package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.Language
import se.vadretapp.vadret.setting.model.SystemSettings

internal class DefaultGetShownLanguageTask(
    private val settingRepository: SettingRepository
) : GetShownLanguageTask {

    override suspend fun invoke(): Either<Failure, Language> =
        settingRepository.getLanguage().flatMap { appLanguage: AppLanguage ->
            settingRepository.getSystemSettings().map { systemSettings: SystemSettings ->
                appLanguage.toShownLanguage(systemLanguageTag = systemSettings.languageTag)
            }
        }
}