package se.vadretapp.vadret.pushalert.usecase

import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal interface AddPlaceNameTask {
    suspend operator fun invoke(
        pushAlertMessage: PushAlertMessage,
        favoriteIds: Set<String>
    ): PushAlertMessage
}