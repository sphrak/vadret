package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

interface OpenPushAlertTask {
    suspend operator fun invoke(sealed: String): Either<Failure, PushAlertMessage>
}