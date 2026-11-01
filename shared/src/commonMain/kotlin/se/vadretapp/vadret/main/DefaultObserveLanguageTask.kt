package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.LanguageRepository
import se.vadretapp.vadret.language.model.AppLanguage

internal class DefaultObserveLanguageTask(
    private val languageRepository: LanguageRepository
) : ObserveLanguageTask {

    override fun invoke(): Flow<Either<Failure, AppLanguage>> = languageRepository.observeLanguage()
}