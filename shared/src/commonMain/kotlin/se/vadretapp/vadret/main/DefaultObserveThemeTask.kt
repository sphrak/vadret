package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.theme.ThemeRepository
import se.vadretapp.vadret.theme.model.Theme

internal class DefaultObserveThemeTask(
    private val themeRepository: ThemeRepository
) : ObserveThemeTask {

    override fun invoke(): Flow<Either<Failure, Theme>> = themeRepository.observeTheme()
}