package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.NotificationTextSettings

internal interface ObserveNotificationTextSettingsTask {
    operator fun invoke(): Flow<Either<Failure, NotificationTextSettings>>
}