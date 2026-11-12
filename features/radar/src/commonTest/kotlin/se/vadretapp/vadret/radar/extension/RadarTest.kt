package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.model.ElapsedTime
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.an_hour_ago
import se.vadretapp.vadret.ui.resources.minutes_ago
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val stockholm: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")

class RadarTest {

    @Test
    fun `frames follow the 12 hour clock with AM and PM whatever the language`() {
        val radar = Radar(
            images = listOf(
                RadarImage(imageUrl = "https://smhi.se/2350.png", validAt = Instant.parse(input = "2026-10-24T21:50:00Z")),
                RadarImage(imageUrl = "https://smhi.se/1405.png", validAt = Instant.parse(input = "2026-10-24T12:05:00Z"))
            ),
            fetchedAt = Instant.parse(input = "2026-10-24T23:00:00Z")
        )

        assertEquals(
            expected = listOf("11:50 PM", "2:05 PM") to listOf("11:50 PM", "2:05 PM"),
            actual = listOf("en", "sv-SE").map { languageTag: String ->
                radar.toRadarFrames(
                    now = Instant.parse(input = "2026-10-24T23:10:00Z"),
                    timeZone = stockholm,
                    languageTag = languageTag,
                    hourFormat = HourFormat.TWELVE_HOUR
                ).map { radarFrame: RadarFrame -> radarFrame.timeText }
            }.zipWithNext().single()
        )
    }

    @Test
    fun `frames show the local time with weekday and age past midnight too`() {
        val radar = Radar(
            images = listOf(
                RadarImage(imageUrl = "https://smhi.se/2350.png", validAt = Instant.parse(input = "2026-10-24T21:50:00Z")),
                RadarImage(imageUrl = "https://smhi.se/0055.png", validAt = Instant.parse(input = "2026-10-24T22:55:00Z"))
            ),
            fetchedAt = Instant.parse(input = "2026-10-24T23:00:00Z")
        )

        assertEquals(
            expected = listOf(
                RadarFrame(
                    imageUrl = "https://smhi.se/2350.png",
                    timeText = "23:50",
                    weekday = "Saturday",
                    elapsedTime = ElapsedTime(resource = Res.string.an_hour_ago)
                ),
                RadarFrame(
                    imageUrl = "https://smhi.se/0055.png",
                    timeText = "00:55",
                    weekday = "Sunday",
                    elapsedTime = ElapsedTime(resource = Res.string.minutes_ago, count = 15)
                )
            ),
            actual = radar.toRadarFrames(
                now = Instant.parse(input = "2026-10-24T23:10:00Z"),
                timeZone = stockholm,
                languageTag = "en",
                hourFormat = HourFormat.TWENTY_FOUR_HOUR
            )
        )
    }

    @Test
    fun `wind hours show the local time and weekday`() {
        val windForecast = WindForecast(
            referenceTime = Instant.parse(input = "2026-10-24T21:30:00Z"),
            grid = WindGrid(
                columnCount = 1,
                rowCount = 1,
                longitudes = listOf(18.0),
                latitudes = listOf(59.3)
            ),
            frames = listOf("2026-10-24T21:00:00Z", "2026-10-24T22:00:00Z").map { validAt: String ->
                WindFrame(
                    validAt = Instant.parse(input = validAt),
                    windSpeeds = listOf(1f),
                    windFromDirections = listOf(0f)
                )
            },
            fetchedAt = Instant.parse(input = "2026-10-24T21:40:00Z")
        )

        assertEquals(
            expected = listOf(
                WindTimeFrame(timeText = "23:00", weekday = "lördag"),
                WindTimeFrame(timeText = "00:00", weekday = "söndag")
            ),
            actual = windForecast.toWindTimeFrames(
                timeZone = stockholm,
                languageTag = "sv-SE",
                hourFormat = HourFormat.TWENTY_FOUR_HOUR
            )
        )
    }

    @Test
    fun `wind hours follow the 12 hour clock in the language shown`() {
        val windForecast = WindForecast(
            referenceTime = Instant.parse(input = "2026-10-24T12:30:00Z"),
            grid = WindGrid(
                columnCount = 1,
                rowCount = 1,
                longitudes = listOf(18.0),
                latitudes = listOf(59.3)
            ),
            frames = listOf(
                WindFrame(
                    validAt = Instant.parse(input = "2026-10-24T13:00:00Z"),
                    windSpeeds = listOf(1f),
                    windFromDirections = listOf(0f)
                )
            ),
            fetchedAt = Instant.parse(input = "2026-10-24T12:40:00Z")
        )

        assertEquals(
            expected = listOf(WindTimeFrame(timeText = "3:00 PM", weekday = "Saturday")),
            actual = windForecast.toWindTimeFrames(
                timeZone = stockholm,
                languageTag = "en",
                hourFormat = HourFormat.TWELVE_HOUR
            )
        )
    }
}