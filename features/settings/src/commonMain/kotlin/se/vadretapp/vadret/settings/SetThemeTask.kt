package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.theme.model.Theme

internal interface SetThemeTask {
    suspend operator fun invoke(theme: Theme): Either<Failure, Unit>
}