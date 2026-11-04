package se.vadretapp.vadret.pushalert.local

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

internal fun <T> corruptionHandler(
    fileName: String,
    defaultValue: T
): ReplaceFileCorruptionHandler<T> =
    ReplaceFileCorruptionHandler { corruptionException ->
        logger.warn { "$fileName starts over: ${corruptionException.message}" }
        defaultValue
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")