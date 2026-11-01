package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.pushalert.usecase.PublishPushAlertPlaceNamesTask

internal class PushAlertPlaceNamesAppStartHook(
    private val publishPushAlertPlaceNamesTask: PublishPushAlertPlaceNamesTask
) : AppStartHook {
    override suspend fun onAppStart() {
        publishPushAlertPlaceNamesTask()
    }
}