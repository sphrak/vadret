package se.vadretapp.vadret.wind

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.wind.local.DefaultWindLocalDataSource
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.remote.DefaultWindRemoteDataSource
import kotlin.time.Clock

interface WindRepository {

    suspend fun getWindForecast(): Either<Failure, WindForecast>
}

fun WindRepository(
    httpClient: Lazy<HttpClient>,
    clock: Clock
): WindRepository =
    DefaultWindRepository(
        windRemoteDataSource = DefaultWindRemoteDataSource(httpClient = httpClient),
        windLocalDataSource = DefaultWindLocalDataSource(),
        clock = clock
    )