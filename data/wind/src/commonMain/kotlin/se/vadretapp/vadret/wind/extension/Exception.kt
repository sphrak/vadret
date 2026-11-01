package se.vadretapp.vadret.wind.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.wind.exception.InvalidWindGridException
import se.vadretapp.vadret.wind.exception.WindNotAvailableException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.wind")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is WindNotAvailableException -> toKnownFailure(
            errorCode = ErrorCode.WIND_NOT_AVAILABLE,
            logger = logger
        )
        is InvalidWindGridException,
        is IllegalArgumentException -> toKnownFailure(
            errorCode = ErrorCode.INVALID_RESPONSE,
            logger = logger
        )
        else -> toNetworkFailure(logger = logger)
    }