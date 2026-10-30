package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.DailyPollenLevel
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenStation

private val forecastReach = DatePeriod(days = 3)

internal class DefaultGetPollenMapTask(
    private val pollenRepository: PollenRepository
) : GetPollenMapTask {

    override suspend fun invoke(date: LocalDate): Either<Failure, PollenMap> =
        pollenRepository
            .getRegions()
            .flatMap { pollenRegions: List<PollenRegion> ->
                pollenRepository
                    .getPollenTypes()
                    .flatMap { pollenTypes: List<PollenType> ->
                        pollenRepository
                            .getForecasts(
                                startDate = date.minus(period = forecastReach),
                                endDate = date.plus(period = forecastReach)
                            ).map { pollenForecasts: List<PollenForecast> ->
                                pollenMap(
                                    date = date,
                                    pollenRegions = pollenRegions,
                                    pollenTypes = pollenTypes,
                                    pollenForecasts = pollenForecasts
                                )
                            }
                    }
            }

    private fun pollenMap(
        date: LocalDate,
        pollenRegions: List<PollenRegion>,
        pollenTypes: List<PollenType>,
        pollenForecasts: List<PollenForecast>
    ): PollenMap {
        val pollenLevelsByRegion: Map<String, Map<String, PollenLevel>> = pollenForecasts
            .filter { pollenForecast: PollenForecast -> pollenForecast.dailyLevels.any { dailyPollenLevel: DailyPollenLevel -> dailyPollenLevel.date == date } }
            .groupBy { pollenForecast: PollenForecast -> pollenForecast.regionId }
            .mapValues { (_, regionForecasts: List<PollenForecast>) -> regionForecasts.maxBy { pollenForecast: PollenForecast -> pollenForecast.startDate }.pollenLevelsOn(date = date) }
        val stations: List<PollenStation> = pollenRegions.mapNotNull { pollenRegion: PollenRegion ->
            pollenRegion.toPollenStation(pollenLevels = pollenLevelsByRegion[pollenRegion.regionId])
        }
        return PollenMap(
            date = date,
            pollenTypes = pollenTypes.filter { pollenType: PollenType -> stations.any { station: PollenStation -> pollenType.pollenId in station.pollenLevels } },
            stations = stations
        )
    }

    private fun PollenForecast.pollenLevelsOn(date: LocalDate): Map<String, PollenLevel> =
        dailyLevels
            .filter { dailyPollenLevel: DailyPollenLevel -> dailyPollenLevel.date == date }
            .associate { dailyPollenLevel: DailyPollenLevel -> dailyPollenLevel.pollenId to dailyPollenLevel.pollenLevel }

    private fun PollenRegion.toPollenStation(pollenLevels: Map<String, PollenLevel>?): PollenStation? =
        if (pollenLevels != null) {
            PollenStation(
                regionId = regionId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                pollenLevels = pollenLevels
            )
        } else {
            null
        }
}