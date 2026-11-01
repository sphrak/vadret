package fi.kroon.vadret.pushalert

import fi.kroon.vadret.pushalert.model.PollenAlertLevel
import fi.kroon.vadret.pushalert.model.PollenChoice
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.WarningAlertLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertChoicesTest {
    @Test
    fun choicesAreWithinOthersOnlyWhenTheyTurnSomethingOffAndChangeNothingElse() {
        val narrower = mapOf(
            "the same choices" to EVERYTHING,
            "nothing" to PushAlertChoices.NOTHING_CHOSEN,
            "without warnings" to EVERYTHING.copy(warningLowestLevel = null),
            "without the temperature" to EVERYTHING.copy(temperatureTrend = false),
            "without one pollen" to EVERYTHING.copy(pollenChoices = listOf(BIRCH_FROM_MODERATE))
        )
        val wider = mapOf(
            "another warning level" to EVERYTHING.copy(warningLowestLevel = WarningAlertLevel.RED),
            "another pollen level" to EVERYTHING.copy(
                pollenChoices = listOf(
                    BIRCH_FROM_MODERATE.copy(lowestLevel = PollenAlertLevel.HIGH),
                    GRASS_FROM_LOW
                )
            ),
            "another pollen" to EVERYTHING.copy(pollenChoices = listOf(BIRCH_FROM_MODERATE, GRASS_FROM_LOW, ALDER_FROM_LOW))
        )

        assertEquals(
            expected = narrower.mapValues { true } + wider.mapValues { false },
            actual = (narrower + wider).mapValues { (_, pushAlertChoices) -> pushAlertChoices.isWithin(other = EVERYTHING) }
        )
        assertEquals(
            expected = listOf(false, false),
            actual = listOf(
                EVERYTHING.isWithin(other = EVERYTHING.copy(warningLowestLevel = null)),
                EVERYTHING.isWithin(other = EVERYTHING.copy(temperatureTrend = false))
            )
        )
    }
}

private val BIRCH_FROM_MODERATE = PollenChoice(
    pollenTypeId = "birch",
    lowestLevel = PollenAlertLevel.MODERATE
)
private val GRASS_FROM_LOW = PollenChoice(
    pollenTypeId = "grass",
    lowestLevel = PollenAlertLevel.LOW
)
private val ALDER_FROM_LOW = PollenChoice(
    pollenTypeId = "alder",
    lowestLevel = PollenAlertLevel.LOW
)
private val EVERYTHING = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenChoices = listOf(BIRCH_FROM_MODERATE, GRASS_FROM_LOW),
    temperatureTrend = true
)