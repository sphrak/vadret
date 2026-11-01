package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal interface ObservePushAlertChoicesTask {
    operator fun invoke(): Flow<Either<Failure, PushAlertChoices>>
}