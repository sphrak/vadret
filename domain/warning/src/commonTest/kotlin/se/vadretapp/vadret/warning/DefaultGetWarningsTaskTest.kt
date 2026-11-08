package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import se.vadretapp.vadret.warning.usecase.DefaultGetWarningsTask
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetWarningsTaskTest {

    private val warningsRepository = FakeWarningsRepository()
    private val getForecastWarningRequestTask = FakeGetForecastWarningRequestTask()
    private val getWarningsTask: GetWarningsTask = DefaultGetWarningsTask(
        warningsRepository = warningsRepository,
        getForecastWarningRequestTask = getForecastWarningRequestTask
    )

    @Test
    fun `every warning is returned unfiltered`() =
        runTest {
            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = warningsRepository.result,
                actual = getWarningsTask()
            )
        }

    @Test
    fun `saved warnings are got without fetching them again`() =
        runTest {
            getWarningsTask()
            getWarningsTask()

            assertEquals(expected = 1, actual = warningsRepository.refreshCount)
        }

    @Test
    fun `a failing feed fails the task`() =
        runTest {
            warningsRepository.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                actual = getWarningsTask()
            )
        }

    @Test
    fun `warnings are fetched with what vadret api needs`() =
        runTest {
            getWarningsTask()

            assertEquals(
                expected = listOf<ForecastWarningRequest?>(FORECAST_WARNING_REQUEST),
                actual = warningsRepository.forecastWarningRequests
            )
        }

    @Test
    fun `a vadret api request that cannot be made is logged and leaves vadret api out`() =
        runTest {
            getForecastWarningRequestTask.result = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            withRecordedLogs { recordingLogSink ->
                getWarningsTask()

                assertEquals(
                    expected = listOf<ForecastWarningRequest?>(null),
                    actual = warningsRepository.forecastWarningRequests
                )
                assertEquals(
                    expected = listOf("Vadret API warnings are left out this time: STORAGE_FAILED"),
                    actual = recordingLogSink.lines.map { line: LogLine -> line.message }
                )
            }
        }
}