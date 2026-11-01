package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

interface SyncPushAlertsTask {
    suspend operator fun invoke(): Either<Failure, Unit>
}