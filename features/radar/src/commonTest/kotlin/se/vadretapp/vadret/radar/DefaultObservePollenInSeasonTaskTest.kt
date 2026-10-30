package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.DailyPollenLevel
import se.vadretapp.vadret.pollen.model.PollenForecast
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenRegion
import se.vadretapp.vadret.pollen.model.PollenType
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeCurrentForecastsPollenRepository(
    private val currentForecasts: Either<Failure, List<PollenForecast>>
) : PollenRepository {

    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> = error("Only the current forecasts tell the season")

    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> = error("Only the current forecasts tell the season")

    override suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): Either<Failure, List<PollenForecast>> = error("Only the current forecasts tell the season")

    override suspend fun getCurrentForecasts(): Either<Failure, List<PollenForecast>> = currentForecasts
}

private fun forecast(
    isEndOfSeason: Boolean,
    hasLevels: Boolean
): PollenForecast =
    PollenForecast(
        forecastId = "stockholm-$isEndOfSeason-$hasLevels",
        regionId = "stockholm",
        startDate = LocalDate.parse(input = "2026-09-18"),
        endDate = LocalDate.parse(input = "2026-09-18"),
        text = "",
        isEndOfSeason = isEndOfSeason,
        dailyLevels = if (hasLevels) {
            listOf(
                DailyPollenLevel(
                    pollenId = "grass",
                    date = LocalDate.parse(input = "2026-09-18"),
                    pollenLevel = PollenLevel.LOW
                )
            )
        } else {
            emptyList()
        }
    )

class DefaultObservePollenInSeasonTaskTest {

    private suspend fun isInSeason(currentForecasts: Either<Failure, List<PollenForecast>>): Either<Failure, Boolean> =
        DefaultObservePollenInSeasonTask(
            pollenRepository = FakeCurrentForecastsPollenRepository(currentForecasts = currentForecasts)
        )().single()

    @Test
    fun `pollen is in season only while some forecast is ongoing and has levels`() =
        runTest {
            assertEquals(
                expected = listOf(
                    true.asRight(),
                    false.asRight(),
                    false.asRight(),
                    false.asRight()
                ),
                actual = listOf(
                    isInSeason(
                        currentForecasts = listOf(
                            forecast(
                                isEndOfSeason = true,
                                hasLevels = false
                            ),
                            forecast(
                                isEndOfSeason = false,
                                hasLevels = true
                            )
                        ).asRight()
                    ),
                    isInSeason(
                        currentForecasts = listOf(
                            forecast(
                                isEndOfSeason = true,
                                hasLevels = false
                            )
                        ).asRight()
                    ),
                    isInSeason(
                        currentForecasts = listOf(
                            forecast(
                                isEndOfSeason = false,
                                hasLevels = false
                            )
                        ).asRight()
                    ),
                    isInSeason(currentForecasts = emptyList<PollenForecast>().asRight())
                )
            )
        }

    @Test
    fun `forecasts that cannot be fetched leave the season unknown`() =
        runTest {
            val networkFailure = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE)

            assertEquals(
                expected = networkFailure.asLeft(),
                actual = isInSeason(currentForecasts = networkFailure.asLeft())
            )
        }
}