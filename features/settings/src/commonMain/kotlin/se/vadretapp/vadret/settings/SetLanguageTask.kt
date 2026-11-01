package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.model.AppLanguage

internal interface SetLanguageTask {
    suspend operator fun invoke(language: AppLanguage): Either<Failure, Unit>
}