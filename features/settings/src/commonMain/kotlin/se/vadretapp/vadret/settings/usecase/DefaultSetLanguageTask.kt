package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.AppLanguage

internal class DefaultSetLanguageTask(
    private val settingRepository: SettingRepository
) : SetLanguageTask {

    override suspend fun invoke(language: AppLanguage): Either<Failure, Unit> = settingRepository.setLanguage(language = language)
}