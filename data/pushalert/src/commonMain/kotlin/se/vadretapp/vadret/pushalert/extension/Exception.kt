package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.error.AttestationException
import se.vadretapp.vadret.pushalert.error.AttestationRefusedException
import se.vadretapp.vadret.pushalert.error.StorageException
import se.vadretapp.vadret.pushalert.error.UnsupportedAreaException
import kotlin.coroutines.cancellation.CancellationException

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is AttestationException -> toKnownFailure(
            errorCode = ErrorCode.ATTESTATION_FAILED,
            logger = logger
        )
        is AttestationRefusedException -> toKnownFailure(
            errorCode = ErrorCode.ATTESTATION_REFUSED,
            logger = logger
        )
        is StorageException -> toKnownFailure(
            errorCode = ErrorCode.STORAGE_FAILED,
            logger = logger
        )
        is UnsupportedAreaException -> toKnownFailure(
            errorCode = ErrorCode.UNSUPPORTED_AREA,
            logger = logger
        )
        else -> toNetworkFailure(logger = logger)
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")