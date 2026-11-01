package se.vadretapp.vadret.radar.wind

import kotlin.test.Test
import kotlin.test.assertEquals

class WindColorScaleTest {

    @Test
    fun `stops keep their exact colors`() {
        assertEquals(expected = 0xFF3B46A0.toInt(), actual = WindColorScale.argb(windSpeed = 0f, windPalette = WindPalette.DAY))
        assertEquals(expected = 0xFF50B446.toInt(), actual = WindColorScale.argb(windSpeed = 8f, windPalette = WindPalette.DAY))
        assertEquals(expected = 0xFF963CAA.toInt(), actual = WindColorScale.argb(windSpeed = 20f, windPalette = WindPalette.DAY))
    }

    @Test
    fun `speeds between stops blend them`() {
        assertEquals(expected = 0xFF3D5AAF.toInt(), actual = WindColorScale.argb(windSpeed = 1f, windPalette = WindPalette.DAY))
    }

    @Test
    fun `speeds outside the scale use its ends`() {
        assertEquals(expected = 0xFF3B46A0.toInt(), actual = WindColorScale.argb(windSpeed = -1f, windPalette = WindPalette.DAY))
        assertEquals(expected = 0xFF6E37A0.toInt(), actual = WindColorScale.argb(windSpeed = 40f, windPalette = WindPalette.DAY))
    }

    @Test
    fun `the night palette keeps its exact colors`() {
        assertEquals(expected = 0xFF0E1430.toInt(), actual = WindColorScale.argb(windSpeed = 0f, windPalette = WindPalette.NIGHT))
        assertEquals(expected = 0xFF1E56A2.toInt(), actual = WindColorScale.argb(windSpeed = 8f, windPalette = WindPalette.NIGHT))
        assertEquals(expected = 0xFF7AC4F0.toInt(), actual = WindColorScale.argb(windSpeed = 40f, windPalette = WindPalette.NIGHT))
    }
}