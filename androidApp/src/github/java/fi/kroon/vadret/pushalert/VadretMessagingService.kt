package fi.kroon.vadret.pushalert

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import fi.kroon.vadret.BaseApplication
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.di.PUSH_ALERT_SCOPE
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.model.PushAlertMessage
import fi.kroon.vadret.pushalert.usecase.OpenPushAlertTask
import fi.kroon.vadret.pushalert.usecase.SyncPushAlertsTask
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.qualifier.named

class VadretMessagingService : FirebaseMessagingService() {
    private val koin: Koin
        get() = (application as BaseApplication).koinApplication.koin

    override fun onNewToken(token: String) {
        val pushAlertScope: CoroutineScope = koin.get(qualifier = named(name = PUSH_ALERT_SCOPE))
        val syncPushAlertsTask: SyncPushAlertsTask = koin.get()
        pushAlertScope.launch {
            syncPushAlertsTask().onResult(
                onError = { failure: Failure -> logger.warn { "A new push token could not be sent: ${failure.errorCode}" } },
                onSuccess = { logger.debug { "A new push token is sent" } }
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val sealed = remoteMessage.data[SEALED_FIELD]
        if (sealed == null) {
            logger.warn { "A push arrived without a sealed alert and is dropped" }
            return
        }
        val openPushAlertTask: OpenPushAlertTask = koin.get()
        val pushAlertNotifier: PushAlertNotifier = koin.get()
        runBlocking { openPushAlertTask(sealed = sealed) }.onResult(
            onError = { failure: Failure -> logger.debug { "A sealed push alert is dropped: ${failure.errorCode}" } },
            onSuccess = { pushAlertMessage: PushAlertMessage -> pushAlertNotifier.show(pushAlertMessage = pushAlertMessage) }
        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")

private const val SEALED_FIELD = "sealed"