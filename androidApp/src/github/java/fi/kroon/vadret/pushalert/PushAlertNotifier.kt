package fi.kroon.vadret.pushalert

import android.content.Context
import fi.kroon.vadret.pushalert.model.PushAlertMessage

internal interface PushAlertNotifier {
    fun show(pushAlertMessage: PushAlertMessage)
}

internal fun PushAlertNotifier(context: Context): PushAlertNotifier = NotificationManagerPushAlertNotifier(context = context)