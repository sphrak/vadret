package se.vadretapp.vadret.radar.wind

import kotlin.test.Test
import kotlin.test.assertEquals

class RasterAreaTest {

    private val screen = RasterArea(
        left = 10f,
        top = 10f,
        right = 20f,
        bottom = 30f
    )

    @Test
    fun `panning right uncovers a strip on the right`() {
        val panned = RasterArea(left = 14f, top = 10f, right = 24f, bottom = 30f)

        assertEquals(
            expected = listOf(RasterArea(left = 20f, top = 10f, right = 24f, bottom = 30f)),
            actual = panned.uncoveredBy(other = screen)
        )
    }

    @Test
    fun `panning diagonally uncovers a side and a bottom strip`() {
        val panned = RasterArea(left = 12f, top = 15f, right = 22f, bottom = 35f)

        val uncovered: List<RasterArea> = panned.uncoveredBy(other = screen)

        assertEquals(expected = panned.area - 8f * 15f, actual = uncovered.sumOf { strip: RasterArea -> strip.area.toDouble() }.toFloat())
    }

    @Test
    fun `zooming out uncovers a ring around the previous screen`() {
        val zoomedOut = RasterArea(left = 5f, top = 0f, right = 25f, bottom = 40f)

        val uncovered: List<RasterArea> = zoomedOut.uncoveredBy(other = screen)

        assertEquals(expected = 4, actual = uncovered.size)
        assertEquals(expected = zoomedOut.area - screen.area, actual = uncovered.sumOf { strip: RasterArea -> strip.area.toDouble() }.toFloat())
    }

    @Test
    fun `zooming in uncovers nothing`() {
        val zoomedIn = RasterArea(left = 12f, top = 12f, right = 18f, bottom = 28f)

        assertEquals(expected = emptyList(), actual = zoomedIn.uncoveredBy(other = screen))
    }

    @Test
    fun `jumping far away uncovers the whole screen`() {
        val elsewhere = RasterArea(left = 100f, top = 100f, right = 110f, bottom = 120f)

        assertEquals(expected = listOf(elsewhere), actual = elsewhere.uncoveredBy(other = screen))
    }
}