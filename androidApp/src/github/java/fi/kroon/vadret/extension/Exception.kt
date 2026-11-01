package fi.kroon.vadret.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toUnknownFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.error.DeviceTokenUnavailableException
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

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")