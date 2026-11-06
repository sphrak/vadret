package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.aggregatedfeed.model.SavedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.common.Failure
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
        )
    )

private val FETCHED_AT: Instant = Instant.parse(input = "2026-10-24T11:30:00Z")

internal class FakeFeedRepository(
    var result: Either<Failure, List<AggregatedFeed>> = listOf(aggregatedFeed(identifier = "smhi-1-1")).asRight()
) : FeedRepository {

    var refreshCount: Int = 0
    val savedFeed: MutableStateFlow<SavedFeed> = MutableStateFlow(
        value = SavedFeed(
            aggregatedFeeds = emptyList(),
            fetchedAt = null
        )
    )
    val seenWarnings: MutableStateFlow<Either<Failure, List<SeenWarning>>> = MutableStateFlow(value = emptyList<SeenWarning>().asRight())
    val markedSeen = mutableListOf<String>()

    override suspend fun getAggregatedFeed(): Either<Failure, List<AggregatedFeed>> {
        val saved: SavedFeed = savedFeed.value
        if (saved.fetchedAt != null) {
            return saved.aggregatedFeeds.asRight()
        }
        return refreshAggregatedFeed().map { refreshed: SavedFeed -> refreshed.aggregatedFeeds }
    }

    override fun observeAggregatedFeed(): Flow<Either<Failure, SavedFeed>> = savedFeed.map { saved: SavedFeed -> saved.asRight() }

    override suspend fun refreshAggregatedFeed(): Either<Failure, SavedFeed> {
        refreshCount++
        return when (val refreshed: Either<Failure, List<AggregatedFeed>> = result) {
            is Either.Left -> refreshed
            is Either.Right -> {
                val saved = SavedFeed(
                    aggregatedFeeds = refreshed.b,
                    fetchedAt = FETCHED_AT
                )
                savedFeed.value = saved
                saved.asRight()
            }
        }
    }

    override fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>> = seenWarnings

    override suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit> {
        markedSeen.add(element = warningIdentifier)
        return Unit.asRight()
    }
}