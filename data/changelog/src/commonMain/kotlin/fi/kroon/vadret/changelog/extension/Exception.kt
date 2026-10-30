package fi.kroon.vadret.changelog.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toUnknownFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.changelog")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is IOException -> toKnownFailure(
            errorCode = ErrorCode.FILE_READ_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }