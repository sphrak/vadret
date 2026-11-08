import UserNotifications

// The server sends every push as a placeholder alert with mutable-content, so iOS shows it even when
// the app is swiped away. Here the placeholder gets the title and body the phone alone can read.
final class NotificationService: UNNotificationServiceExtension {
    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        guard let content = request.content.mutableCopy() as? UNMutableNotificationContent,
              let pushAlert = SealedPushAlert(userInfo: request.content.userInfo) else {
            contentHandler(request.content)
            return
        }
        content.title = pushAlert.title
        content.body = pushAlert.body
        content.threadIdentifier = pushAlert.kind
        content.userInfo[pushAlertDataKey] = pushAlert.data
        contentHandler(content)
    }
}

private let pushAlertDataKey = "push_alert_data"
