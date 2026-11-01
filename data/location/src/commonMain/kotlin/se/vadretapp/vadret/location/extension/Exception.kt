package se.vadretapp.vadret.location.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toUnknownFailure
import se.vadretapp.vadret.location.exception.LocationNotAvailableException
import se.vadretapp.vadret.location.exception.LocationPermissionMissingException
import se.vadretapp.vadret.location.exception.LocationProviderDisabledException
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import androidx.datastore.core.IOException as DataStoreIOException
import okio.IOException as FileIOException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.location")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is LocationProviderDisabledException -> toKnownFailure(
            errorCode = ErrorCode.LOCATION_PROVIDER_DISABLED,
            logger = logger
        )
        is LocationNotAvailableException -> toKnownFailure(
            errorCode = ErrorCode.LOCATION_NOT_AVAILABLE,
            logger = logger
        )
        is LocationPermissionMissingException -> toKnownFailure(
            errorCode = ErrorCode.LOCATION_PERMISSION_MISSING,
            logger = logger
        )
        is DataStoreIOException,
        is FileIOException -> toKnownFailure(
            errorCode = ErrorCode.STORAGE_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }