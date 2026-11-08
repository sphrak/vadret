package se.vadretapp.vadret.pushalert.ui

import se.vadretapp.vadret.pushalert.PushAlertsState
import se.vadretapp.vadret.pushalert.model.HourFormat
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal fun pushAlertsPreviewState(): PushAlertsState = PushAlertsState(
    isLoading = false,
    choices = PushAlertChoices(
        warningLowestLevel = WarningAlertLevel.YELLOW,
        pollenLowestLevel = PollenAlertLevel.MODERATE_TO_HIGH,
        temperatureTrend = true,
        strongWind = false,
        slipperiness = true,
        hourFormat = HourFormat.TWENTY_FOUR_HOUR,
        isChosenPlaceNotified = true,
        notifiedFavoriteIds = setOf(PREVIEW_UPPSALA_ID)
    ),
    hasPollenAllergens = true,
    places = listOf(
        PushAlertPlace.ChosenPlace(name = null),
        PushAlertPlace.SavedPlace(
            favoriteId = PREVIEW_UPPSALA_ID,
            name = "Uppsala"
        ),
        PushAlertPlace.SavedPlace(
            favoriteId = PREVIEW_KIRUNA_ID,
            name = "Kiruna"
        )
    ),
    isSyncPending = true
)

private const val PREVIEW_UPPSALA_ID = "preview-uppsala"
private const val PREVIEW_KIRUNA_ID = "preview-kiruna"