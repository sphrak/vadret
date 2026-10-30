package se.vadretapp.vadret.radar.pollen

import androidx.compose.ui.graphics.Color
import se.vadretapp.vadret.pollen.model.PollenLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class PollenColorScaleTest {

    @Test
    fun `every pollen level has a color of its own`() {
        assertEquals(
            expected = PollenLevel.entries.size,
            actual = PollenLevel.entries.map { pollenLevel: PollenLevel -> PollenColorScale.argb(pollenLevel = pollenLevel) }.distinct().size
        )
    }

    @Test
    fun `the legend runs from low to very high in the colors of the map`() {
        assertEquals(
            expected = PollenLevel.entries
                .filter { pollenLevel: PollenLevel -> pollenLevel != PollenLevel.NONE }
                .map { pollenLevel: PollenLevel -> Color(color = PollenColorScale.argb(pollenLevel = pollenLevel)) },
            actual = PollenColorScale.legendColors
        )
    }
}