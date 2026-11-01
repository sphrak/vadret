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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val momentsAgoLimit: Duration = 1.minutes
private val aMinuteAgoLimit: Duration = 2.minutes
private val minutesAgoLimit: Duration = 1.hours
private val anHourAgoLimit: Duration = 2.hours
private val hoursAgoLimit: Duration = 1.days
private val yesterdayLimit: Duration = 2.days

fun Instant.toElapsedTime(now: Instant): ElapsedTime {
    val elapsed: Duration = now - this
    return when {
        elapsed.isNegative() -> ElapsedTime(resource = Res.string.in_the_future)
        elapsed < momentsAgoLimit -> ElapsedTime(resource = Res.string.moments_ago)
        elapsed < aMinuteAgoLimit -> ElapsedTime(resource = Res.string.a_minute_ago)
        elapsed < minutesAgoLimit -> ElapsedTime(
            resource = Res.string.minutes_ago,
            count = elapsed.inWholeMinutes
        )
        elapsed < anHourAgoLimit -> ElapsedTime(resource = Res.string.an_hour_ago)
        elapsed < hoursAgoLimit -> ElapsedTime(
            resource = Res.string.hours_ago,
            count = elapsed.inWholeHours
        )
        elapsed < yesterdayLimit -> ElapsedTime(resource = Res.string.yesterday)
        else -> ElapsedTime(
            resource = Res.string.days_ago,
            count = elapsed.inWholeDays
        )
    }
}