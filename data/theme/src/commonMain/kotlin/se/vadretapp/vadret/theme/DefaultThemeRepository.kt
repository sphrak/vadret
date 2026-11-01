package se.vadretapp.vadret.theme

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.theme.extension.toFailure
import se.vadretapp.vadret.theme.extension.toPreferenceValue
import se.vadretapp.vadret.theme.extension.toTheme
import se.vadretapp.vadret.theme.local.ThemeLocalDataSource
import se.vadretapp.vadret.theme.model.Theme

internal class DefaultThemeRepository(
    private val themeLocalDataSource: ThemeLocalDataSource
) : ThemeRepository {

    override suspend fun getTheme(): Either<Failure, Theme> =
        either { themeLocalDataSource.getTheme().toTheme() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeTheme(): Flow<Either<Failure, Theme>> =
        themeLocalDataSource
            .observeTheme()
            .map<String?, Either<Failure, Theme>> { value: String? -> value.toTheme().asRight() }
            .catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun setTheme(theme: Theme): Either<Failure, Unit> =
        either { themeLocalDataSource.setTheme(value = theme.toPreferenceValue()) }
            .catch { exception: Exception -> exception.toFailure() }
}