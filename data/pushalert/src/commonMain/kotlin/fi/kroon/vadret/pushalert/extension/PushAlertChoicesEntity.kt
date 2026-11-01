package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.PollenAlertLevel
import fi.kroon.vadret.pushalert.model.PollenChoice
import fi.kroon.vadret.pushalert.model.PollenChoiceEntity
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertChoicesEntity
import fi.kroon.vadret.pushalert.model.WarningAlertLevel

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