package fi.kroon.vadret.pollen.extension

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.extension.toNetworkFailure
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pollen")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        else -> toNetworkFailure(logger = logger)
    }