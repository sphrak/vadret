package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.DeviceToken

interface DeviceTokenSource {
    suspend fun deviceToken(): Either<Failure, DeviceToken>
}