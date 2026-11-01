package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.usecase.SavePushAlertChoicesTask
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight

internal class FakeSavePushAlertChoicesTask(
    private val result: Either<Failure, Unit> = Unit.asRight()
) : SavePushAlertChoicesTask {
    val saved = mutableListOf<PushAlertChoices>()

    override suspend fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> {
        saved += pushAlertChoices
        return result
    }
}