package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.comingDay
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherForecastStateTest {

    private val openState: WeatherForecastState = WeatherForecastState().withComingDays(
        comingDays = persistentListOf(
            comingDay(
                dayKey = "coming-day-2026-11-16",
                isExpanded = true
            )
        )
    )
    private val newForecast = WeatherForecast(
        comingDays = persistentListOf(
            comingDay(dayKey = "coming-day-2026-11-16"),
            comingDay(dayKey = "coming-day-2026-11-17")
        )
    )

    private fun WeatherForecastState.expansion(): List<Boolean> =
        forecast.comingDays.map { comingDay: ComingDay ->
            comingDay.isExpanded
        }

    @Test
    fun `new coming days replace the shown ones`() {
        val comingDays: ImmutableList<ComingDay> = persistentListOf(comingDay(dayKey = "coming-day-2026-11-17"))

        assertEquals(
            expected = comingDays,
            actual = openState.withComingDays(comingDays = comingDays).forecast.comingDays
        )
    }

    @Test
    fun `a new forecast for the same place keeps the open days`() {
        val state: WeatherForecastState = openState.withForecast(
            forecast = newForecast,
            isSamePlace = true
        )

        assertEquals(expected = listOf(true, false), actual = state.expansion())
    }

    @Test
    fun `a forecast for another place starts with every day closed`() {
        val state: WeatherForecastState = openState.withForecast(
            forecast = newForecast,
            isSamePlace = false
        )

        assertEquals(expected = listOf(false, false), actual = state.expansion())
    }
}