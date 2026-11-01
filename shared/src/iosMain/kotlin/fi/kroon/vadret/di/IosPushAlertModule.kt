package fi.kroon.vadret.di

import fi.kroon.vadret.encryption.SealedMessageKey
import fi.kroon.vadret.encryption.model.KeyAlias
import fi.kroon.vadret.model.AppDirectories
import fi.kroon.vadret.pushalert.DeviceTokenSource
import fi.kroon.vadret.pushalert.IosDeviceTokenSource
import fi.kroon.vadret.pushalert.IosNotificationPermissionSource
import fi.kroon.vadret.pushalert.NotificationPermissionSource
import fi.kroon.vadret.pushalert.PushAlertNotifier
import fi.kroon.vadret.pushalert.PushAlertRepository
import fi.kroon.vadret.pushalert.PushTokenProviderHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

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

private const val PUSH_ALERT_KEY_ALIAS = "fi.kroon.vadret.pushalert"