package fi.kroon.vadret.favorite.extension

import androidx.sqlite.SQLiteException
import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toUnknownFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.favorite")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is SQLiteException -> toKnownFailure(
            errorCode = ErrorCode.STORAGE_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }