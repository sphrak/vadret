package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.AppLanguage

internal interface SetLanguageTask {
    suspend operator fun invoke(language: AppLanguage): Either<Failure, Unit>
}