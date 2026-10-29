package fi.kroon.vadret.pollen.extension

import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pollen.model.DailyPollenLevel
import fi.kroon.vadret.pollen.model.PollenForecast
import fi.kroon.vadret.pollen.model.PollenLevel
import fi.kroon.vadret.pollen.remote.PollenForecastDto
import fi.kroon.vadret.pollen.remote.PollenLevelDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

private const val LINE_SEPARATOR = "\n"

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pollen")

internal fun PollenForecastDto.toPollenForecast(): PollenForecast? {
    val forecastStartDate: LocalDate? = LocalDate.Formats.ISO.parseOrNull(input = startDate)
    val forecastEndDate: LocalDate? = LocalDate.Formats.ISO.parseOrNull(input = endDate)
    return if (forecastStartDate != null && forecastEndDate != null) {
        PollenForecast(
            forecastId = id,
            regionId = regionId,
            startDate = forecastStartDate,
            endDate = forecastEndDate,
            text = text.orEmpty().lines().joinToString(separator = LINE_SEPARATOR).trim(),
            isEndOfSeason = isEndOfSeason == true,
            dailyLevels = levelSeries.orEmpty().mapNotNull { pollenLevelDto: PollenLevelDto -> pollenLevelDto.toDailyPollenLevel() }
        )
    } else {
        logger.warn { "Skipping pollen forecast $id from $startDate to $endDate" }
        null
    }
}

private fun PollenLevelDto.toDailyPollenLevel(): DailyPollenLevel? {
    val levelDate: LocalDate? = LocalDateTime.Formats.ISO.parseOrNull(input = time)?.date
    val pollenLevel: PollenLevel? = level.toPollenLevel()
    return if (levelDate != null && pollenLevel != null) {
        DailyPollenLevel(
            pollenId = pollenId,
            date = levelDate,
            pollenLevel = pollenLevel
        )
    } else {
        logger.warn { "Skipping pollen level $level of $pollenId at $time" }
        null
    }
}