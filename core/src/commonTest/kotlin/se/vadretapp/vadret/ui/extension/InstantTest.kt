package se.vadretapp.vadret.ui.extension

import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.a_minute_ago
import se.vadretapp.vadret.core.resources.an_hour_ago
import se.vadretapp.vadret.core.resources.days_ago
import se.vadretapp.vadret.core.resources.hours_ago
import se.vadretapp.vadret.core.resources.in_the_future
import se.vadretapp.vadret.core.resources.minutes_ago
import se.vadretapp.vadret.core.resources.moments_ago
import se.vadretapp.vadret.core.resources.yesterday
import se.vadretapp.vadret.ui.model.ElapsedTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class InstantTest {

    private val now: Instant = Instant.parse(input = "2026-10-24T08:30:00Z")

    @Test
    fun `elapsed time picks the closest unit with the edges included`() {
        val cases: Map<Duration, ElapsedTime> = mapOf(
            (-1).seconds to ElapsedTime(resource = Res.string.in_the_future),
            0.seconds to ElapsedTime(resource = Res.string.moments_ago),
            59.seconds to ElapsedTime(resource = Res.string.moments_ago),
            1.minutes to ElapsedTime(resource = Res.string.a_minute_ago),
            119.seconds to ElapsedTime(resource = Res.string.a_minute_ago),
            2.minutes to ElapsedTime(resource = Res.string.minutes_ago, count = 2),
            59.minutes to ElapsedTime(resource = Res.string.minutes_ago, count = 59),
            1.hours to ElapsedTime(resource = Res.string.an_hour_ago),
            119.minutes to ElapsedTime(resource = Res.string.an_hour_ago),
            2.hours to ElapsedTime(resource = Res.string.hours_ago, count = 2),
            23.hours to ElapsedTime(resource = Res.string.hours_ago, count = 23),
            1.days to ElapsedTime(resource = Res.string.yesterday),
            47.hours to ElapsedTime(resource = Res.string.yesterday),
            2.days to ElapsedTime(resource = Res.string.days_ago, count = 2),
            30.days to ElapsedTime(resource = Res.string.days_ago, count = 30)
        )

        assertEquals(
            expected = cases,
            actual = cases.keys.associateWith { elapsed: Duration -> (now - elapsed).toElapsedTime(now = now) }
        )
    }
}