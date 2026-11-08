package se.vadretapp.vadret.pushalert.ui

import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.PushAlertsState
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal fun pushAlertsPreviewState(): PushAlertsState = PushAlertsState(
    isLoading = false,
    choices = PushAlertChoices(
        warningLowestLevel = WarningAlertLevel.YELLOW,
        pollenChoices = listOf(
            PollenChoice(
                pollenTypeId = PREVIEW_BIRCH_ID,
                lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH
            )
        ),
        temperatureTrend = true,
        strongWind = false,
        slipperiness = true,
        isChosenPlaceNotified = true,
        notifiedFavoriteIds = setOf(PREVIEW_UPPSALA_ID)
    ),
    chosenPollenTypeIds = setOf(PREVIEW_BIRCH_ID),
    pollenLevel = PollenAlertLevel.MODERATE_TO_HIGH,
    pollenTypes = listOf(
        PollenType(
            pollenId = PREVIEW_BIRCH_ID,
            name = "Björk",
            hasForecasts = true
        ),
        PollenType(
            pollenId = PREVIEW_GRASS_ID,
            name = "Gräs",
            hasForecasts = true
        )
    ),
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

private const val PREVIEW_BIRCH_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"
private const val PREVIEW_GRASS_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323237"
private const val PREVIEW_UPPSALA_ID = "preview-uppsala"
private const val PREVIEW_KIRUNA_ID = "preview-kiruna"