package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.radar.local.RadarCacheEntity
import se.vadretapp.vadret.radar.local.RadarImageEntity
import se.vadretapp.vadret.radar.remote.RadarDto
import se.vadretapp.vadret.radar.remote.RadarFileDto
import se.vadretapp.vadret.radar.remote.RadarFormatDto
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.radar")

private val validFormat = LocalDateTime.Format {
    date(format = LocalDate.Formats.ISO)
    char(value = ' ')
    hour()
    char(value = ':')
    minute()
}

internal fun RadarDto.toRadarCacheEntity(
    fetchedAt: Instant,
    date: LocalDate
): RadarCacheEntity =
    RadarCacheEntity(
        fetchedAtEpochMillis = fetchedAt.toEpochMilliseconds(),
        date = date.toString(),
        images = files
            .mapNotNull { radarFileDto: RadarFileDto -> radarFileDto.toRadarImageEntity() }
            .sortedBy { radarImageEntity: RadarImageEntity -> radarImageEntity.validAtEpochMillis }
    )

private fun RadarFileDto.toRadarImageEntity(): RadarImageEntity? {
    val format: RadarFormatDto? = formats.firstOrNull()
    val validAt: LocalDateTime? = validFormat.parseOrNull(input = valid)
    return if (format != null && validAt != null) {
        RadarImageEntity(
            imageUrl = format.link,
            validAtEpochMillis = validAt
                .toInstant(timeZone = TimeZone.UTC)
                .toEpochMilliseconds()
        )
    } else {
        logger.warn { "Skipping radar file $key, valid $valid" }
        null
    }
}