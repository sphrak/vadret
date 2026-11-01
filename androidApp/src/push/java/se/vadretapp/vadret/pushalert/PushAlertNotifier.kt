package se.vadretapp.vadret.pushalert

import android.content.Context
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal interface PushAlertNotifier {
    fun show(pushAlertMessage: PushAlertMessage)
}

internal fun PushAlertNotifier(context: Context): PushAlertNotifier = NotificationManagerPushAlertNotifier(context = context)