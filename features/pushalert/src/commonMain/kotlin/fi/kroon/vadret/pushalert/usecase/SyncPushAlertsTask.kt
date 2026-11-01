package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import io.github.sphrak.either.Either

interface SyncPushAlertsTask {
    suspend operator fun invoke(): Either<Failure, Unit>
}