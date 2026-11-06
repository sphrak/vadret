package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.local.DefaultRadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.DefaultRadarLocalDataSource
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.SavedRadar
import se.vadretapp.vadret.radar.remote.DefaultRadarRemoteDataSource
import kotlin.time.Clock

interface RadarRepository {

    fun observeRadar(): Flow<Either<Failure, SavedRadar>>
    suspend fun refreshRadar(): Either<Failure, SavedRadar>
    suspend fun getRadarContaining(imageUrl: String): Either<Failure, Radar>
    suspend fun getRadarImage(imageUrl: String): Either<Failure, ByteArray>
    suspend fun getMissingRadarImageCount(imageUrls: List<String>): Either<Failure, Int>
    suspend fun downloadRadarDays(imageUrls: List<String>): Either<Failure, Unit>
}

fun RadarRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    imageDirectoryPath: String,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): RadarRepository =
    DefaultRadarRepository(
        radarRemoteDataSource = DefaultRadarRemoteDataSource(httpClient = httpClient),
        radarLocalDataSource = DefaultRadarLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        ),
        radarImageLocalDataSource = DefaultRadarImageLocalDataSource(
            directoryPath = imageDirectoryPath,
            coroutineDispatcher = coroutineDispatcher
        ),
        clock = clock
    )