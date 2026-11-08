package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.warning.usecase.DefaultGetReferencedWarningIdentifiersTask
import se.vadretapp.vadret.warning.usecase.GetReferencedWarningIdentifiersTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetReferencedWarningIdentifiersTaskTest {

    private val feedRepository = FakeFeedRepository()
    private val getReferencedWarningIdentifiersTask: GetReferencedWarningIdentifiersTask = DefaultGetReferencedWarningIdentifiersTask(
        feedRepository = feedRepository,
        getForecastWarningRequestTask = FakeGetForecastWarningRequestTask()
    )

    @Test
    fun `a saved warning the notification refers to is found without fetching the feed again`() =
        runTest {
            feedRepository.savedFeed.value = WarningFeed(
                aggregatedFeeds = listOf(
                    aggregatedFeed(identifier = "smhi-1-12"),
                    aggregatedFeed(identifier = "smhi-2-13")
                ),
                fetchedAt = NOW
            )

            assertEquals<Pair<Either<Failure, List<String>>, Int>>(
                expected = listOf("smhi-1-12").asRight() to 0,
                actual = getReferencedWarningIdentifiersTask(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 12)) to feedRepository.refreshCount
            )
        }

    @Test
    fun `a warning missing from the saved feed is looked for once more in a fetched feed`() =
        runTest {
            feedRepository.savedFeed.value = WarningFeed(
                aggregatedFeeds = listOf(aggregatedFeed(identifier = "smhi-1-12")),
                fetchedAt = NOW
            )
            feedRepository.result = listOf(
                aggregatedFeed(identifier = "smhi-1-12"),
                aggregatedFeed(
                    identifier = "vadret-slipperiness-u6sce-2026-11-08-HIGH",
                    source = FeedSource.VADRET_API
                )
            ).asRight()

            assertEquals<Pair<Either<Failure, List<String>>, List<ForecastWarningRequest?>>>(
                expected = listOf("vadret-slipperiness-u6sce-2026-11-08-HIGH").asRight() to listOf<ForecastWarningRequest?>(FORECAST_WARNING_REQUEST),
                actual = getReferencedWarningIdentifiersTask(
                    warningReference = WarningReference.Slipperiness(
                        geohash = Geohash(value = "u6sce"),
                        slipperinessDay = "2026-11-08"
                    )
                ) to feedRepository.forecastWarningRequests
            )
        }

    @Test
    fun `a warning that is gone also from a fetched feed refers to nothing`() =
        runTest {
            assertEquals<Pair<Either<Failure, List<String>>, Int>>(
                expected = emptyList<String>().asRight() to 2,
                actual = getReferencedWarningIdentifiersTask(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 99)) to feedRepository.refreshCount
            )
        }

    @Test
    fun `a feed that cannot be fetched fails the task`() =
        runTest {
            feedRepository.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, List<String>>>(
                expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                actual = getReferencedWarningIdentifiersTask(warningReference = WarningReference.SmhiWarningArea(warningAreaId = 12))
            )
        }
}