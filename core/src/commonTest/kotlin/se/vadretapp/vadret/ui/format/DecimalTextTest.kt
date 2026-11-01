package se.vadretapp.vadret.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalTextTest {

    @Test
    fun `one decimal is always kept with a dot`() {
        assertEquals(
            expected = listOf("5.0", "0.1", "12.3", "-3.0"),
            actual = listOf(5.0, 0.1, 12.34, -3.0).map { value: Double -> value.toOneDecimalText() }
        )
    }

    @Test
    fun `one decimal rounds half away from zero`() {
        assertEquals(
            expected = listOf("2.3", "-2.3", "0.2"),
            actual = listOf(2.25, -2.25, 0.15).map { value: Double -> value.toOneDecimalText() }
        )
    }

    @Test
    fun `a value that rounds to zero has no sign`() {
        assertEquals(expected = "0.0", actual = (-0.04).toOneDecimalText())
    }
}