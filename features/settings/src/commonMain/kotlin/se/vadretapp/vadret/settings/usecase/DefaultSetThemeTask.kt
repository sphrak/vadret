package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme

internal class DefaultSetThemeTask(
    private val themeRepository: ThemeRepository
) : SetThemeTask {

    override suspend fun invoke(theme: Theme): Either<Failure, Unit> = themeRepository.setTheme(theme = theme)
}