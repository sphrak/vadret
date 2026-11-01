package se.vadretapp.vadret.pushalert

interface NotificationPermissionSource {
    suspend fun isNotificationAllowed(): Boolean
}