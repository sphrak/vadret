package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.error.AttestationException
import fi.kroon.vadret.pushalert.error.AttestationRefusedException
import fi.kroon.vadret.pushalert.error.StorageException
import fi.kroon.vadret.pushalert.error.UnsupportedAreaException
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

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")