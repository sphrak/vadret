package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.comingDay
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import kotlin.test.Test
import kotlin.test.assertEquals

class ComingDayTest {

    private val monday: ComingDay = comingDay(dayKey = "coming-day-2026-11-16")
    private val tuesday: ComingDay = comingDay(dayKey = "coming-day-2026-11-17")

    private fun List<ComingDay>.expansion(): List<Boolean> = map { comingDay: ComingDay -> comingDay.isExpanded }

    @Test
    fun `a toggled day opens alone and closes again`() {
        val opened: List<ComingDay> = listOf(monday, tuesday).withDayToggled(
            dayKey = tuesday.key,
            isExpandOnceEnabled = false
        )

        assertEquals(expected = listOf(false, true), actual = opened.expansion())
        assertEquals(
            expected = listOf(false, false),
            actual = opened.withDayToggled(
                dayKey = tuesday.key,
                isExpandOnceEnabled = false
            ).expansion()
        )
    }

    @Test
    fun `with expand once a toggled day closes every other day`() {
        val bothOpen: List<ComingDay> = listOf(
            monday.copy(isExpanded = true),
            tuesday
        )

        assertEquals(
            expected = listOf(false, true),
            actual = bothOpen
                .withDayToggled(
                    dayKey = tuesday.key,
                    isExpandOnceEnabled = true
                ).expansion()
        )
        assertEquals(
            expected = listOf(false, false),
            actual = listOf(monday.copy(isExpanded = true), tuesday.copy(isExpanded = true))
                .withDayToggled(
                    dayKey = tuesday.key,
                    isExpandOnceEnabled = true
                ).expansion()
        )
    }

    @Test
    fun `toggling a day that is not shown changes nothing`() {
        assertEquals(
            expected = listOf(monday, tuesday),
            actual = listOf(monday, tuesday).withDayToggled(
                dayKey = "coming-day-2026-11-18",
                isExpandOnceEnabled = false
            )
        )
    }

    @Test
    fun `open days stay open in a new forecast and new days start closed`() {
        val previousComingDays: List<ComingDay> = listOf(
            comingDay(
                dayKey = "coming-day-2026-11-15",
                isExpanded = true
            ),
            monday.copy(isExpanded = true),
            tuesday
        )

        val comingDays: List<ComingDay> = listOf(
            monday,
            tuesday.copy(isExpanded = true),
            comingDay(dayKey = "coming-day-2026-11-18")
        ).withExpansionOf(previousComingDays = previousComingDays)

        assertEquals(expected = listOf(true, false, false), actual = comingDays.expansion())
    }
}