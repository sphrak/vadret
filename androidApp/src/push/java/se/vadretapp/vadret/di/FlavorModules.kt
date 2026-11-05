package se.vadretapp.vadret.di

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.pushalert.AndroidDeviceTokenSource
import se.vadretapp.vadret.pushalert.AndroidNotificationPermissionSource
import se.vadretapp.vadret.pushalert.DeviceTokenSource
import se.vadretapp.vadret.pushalert.NotificationManagerPushAlertNotifier
import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertNotifier
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.di.pushAlertModule
import se.vadretapp.vadret.pushalert.di.pushAlertSimulationModule
import java.io.File

internal fun flavorModules(context: Context): List<Module> = listOf(
    pushAlertModule,
    pushAlertSimulationModule,
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
    single<PushAlertNotifier> { NotificationManagerPushAlertNotifier(context = context) }
    single<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)) { CoroutineScope(context = SupervisorJob() + get<CoroutineDispatcher>()) }
}

internal const val PUSH_ALERT_SCOPE = "push-alert-scope"

private const val SEALED_MESSAGE_KEY_FILE_NAME = "push_alert_key"
private const val PUSH_ALERT_KEY_ALIAS = "se.vadretapp.vadret.pushalert"