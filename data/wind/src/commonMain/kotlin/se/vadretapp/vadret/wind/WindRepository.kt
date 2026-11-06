package se.vadretapp.vadret.wind

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.wind.local.DefaultWindLocalDataSource
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.remote.DefaultWindRemoteDataSource
import kotlin.time.Clock

interface WindRepository {

    fun observeWindForecast(): Flow<Either<Failure, WindForecast?>>

    suspend fun refreshWindForecast(): Either<Failure, WindForecast>
}

fun WindRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): WindRepository =
    DefaultWindRepository(
        windRemoteDataSource = DefaultWindRemoteDataSource(httpClient = httpClient),
        windLocalDataSource = DefaultWindLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        ),
        clock = clock
    )