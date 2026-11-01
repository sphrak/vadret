package se.vadretapp.vadret.pollen

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.DefaultPollenRemoteDataSource

interface PollenRepository {

    suspend fun getRegions(): Either<Failure, List<PollenRegion>>

    suspend fun getPollenTypes(): Either<Failure, List<PollenType>>

    suspend fun getCurrentForecasts(regionId: String): Either<Failure, List<PollenForecast>>
}

fun PollenRepository(httpClient: Lazy<HttpClient>): PollenRepository =
    DefaultPollenRepository(
        pollenRemoteDataSource = DefaultPollenRemoteDataSource(httpClient = httpClient)
    )