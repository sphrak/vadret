package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.local.DefaultPollenLocalDataSource
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.DefaultPollenRemoteDataSource

interface PollenRepository {

    suspend fun getRegions(): Either<Failure, List<PollenRegion>>

    suspend fun getPollenTypes(): Either<Failure, List<PollenType>>

    fun observePollenTypes(): Flow<Either<Failure, List<PollenType>>>

    suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): Either<Failure, List<PollenForecast>>

    suspend fun getCurrentForecasts(): Either<Failure, List<PollenForecast>>
}

fun PollenRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    coroutineDispatcher: CoroutineDispatcher
): PollenRepository =
    DefaultPollenRepository(
        pollenRemoteDataSource = DefaultPollenRemoteDataSource(httpClient = httpClient),
        pollenLocalDataSource = DefaultPollenLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        )
    )