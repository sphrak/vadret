package se.vadretapp.vadret.main

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.theme.model.Theme

internal interface ObserveThemeTask {

    operator fun invoke(): Flow<Either<Failure, Theme>>
}