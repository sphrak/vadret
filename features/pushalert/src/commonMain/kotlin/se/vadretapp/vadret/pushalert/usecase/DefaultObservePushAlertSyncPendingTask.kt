package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.PushAlertRepository

internal class DefaultObservePushAlertSyncPendingTask(
    private val pushAlertRepository: PushAlertRepository
) : ObservePushAlertSyncPendingTask {
    override fun invoke(): Flow<Either<Failure, Boolean>> = pushAlertRepository.observeIsSyncPending()
}