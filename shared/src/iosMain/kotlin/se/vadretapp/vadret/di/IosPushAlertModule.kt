package se.vadretapp.vadret.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.encryption.model.KeychainAccessGroup
import se.vadretapp.vadret.pushalert.DeviceTokenSource
import se.vadretapp.vadret.pushalert.IosDeviceTokenSource
import se.vadretapp.vadret.pushalert.IosNotificationPermissionSource
import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.PushTokenProviderHolder

// The app group is shared with the Notification Service Extension, which opens sealed pushes with the same key.
internal fun iosPushAlertModule(appGroup: String): Module = module {
    single<PushTokenProviderHolder> { PushTokenProviderHolder() }
    single<DeviceTokenSource> { IosDeviceTokenSource(pushTokenProviderHolder = get()) }
    single<NotificationPermissionSource> { IosNotificationPermissionSource() }
    single<SealedMessageKey> {
        SealedMessageKey(
            keyAlias = KeyAlias(value = PUSH_ALERT_KEY_ALIAS),
            keychainAccessGroup = KeychainAccessGroup(value = appGroup)
        )
    }
    single<PushAlertRepository> {
        PushAlertRepository(
            httpClient = inject(),
            directoryPath = get<AppDirectories>().filesDirectoryPath,
            ioDispatcher = get()
        )
    }
    single<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)) { CoroutineScope(context = SupervisorJob() + get<CoroutineDispatcher>()) }
}

internal const val PUSH_ALERT_SCOPE = "push-alert-scope"

// The Notification Service Extension looks the key up by this tag.
private const val PUSH_ALERT_KEY_ALIAS = "se.vadretapp.vadret.pushalert"