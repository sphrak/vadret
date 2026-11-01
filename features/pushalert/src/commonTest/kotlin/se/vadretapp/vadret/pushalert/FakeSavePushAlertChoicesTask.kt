package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.usecase.SavePushAlertChoicesTask

internal class FakeSavePushAlertChoicesTask(
    private val result: Either<Failure, Unit> = Unit.asRight()
) : SavePushAlertChoicesTask {
    val saved = mutableListOf<PushAlertChoices>()

    override suspend fun invoke(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> {
        saved += pushAlertChoices
        return result
    }
}