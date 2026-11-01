package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal interface PushAlertNotifier {
    fun show(pushAlertMessage: PushAlertMessage)
}

internal fun PushAlertNotifier(): PushAlertNotifier = UserNotificationPushAlertNotifier()