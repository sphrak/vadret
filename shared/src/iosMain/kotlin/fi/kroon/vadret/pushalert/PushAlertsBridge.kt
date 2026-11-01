package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.di.PUSH_ALERT_SCOPE
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.main.koinApplication
import fi.kroon.vadret.pushalert.model.PushAlertMessage
import fi.kroon.vadret.pushalert.usecase.OpenPushAlertTask
import fi.kroon.vadret.pushalert.usecase.SyncPushAlertsTask
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named

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

fun showSealedPushAlert(
    sealed: String,
    onHandled: () -> Unit
) {
    val koin = koinApplication.koin
    val openPushAlertTask: OpenPushAlertTask = koin.get()
    val pushAlertNotifier: PushAlertNotifier = koin.get()
    koin.get<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)).launch {
        try {
            openPushAlertTask(sealed = sealed).onResult(
                onError = { failure: Failure -> logger.debug { "A sealed push alert is dropped: ${failure.errorCode}" } },
                onSuccess = { pushAlertMessage: PushAlertMessage -> pushAlertNotifier.show(pushAlertMessage = pushAlertMessage) }
            )
        } finally {
            onHandled()
        }
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")