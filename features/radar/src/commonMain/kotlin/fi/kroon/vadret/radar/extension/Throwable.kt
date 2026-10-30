package fi.kroon.vadret.radar.extension

import coil3.network.HttpException
import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.radar.exception.TiffFormatException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.radar")

internal fun Throwable.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is TiffFormatException -> toKnownFailure(
            errorCode = ErrorCode.INVALID_RESPONSE,
            logger = logger
        )
        is HttpException -> toKnownFailure(
            errorCode = ErrorCode.NETWORK_FAILURE,
            logger = logger
        )
        is Exception -> toNetworkFailure(logger = logger)
        else -> {
            logger.error { stackTraceToString() }
            Failure.unknown()
        }
    }