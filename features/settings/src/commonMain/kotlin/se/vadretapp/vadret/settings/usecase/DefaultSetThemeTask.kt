package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.SettingRepository
import se.vadretapp.vadret.setting.model.Theme

internal class DefaultSetThemeTask(
    private val settingRepository: SettingRepository
) : SetThemeTask {

    override suspend fun invoke(theme: Theme): Either<Failure, Unit> = settingRepository.setTheme(theme = theme)
}