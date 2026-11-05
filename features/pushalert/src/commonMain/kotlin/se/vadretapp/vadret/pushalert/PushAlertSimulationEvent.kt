package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal sealed interface PushAlertSimulationEvent {
    data class OnPushAlertClicked(val pushAlertMessage: PushAlertMessage) : PushAlertSimulationEvent
    data object OnNotificationsOffDismissed : PushAlertSimulationEvent
}