package se.vadretapp.vadret.pushalert

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal class IosNotificationPermissionSource : NotificationPermissionSource {
    override suspend fun isNotificationAllowed(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { notificationSettings: UNNotificationSettings? ->
                val authorizationStatus: UNAuthorizationStatus? = notificationSettings?.authorizationStatus
                continuation.resume(value = authorizationStatus != null && authorizationStatus in allowingAuthorizationStatuses)
            }
        }
}

private val allowingAuthorizationStatuses: Set<UNAuthorizationStatus> = setOf(
    UNAuthorizationStatusAuthorized,
    UNAuthorizationStatusProvisional
)