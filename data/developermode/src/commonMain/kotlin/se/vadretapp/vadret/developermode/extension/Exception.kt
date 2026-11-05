package se.vadretapp.vadret.developermode.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toUnknownFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import androidx.datastore.core.IOException as DataStoreIOException
import okio.IOException as FileIOException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.developermode")

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