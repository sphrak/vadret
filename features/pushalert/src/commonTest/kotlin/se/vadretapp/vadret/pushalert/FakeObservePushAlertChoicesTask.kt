package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertChoicesTask

internal class FakeObservePushAlertChoicesTask(choices: PushAlertChoices) : ObservePushAlertChoicesTask {
    val choices = MutableStateFlow(value = choices)

    override fun invoke(): Flow<Either<Failure, PushAlertChoices>> = choices.map { pushAlertChoices -> pushAlertChoices.asRight() }
}