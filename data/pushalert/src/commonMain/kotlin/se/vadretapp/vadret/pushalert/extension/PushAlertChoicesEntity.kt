package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PollenChoiceEntity
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertChoicesEntity
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal fun PushAlertChoicesEntity.toPushAlertChoices(): PushAlertChoices = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.entries.firstOrNull { warningAlertLevel -> warningAlertLevel.name == warningLowestLevel },
    pollenChoices = pollenChoices.mapNotNull { pollenChoiceEntity -> pollenChoiceEntity.toPollenChoiceOrNull() },
    temperatureTrend = temperatureTrend
)

private fun PollenChoiceEntity.toPollenChoiceOrNull(): PollenChoice? {
    val pollenAlertLevel = PollenAlertLevel.entries.firstOrNull { pollenAlertLevel -> pollenAlertLevel.level == lowestLevel } ?: return null
    return PollenChoice(
        pollenTypeId = pollenTypeId,
        lowestLevel = pollenAlertLevel
    )
}