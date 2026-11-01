package se.vadretapp.vadret.weatherforecast.util

import se.vadretapp.vadret.icons.Overcast
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.VadretIcons
import kotlin.test.Test
import kotlin.test.assertEquals

private val symbolCodes: IntRange = 1..27
private val symbolCodesWithNightIcons: IntRange = 1..4
private const val UNKNOWN_SYMBOL_CODE = 99

class WeatherForecastUtilTest {

    @Test
    fun `every weather symbol has its own icon`() {
        val icons: List<VadretIconGraphic> = symbolCodes.map { symbolCode: Int ->
            WeatherForecastUtil.getWeatherSymbolIcon(
                symbolCode = symbolCode,
                isDaytime = true
            )
        }

        assertEquals(expected = symbolCodes.count(), actual = icons.toSet().size)
    }

    @Test
    fun `only clear to half clear skies change at night`() {
        val changingSymbolCodes: List<Int> = symbolCodes.filter { symbolCode: Int ->
            WeatherForecastUtil.getWeatherSymbolIcon(symbolCode = symbolCode, isDaytime = true) !=
                WeatherForecastUtil.getWeatherSymbolIcon(symbolCode = symbolCode, isDaytime = false)
        }

        assertEquals(expected = symbolCodesWithNightIcons.toList(), actual = changingSymbolCodes)
    }

    @Test
    fun `an unknown symbol is shown as overcast`() {
        assertEquals(
            expected = VadretIcons.Weather.Overcast,
            actual = WeatherForecastUtil.getWeatherSymbolIcon(
                symbolCode = UNKNOWN_SYMBOL_CODE,
                isDaytime = true
            )
        )
    }
}