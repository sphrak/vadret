package se.vadretapp.vadret.aurora

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.remote.DefaultAuroraRemoteDataSource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash

interface AuroraRepository {

    suspend fun getAuroraForecast(
        installationToken: String,
        geohashes: List<Geohash>
    ): Either<Failure, List<AuroraCellForecast>>
}

fun AuroraRepository(httpClient: Lazy<HttpClient>): AuroraRepository =
    DefaultAuroraRepository(auroraRemoteDataSource = DefaultAuroraRemoteDataSource(httpClient = httpClient))