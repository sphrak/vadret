package fi.kroon.vadret.pushalert.ui

import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pushalert.PushAlertsState
import fi.kroon.vadret.pushalert.model.PollenAlertLevel
import fi.kroon.vadret.pushalert.model.PollenChoice
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.WarningAlertLevel

internal fun pushAlertsPreviewState(): PushAlertsState = PushAlertsState(
    choices = PushAlertChoices(
        warningLowestLevel = WarningAlertLevel.YELLOW,
        pollenChoices = listOf(
            PollenChoice(
                pollenTypeId = PREVIEW_BIRCH_ID,
                lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH
            )
        ),
        temperatureTrend = true
    ),
    chosenPollenTypeIds = setOf(PREVIEW_BIRCH_ID),
    pollenLevel = PollenAlertLevel.MODERATE_TO_HIGH,
    pollenTypes = listOf(
        PollenType(
            pollenId = PREVIEW_BIRCH_ID,
            name = "Björk"
        ),
        PollenType(
            pollenId = PREVIEW_GRASS_ID,
            name = "Gräs"
        )
    ),
    isSyncPending = true
)

private const val PREVIEW_BIRCH_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"
private const val PREVIEW_GRASS_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323237"