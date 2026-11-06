package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.LanguageRepository
import se.vadretapp.vadret.language.model.AppLanguage

internal class DefaultSetLanguageTask(
    private val languageRepository: LanguageRepository
) : SetLanguageTask {

    override suspend fun invoke(language: AppLanguage): Either<Failure, Unit> = languageRepository.setLanguage(language = language)
}