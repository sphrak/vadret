package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.lifecycle.AppStartHook
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.usecase.SyncPushAlertsTask
import io.github.sphrak.either.extension.onResult

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

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")