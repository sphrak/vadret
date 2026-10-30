package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.PollenStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun pollenMap(
    pollenTypes: List<PollenType>,
    vararg pollenLevels: Map<String, PollenLevel>
): PollenMap =
    PollenMap(
        date = LocalDate.parse(input = "2026-07-13"),
        pollenTypes = pollenTypes,
        stations = pollenLevels.mapIndexed { index: Int, stationLevels: Map<String, PollenLevel> ->
            PollenStation(
                regionId = "station-$index",
                name = "Station $index",
                latitude = index.toDouble(),
                longitude = index.toDouble(),
                pollenLevels = stationLevels
            )
        }
    )

private val birch = PollenType(
    pollenId = "birch",
    name = "Björk",
    hasForecasts = true
)

private val grass = PollenType(
    pollenId = "grass",
    name = "Gräs",
    hasForecasts = true
)

class PollenMapTest {

    @Test
    fun `the strongest type is the one with the highest level at any station`() {
        assertEquals(
            expected = "grass",
            actual = pollenMap(
                pollenTypes = listOf(birch, grass),
                mapOf("birch" to PollenLevel.MODERATE, "grass" to PollenLevel.LOW),
                mapOf("grass" to PollenLevel.HIGH)
            ).strongestPollenId(allergenPollenIds = emptySet())
        )
    }

    @Test
    fun `equally strong types keep their order and no types means none`() {
        assertEquals(
            expected = "birch",
            actual = pollenMap(
                pollenTypes = listOf(birch, grass),
                mapOf("birch" to PollenLevel.LOW, "grass" to PollenLevel.LOW)
            ).strongestPollenId(allergenPollenIds = emptySet())
        )
        assertNull(actual = pollenMap(pollenTypes = emptyList()).strongestPollenId(allergenPollenIds = emptySet()))
    }

    @Test
    fun `the strongest allergen that is flying wins over stronger types`() {
        val pollenTypes: List<PollenType> = listOf(birch, grass)

        assertEquals(
            expected = listOf("birch", "grass", "grass"),
            actual = listOf(
                pollenMap(
                    pollenTypes = pollenTypes,
                    mapOf("birch" to PollenLevel.LOW, "grass" to PollenLevel.VERY_HIGH)
                ).strongestPollenId(allergenPollenIds = setOf("birch")),
                pollenMap(
                    pollenTypes = pollenTypes,
                    mapOf("birch" to PollenLevel.NONE, "grass" to PollenLevel.LOW)
                ).strongestPollenId(allergenPollenIds = setOf("birch")),
                pollenMap(
                    pollenTypes = pollenTypes,
                    mapOf("birch" to PollenLevel.MODERATE, "grass" to PollenLevel.HIGH)
                ).strongestPollenId(allergenPollenIds = setOf("oak"))
            )
        )
    }

    @Test
    fun `allergens are listed first and the other types keep their order`() {
        val hazel = PollenType(
            pollenId = "hazel",
            name = "Hassel",
            hasForecasts = true
        )
        val threeTypes: PollenMap = pollenMap(pollenTypes = listOf(birch, grass, hazel))

        assertEquals(
            expected = listOf(
                listOf(hazel, birch, grass),
                listOf(grass, hazel, birch),
                listOf(birch, grass, hazel)
            ),
            actual = listOf(
                threeTypes.pollenTypesWithAllergensFirst(allergenPollenIds = setOf("hazel")),
                threeTypes.pollenTypesWithAllergensFirst(allergenPollenIds = setOf("hazel", "grass")),
                threeTypes.pollenTypesWithAllergensFirst(allergenPollenIds = emptySet())
            )
        )
    }

    @Test
    fun `markers come only from stations that forecast the type`() {
        val map: PollenMap = pollenMap(
            pollenTypes = listOf(birch, grass),
            mapOf("grass" to PollenLevel.NONE),
            mapOf("birch" to PollenLevel.HIGH)
        )

        assertEquals(
            expected = listOf(
                PollenMarker(
                    latitude = 0.0,
                    longitude = 0.0,
                    pollenLevel = PollenLevel.NONE
                )
            ),
            actual = map.markersFor(pollenId = "grass")
        )
        assertEquals(expected = emptyList(), actual = map.markersFor(pollenId = null))
    }
}