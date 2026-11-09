package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import se.vadretapp.vadret.weatherforecast.comingDay
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.ComingWeek
import se.vadretapp.vadret.weatherforecast.model.WeekDay
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LEVEL_TOLERANCE = 0.0001f

private val shortMonthNames: Map<Int, String> = mapOf(
    11 to "Nov",
    12 to "Dec",
    1 to "Jan"
)

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

    @Test
    fun `a day clicked in the week opens and stays open`() {
        assertEquals(
            expected = listOf(false, true) to listOf(false, true),
            actual = listOf(monday, tuesday).withDayExpanded(
                dayKey = tuesday.key,
                isExpandOnceEnabled = false
            ).expansion() to listOf(monday, tuesday.copy(isExpanded = true)).withDayExpanded(
                dayKey = tuesday.key,
                isExpandOnceEnabled = false
            ).expansion()
        )
    }

    @Test
    fun `a day clicked in the week closes the others only with expand once`() {
        val bothOpen: List<ComingDay> = listOf(
            monday.copy(isExpanded = true),
            tuesday
        )

        assertEquals(
            expected = listOf(false, true) to listOf(true, true),
            actual = bothOpen.withDayExpanded(
                dayKey = tuesday.key,
                isExpandOnceEnabled = true
            ).expansion() to bothOpen.withDayExpanded(
                dayKey = tuesday.key,
                isExpandOnceEnabled = false
            ).expansion()
        )
    }

    @Test
    fun `a day clicked in the week closes the others first only with expand once`() {
        val bothOpen: List<ComingDay> = listOf(
            monday.copy(isExpanded = true),
            tuesday.copy(isExpanded = true)
        )

        assertEquals(
            expected = listOf(false, true) to listOf(true, true),
            actual = bothOpen.withOtherDaysClosed(
                dayKey = tuesday.key,
                isExpandOnceEnabled = true
            ).expansion() to bothOpen.withOtherDaysClosed(
                dayKey = tuesday.key,
                isExpandOnceEnabled = false
            ).expansion()
        )
    }

    @Test
    fun `closing the others leaves a closed clicked day closed`() {
        assertEquals(
            expected = listOf(false, false),
            actual = listOf(monday.copy(isExpanded = true), tuesday).withOtherDaysClosed(
                dayKey = tuesday.key,
                isExpandOnceEnabled = true
            ).expansion()
        )
    }

    @Test
    fun `the week holds the first seven coming days`() {
        val comingWeek: ComingWeek? = comingDays(
            firstDate = LocalDate(
                year = 2026,
                month = 11,
                day = 9
            ),
            dayCount = 10
        ).toComingWeek()

        assertEquals(
            expected = listOf(
                "coming-day-2026-11-09",
                "coming-day-2026-11-10",
                "coming-day-2026-11-11",
                "coming-day-2026-11-12",
                "coming-day-2026-11-13",
                "coming-day-2026-11-14",
                "coming-day-2026-11-15"
            ),
            actual = comingWeek?.days?.map { weekDay: WeekDay -> weekDay.dayKey }
        )
    }

    @Test
    fun `a week starting on any weekday names the week numbers it covers`() {
        val weekNumberTexts: List<String?> = (9..15).map { day: Int ->
            comingDays(
                firstDate = LocalDate(
                    year = 2026,
                    month = 11,
                    day = day
                ),
                dayCount = 7
            ).toComingWeek()?.weekNumberText
        }

        assertEquals(
            expected = listOf("46", "46-47", "46-47", "46-47", "46-47", "46-47", "46-47"),
            actual = weekNumberTexts
        )
    }

    @Test
    fun `the week number follows the year its thursday falls in`() {
        assertEquals(
            expected = listOf("53", "53-1", "1"),
            actual = listOf(
                LocalDate(
                    year = 2026,
                    month = 12,
                    day = 28
                ),
                LocalDate(
                    year = 2027,
                    month = 1,
                    day = 1
                ),
                LocalDate(
                    year = 2027,
                    month = 1,
                    day = 4
                )
            ).map { firstDate: LocalDate ->
                comingDays(
                    firstDate = firstDate,
                    dayCount = 7
                ).toComingWeek()?.weekNumberText
            }
        )
    }

    @Test
    fun `the dates of a week in one month share the month and others name both`() {
        assertEquals(
            expected = listOf("9-15 Nov", "27 Nov-3 Dec"),
            actual = listOf(
                LocalDate(
                    year = 2026,
                    month = 11,
                    day = 9
                ),
                LocalDate(
                    year = 2026,
                    month = 11,
                    day = 27
                )
            ).map { firstDate: LocalDate ->
                comingDays(
                    firstDate = firstDate,
                    dayCount = 7
                ).toComingWeek()?.dateRangeText
            }
        )
    }

    @Test
    fun `the temperatures of the week stretch over the whole chart`() {
        val comingWeek: ComingWeek? = listOf(
            weekComingDay(
                date = LocalDate(
                    year = 2026,
                    month = 11,
                    day = 9
                ),
                temperatureRangeStart = 0.2f,
                temperatureRangeEnd = 0.4f
            ),
            weekComingDay(
                date = LocalDate(
                    year = 2026,
                    month = 11,
                    day = 10
                ),
                temperatureRangeStart = 0.3f,
                temperatureRangeEnd = 0.6f
            )
        ).toComingWeek()

        assertEquals(expected = 2, actual = comingWeek?.days?.size)
        assertLevels(
            expected = listOf(0f to 0.5f, 0.25f to 1f),
            actual = comingWeek?.days.orEmpty()
        )
    }

    @Test
    fun `a week of one unchanging temperature spans the whole chart`() {
        val comingWeek: ComingWeek? = listOf(
            weekComingDay(
                date = LocalDate(
                    year = 2026,
                    month = 11,
                    day = 9
                ),
                temperatureRangeStart = 0.5f,
                temperatureRangeEnd = 0.5f
            ),
            weekComingDay(
                date = LocalDate(
                    year = 2026,
                    month = 11,
                    day = 10
                ),
                temperatureRangeStart = 0.5f,
                temperatureRangeEnd = 0.5f
            )
        ).toComingWeek()

        assertLevels(
            expected = listOf(0f to 1f, 0f to 1f),
            actual = comingWeek?.days.orEmpty()
        )
    }

    @Test
    fun `a single day makes no week`() {
        assertEquals(
            expected = null to null,
            actual = emptyList<ComingDay>().toComingWeek() to listOf(monday).toComingWeek()
        )
    }

    private fun comingDays(
        firstDate: LocalDate,
        dayCount: Int
    ): List<ComingDay> =
        (0 until dayCount).map { dayOffset: Int ->
            val date: LocalDate = firstDate.plus(
                value = dayOffset,
                unit = DateTimeUnit.DAY
            )
            comingDay(
                dayKey = "coming-day-$date",
                date = date
            ).copy(dateText = "${date.day} ${shortMonthNames.getValue(key = date.month.number)}")
        }

    private fun weekComingDay(
        date: LocalDate,
        temperatureRangeStart: Float,
        temperatureRangeEnd: Float
    ): ComingDay =
        comingDay(
            dayKey = "coming-day-$date",
            date = date
        ).copy(
            temperatureRangeStart = temperatureRangeStart,
            temperatureRangeEnd = temperatureRangeEnd
        )

    private fun assertLevels(
        expected: List<Pair<Float, Float>>,
        actual: List<WeekDay>
    ) {
        assertEquals(expected = expected.size, actual = actual.size)
        expected.zip(other = actual).forEach { (expectedLevels: Pair<Float, Float>, weekDay: WeekDay) ->
            assertEquals(
                expected = expectedLevels.first,
                actual = weekDay.lowTemperatureLevel,
                absoluteTolerance = LEVEL_TOLERANCE
            )
            assertEquals(
                expected = expectedLevels.second,
                actual = weekDay.highTemperatureLevel,
                absoluteTolerance = LEVEL_TOLERANCE
            )
        }
    }
}