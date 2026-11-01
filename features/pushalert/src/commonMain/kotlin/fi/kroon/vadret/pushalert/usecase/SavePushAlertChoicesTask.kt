package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import io.github.sphrak.either.Either

internal interface SavePushAlertChoicesTask {
    suspend operator fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit>
}