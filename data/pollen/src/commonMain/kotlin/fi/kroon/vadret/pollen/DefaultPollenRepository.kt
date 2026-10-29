package fi.kroon.vadret.pollen

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.catch
import fi.kroon.vadret.common.either
import fi.kroon.vadret.pollen.extension.toFailure
import fi.kroon.vadret.pollen.extension.toPollenForecast
import fi.kroon.vadret.pollen.extension.toPollenRegion
import fi.kroon.vadret.pollen.extension.toPollenType
import fi.kroon.vadret.pollen.model.PollenForecast
import fi.kroon.vadret.pollen.model.PollenRegion
import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pollen.remote.PollenForecastDto
import fi.kroon.vadret.pollen.remote.PollenRegionDto
import fi.kroon.vadret.pollen.remote.PollenRemoteDataSource
import fi.kroon.vadret.pollen.remote.PollenTypeDto
import io.github.sphrak.either.Either

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

    override suspend fun getCurrentForecasts(regionId: String): Either<Failure, List<PollenForecast>> =
        either {
            pollenRemoteDataSource
                .getCurrentForecasts(regionId = regionId)
                .mapNotNull { pollenForecastDto: PollenForecastDto -> pollenForecastDto.toPollenForecast() }
        }.catch { exception: Exception -> exception.toFailure() }
}