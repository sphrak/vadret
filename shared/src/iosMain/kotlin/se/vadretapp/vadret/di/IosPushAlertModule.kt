package se.vadretapp.vadret.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSFileManager
import se.vadretapp.vadret.di.model.AppDirectories
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.encryption.model.KeychainAccessGroup
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.DeviceTokenSource
import se.vadretapp.vadret.pushalert.IosDeviceTokenSource
import se.vadretapp.vadret.pushalert.IosNotificationPermissionSource
import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertPlaceNameRepository
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
    single<PushAlertPlaceNameRepository> {
        PushAlertPlaceNameRepository(
            directoryPath = appGroupDirectoryPath(appGroup = appGroup) ?: get<AppDirectories>().filesDirectoryPath,
            ioDispatcher = get()
        )
    }
    single<CoroutineScope>(qualifier = named(name = PUSH_ALERT_SCOPE)) { CoroutineScope(context = SupervisorJob() + get<CoroutineDispatcher>()) }
}

internal const val PUSH_ALERT_SCOPE = "push-alert-scope"

private fun appGroupDirectoryPath(appGroup: String): String? {
    val appGroupDirectoryPath = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(groupIdentifier = appGroup)?.path
    if (appGroupDirectoryPath == null) {
        logger.warn { "The app group $appGroup has no directory, so the Notification Service Extension cannot name saved places" }
    }
    return appGroupDirectoryPath
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

// The Notification Service Extension looks the key up by this tag.
private const val PUSH_ALERT_KEY_ALIAS = "se.vadretapp.vadret.pushalert"