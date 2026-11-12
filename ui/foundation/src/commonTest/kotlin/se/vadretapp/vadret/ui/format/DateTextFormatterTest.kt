package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.setting.model.HourFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private const val ENGLISH = "en"
private const val SWEDISH = "sv-SE"

class DateTextFormatterTest {

    private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")
    private val lateMondayInUtc: Instant = Instant.parse(input = "2026-10-26T23:30:00Z")

    private fun format(
        dateTextStyle: DateTextStyle,
        languageTag: String
    ): String =
        DateTextFormatter(
            dateTextStyle = dateTextStyle,
            languageTag = languageTag,
            timeZone = stockholm
        ).format(instant = lateMondayInUtc)

    @Test
    fun `weekdays follow the time zone and language`() {
        assertEquals(
            expected = listOf("Tuesday", "Tue", "tisdag", "tis"),
            actual = listOf(
                format(dateTextStyle = DateTextStyle.WEEKDAY, languageTag = ENGLISH),
                format(dateTextStyle = DateTextStyle.SHORT_WEEKDAY, languageTag = ENGLISH),
                format(dateTextStyle = DateTextStyle.WEEKDAY, languageTag = SWEDISH),
                format(dateTextStyle = DateTextStyle.SHORT_WEEKDAY, languageTag = SWEDISH)
            )
        )
    }

    @Test
    fun `a day and short month follow the language`() {
        assertEquals(
            expected = listOf("27 Oct", "27 okt."),
            actual = listOf(
                format(dateTextStyle = DateTextStyle.DAY_AND_SHORT_MONTH, languageTag = ENGLISH),
                format(dateTextStyle = DateTextStyle.DAY_AND_SHORT_MONTH, languageTag = SWEDISH)
            )
        )
    }

    @Test
    fun `a long date follows the language`() {
        assertEquals(
            expected = listOf("October 27, 2026", "27 oktober 2026"),
            actual = listOf(
                format(dateTextStyle = DateTextStyle.LONG_DATE, languageTag = ENGLISH),
                format(dateTextStyle = DateTextStyle.LONG_DATE, languageTag = SWEDISH)
            )
        )
    }

    @Test
    fun `a time on the 12 hour clock names the half of the day AM or PM`() {
        val afternoonInUtc: Instant = Instant.parse(input = "2026-10-27T13:05:00Z")

        assertEquals(
            expected = listOf("2:05 PM", "14:05"),
            actual = listOf(
                afternoonInUtc.toTimeText(hourFormat = HourFormat.TWELVE_HOUR, timeZone = stockholm),
                afternoonInUtc.toTimeText(hourFormat = HourFormat.TWENTY_FOUR_HOUR, timeZone = stockholm)
            )
        )
    }

    @Test
    fun `an hour alone is two digits on the 24 hour clock and names the half of the day on the 12 hour clock`() {
        val morningInUtc: Instant = Instant.parse(input = "2026-10-27T08:05:00Z")

        assertEquals(
            expected = listOf("9 AM", "09"),
            actual = listOf(
                morningInUtc.toHourText(hourFormat = HourFormat.TWELVE_HOUR, timeZone = stockholm),
                morningInUtc.toHourText(hourFormat = HourFormat.TWENTY_FOUR_HOUR, timeZone = stockholm)
            )
        )
    }

    @Test
    fun `midnight and noon are twelve on the 12 hour clock`() {
        val midnightInUtc: Instant = Instant.parse(input = "2026-10-26T23:05:00Z")
        val noonInUtc: Instant = Instant.parse(input = "2026-10-27T11:00:00Z")

        assertEquals(
            expected = listOf("12:05 AM", "12 AM", "12:00 PM", "12 PM"),
            actual = listOf(
                midnightInUtc.toTimeText(
                    hourFormat = HourFormat.TWELVE_HOUR,
                    timeZone = stockholm
                ),
                midnightInUtc.toHourText(
                    hourFormat = HourFormat.TWELVE_HOUR,
                    timeZone = stockholm
                ),
                noonInUtc.toTimeText(
                    hourFormat = HourFormat.TWELVE_HOUR,
                    timeZone = stockholm
                ),
                noonInUtc.toHourText(
                    hourFormat = HourFormat.TWELVE_HOUR,
                    timeZone = stockholm
                )
            )
        )
    }
}