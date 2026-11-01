package se.vadretapp.vadret.language

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.local.DefaultLanguageLocalDataSource
import se.vadretapp.vadret.language.model.AppLanguage

interface LanguageRepository {
    suspend fun getLanguage(): Either<Failure, AppLanguage>
    fun observeLanguage(): Flow<Either<Failure, AppLanguage>>
    suspend fun setLanguage(language: AppLanguage): Either<Failure, Unit>
}

fun LanguageRepository(dataStore: DataStore<Preferences>): LanguageRepository =
    DefaultLanguageRepository(
        languageLocalDataSource = DefaultLanguageLocalDataSource(dataStore = dataStore)
    )