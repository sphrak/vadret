package se.vadretapp.vadret.place.extension

import kotlinx.io.IOException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toUnknownFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.place")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is IOException,
        is IllegalArgumentException,
        is IllegalStateException -> toKnownFailure(
            errorCode = ErrorCode.FILE_READ_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }