package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow

internal interface ObservePushAlertChoicesTask {
    operator fun invoke(): Flow<Either<Failure, PushAlertChoices>>
}