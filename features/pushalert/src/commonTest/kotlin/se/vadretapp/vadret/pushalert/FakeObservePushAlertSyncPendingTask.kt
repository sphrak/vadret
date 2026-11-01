package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask

internal class FakeObservePushAlertSyncPendingTask : ObservePushAlertSyncPendingTask {
    val isSyncPending = MutableStateFlow(value = false)

    override fun invoke(): Flow<Either<Failure, Boolean>> = isSyncPending.map { isSyncPending -> isSyncPending.asRight() }
}