package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace

internal fun PushAlertChoices.isNotified(pushAlertPlace: PushAlertPlace): Boolean =
    when (pushAlertPlace) {
        is PushAlertPlace.ChosenPlace -> isChosenPlaceNotified
        is PushAlertPlace.SavedPlace -> pushAlertPlace.favoriteId in notifiedFavoriteIds
    }

internal fun PushAlertChoices.withNotified(
    pushAlertPlace: PushAlertPlace,
    isNotified: Boolean
): PushAlertChoices =
    when (pushAlertPlace) {
        is PushAlertPlace.ChosenPlace -> copy(isChosenPlaceNotified = isNotified)
        is PushAlertPlace.SavedPlace -> copy(
            notifiedFavoriteIds = if (isNotified) {
                notifiedFavoriteIds + pushAlertPlace.favoriteId
            } else {
                notifiedFavoriteIds - pushAlertPlace.favoriteId
            }
        )
    }