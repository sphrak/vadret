package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarImage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.toElapsedTime
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.toTimeText
import kotlin.time.Instant

internal fun Radar.toRadarFrames(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): List<RadarFrame> {
    val weekdayFormatter: DateTextFormatter = weekdayFormatter(
        timeZone = timeZone,
        languageTag = languageTag
    )
    return images.map { radarImage: RadarImage ->
        RadarFrame(
            imageUrl = radarImage.imageUrl,
            timeText = radarImage.validAt.toTimeText(
                hourFormat = hourFormat,
                timeZone = timeZone,
                languageTag = languageTag
            ),
            weekday = weekdayFormatter.format(instant = radarImage.validAt),
            elapsedTime = radarImage.validAt.toElapsedTime(now = now)
        )
    }
}