package se.vadretapp.vadret.pushalert

import platform.Foundation.NSError
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal class UserNotificationPushAlertNotifier : PushAlertNotifier {
    override fun show(pushAlertMessage: PushAlertMessage) {
        val notificationContent = UNMutableNotificationContent()
        notificationContent.setTitle(pushAlertMessage.title)
        notificationContent.setBody(pushAlertMessage.body)
        notificationContent.setThreadIdentifier(pushAlertMessage.kind.name)
        val notificationRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = pushAlertMessage.notificationId ?: pushAlertMessage.title,
            content = notificationContent,
            trigger = null
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request = notificationRequest) { error: NSError? ->
            if (error != null) {
                logger.warn { "A push alert of kind ${pushAlertMessage.kind} could not be shown: ${error.localizedDescription}" }
            }
        }
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")