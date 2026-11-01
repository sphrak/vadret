package se.vadretapp.vadret.wind

import io.github.sphrak.either.Either
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.wind.exception.InvalidWindGridException
import se.vadretapp.vadret.wind.exception.WindNotAvailableException
import se.vadretapp.vadret.wind.extension.columnCount
import se.vadretapp.vadret.wind.extension.latitudes
import se.vadretapp.vadret.wind.extension.longitudes
import se.vadretapp.vadret.wind.extension.toFailure
import se.vadretapp.vadret.wind.extension.toWindForecast
import se.vadretapp.vadret.wind.extension.valuesOrNull
import se.vadretapp.vadret.wind.local.WindForecastEntity
import se.vadretapp.vadret.wind.local.WindFrameEntity
import se.vadretapp.vadret.wind.local.WindLocalDataSource
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.remote.MultiPointDto
import se.vadretapp.vadret.wind.remote.WindParameter
import se.vadretapp.vadret.wind.remote.WindParameterDto
import se.vadretapp.vadret.wind.remote.WindRemoteDataSource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val FORECAST_HOUR_COUNT = 12
private const val MILLISECONDS_IN_HOUR = 3_600_000L

internal class DefaultWindRepository(
    private val windRemoteDataSource: WindRemoteDataSource,
    private val windLocalDataSource: WindLocalDataSource,
    private val clock: Clock
) : WindRepository {

    override suspend fun getWindForecast(): Either<Failure, WindForecast> =
        either { getWindForecastEntity().toWindForecast() }
            .catch { exception: Exception -> exception.toFailure() }

    private suspend fun getWindForecastEntity(): WindForecastEntity {
        val referenceTime: Instant = Instant.parse(input = windRemoteDataSource.getCreatedTime().referenceTime)
        val firstValidAt: Instant = firstValidAt(referenceTime = referenceTime)
        val cachedWindForecast: WindForecastEntity? = windLocalDataSource.getWindForecast()
        val isCurrent: Boolean = cachedWindForecast != null &&
            cachedWindForecast.isFor(
                referenceTime = referenceTime,
                firstValidAt = firstValidAt
            )
        return if (cachedWindForecast != null && isCurrent) {
            cachedWindForecast
        } else {
            val fetchedWindForecast: WindForecastEntity = fetchWindForecast(
                referenceTime = referenceTime,
                firstValidAt = firstValidAt
            )
            windLocalDataSource.setWindForecast(windForecastEntity = fetchedWindForecast)
            fetchedWindForecast
        }
    }

    private fun firstValidAt(referenceTime: Instant): Instant {
        val firstForecastHour: Instant = referenceTime.floorToHour() + 1.hours
        val currentHour: Instant = clock.now().floorToHour()
        return maxOf(firstForecastHour, currentHour)
    }

    private suspend fun fetchWindForecast(
        referenceTime: Instant,
        firstValidAt: Instant
    ): WindForecastEntity =
        coroutineScope {
            val validTimes: List<Instant> = List(size = FORECAST_HOUR_COUNT) { hour: Int -> firstValidAt + hour.hours }
            val windSpeeds: List<Deferred<WindParameterDto>> = validTimes.mapIndexed { index: Int, validAt: Instant ->
                async {
                    windRemoteDataSource.getWindParameter(
                        validAt = validAt,
                        windParameter = WindParameter.SPEED,
                        isGeometryIncluded = index == 0
                    )
                }
            }
            val windFromDirections: List<Deferred<WindParameterDto>> = validTimes.map { validAt: Instant ->
                async {
                    windRemoteDataSource.getWindParameter(
                        validAt = validAt,
                        windParameter = WindParameter.FROM_DIRECTION,
                        isGeometryIncluded = false
                    )
                }
            }
            toWindForecastEntity(
                referenceTime = referenceTime,
                validTimes = validTimes,
                windSpeedDtos = windSpeeds.awaitAll(),
                windFromDirectionDtos = windFromDirections.awaitAll()
            )
        }

    private fun toWindForecastEntity(
        referenceTime: Instant,
        validTimes: List<Instant>,
        windSpeedDtos: List<WindParameterDto>,
        windFromDirectionDtos: List<WindParameterDto>
    ): WindForecastEntity {
        val geometry: MultiPointDto = windSpeedDtos.first().geometry
            ?: throw InvalidWindGridException(message = "Wind grid geometry missing")
        val columnCount: Int = geometry.columnCount()
        val pointCount: Int = geometry.coordinates.size
        val frames: List<WindFrameEntity> = validTimes.indices
            .asSequence()
            .map { index: Int ->
                toWindFrameEntity(
                    validAt = validTimes[index],
                    windSpeedDto = windSpeedDtos[index],
                    windFromDirectionDto = windFromDirectionDtos[index],
                    pointCount = pointCount
                )
            }.takeWhile { windFrameEntity: WindFrameEntity? -> windFrameEntity != null }
            .filterNotNull()
            .toList()
        if (frames.isEmpty()) {
            throw WindNotAvailableException(message = "No wind forecast from ${validTimes.first()}")
        }
        return WindForecastEntity(
            referenceTimeEpochMillis = referenceTime.toEpochMilliseconds(),
            firstValidAtEpochMillis = validTimes.first().toEpochMilliseconds(),
            columnCount = columnCount,
            rowCount = pointCount / columnCount,
            longitudes = geometry.longitudes(),
            latitudes = geometry.latitudes(),
            frames = frames
        )
    }

    private fun toWindFrameEntity(
        validAt: Instant,
        windSpeedDto: WindParameterDto,
        windFromDirectionDto: WindParameterDto,
        pointCount: Int
    ): WindFrameEntity? {
        val windSpeeds: List<Float> = windSpeedDto.valuesOrNull(
            windParameter = WindParameter.SPEED,
            pointCount = pointCount
        ) ?: return null
        val windFromDirections: List<Float> = windFromDirectionDto.valuesOrNull(
            windParameter = WindParameter.FROM_DIRECTION,
            pointCount = pointCount
        ) ?: return null
        return WindFrameEntity(
            validAtEpochMillis = validAt.toEpochMilliseconds(),
            windSpeeds = windSpeeds,
            windFromDirections = windFromDirections
        )
    }

    private fun WindForecastEntity.isFor(
        referenceTime: Instant,
        firstValidAt: Instant
    ): Boolean =
        referenceTimeEpochMillis == referenceTime.toEpochMilliseconds() &&
            firstValidAtEpochMillis == firstValidAt.toEpochMilliseconds()

    private fun Instant.floorToHour(): Instant =
        Instant.fromEpochMilliseconds(
            epochMilliseconds = toEpochMilliseconds() - toEpochMilliseconds().mod(other = MILLISECONDS_IN_HOUR)
        )
}