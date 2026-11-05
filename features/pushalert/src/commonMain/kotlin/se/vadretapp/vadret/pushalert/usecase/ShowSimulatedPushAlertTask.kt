package se.vadretapp.vadret.pushalert.usecase

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal interface ShowSimulatedPushAlertTask {
    suspend operator fun invoke(pushAlertMessage: PushAlertMessage): Boolean
}