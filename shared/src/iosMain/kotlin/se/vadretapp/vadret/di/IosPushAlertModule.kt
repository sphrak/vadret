package se.vadretapp.vadret.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.model.AppDirectories
import se.vadretapp.vadret.pushalert.DeviceTokenSource
import se.vadretapp.vadret.pushalert.IosDeviceTokenSource
import se.vadretapp.vadret.pushalert.IosNotificationPermissionSource
import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertNotifier
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.PushTokenProviderHolder

internal val iosPushAlertModule: Module = module {
    single<PushTokenProviderHolder> { PushTokenProviderHolder() }
    single<DeviceTokenSource> { IosDeviceTokenSource(pushTokenProviderHolder = get()) }
    single<NotificationPermissionSource> { IosNotificationPermissionSource() }
    single<SealedMessageKey> { SealedMessageKey(keyAlias = KeyAlias(value = PUSH_ALERT_KEY_ALIAS)) }
    single<PushAlertRepository> {
        PushAlertRepository(
            httpClient = inject(),
            directoryPath = get<AppDirectories>().filesDirectoryPath,
            ioDispatcher = get()
        )
    }
    single<PushAlertNotifier> { PushAlertNotifier() }
    single<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)) { CoroutineScope(context = SupervisorJob() + get<CoroutineDispatcher>()) }
}

internal const val PUSH_ALERT_SCOPE = "push-alert-scope"

private const val PUSH_ALERT_KEY_ALIAS = "se.vadretapp.vadret.pushalert"