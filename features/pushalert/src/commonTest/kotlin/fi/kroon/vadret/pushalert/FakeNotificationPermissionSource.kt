package fi.kroon.vadret.pushalert

internal class FakeNotificationPermissionSource(
    var isNotificationAllowed: Boolean = true
) : NotificationPermissionSource {
    override suspend fun isNotificationAllowed(): Boolean = isNotificationAllowed
}