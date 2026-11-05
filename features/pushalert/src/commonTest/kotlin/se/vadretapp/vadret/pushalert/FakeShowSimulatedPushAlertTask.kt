package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.usecase.ShowSimulatedPushAlertTask

internal class FakeShowSimulatedPushAlertTask(
    private val isShown: Boolean
) : ShowSimulatedPushAlertTask {
    val shown: MutableList<PushAlertMessage> = mutableListOf()

    override suspend fun invoke(pushAlertMessage: PushAlertMessage): Boolean {
        if (isShown) {
            shown += pushAlertMessage
        }
        return isShown
    }
}