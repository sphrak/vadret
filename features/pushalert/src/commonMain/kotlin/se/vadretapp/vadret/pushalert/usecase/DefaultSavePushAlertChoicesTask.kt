package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal class DefaultSavePushAlertChoicesTask(
    private val pushAlertRepository: PushAlertRepository
) : SavePushAlertChoicesTask {
    override suspend fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> =
        pushAlertRepository.saveChoices(pushAlertChoices = pushAlertChoices)
}