package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
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
}