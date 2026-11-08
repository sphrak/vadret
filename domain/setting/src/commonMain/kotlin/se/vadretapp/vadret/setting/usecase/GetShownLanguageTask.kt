package se.vadretapp.vadret.setting.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.setting.model.Language

interface GetShownLanguageTask {
    suspend operator fun invoke(): Either<Failure, Language>
}