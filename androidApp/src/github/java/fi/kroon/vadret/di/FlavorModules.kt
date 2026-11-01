package fi.kroon.vadret.di

import android.content.Context
import fi.kroon.vadret.encryption.DeviceCipher
import fi.kroon.vadret.encryption.SealedMessageKey
import fi.kroon.vadret.encryption.model.KeyAlias
import fi.kroon.vadret.pushalert.AndroidDeviceTokenSource
import fi.kroon.vadret.pushalert.AndroidNotificationPermissionSource
import fi.kroon.vadret.pushalert.DeviceTokenSource
import fi.kroon.vadret.pushalert.NotificationPermissionSource
import fi.kroon.vadret.pushalert.PushAlertNotifier
import fi.kroon.vadret.pushalert.PushAlertRepository
import fi.kroon.vadret.pushalert.di.pushAlertModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

internal fun flavorModules(context: Context): List<Module> = listOf(
    pushAlertModule,
    githubPushAlertModule(context = context)
)

private fun githubPushAlertModule(context: Context): Module = module {
    single<DeviceTokenSource> { AndroidDeviceTokenSource() }
    single<NotificationPermissionSource> { AndroidNotificationPermissionSource(context = context) }
    single<SealedMessageKey> {
        SealedMessageKey(
            keyFile = File(context.noBackupFilesDir, SEALED_MESSAGE_KEY_FILE_NAME),
            deviceCipher = DeviceCipher(keyAlias = KeyAlias(value = PUSH_ALERT_KEY_ALIAS))
        )
    }
    single<PushAlertRepository> {
        PushAlertRepository(
            httpClient = inject(),
            directoryPath = context.noBackupFilesDir.absolutePath,
            ioDispatcher = get()
        )
    }
    single<PushAlertNotifier> { PushAlertNotifier(context = context) }
    single<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)) { CoroutineScope(context = SupervisorJob() + get<CoroutineDispatcher>()) }
}

internal const val PUSH_ALERT_SCOPE = "push-alert-scope"

private const val SEALED_MESSAGE_KEY_FILE_NAME = "push_alert_key"
private const val PUSH_ALERT_KEY_ALIAS = "fi.kroon.vadret.pushalert"