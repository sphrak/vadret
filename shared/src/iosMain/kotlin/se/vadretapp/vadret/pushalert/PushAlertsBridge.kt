package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.PUSH_ALERT_SCOPE
import se.vadretapp.vadret.di.koinApplication
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.main.OpenedWarningReferences
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask

fun registerPushTokenProvider(pushTokenProvider: PushTokenProvider) {
    koinApplication.koin.get<PushTokenProviderHolder>().register(pushTokenProvider = pushTokenProvider)
}

fun onPushTokenRefreshed() {
    val koin = koinApplication.koin
    val syncPushAlertsTask: SyncPushAlertsTask = koin.get()
    koin.get<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)).launch {
        syncPushAlertsTask().onResult(
            onError = { failure: Failure -> logger.warn { "A new push token could not be sent: ${failure.errorCode}" } },
            onSuccess = { logger.debug { "A new push token is sent" } }
        )
    }
}

fun onPushAlertOpened(pushAlertData: Map<String, String>) {
    val warningReference: WarningReference = pushAlertWarningReferenceOrNull(pushAlertData = pushAlertData) ?: return
    koinApplication.koin.get<OpenedWarningReferences>().open(warningReference = warningReference)
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")