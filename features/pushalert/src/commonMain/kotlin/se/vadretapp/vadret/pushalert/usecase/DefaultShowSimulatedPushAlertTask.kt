package se.vadretapp.vadret.pushalert.usecase

import se.vadretapp.vadret.pushalert.NotificationPermissionSource
import se.vadretapp.vadret.pushalert.PushAlertNotifier
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

// Tells whether the alert was shown, so the caller can explain why nothing appeared when notifications are off.
internal class DefaultShowSimulatedPushAlertTask(
    private val notificationPermissionSource: NotificationPermissionSource,
    private val pushAlertNotifier: PushAlertNotifier
) : ShowSimulatedPushAlertTask {
    override suspend fun invoke(pushAlertMessage: PushAlertMessage): Boolean {
        val isNotificationAllowed: Boolean = notificationPermissionSource.isNotificationAllowed()
        if (isNotificationAllowed) {
            pushAlertNotifier.show(pushAlertMessage = pushAlertMessage)
        }
        return isNotificationAllowed
    }
}