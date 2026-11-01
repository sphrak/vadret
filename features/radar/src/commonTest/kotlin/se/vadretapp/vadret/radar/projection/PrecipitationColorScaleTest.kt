package se.vadretapp.vadret.radar.projection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val TRANSPARENT = 0

class PrecipitationColorScaleTest {

    @Test
    fun `weak echoes and unknown values are not drawn`() {
        assertEquals(
            expected = List(size = 4) { TRANSPARENT },
            actual = listOf(Float.NaN, -30f, 0f, 4.99f).map { reflectivity: Float -> PrecipitationColorScale.argb(reflectivity = reflectivity) }
        )
    }

    @Test
    fun `each stop has the legend colour`() {
        assertEquals(
            expected = listOf(0xFF9CC9FF, 0xFF2F6FE0, 0xFF2DB34A, 0xFFF2CF2E, 0xFFEC5A2A).map { color: Long -> color.toInt() },
            actual = listOf(5f, 15f, 25f, 35f, 45f).map { reflectivity: Float -> PrecipitationColorScale.argb(reflectivity = reflectivity) }
        )
    }

    @Test
    fun `between two stops the colours blend`() {
        assertEquals(expected = 0xFF2E9195.toInt(), actual = PrecipitationColorScale.argb(reflectivity = 20f))
    }

    @Test
    fun `anything heavier than the last stop keeps its colour`() {
        assertEquals(expected = 0xFFEC5A2A.toInt(), actual = PrecipitationColorScale.argb(reflectivity = 72f))
    }

    @Test
    fun `the palette colours each echo level like its reflectivity`() {
        val palette: IntArray = PrecipitationColorScale.palette()

        assertEquals(expected = 256, actual = palette.size)
        assertEquals(
            expected = listOf(TRANSPARENT, PrecipitationColorScale.argb(reflectivity = 20f), PrecipitationColorScale.argb(reflectivity = 50f)),
            actual = listOf(palette[0], palette[125], palette[200])
        )
    }

    @Test
    fun `the first visible echo level is the first one the palette draws`() {
        val palette: IntArray = PrecipitationColorScale.palette()
        val firstVisibleEchoLevel: Int = PrecipitationColorScale.firstVisibleEchoLevel

        assertEquals(expected = 88, actual = firstVisibleEchoLevel)
        assertEquals(expected = TRANSPARENT, actual = palette[firstVisibleEchoLevel - 1])
        assertNotEquals(illegal = TRANSPARENT, actual = palette[firstVisibleEchoLevel])
    }
}