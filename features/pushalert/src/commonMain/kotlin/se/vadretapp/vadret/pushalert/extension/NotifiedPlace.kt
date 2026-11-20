package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace

internal fun PushAlertChoices.isNotified(pushAlertPlace: PushAlertPlace): Boolean =
    when (pushAlertPlace) {
        is PushAlertPlace.ChosenPlace -> isChosenPlaceNotified
        is PushAlertPlace.SavedPlace -> pushAlertPlace.isNotified
    }