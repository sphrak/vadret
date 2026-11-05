package se.vadretapp.vadret.pushalert

import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal data class PushAlertSimulationState(
    val pushAlertMessages: ImmutableList<PushAlertMessage> = simulatedPushAlertMessages,
    val isNotificationsOffDialogVisible: Boolean = false
)