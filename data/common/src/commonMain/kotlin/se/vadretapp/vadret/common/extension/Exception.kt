package se.vadretapp.vadret.common.extension

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.Logger

internal expect fun IOException.isNetworkUnavailable(): Boolean

internal fun Exception.networkErrorCode(): ErrorCode? =
    when (this) {
        is ResponseException -> response.status.value.toHttpErrorCode()
        is IOException -> if (isNetworkUnavailable()) {
            ErrorCode.NETWORK_UNAVAILABLE
        } else {
            ErrorCode.NETWORK_FAILURE
        }
        is ContentConvertException,
        is NoTransformationFoundException,
        is SerializationException -> ErrorCode.INVALID_RESPONSE
        else -> null
    }

fun Exception.toKnownFailure(
    errorCode: ErrorCode,
    logger: Logger
): Failure {
    logger.warn { "$errorCode: ${this::class.simpleName}: $message" }
    return Failure(errorCode = errorCode)
}

fun Exception.toUnknownFailure(logger: Logger): Failure {
    logger.error { stackTraceToString() }
    return Failure.unknown()
}

fun Exception.toNetworkFailure(logger: Logger): Failure {
    val errorCode: ErrorCode? = networkErrorCode()
    return if (errorCode != null) {
        toKnownFailure(
            errorCode = errorCode,
            logger = logger
        )
    } else {
        toUnknownFailure(logger = logger)
    }
}