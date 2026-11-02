package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.GetWarningsTask
import se.vadretapp.vadret.warning.ObserveChosenPlaceWarningsTask
import kotlin.time.Instant

internal fun aggregatedFeed(
    identifier: String,
    publishedAt: Instant = Instant.parse(input = "2026-10-24T08:00:00Z"),
    source: FeedSource = FeedSource.SMHI,
    headline: String = "Vind",
    areaName: String? = "Gotlands län",
    warningLevel: WarningLevel? = WarningLevel.YELLOW,
    details: List<FeedDetail> = emptyList(),
    senderName: String? = null,
    startsAt: Instant? = null,
    endsAt: Instant? = null,
    polygons: List<GeoPolygon> = emptyList(),
    lines: List<GeoLine> = emptyList(),
    regions: List<FeedRegion> = emptyList()
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = headline,
        areaName = areaName,
        warningLevel = warningLevel,
        details = details,
        source = source,
        isPublicAlert = false,
        senderName = senderName,
        publishedAt = publishedAt,
        startsAt = startsAt,
        endsAt = endsAt,
        area = FeedArea(
            polygons = polygons,
            lines = lines,
            regions = regions
        )
    )

internal class FakeGetWarningsTask(
    var result: Either<Failure, List<AggregatedFeed>> = listOf(
        aggregatedFeed(
            identifier = "smhi-1-2",
            publishedAt = Instant.parse(input = "2026-10-24T08:00:00Z")
        ),
        aggregatedFeed(
            identifier = "smhi-1-1",
            publishedAt = Instant.parse(input = "2026-10-24T07:00:00Z")
        )
    ).asRight()
) : GetWarningsTask {

    val refreshes = mutableListOf<Boolean>()

    override suspend fun invoke(isRefresh: Boolean): Either<Failure, List<AggregatedFeed>> {
        refreshes.add(element = isRefresh)
        return result
    }
}

internal class FakeObserveChosenPlaceWarningsTask : ObserveChosenPlaceWarningsTask {

    val results: MutableStateFlow<Either<Failure, List<AggregatedFeed>>> = MutableStateFlow(
        value = listOf(
            aggregatedFeed(
                identifier = "smhi-1-2",
                publishedAt = Instant.parse(input = "2026-10-24T08:00:00Z")
            ),
            aggregatedFeed(
                identifier = "smhi-1-1",
                publishedAt = Instant.parse(input = "2026-10-24T07:00:00Z")
            )
        ).asRight()
    )
    val refreshes = mutableListOf<Boolean>()

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>> =
        flow {
            refreshes.add(element = isRefresh)
            emitAll(results)
        }
}

internal class FakeMarkWarningSeenTask : MarkWarningSeenTask {

    val warningIdentifiers = mutableListOf<String>()

    override suspend fun invoke(warningIdentifier: String): Either<Failure, Unit> {
        warningIdentifiers.add(element = warningIdentifier)
        return Unit.asRight()
    }
}