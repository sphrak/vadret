package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal class FakePushAlertNotifier : PushAlertNotifier {
    val shown: MutableList<PushAlertMessage> = mutableListOf()

    override fun show(pushAlertMessage: PushAlertMessage) {
        shown += pushAlertMessage
    }
}