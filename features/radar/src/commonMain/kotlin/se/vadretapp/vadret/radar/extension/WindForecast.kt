package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame

internal fun WindForecast.toWindTimeFrames(
    timeZone: TimeZone,
    languageTag: String
): List<WindTimeFrame> {
    val weekdayFormatter: DateTextFormatter = weekdayFormatter(
        timeZone = timeZone,
        languageTag = languageTag
    )
    return frames.map { windFrame: WindFrame ->
        WindTimeFrame(
            timeText = windFrame.validAt.toTimeText(timeZone = timeZone),
            weekday = weekdayFormatter.format(instant = windFrame.validAt)
        )
    }
}