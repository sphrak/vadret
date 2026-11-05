package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

interface PushAlertNotifier {
    fun show(pushAlertMessage: PushAlertMessage)
}