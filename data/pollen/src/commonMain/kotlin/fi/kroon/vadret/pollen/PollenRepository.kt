package fi.kroon.vadret.pollen

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pollen.model.PollenForecast
import fi.kroon.vadret.pollen.model.PollenRegion
import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pollen.remote.DefaultPollenRemoteDataSource
import io.github.sphrak.either.Either
import io.ktor.client.HttpClient

interface PollenRepository {

    suspend fun getRegions(): Either<Failure, List<PollenRegion>>

    suspend fun getPollenTypes(): Either<Failure, List<PollenType>>

    suspend fun getCurrentForecasts(regionId: String): Either<Failure, List<PollenForecast>>
}

fun PollenRepository(httpClient: Lazy<HttpClient>): PollenRepository =
    DefaultPollenRepository(
        pollenRemoteDataSource = DefaultPollenRemoteDataSource(httpClient = httpClient)
    )