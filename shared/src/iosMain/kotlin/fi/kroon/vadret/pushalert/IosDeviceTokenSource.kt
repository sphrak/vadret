package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.model.DeviceToken
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class IosDeviceTokenSource(
    private val pushTokenProviderHolder: PushTokenProviderHolder
) : DeviceTokenSource {
    override suspend fun deviceToken(): Either<Failure, DeviceToken> {
        val pushTokenProvider = pushTokenProviderHolder.pushTokenProvider() ?: return notConfigured()
        return suspendCancellableCoroutine { continuation ->
            pushTokenProvider.fetchToken { deviceToken: String?, errorDescription: String? ->
                if (deviceToken == null) {
                    continuation.resume(value = unavailable(description = "Firebase gave no push token: $errorDescription"))
                } else {
                    continuation.resume(value = DeviceToken(value = deviceToken).asRight())
                }
            }
        }
    }

    private fun notConfigured(): Either<Failure, DeviceToken> {
        logger.warn { "${ErrorCode.UNKNOWN}: this build has no Firebase configuration, so it gets no push token" }
        return Failure(errorCode = ErrorCode.UNKNOWN).asLeft()
    }

    private fun unavailable(description: String): Either<Failure, DeviceToken> {
        logger.warn { "${ErrorCode.NETWORK_FAILURE}: $description" }
        return Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")