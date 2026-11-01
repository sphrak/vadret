package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure

internal interface ObservePushAlertSyncPendingTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}