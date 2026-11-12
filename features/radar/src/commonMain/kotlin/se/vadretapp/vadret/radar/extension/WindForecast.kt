package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.toTimeText
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame

internal fun WindForecast.toWindTimeFrames(
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): List<WindTimeFrame> {
    val weekdayFormatter: DateTextFormatter = weekdayFormatter(
        timeZone = timeZone,
        languageTag = languageTag
    )
    return frames.map { windFrame: WindFrame ->
        WindTimeFrame(
            timeText = windFrame.validAt.toTimeText(
                hourFormat = hourFormat,
                timeZone = timeZone
            ),
            weekday = weekdayFormatter.format(instant = windFrame.validAt)
        )
    }
}