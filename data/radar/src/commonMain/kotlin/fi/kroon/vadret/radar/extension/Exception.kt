package fi.kroon.vadret.radar.extension

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toKnownFailure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.radar.exception.RadarImageUrlException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.radar")

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
        else -> toNetworkFailure(logger = logger)
    }