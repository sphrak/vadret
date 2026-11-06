package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.pollen.extension.toFailure
import se.vadretapp.vadret.pollen.extension.toPollenForecast
import se.vadretapp.vadret.pollen.extension.toPollenRegion
import se.vadretapp.vadret.pollen.extension.toPollenType
import se.vadretapp.vadret.pollen.extension.toPollenTypeEntity
import se.vadretapp.vadret.pollen.local.PollenLocalDataSource
import se.vadretapp.vadret.pollen.local.PollenTypeEntity
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.PollenForecastDto
import se.vadretapp.vadret.pollen.remote.PollenRegionDto
import se.vadretapp.vadret.pollen.remote.PollenRemoteDataSource
import se.vadretapp.vadret.pollen.remote.PollenTypeDto

internal class DefaultPollenRepository(
    private val pollenRemoteDataSource: PollenRemoteDataSource,
    private val pollenLocalDataSource: PollenLocalDataSource
) : PollenRepository {

    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> =
        either {
            pollenRemoteDataSource
                .getRegions()
                .mapNotNull { pollenRegionDto: PollenRegionDto -> pollenRegionDto.toPollenRegion() }
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> =
        either {
            (pollenLocalDataSource.getPollenTypes() ?: fetchPollenTypes()).map { pollenTypeEntity: PollenTypeEntity -> pollenTypeEntity.toPollenType() }
        }.catch { exception: Exception -> exception.toFailure() }

    override fun observePollenTypes(): Flow<Either<Failure, List<PollenType>>> =
        pollenLocalDataSource
            .observePollenTypes()
            .map<List<PollenTypeEntity>?, Either<Failure, List<PollenType>>> { pollenTypeEntities: List<PollenTypeEntity>? ->
                pollenTypeEntities.orEmpty().map { pollenTypeEntity: PollenTypeEntity -> pollenTypeEntity.toPollenType() }.asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(value = throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun refreshPollenTypes(): Either<Failure, List<PollenType>> =
        either {
            fetchPollenTypes().map { pollenTypeEntity: PollenTypeEntity -> pollenTypeEntity.toPollenType() }
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

    // The pollen types are asked for every time and saved, so the last saved list is used when asking fails.
    private suspend fun fetchPollenTypes(): List<PollenTypeEntity> {
        val fetchedPollenTypes: List<PollenTypeEntity> = pollenRemoteDataSource
            .getPollenTypes()
            .map { pollenTypeDto: PollenTypeDto -> pollenTypeDto.toPollenTypeEntity() }
        pollenLocalDataSource.setPollenTypes(pollenTypeEntities = fetchedPollenTypes)
        return fetchedPollenTypes
    }
}