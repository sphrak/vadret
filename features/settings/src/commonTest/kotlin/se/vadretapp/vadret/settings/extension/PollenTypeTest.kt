package se.vadretapp.vadret.settings.extension

import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.settings.model.PollenGroup
import se.vadretapp.vadret.settings.model.PollenGroupRows
import kotlin.test.Test
import kotlin.test.assertEquals

private val spruce = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323338",
    name = "Gran",
    hasForecasts = true
)

private val cladosporium = PollenType(
    pollenId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323834",
    name = "Cladosporium",
    hasForecasts = true
)

private val unknownPollen = PollenType(
    pollenId = "new-pollen",
    name = "Ny pollen",
    hasForecasts = true
)

class PollenTypeTest {

    @Test
    fun `pollen are grouped in a fixed order and a group without pollen is left out`() {
        assertEquals(
            expected = listOf(
                PollenGroup.TREES to listOf("Gran"),
                PollenGroup.SPORES to listOf("Cladosporium"),
                PollenGroup.OTHER to listOf("Ny pollen")
            ),
            actual = listOf(unknownPollen, cladosporium, spruce)
                .toPollenGroupRows(allergenPollenIds = emptySet())
                .map { pollenGroupRows: PollenGroupRows -> pollenGroupRows.pollenGroup to pollenGroupRows.pollenAllergenRows.map { pollenAllergenRow: PollenAllergenRow -> pollenAllergenRow.name } }
        )
    }

    @Test
    fun `a pollen marked before is marked in its group`() {
        assertEquals(
            expected = listOf(true),
            actual = listOf(spruce)
                .toPollenGroupRows(allergenPollenIds = setOf(spruce.pollenId))
                .flatMap { pollenGroupRows: PollenGroupRows -> pollenGroupRows.pollenAllergenRows.map { pollenAllergenRow: PollenAllergenRow -> pollenAllergenRow.isAllergen } }
        )
    }
}