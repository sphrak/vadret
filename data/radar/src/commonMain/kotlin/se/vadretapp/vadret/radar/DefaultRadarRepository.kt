package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.BufferedSink
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.radar.extension.toFailure
import se.vadretapp.vadret.radar.extension.toRadar
import se.vadretapp.vadret.radar.extension.toRadarCacheEntity
import se.vadretapp.vadret.radar.extension.toRadarDate
import se.vadretapp.vadret.radar.extension.toRadarImageName
import se.vadretapp.vadret.radar.local.RadarCacheEntity
import se.vadretapp.vadret.radar.local.RadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.RadarLocalDataSource
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.remote.RadarRemoteDataSource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.radar")

private val cacheLifetime: Duration = 15.minutes

internal class DefaultRadarRepository(
    private val radarRemoteDataSource: RadarRemoteDataSource,
    private val radarLocalDataSource: RadarLocalDataSource,
    private val radarImageLocalDataSource: RadarImageLocalDataSource,
    private val clock: Clock
) : RadarRepository {

    // An image asked for while its day is downloading is read from disk afterwards instead of downloaded twice.
    private val imageDownloadMutex: Mutex = Mutex()

    override suspend fun getRadar(): Either<Failure, Radar> = either {
        getRadarCacheEntity(
            date = clock.now().toLocalDateTime(timeZone = TimeZone.UTC).date
        ).toRadar()
    }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getRadarContaining(
        imageUrl: String
    ): Either<Failure, Radar> = either {
        getRadarCacheEntity(date = imageUrl.toRadarDate()).toRadar()
    }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getRadarImage(imageUrl: String): Either<Failure, ByteArray> = either {
        val date: LocalDate = imageUrl.toRadarDate()
        val imageName: String = imageUrl.toRadarImageName()
        radarImageLocalDataSource.getImage(
            date = date,
            imageName = imageName
        ) ?: imageDownloadMutex.withLock {
            radarImageLocalDataSource.getImage(
                date = date,
                imageName = imageName
            ) ?: downloadImage(
                date = date,
                imageName = imageName
            )
        }
    }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getMissingRadarImageCount(imageUrls: List<String>): Either<Failure, Int> = either {
        imageUrls
            .groupBy(
                keySelector = { imageUrl: String -> imageUrl.toRadarDate() },
                valueTransform = { imageUrl: String -> imageUrl.toRadarImageName() }
            )
            .entries
            .sumOf { day: Map.Entry<LocalDate, List<String>> ->
                val storedImageNames: Set<String> = radarImageLocalDataSource.getImageNames(date = day.key)
                day.value.count { imageName: String -> imageName !in storedImageNames }
            }
    }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun downloadRadarDays(imageUrls: List<String>): Either<Failure, Unit> = either {
        imageUrls
            .map { imageUrl: String -> imageUrl.toRadarDate() }
            .distinct()
            .forEach { date: LocalDate ->
                imageDownloadMutex.withLock {
                    radarImageLocalDataSource.setArchive(date = date) { archiveSink: BufferedSink ->
                        radarRemoteDataSource.getRadarArchive(
                            date = date,
                            archiveSink = archiveSink
                        )
                    }
                }
            }
    }.catch { exception: Exception -> exception.toFailure() }

    private suspend fun downloadImage(
        date: LocalDate,
        imageName: String
    ): ByteArray {
        val image: ByteArray = radarRemoteDataSource.getRadarImage(
            date = date,
            imageName = imageName
        )
        radarImageLocalDataSource.setImage(
            date = date,
            imageName = imageName,
            image = image
        )
        return image
    }

    private suspend fun getRadarCacheEntity(date: LocalDate): RadarCacheEntity {
        val now: Instant = clock.now()
        val cachedRadar: RadarCacheEntity? = radarLocalDataSource.getRadar()
        return if (cachedRadar != null && cachedRadar.isCurrent(now = now, date = date)) {
            cachedRadar
        } else {
            fetchRadar(
                now = now,
                date = date,
                cachedRadar = cachedRadar
            )
        }
    }

    // The saved radar of the same day is used when fetching fails, however old it is. A saved radar of another day is
    // not, since its images are not the ones of the day asked for.
    private suspend fun fetchRadar(
        now: Instant,
        date: LocalDate,
        cachedRadar: RadarCacheEntity?
    ): RadarCacheEntity {
        val fetchedRadar: RadarCacheEntity = try {
            radarRemoteDataSource
                .getRadar(date = date)
                .toRadarCacheEntity(
                    fetchedAt = now,
                    date = date
                )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            if (cachedRadar == null || cachedRadar.date != date.toString() || cachedRadar.images.isEmpty()) {
                throw exception
            }
            logger.warn { "Using the radar fetched at ${cachedRadar.fetchedAtEpochMillis}: ${exception::class.simpleName}: ${exception.message}" }
            return cachedRadar
        }
        radarLocalDataSource.setRadar(radarCacheEntity = fetchedRadar)
        return fetchedRadar
    }

    private fun RadarCacheEntity.isCurrent(
        now: Instant,
        date: LocalDate
    ): Boolean {
        val age: Duration = now - Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
        return this.date == date.toString() &&
            age >= Duration.ZERO &&
            age < cacheLifetime &&
            images.isNotEmpty()
    }
}