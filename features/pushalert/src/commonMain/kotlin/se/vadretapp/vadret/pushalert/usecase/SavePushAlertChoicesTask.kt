package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal interface SavePushAlertChoicesTask {
    suspend operator fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit>
}