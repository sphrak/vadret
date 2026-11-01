package se.vadretapp.vadret.radar.extension

import coil3.network.HttpException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.radar.exception.TiffFormatException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.radar")

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