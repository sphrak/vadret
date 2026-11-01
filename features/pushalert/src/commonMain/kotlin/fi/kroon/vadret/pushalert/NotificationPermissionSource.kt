package fi.kroon.vadret.pushalert

interface NotificationPermissionSource {
    suspend fun isNotificationAllowed(): Boolean
}