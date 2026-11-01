package se.vadretapp.vadret.weatherforecast.extension

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

internal fun Exception.toFailure(notFoundErrorCode: ErrorCode): Failure =
    when {
        this is CancellationException -> throw this
        this is ClientRequestException && response.status == HttpStatusCode.NotFound -> toKnownFailure(
            errorCode = notFoundErrorCode,
            logger = logger
        )
        this is IllegalArgumentException -> toKnownFailure(
            errorCode = ErrorCode.INVALID_RESPONSE,
            logger = logger
        )
        else -> toNetworkFailure(logger = logger)
    }