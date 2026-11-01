package fi.kroon.vadret.common.extension

import fi.kroon.vadret.common.ErrorCode

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_INTERNAL_SERVER_ERROR = 500
private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val HTTP_GATEWAY_TIMEOUT = 504

internal fun Int.toHttpErrorCode(): ErrorCode =
    when (this) {
        HTTP_BAD_REQUEST -> ErrorCode.HTTP_BAD_REQUEST
        HTTP_UNAUTHORIZED -> ErrorCode.HTTP_UNAUTHORIZED
        HTTP_FORBIDDEN -> ErrorCode.HTTP_FORBIDDEN
        HTTP_NOT_FOUND -> ErrorCode.HTTP_NOT_FOUND
        HTTP_INTERNAL_SERVER_ERROR -> ErrorCode.HTTP_INTERNAL_SERVER_ERROR
        HTTP_SERVICE_UNAVAILABLE -> ErrorCode.HTTP_SERVICE_UNAVAILABLE
        HTTP_GATEWAY_TIMEOUT -> ErrorCode.HTTP_GATEWAY_TIMEOUT
        else -> ErrorCode.HTTP_UNEXPECTED_STATUS
    }