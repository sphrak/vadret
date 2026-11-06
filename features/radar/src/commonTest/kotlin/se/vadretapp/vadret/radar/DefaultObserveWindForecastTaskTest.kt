package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.radar.usecase.DefaultObserveWindForecastTask
import se.vadretapp.vadret.radar.usecase.ObserveWindForecastTask
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.wind.WindRepository
import se.vadretapp.vadret.wind.model.WindForecast
import se.vadretapp.vadret.wind.model.WindFrame
import se.vadretapp.vadret.wind.model.WindGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val now: Instant = Instant.parse(input = "2026-10-24T14:20:00Z")

private fun windForecast(firstHour: Instant): WindForecast =
    WindForecast(
        referenceTime = firstHour - 1.hours,
        grid = WindGrid(
            columnCount = 1,
            rowCount = 1,
            longitudes = listOf(18.0),
            latitudes = listOf(59.0)
        ),
        frames = List(size = 3) { hour: Int ->
            WindFrame(
                validAt = firstHour + hour.hours,
                windSpeeds = listOf(4f),
                windFromDirections = listOf(90f)
            )
        },
        fetchedAt = now
    )

private class FakeWindRepository(
    saved: WindForecast?,
    var refreshed: Either<Failure, WindForecast>
) : WindRepository {

    val savedWindForecast: MutableStateFlow<WindForecast?> = MutableStateFlow(value = saved)
    var refreshes: Int = 0

    override fun observeWindForecast(): Flow<Either<Failure, WindForecast?>> = savedWindForecast.map { saved: WindForecast? -> saved.asRight() }

    override suspend fun refreshWindForecast(): Either<Failure, WindForecast> {
        refreshes++
        return when (val result: Either<Failure, WindForecast> = refreshed) {
            is Either.Left -> result
            is Either.Right -> {
                savedWindForecast.value = result.b
                result
            }
        }
    }
}

class DefaultObserveWindForecastTaskTest {

    private fun observeWindForecastTask(windRepository: WindRepository): ObserveWindForecastTask =
        DefaultObserveWindForecastTask(
            windRepository = windRepository,
            clock = FixedClock(now = now)
        )

    @Test
    fun `the hours that already passed are left out of the saved wind`() =
        runTest {
            val windRepository = FakeWindRepository(
                saved = windForecast(firstHour = Instant.parse(input = "2026-10-24T13:00:00Z")),
                refreshed = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            )

            assertEquals(
                expected = listOf(
                    Instant.parse(input = "2026-10-24T14:00:00Z"),
                    Instant.parse(input = "2026-10-24T15:00:00Z")
                ),
                actual = (observeWindForecastTask(windRepository = windRepository)().first() as Either.Right).b.frames.map { windFrame: WindFrame -> windFrame.validAt }
            )
        }

    @Test
    fun `saved wind of hours that all passed waits for the refreshed wind`() =
        runTest {
            val refreshedWind: WindForecast = windForecast(firstHour = Instant.parse(input = "2026-10-24T14:00:00Z"))
            val windRepository = FakeWindRepository(
                saved = windForecast(firstHour = Instant.parse(input = "2026-10-24T06:00:00Z")),
                refreshed = refreshedWind.asRight()
            )

            assertEquals<Either<Failure, WindForecast>>(
                expected = refreshedWind.asRight(),
                actual = observeWindForecastTask(windRepository = windRepository)().first()
            )
        }

    @Test
    fun `no current wind saved and none fetched is wind that is not available`() =
        runTest {
            val windRepository = FakeWindRepository(
                saved = windForecast(firstHour = Instant.parse(input = "2026-10-24T06:00:00Z")),
                refreshed = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            )

            assertEquals<Either<Failure, WindForecast>>(
                expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                actual = observeWindForecastTask(windRepository = windRepository)().first()
            )
        }
}