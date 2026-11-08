package se.vadretapp.vadret.aurora.extension

import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.toNetworkFailure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aurora")

internal fun Exception.toFailure(): Failure =
    when (this) {
        is CancellationException -> throw this
        else -> toNetworkFailure(logger = logger)
    }