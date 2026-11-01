package se.vadretapp.vadret.weatherforecast.util

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TENTHS_PER_DEGREE = 10

private fun Double?.inTenths(): Int? =
    if (this == null) {
        null
    } else {
        (this * TENTHS_PER_DEGREE).roundToInt()
    }

class WindChillTest {

    @Test
    fun `wind makes the cold feel colder`() {
        assertEquals(
            expected = listOf(-141, -336),
            actual = listOf(
                WindChill.calculate(temperatureCelcius = -10.0, windSpeedMeterPerSecond = 2.0),
                WindChill.calculate(temperatureCelcius = -20.0, windSpeedMeterPerSecond = 10.0)
            ).map { windChill: Double? -> windChill.inTenths() }
        )
    }

    @Test
    fun `the edges of the formula are included`() {
        assertEquals(
            expected = listOf(-491, 92),
            actual = listOf(
                WindChill.calculate(temperatureCelcius = -40.0, windSpeedMeterPerSecond = 2.0),
                WindChill.calculate(temperatureCelcius = 10.0, windSpeedMeterPerSecond = 2.0)
            ).map { windChill: Double? -> windChill.inTenths() }
        )
    }

    @Test
    fun `outside the formula there is no wind chill`() {
        assertEquals(
            expected = listOf(null, null, null, null),
            actual = listOf(
                WindChill.calculate(temperatureCelcius = -10.0, windSpeedMeterPerSecond = 1.9),
                WindChill.calculate(temperatureCelcius = -10.0, windSpeedMeterPerSecond = 35.1),
                WindChill.calculate(temperatureCelcius = 10.1, windSpeedMeterPerSecond = 5.0),
                WindChill.calculate(temperatureCelcius = -40.1, windSpeedMeterPerSecond = 5.0)
            )
        )
    }
}