package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.pollen.extension.toFailure
import se.vadretapp.vadret.pollen.extension.toPollenForecast
import se.vadretapp.vadret.pollen.extension.toPollenRegion
import se.vadretapp.vadret.pollen.extension.toPollenType
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.PollenForecastDto
import se.vadretapp.vadret.pollen.remote.PollenRegionDto
import se.vadretapp.vadret.pollen.remote.PollenRemoteDataSource
import se.vadretapp.vadret.pollen.remote.PollenTypeDto

internal class DefaultPollenRepository(
    private val pollenRemoteDataSource: PollenRemoteDataSource
) : PollenRepository {

    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> =
        either {
            pollenRemoteDataSource
                .getRegions()
                .mapNotNull { pollenRegionDto: PollenRegionDto -> pollenRegionDto.toPollenRegion() }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> =
        either {
            pollenRemoteDataSource
                .getPollenTypes()
                .map { pollenTypeDto: PollenTypeDto -> pollenTypeDto.toPollenType() }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): Either<Failure, List<PollenForecast>> =
        either {
            pollenRemoteDataSource
                .getForecasts(
                    startDate = startDate,
                    endDate = endDate
                ).mapNotNull { pollenForecastDto: PollenForecastDto -> pollenForecastDto.toPollenForecast() }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getCurrentForecasts(): Either<Failure, List<PollenForecast>> =
        either {
            pollenRemoteDataSource
                .getCurrentForecasts()
                .mapNotNull { pollenForecastDto: PollenForecastDto -> pollenForecastDto.toPollenForecast() }
        }.catch { exception: Exception -> exception.toFailure() }
}