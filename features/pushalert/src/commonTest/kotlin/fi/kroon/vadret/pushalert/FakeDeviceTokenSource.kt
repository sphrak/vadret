package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pushalert.model.DeviceToken
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight

internal class FakeDeviceTokenSource(
    var deviceToken: Either<Failure, DeviceToken> = DeviceToken(value = "fcm-1").asRight()
) : DeviceTokenSource {
    override suspend fun deviceToken(): Either<Failure, DeviceToken> = deviceToken
}