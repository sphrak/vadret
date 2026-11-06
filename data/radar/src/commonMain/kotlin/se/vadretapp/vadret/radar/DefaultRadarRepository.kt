package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.BufferedSink
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.radar.extension.toFailure
import se.vadretapp.vadret.radar.extension.toRadar
import se.vadretapp.vadret.radar.extension.toRadarCacheEntity
import se.vadretapp.vadret.radar.extension.toRadarDate
import se.vadretapp.vadret.radar.extension.toRadarImageName
import se.vadretapp.vadret.radar.extension.toSavedRadar
import se.vadretapp.vadret.radar.local.RadarCacheEntity
import se.vadretapp.vadret.radar.local.RadarImageLocalDataSource
import se.vadretapp.vadret.radar.local.RadarLocalDataSource
import se.vadretapp.vadret.radar.model.Radar
import se.vadretapp.vadret.radar.model.SavedRadar
import se.vadretapp.vadret.radar.remote.RadarRemoteDataSource
import kotlin.time.Clock

internal class DefaultRadarRepository(
    private val radarRemoteDataSource: RadarRemoteDataSource,
    private val radarLocalDataSource: RadarLocalDataSource,
    private val radarImageLocalDataSource: RadarImageLocalDataSource,
    private val clock: Clock
) : RadarRepository {

    // An image asked for while its day is downloading is read from disk afterwards instead of downloaded twice.
    private val imageDownloadMutex: Mutex = Mutex()

    override fun observeRadar(): Flow<Either<Failure, SavedRadar>> =
        radarLocalDataSource
            .observeRadar()
            .map<RadarCacheEntity?, Either<Failure, SavedRadar>> { radarCacheEntity: RadarCacheEntity? ->
                val savedToday: RadarCacheEntity? = if (radarCacheEntity?.date == today().toString()) {
                    radarCacheEntity
                } else {
                    null
                }
                savedToday.toSavedRadar().asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun refreshRadar(): Either<Failure, SavedRadar> = either {
        fetchRadar(date = today()).toSavedRadar()
    }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun getRadarContaining(
        imageUrl: String
    ): Either<Failure, Radar> = either {
        val date: LocalDate = imageUrl.toRadarDate()
        val savedRadar: RadarCacheEntity? = radarLocalDataSource.getRadar()
        val radarOfTheDay: RadarCacheEntity = if (savedRadar != null && savedRadar.date == date.toString() && savedRadar.images.isNotEmpty()) {
            savedRadar
        } else {
            fetchRadar(date = date)
        }
        radarOfTheDay.toRadar()
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

    private suspend fun fetchRadar(date: LocalDate): RadarCacheEntity {
        val fetchedRadar: RadarCacheEntity = radarRemoteDataSource
            .getRadar(date = date)
            .toRadarCacheEntity(
                fetchedAt = clock.now(),
                date = date
            )
        radarLocalDataSource.setRadar(radarCacheEntity = fetchedRadar)
        return fetchedRadar
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(timeZone = TimeZone.UTC).date
}