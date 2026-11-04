package se.vadretapp.vadret.radar.extension

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.radar.exception.RadarDownloadException
import se.vadretapp.vadret.radar.exception.RadarImageUrlException
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.radar")

internal fun Exception.toFailure(): Failure =
    when {
        this is CancellationException -> throw this
        this is ClientRequestException && response.status == HttpStatusCode.NotFound -> toKnownFailure(
            errorCode = ErrorCode.RADAR_NOT_AVAILABLE,
            logger = logger
        )
        this is RadarImageUrlException -> toKnownFailure(
            errorCode = ErrorCode.RADAR_NOT_AVAILABLE,
            logger = logger
        )
        this is RadarDownloadException -> toKnownFailure(
            errorCode = ErrorCode.INVALID_RESPONSE,
            logger = logger
        )
        else -> toNetworkFailure(logger = logger)
    }