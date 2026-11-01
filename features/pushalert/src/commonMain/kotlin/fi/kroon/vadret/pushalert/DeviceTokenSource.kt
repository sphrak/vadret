package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.DeviceToken
import io.github.sphrak.either.Either

interface DeviceTokenSource {
    suspend fun deviceToken(): Either<Failure, DeviceToken>
}