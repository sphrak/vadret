package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow

internal interface ObservePushAlertSyncPendingTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}