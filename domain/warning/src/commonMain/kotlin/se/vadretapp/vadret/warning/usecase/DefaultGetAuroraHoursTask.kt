package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aurora.AuroraRepository
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

internal class DefaultGetAuroraHoursTask(
    private val auroraRepository: AuroraRepository,
    private val getForecastWarningRequestTask: GetForecastWarningRequestTask
) : GetAuroraHoursTask {

    override suspend fun invoke(aurora: Aurora): Either<Failure, List<AuroraHour>> {
        val forecastWarningRequest: ForecastWarningRequest = getForecastWarningRequestTask.forecastWarningRequestOrNull()
            ?: return emptyList<AuroraHour>().asRight()
        val nightDate: LocalDate = aurora.nightDateOrNull() ?: return emptyList<AuroraHour>().asRight()
        val nightStartsAt: Instant = nightDate.atNoonInSweden()
        val nightEndsAt: Instant = nightDate
            .plus(
                value = NIGHT_DAYS,
                unit = DateTimeUnit.DAY
            ).atNoonInSweden()
        return auroraRepository
            .getAuroraForecast(
                installationToken = forecastWarningRequest.installationToken,
                geohashes = listOf(aurora.geohash)
            ).map { auroraCellForecasts: List<AuroraCellForecast> ->
                auroraCellForecasts
                    .firstOrNull { auroraCellForecast: AuroraCellForecast -> auroraCellForecast.geohash == aurora.geohash }
                    ?.hours
                    .orEmpty()
                    .filter { auroraHour: AuroraHour -> auroraHour.time >= nightStartsAt && auroraHour.time < nightEndsAt && auroraHour.score > NO_CHANCE }
            }
    }
}

private fun Aurora.nightDateOrNull(): LocalDate? {
    val nightDate: LocalDate? = LocalDate.Formats.ISO.parseOrNull(input = auroraNight)
    if (nightDate == null) {
        logger.warn { "The aurora night $auroraNight is no date, so its hours are left out" }
    }
    return nightDate
}

private fun LocalDate.atNoonInSweden(): Instant =
    LocalDateTime(
        date = this,
        time = LocalTime(
            hour = NOON_HOUR,
            minute = ON_THE_HOUR
        )
    ).toInstant(timeZone = TimeZone.of(zoneId = SWEDISH_TIME_ZONE))

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.warning")

private const val NO_CHANCE = 0
private const val NIGHT_DAYS = 1
private const val NOON_HOUR = 12
private const val ON_THE_HOUR = 0
private const val SWEDISH_TIME_ZONE = "Europe/Stockholm"