package se.vadretapp.vadret.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toUnknownFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.error.DeviceTokenUnavailableException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is DeviceTokenUnavailableException,
        is IOException -> toKnownFailure(
            errorCode = ErrorCode.NETWORK_FAILURE,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")