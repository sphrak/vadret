package se.vadretapp.vadret.language

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.language.extension.toAppLanguage
import se.vadretapp.vadret.language.extension.toFailure
import se.vadretapp.vadret.language.extension.toPreferenceValue
import se.vadretapp.vadret.language.local.LanguageLocalDataSource
import se.vadretapp.vadret.language.model.AppLanguage

internal class DefaultLanguageRepository(
    private val languageLocalDataSource: LanguageLocalDataSource
) : LanguageRepository {

    override suspend fun getLanguage(): Either<Failure, AppLanguage> =
        either { languageLocalDataSource.getLanguage().toAppLanguage() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeLanguage(): Flow<Either<Failure, AppLanguage>> =
        languageLocalDataSource
            .observeLanguage()
            .map<String?, Either<Failure, AppLanguage>> { value: String? -> value.toAppLanguage().asRight() }
            .catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit> =
        either { languageLocalDataSource.setLanguage(value = language.toPreferenceValue()) }
            .catch { exception: Exception -> exception.toFailure() }
}