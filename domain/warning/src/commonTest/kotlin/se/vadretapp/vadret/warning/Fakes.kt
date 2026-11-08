package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask
import kotlin.time.Instant

internal fun square(
    south: Double,
    west: Double,
    north: Double,
    east: Double
): List<GeoPosition> =
    listOf(
        GeoPosition(
            latitude = south,
            longitude = west
        ),
        GeoPosition(
            latitude = south,
            longitude = east
        ),
        GeoPosition(
            latitude = north,
            longitude = east
        ),
        GeoPosition(
            latitude = north,
            longitude = west
        ),
        GeoPosition(
            latitude = south,
            longitude = west
        )
    )

internal fun aggregatedFeed(
    identifier: String,
    publishedAt: Instant = Instant.parse(input = "2026-10-24T08:00:00Z"),
    source: FeedSource = FeedSource.SMHI,
    polygons: List<GeoPolygon> = emptyList(),
    lines: List<GeoLine> = emptyList(),
    regions: List<FeedRegion> = emptyList()
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = "Vind",
        areaName = "Jämtlands län",
        warningLevel = WarningLevel.YELLOW,
        details = emptyList(),
        source = source,
        isPublicAlert = false,
        senderName = null,
        publishedAt = publishedAt,
        startsAt = null,
        endsAt = null,
        area = FeedArea(
            polygons = polygons,
            lines = lines,
            regions = regions
        ),
        slipperiness = null
    )

internal val NOW: Instant = Instant.parse(input = "2026-10-24T12:00:00Z")

internal class FakeWarningsRepository(
    var result: Either<Failure, List<AggregatedFeed>> = listOf(aggregatedFeed(identifier = "smhi-1-1")).asRight()
) : WarningsRepository {

    var refreshCount: Int = 0
    val savedFeed: MutableStateFlow<WarningFeed?> = MutableStateFlow(value = null)
    val forecastWarningRequests = mutableListOf<ForecastWarningRequest?>()

    override suspend fun getAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, List<AggregatedFeed>> {
        val saved: WarningFeed? = savedFeed.value
        if (saved != null) {
            return saved.aggregatedFeeds.asRight()
        }
        return refreshAggregatedFeed(forecastWarningRequest = forecastWarningRequest).map { refreshed: WarningFeed -> refreshed.aggregatedFeeds }
    }

    override fun observeAggregatedFeed(): Flow<Either<Failure, WarningFeed?>> =
        savedFeed.map { saved: WarningFeed? -> saved.asRight() }

    override suspend fun refreshAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, WarningFeed> {
        refreshCount++
        forecastWarningRequests += forecastWarningRequest
        return when (val refreshed: Either<Failure, List<AggregatedFeed>> = result) {
            is Either.Left -> refreshed
            is Either.Right -> {
                val saved = WarningFeed(
                    aggregatedFeeds = refreshed.b,
                    fetchedAt = NOW
                )
                savedFeed.value = saved
                saved.asRight()
            }
        }
    }
}

internal class FakeSeenWarningRepository : SeenWarningRepository {

    val seenWarnings: MutableStateFlow<Either<Failure, List<SeenWarning>>> = MutableStateFlow(value = emptyList<SeenWarning>().asRight())
    val markedSeen = mutableListOf<String>()

    override fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>> = seenWarnings

    override suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit> {
        markedSeen.add(element = warningIdentifier)
        return Unit.asRight()
    }
}

internal class FakeGetForecastWarningRequestTask(
    var result: Either<Failure, ForecastWarningRequest?> = FORECAST_WARNING_REQUEST.asRight()
) : GetForecastWarningRequestTask {

    override suspend fun invoke(): Either<Failure, ForecastWarningRequest?> = result
}

internal val FORECAST_WARNING_REQUEST = ForecastWarningRequest(
    installationToken = "vadret_token",
    geohashes = listOf(Geohash(value = "u6sce"))
)