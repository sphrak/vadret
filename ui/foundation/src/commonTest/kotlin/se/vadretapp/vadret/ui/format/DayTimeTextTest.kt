package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.setting.model.HourFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val ENGLISH = "en"
private const val SWEDISH = "sv-SE"
private const val DAYS_IN_A_WEEK = 7
private const val ONE_DAY = 1
private const val SIX_DAYS = 6

private val toOnePmLocal = 3.hours
private val toHalfPastMidnightLocal = 14.hours + 30.minutes

class DayTimeTextTest {

    private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")
    private val mondayMorning: Instant = Instant.parse(input = "2026-10-26T09:00:00Z")
    private val weekMornings: List<Instant> = List(size = DAYS_IN_A_WEEK) { dayIndex: Int -> mondayMorning + dayIndex.days }

    private fun Instant.text(
        now: Instant,
        languageTag: String = ENGLISH,
        hourFormat: HourFormat = HourFormat.TWENTY_FOUR_HOUR
    ): String =
        toDayTimeText(
            now = now,
            hourFormat = hourFormat,
            timeZone = stockholm,
            languageTag = languageTag
        )

    @Test
    fun `today is told by its time alone on every weekday`() {
        assertEquals(
            expected = List(size = DAYS_IN_A_WEEK) { "13:00" },
            actual = weekMornings.map { now: Instant -> (now + toOnePmLocal).text(now = now) }
        )
    }

    @Test
    fun `today and the coming week follow the 12 hour clock with AM and PM whatever the language`() {
        val now: Instant = mondayMorning

        assertEquals(
            expected = listOf("1:00 PM", "Tue 12:30 AM", "tis 12:30 AM"),
            actual = listOf(
                (now + toOnePmLocal).text(
                    now = now,
                    hourFormat = HourFormat.TWELVE_HOUR
                ),
                (now + toHalfPastMidnightLocal).text(
                    now = now,
                    hourFormat = HourFormat.TWELVE_HOUR
                ),
                (now + toHalfPastMidnightLocal).text(
                    now = now,
                    languageTag = SWEDISH,
                    hourFormat = HourFormat.TWELVE_HOUR
                )
            )
        )
    }

    @Test
    fun `the coming week is told by weekday and local time on every weekday`() {
        assertEquals(
            expected = listOf(
                listOf("Tue 00:30", "Sun 13:00", "tis 13:00"),
                listOf("Wed 00:30", "Mon 13:00", "ons 13:00"),
                listOf("Thu 00:30", "Tue 13:00", "tors 13:00"),
                listOf("Fri 00:30", "Wed 13:00", "fre 13:00"),
                listOf("Sat 00:30", "Thu 13:00", "lör 13:00"),
                listOf("Sun 00:30", "Fri 13:00", "sön 13:00"),
                listOf("Mon 00:30", "Sat 13:00", "mån 13:00")
            ),
            actual = weekMornings.map { now: Instant ->
                listOf(
                    (now + toHalfPastMidnightLocal).text(now = now),
                    (now + SIX_DAYS.days + toOnePmLocal).text(now = now),
                    (now + ONE_DAY.days + toOnePmLocal).text(
                        now = now,
                        languageTag = SWEDISH
                    )
                )
            }
        )
    }

    @Test
    fun `the past week is told by weekday and local time on every weekday`() {
        assertEquals(
            expected = listOf("Sun 13:00", "Mon 13:00", "Tue 13:00", "Wed 13:00", "Thu 13:00", "Fri 13:00", "Sat 13:00"),
            actual = weekMornings.map { now: Instant -> (now - ONE_DAY.days + toOnePmLocal).text(now = now) }
        )
    }

    @Test
    fun `a week or more away is told by its date on every weekday`() {
        assertEquals(
            expected = listOf(
                listOf("2 Nov", "19 Oct", "21 Apr"),
                listOf("3 Nov", "20 Oct", "21 Apr"),
                listOf("4 Nov", "21 Oct", "21 Apr"),
                listOf("5 Nov", "22 Oct", "21 Apr"),
                listOf("6 Nov", "23 Oct", "21 Apr"),
                listOf("7 Nov", "24 Oct", "21 Apr"),
                listOf("8 Nov", "25 Oct", "21 Apr")
            ),
            actual = weekMornings.map { now: Instant ->
                listOf(
                    (now + DAYS_IN_A_WEEK.days + toOnePmLocal).text(now = now),
                    (now - DAYS_IN_A_WEEK.days + toOnePmLocal).text(now = now),
                    Instant.parse(input = "2026-04-20T22:00:00Z").text(now = now)
                )
            }
        )
    }
}