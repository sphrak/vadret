package se.vadretapp.vadret.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.theme.local.DefaultThemeLocalDataSource
import se.vadretapp.vadret.theme.model.Theme

interface ThemeRepository {
    suspend fun getTheme(): Either<Failure, Theme>
    fun observeTheme(): Flow<Either<Failure, Theme>>
    suspend fun setTheme(theme: Theme): Either<Failure, Unit>
}

fun ThemeRepository(dataStore: DataStore<Preferences>): ThemeRepository =
    DefaultThemeRepository(
        themeLocalDataSource = DefaultThemeLocalDataSource(dataStore = dataStore)
    )