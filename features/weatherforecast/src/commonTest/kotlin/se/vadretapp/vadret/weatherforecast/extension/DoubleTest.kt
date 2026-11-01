package se.vadretapp.vadret.weatherforecast.extension

import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleTest {

    @Test
    fun `a whole degree has no trailing zero`() {
        assertEquals(
            expected = listOf("11", "-10", "0", "11"),
            actual = listOf(11.0, -10.0, 0.0, 10.96).map { temperature: Double -> temperature.toTemperatureText() }
        )
    }

    @Test
    fun `a fraction of a degree keeps one decimal`() {
        assertEquals(
            expected = listOf("10.9", "-0.5", "3.1", "-14.1"),
            actual = listOf(10.9, -0.5, 3.14, -14.1).map { temperature: Double -> temperature.toTemperatureText() }
        )
    }

    @Test
    fun `just below zero is not shown as minus zero`() {
        assertEquals(expected = "0", actual = (-0.04).toTemperatureText())
    }
}