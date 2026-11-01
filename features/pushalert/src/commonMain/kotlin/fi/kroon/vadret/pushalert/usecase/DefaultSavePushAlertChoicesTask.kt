package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.PushAlertRepository
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import io.github.sphrak.either.Either

internal class DefaultSavePushAlertChoicesTask(
    private val pushAlertRepository: PushAlertRepository
) : SavePushAlertChoicesTask {
    override suspend fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> =
        pushAlertRepository.saveChoices(pushAlertChoices = pushAlertChoices)
}