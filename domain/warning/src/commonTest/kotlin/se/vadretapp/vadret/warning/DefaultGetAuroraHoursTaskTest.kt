package se.vadretapp.vadret.warning

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.aurora.model.AuroraCellForecast
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.warning.usecase.DefaultGetAuroraHoursTask
import se.vadretapp.vadret.warning.usecase.GetAuroraHoursTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DefaultGetAuroraHoursTaskTest {

    private val auroraRepository = FakeAuroraRepository()
    private val getForecastWarningRequestTask = FakeGetForecastWarningRequestTask()
    private val getAuroraHoursTask: GetAuroraHoursTask = DefaultGetAuroraHoursTask(
        auroraRepository = auroraRepository,
        getForecastWarningRequestTask = getForecastWarningRequestTask
    )

    @Test
    fun `the hours with a chance from swedish noon to noon of the night are got for the cell of the aurora`() =
        runTest {
            auroraRepository.result = listOf(
                AuroraCellForecast(
                    geohash = Geohash(value = "u3chr"),
                    hours = listOf(hour(time = "2026-10-24T20:00:00Z"))
                ),
                AuroraCellForecast(
                    geohash = AURORA.geohash,
                    hours = listOf(
                        hour(time = "2026-10-24T09:00:00Z"),
                        hour(time = "2026-10-24T10:00:00Z"),
                        hour(
                            time = "2026-10-24T15:00:00Z",
                            score = 0
                        ),
                        hour(time = "2026-10-24T20:00:00Z"),
                        hour(time = "2026-10-25T10:00:00Z"),
                        hour(time = "2026-10-25T11:00:00Z")
                    )
                )
            ).asRight()

            assertEquals(
                expected = listOf(
                    hour(time = "2026-10-24T10:00:00Z"),
                    hour(time = "2026-10-24T20:00:00Z"),
                    hour(time = "2026-10-25T10:00:00Z")
                ).asRight(),
                actual = getAuroraHoursTask(aurora = AURORA)
            )
            assertEquals(
                expected = listOf("vadret_token" to listOf(AURORA.geohash)),
                actual = auroraRepository.requests
            )
        }

    @Test
    fun `without an installation vadret api is not asked and there are no hours`() =
        runTest {
            getForecastWarningRequestTask.result = null.asRight()

            assertEquals(
                expected = emptyList<AuroraHour>().asRight(),
                actual = getAuroraHoursTask(aurora = AURORA)
            )
            assertEquals(
                expected = emptyList(),
                actual = auroraRepository.requests
            )
        }

    @Test
    fun `a failing aurora forecast is the failure`() =
        runTest {
            auroraRepository.result = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()

            assertEquals(
                expected = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft(),
                actual = getAuroraHoursTask(aurora = AURORA)
            )
        }

    @Test
    fun `a night that is no date is logged and has no hours`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = emptyList<AuroraHour>().asRight(),
                    actual = getAuroraHoursTask(aurora = AURORA.copy(auroraNight = "tonight"))
                )
                assertEquals(
                    expected = listOf("The aurora night tonight is no date, so its hours are left out"),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }
}

private val AURORA = Aurora(
    chance = AuroraChance.HIGH,
    score = 72,
    peakAt = Instant.parse(input = "2026-10-24T20:00:00Z"),
    geohash = Geohash(value = "u6sce"),
    auroraNight = "2026-10-24"
)

private fun hour(
    time: String,
    score: Int = 40
): AuroraHour =
    AuroraHour(
        time = Instant.parse(input = time),
        score = score
    )