package fi.kroon.vadret.encrypteddatastore

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory

internal fun <T> corruptionHandler(
    fileName: String,
    defaultValue: T
): ReplaceFileCorruptionHandler<T> =
    ReplaceFileCorruptionHandler { corruptionException ->
        logger.warn { "$fileName starts over: ${corruptionException.message}" }
        defaultValue
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.encrypteddatastore")