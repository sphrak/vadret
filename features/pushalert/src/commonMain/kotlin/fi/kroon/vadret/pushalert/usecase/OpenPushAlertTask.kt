package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.PushAlertMessage
import io.github.sphrak.either.Either

interface OpenPushAlertTask {
    suspend operator fun invoke(sealed: String): Either<Failure, PushAlertMessage>
}