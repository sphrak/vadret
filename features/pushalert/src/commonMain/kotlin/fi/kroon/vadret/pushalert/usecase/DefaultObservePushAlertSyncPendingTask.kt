package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.PushAlertRepository
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow

internal class DefaultObservePushAlertSyncPendingTask(
    private val pushAlertRepository: PushAlertRepository
) : ObservePushAlertSyncPendingTask {
    override fun invoke(): Flow<Either<Failure, Boolean>> = pushAlertRepository.observeIsSyncPending()
}