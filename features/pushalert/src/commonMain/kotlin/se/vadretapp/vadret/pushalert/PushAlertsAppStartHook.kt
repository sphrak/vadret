package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.onResult
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask

internal class PushAlertsAppStartHook(
    private val syncPushAlertsTask: SyncPushAlertsTask
) : AppStartHook {
    override suspend fun onAppStart() {
        syncPushAlertsTask().onResult(
            onError = { failure: Failure -> logger.warn { "Push alerts could not be brought up to date: ${failure.errorCode}" } },
            onSuccess = { logger.debug { "Push alerts are up to date" } }
        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")