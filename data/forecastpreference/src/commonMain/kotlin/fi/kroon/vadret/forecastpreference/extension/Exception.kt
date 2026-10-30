package fi.kroon.vadret.forecastpreference.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toUnknownFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import androidx.datastore.core.IOException as DataStoreIOException
import okio.IOException as FileIOException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.forecastpreference")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is DataStoreIOException,
        is FileIOException -> toKnownFailure(
            errorCode = ErrorCode.STORAGE_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }