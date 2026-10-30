package fi.kroon.vadret.location.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toUnknownFailure
import fi.kroon.vadret.location.exception.LocationNotAvailableException
import fi.kroon.vadret.location.exception.LocationPermissionMissingException
import fi.kroon.vadret.location.exception.LocationProviderDisabledException
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import androidx.datastore.core.IOException as DataStoreIOException
import okio.IOException as FileIOException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.location")

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