package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.DeviceToken

internal class FakeDeviceTokenSource(
    var deviceToken: Either<Failure, DeviceToken> = DeviceToken(value = "fcm-1").asRight()
) : DeviceTokenSource {
    override suspend fun deviceToken(): Either<Failure, DeviceToken> = deviceToken
}