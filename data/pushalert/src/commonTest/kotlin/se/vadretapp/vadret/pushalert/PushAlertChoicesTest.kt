package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
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
            "without strong wind" to EVERYTHING.copy(strongWind = false),
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
            expected = listOf(false, false, false),
            actual = listOf(
                EVERYTHING.isWithin(other = EVERYTHING.copy(warningLowestLevel = null)),
                EVERYTHING.isWithin(other = EVERYTHING.copy(temperatureTrend = false)),
                EVERYTHING.isWithin(other = EVERYTHING.copy(strongWind = false))
            )
        )
    }

    @Test
    fun thePlacesThatAreNotifiedTurnNoAlertOnOrOff() {
        assertEquals(
            expected = listOf(true, true, false),
            actual = listOf(
                EVERYTHING.copy(notifiedFavoriteIds = setOf("solna")).isWithin(other = EVERYTHING),
                EVERYTHING.isWithin(other = EVERYTHING.copy(isChosenPlaceNotified = false)),
                PushAlertChoices.NOTHING_CHOSEN.copy(notifiedFavoriteIds = setOf("solna")).isAnythingChosen()
            )
        )
    }

    @Test
    fun nothingChosenNotifiesOnlyTheChosenPlace() {
        assertEquals(
            expected = true to emptySet(),
            actual = PushAlertChoices.NOTHING_CHOSEN.isChosenPlaceNotified to PushAlertChoices.NOTHING_CHOSEN.notifiedFavoriteIds
        )
    }

    @Test
    fun anyOneAlertTurnedOnIsEnoughForSomethingToBeChosen() {
        val choices = mapOf(
            "nothing" to PushAlertChoices.NOTHING_CHOSEN,
            "only warnings" to PushAlertChoices.NOTHING_CHOSEN.copy(warningLowestLevel = WarningAlertLevel.YELLOW),
            "only pollen" to PushAlertChoices.NOTHING_CHOSEN.copy(pollenChoices = listOf(BIRCH_FROM_MODERATE)),
            "only the temperature" to PushAlertChoices.NOTHING_CHOSEN.copy(temperatureTrend = true),
            "only strong wind" to PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true),
            "everything" to EVERYTHING
        )

        assertEquals(
            expected = mapOf(
                "nothing" to false,
                "only warnings" to true,
                "only pollen" to true,
                "only the temperature" to true,
                "only strong wind" to true,
                "everything" to true
            ),
            actual = choices.mapValues { (_, pushAlertChoices) -> pushAlertChoices.isAnythingChosen() }
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
    temperatureTrend = true,
    strongWind = true,
    isChosenPlaceNotified = true,
    notifiedFavoriteIds = emptySet()
)