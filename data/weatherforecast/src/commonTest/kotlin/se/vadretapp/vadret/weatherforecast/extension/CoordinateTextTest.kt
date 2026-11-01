package se.vadretapp.vadret.weatherforecast.extension

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateTextTest {

    @Test
    fun `a coordinate keeps up to six decimals without trailing zeros`() {
        assertEquals(
            expected = listOf("18.0686", "59.329381", "12", "0.5"),
            actual = listOf(18.0686, 59.329381, 12.0, 0.5).map { coordinate: Double -> coordinate.toCoordinateText() }
        )
    }

    @Test
    fun `a seventh decimal rounds half away from zero`() {
        assertEquals(
            expected = listOf("18.068601", "18.0686", "-18.068601", "-18.0686"),
            actual = listOf(18.0686005, 18.0686004, -18.0686005, -18.0686004).map { coordinate: Double -> coordinate.toCoordinateText() }
        )
    }

    @Test
    fun `rounding carries into the whole degrees`() {
        assertEquals(
            expected = listOf("13", "-13"),
            actual = listOf(12.9999996, -12.9999996).map { coordinate: Double -> coordinate.toCoordinateText() }
        )
    }

    @Test
    fun `a negative coordinate that rounds to zero has no sign`() {
        assertEquals(
            expected = listOf("0", "0"),
            actual = listOf(-0.0000001, 0.0).map { coordinate: Double -> coordinate.toCoordinateText() }
        )
    }
}