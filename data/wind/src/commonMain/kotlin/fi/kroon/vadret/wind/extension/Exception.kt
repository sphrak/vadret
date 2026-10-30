package fi.kroon.vadret.wind.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.wind.exception.InvalidWindGridException
import fi.kroon.vadret.wind.exception.WindNotAvailableException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.wind")

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