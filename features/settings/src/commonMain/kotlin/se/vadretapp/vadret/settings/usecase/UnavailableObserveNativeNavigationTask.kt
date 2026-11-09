package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import se.vadretapp.vadret.common.Failure

internal class UnavailableObserveNativeNavigationTask : ObserveNativeNavigationTask {

    override fun invoke(): Flow<Either<Failure, Boolean>> = emptyFlow()
}