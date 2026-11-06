package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
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
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenStation
import kotlin.test.Test
import kotlin.test.assertEquals

private val date: LocalDate = LocalDate.parse(input = "2026-07-13")

private val stockholm = PollenRegion(
    regionId = "stockholm",
    name = "Stockholm",
    latitude = 59.37,
    longitude = 18.06
)

private val kiruna = PollenRegion(
    regionId = "kiruna",
    name = "Kiruna",
    latitude = 67.85,
    longitude = 20.3
)

private val visby = PollenRegion(
    regionId = "visby",
    name = "Visby",
    latitude = 57.61,
    longitude = 18.28
)

private val birch = PollenType(
    pollenId = "birch",
    name = "Björk",
    hasForecasts = true
)

private val grass = PollenType(
    pollenId = "grass",
    name = "Gräs",
    hasForecasts = true
)

private val hazel = PollenType(
    pollenId = "hazel",
    name = "Hassel",
    hasForecasts = true
)

private fun forecast(
    regionId: String,
    startDate: String,
    vararg dailyLevels: DailyPollenLevel
): PollenForecast =
    PollenForecast(
        forecastId = "$regionId-$startDate",
        regionId = regionId,
        startDate = LocalDate.parse(input = startDate),
        endDate = LocalDate.parse(input = startDate),
        text = "",
        isEndOfSeason = false,
        dailyLevels = dailyLevels.toList()
    )

private fun level(
    pollenId: String,
    date: String,
    pollenLevel: PollenLevel
): DailyPollenLevel =
    DailyPollenLevel(
        pollenId = pollenId,
        date = LocalDate.parse(input = date),
        pollenLevel = pollenLevel
    )

private class FakePollenRepository(
    var regions: Either<Failure, List<PollenRegion>> = listOf(stockholm, kiruna, visby).asRight(),
    var pollenTypes: Either<Failure, List<PollenType>> = listOf(hazel, birch, grass).asRight(),
    var forecasts: Either<Failure, List<PollenForecast>> = emptyList<PollenForecast>().asRight()
) : PollenRepository {

    val requestedWindows = mutableListOf<Pair<LocalDate, LocalDate>>()

    override suspend fun getRegions(): Either<Failure, List<PollenRegion>> = regions

    override suspend fun getPollenTypes(): Either<Failure, List<PollenType>> = pollenTypes

    override fun observePollenTypes(): Flow<Either<Failure, List<PollenType>>> = error("The pollen map asks for the pollen types once")

    override suspend fun refreshPollenTypes(): Either<Failure, List<PollenType>> = error("The pollen map asks for the pollen types once")

    override suspend fun getForecasts(
        startDate: LocalDate,
        endDate: LocalDate
    ): Either<Failure, List<PollenForecast>> {
        requestedWindows.add(startDate to endDate)
        return forecasts
    }

    override suspend fun getCurrentForecasts(): Either<Failure, List<PollenForecast>> = error("The pollen map never asks for current forecasts")
}

class DefaultGetPollenMapTaskTest {

    private val pollenRepository = FakePollenRepository()
    private val getPollenMapTask: GetPollenMapTask = DefaultGetPollenMapTask(pollenRepository = pollenRepository)

    @Test
    fun `forecasts are asked for three days around the date`() =
        runTest {
            getPollenMapTask(date = date)

            assertEquals(
                expected = listOf(LocalDate.parse(input = "2026-07-10") to LocalDate.parse(input = "2026-07-16")),
                actual = pollenRepository.requestedWindows
            )
        }

    @Test
    fun `each station shows the newest forecast that covers the date`() =
        runTest {
            pollenRepository.forecasts = listOf(
                forecast(
                    regionId = "stockholm",
                    startDate = "2026-07-11",
                    level(
                        pollenId = "grass",
                        date = "2026-07-13",
                        pollenLevel = PollenLevel.LOW
                    )
                ),
                forecast(
                    regionId = "stockholm",
                    startDate = "2026-07-13",
                    level(
                        pollenId = "grass",
                        date = "2026-07-13",
                        pollenLevel = PollenLevel.HIGH
                    ),
                    level(
                        pollenId = "birch",
                        date = "2026-07-13",
                        pollenLevel = PollenLevel.NONE
                    ),
                    level(
                        pollenId = "grass",
                        date = "2026-07-14",
                        pollenLevel = PollenLevel.VERY_HIGH
                    )
                ),
                forecast(
                    regionId = "stockholm",
                    startDate = "2026-07-14",
                    level(
                        pollenId = "grass",
                        date = "2026-07-14",
                        pollenLevel = PollenLevel.MODERATE
                    )
                ),
                forecast(
                    regionId = "kiruna",
                    startDate = "2026-07-13",
                    level(
                        pollenId = "grass",
                        date = "2026-07-13",
                        pollenLevel = PollenLevel.MODERATE
                    )
                ),
                forecast(
                    regionId = "visby",
                    startDate = "2026-07-14",
                    level(
                        pollenId = "grass",
                        date = "2026-07-14",
                        pollenLevel = PollenLevel.HIGH
                    )
                ),
                forecast(
                    regionId = "sverige",
                    startDate = "2026-07-13",
                    level(
                        pollenId = "grass",
                        date = "2026-07-13",
                        pollenLevel = PollenLevel.HIGH
                    )
                )
            ).asRight()

            assertEquals(
                expected = PollenMap(
                    date = date,
                    pollenTypes = listOf(birch, grass),
                    stations = listOf(
                        PollenStation(
                            regionId = "stockholm",
                            name = "Stockholm",
                            latitude = 59.37,
                            longitude = 18.06,
                            pollenLevels = mapOf(
                                "grass" to PollenLevel.HIGH,
                                "birch" to PollenLevel.NONE
                            )
                        ),
                        PollenStation(
                            regionId = "kiruna",
                            name = "Kiruna",
                            latitude = 67.85,
                            longitude = 20.3,
                            pollenLevels = mapOf("grass" to PollenLevel.MODERATE)
                        )
                    )
                ).asRight(),
                actual = getPollenMapTask(date = date)
            )
        }

    @Test
    fun `any failure along the way is the failure of the map`() =
        runTest {
            val failure: Either<Failure, Nothing> = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            val failingRepositories: List<FakePollenRepository> = listOf(
                FakePollenRepository(regions = failure),
                FakePollenRepository(pollenTypes = failure),
                FakePollenRepository(forecasts = failure)
            )

            assertEquals(
                expected = failingRepositories.map { failure },
                actual = failingRepositories.map { failingRepository: FakePollenRepository ->
                    DefaultGetPollenMapTask(pollenRepository = failingRepository)(date = date)
                }
            )
        }
}