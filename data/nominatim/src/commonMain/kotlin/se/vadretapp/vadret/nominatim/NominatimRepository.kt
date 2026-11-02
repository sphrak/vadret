package se.vadretapp.vadret.nominatim

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.local.DefaultLocalityInMemoryDataSource
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.nominatim.remote.DefaultNominatimRemoteDataSource

interface NominatimRepository {

    suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Either<Failure, Locality>
}

fun NominatimRepository(httpClient: Lazy<HttpClient>): NominatimRepository =
    DefaultNominatimRepository(
        nominatimRemoteDataSource = DefaultNominatimRemoteDataSource(httpClient = httpClient),
        localityInMemoryDataSource = DefaultLocalityInMemoryDataSource()
    )