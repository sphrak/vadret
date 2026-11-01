package fi.kroon.vadret.pushalert

import fi.kroon.vadret.pushalert.model.PushAlertMessage

internal interface PushAlertNotifier {
    fun show(pushAlertMessage: PushAlertMessage)
}

internal fun PushAlertNotifier(): PushAlertNotifier = UserNotificationPushAlertNotifier()