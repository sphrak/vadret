package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.usecase.SyncPushAlertsTask
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight

internal class FakeSyncPushAlertsTask(
    private val result: Either<Failure, Unit> = Unit.asRight()
) : SyncPushAlertsTask {
    var syncs = 0

    override suspend fun invoke(): Either<Failure, Unit> {
        syncs++
        return result
    }
}