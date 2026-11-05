package se.vadretapp.vadret.favorite.extension

import androidx.sqlite.SQLiteException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toKnownFailure
import se.vadretapp.vadret.common.extension.toUnknownFailure
import se.vadretapp.vadret.favorite.exception.FavoriteLimitException
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.favorite")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        is FavoriteLimitException -> toKnownFailure(
            errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT,
            logger = logger
        )
        is SQLiteException -> toKnownFailure(
            errorCode = ErrorCode.STORAGE_FAILED,
            logger = logger
        )
        else -> toUnknownFailure(logger = logger)
    }