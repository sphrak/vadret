package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeObservePushAlertSyncPendingTask : ObservePushAlertSyncPendingTask {
    val isSyncPending = MutableStateFlow(value = false)

    override fun invoke(): Flow<Either<Failure, Boolean>> = isSyncPending.map { isSyncPending -> isSyncPending.asRight() }
}