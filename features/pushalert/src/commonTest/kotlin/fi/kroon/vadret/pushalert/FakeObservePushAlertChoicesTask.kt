package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeObservePushAlertChoicesTask(choices: PushAlertChoices) : ObservePushAlertChoicesTask {
    val choices = MutableStateFlow(value = choices)

    override fun invoke(): Flow<Either<Failure, PushAlertChoices>> = choices.map { pushAlertChoices -> pushAlertChoices.asRight() }
}