package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal class DefaultObservePushAlertChoicesTask(
    private val pushAlertRepository: PushAlertRepository
) : ObservePushAlertChoicesTask {
    override fun invoke(): Flow<Either<Failure, PushAlertChoices>> = pushAlertRepository.observeChoices()
}