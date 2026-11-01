package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask

internal class FakeSyncPushAlertsTask(
    private val result: Either<Failure, Unit> = Unit.asRight()
) : SyncPushAlertsTask {
    var syncs = 0

    override suspend fun invoke(): Either<Failure, Unit> {
        syncs++
        return result
    }
}