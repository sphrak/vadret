package se.vadretapp.vadret.aurora.extension

import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.aurora.remote.AuroraCellDto
import se.vadretapp.vadret.aurora.remote.AuroraForecastDto
import se.vadretapp.vadret.aurora.remote.AuroraHourDto
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOrNull
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aurora")

internal fun AuroraForecastDto.toAuroraCellForecasts(): List<AuroraCellForecast> =
    cells
        .orEmpty()
        .mapNotNull { auroraCellDto: AuroraCellDto -> auroraCellDto.toAuroraCellForecastOrNull() }

private fun AuroraCellDto.toAuroraCellForecastOrNull(): AuroraCellForecast? {
    val cellGeohash: Geohash? = if (geohash == null) {
        null
    } else {
        geohashOrNull(value = geohash)
    }
    if (cellGeohash == null) {
        logger.warn { "Skipping an aurora forecast for the unknown cell $geohash" }
        return null
    }
    val auroraHourDtos: List<AuroraHourDto> = hours.orEmpty()
    val auroraHours: List<AuroraHour> = auroraHourDtos.mapNotNull { auroraHourDto: AuroraHourDto -> auroraHourDto.toAuroraHourOrNull() }
    if (auroraHours.size < auroraHourDtos.size) {
        logger.warn { "Skipping ${auroraHourDtos.size - auroraHours.size} aurora hours in ${cellGeohash.value} without a readable time or a score from $MINIMUM_SCORE to $MAXIMUM_SCORE" }
    }
    return AuroraCellForecast(
        geohash = cellGeohash,
        hours = auroraHours.sortedBy { auroraHour: AuroraHour -> auroraHour.time }
    )
}

private fun AuroraHourDto.toAuroraHourOrNull(): AuroraHour? {
    val hourTime: Instant? = if (time == null) {
        null
    } else {
        Instant.parseOrNull(input = time)
    }
    if (hourTime == null || score == null || score !in MINIMUM_SCORE..MAXIMUM_SCORE) {
        return null
    }
    return AuroraHour(
        time = hourTime,
        score = score
    )
}

private const val MINIMUM_SCORE = 0
private const val MAXIMUM_SCORE = 100