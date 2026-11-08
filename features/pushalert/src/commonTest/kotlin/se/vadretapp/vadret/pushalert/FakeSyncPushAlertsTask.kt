package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask

internal class FakeSyncPushAlertsTask(
    private val result: Either<Failure, Unit> = Unit.asRight(),
    private val whileSyncing: (sync: Int) -> Unit = {}
) : SyncPushAlertsTask {
    var syncs = 0

    override suspend fun invoke(): Either<Failure, Unit> {
        syncs++
        whileSyncing(syncs)
        return result
    }
}