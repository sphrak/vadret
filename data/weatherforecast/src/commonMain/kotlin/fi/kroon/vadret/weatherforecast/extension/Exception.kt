package fi.kroon.vadret.weatherforecast.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.weatherforecast")

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