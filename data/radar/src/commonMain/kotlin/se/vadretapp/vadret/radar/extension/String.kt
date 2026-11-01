package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.char
import se.vadretapp.vadret.radar.exception.RadarImageUrlException

private const val PATH_DATE_GROUP = 1

private val radarImagePathDatePattern = Regex(pattern = """/comp/(\d{4}/\d{2}/\d{2})/radar_\d{10}\.tif$""")

private val radarImagePathDateFormat = LocalDate.Format {
    year()
    char(value = '/')
    monthNumber()
    char(value = '/')
    day()
}

internal fun String.toRadarDate(): LocalDate {
    val pathDate: String? = radarImagePathDatePattern.find(input = this)?.groupValues?.getOrNull(index = PATH_DATE_GROUP)
    val radarDate: LocalDate? = if (pathDate != null) {
        radarImagePathDateFormat.parseOrNull(input = pathDate)
    } else {
        null
    }
    return radarDate ?: throw RadarImageUrlException(message = "Not an SMHI radar image: $this")
}