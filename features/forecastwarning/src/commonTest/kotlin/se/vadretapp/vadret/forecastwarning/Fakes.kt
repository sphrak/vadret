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
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.warning.GetWarningsTask
import se.vadretapp.vadret.warning.ObserveFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning
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

    var calls: Int = 0

    override suspend fun invoke(): Either<Failure, List<AggregatedFeed>> {
        calls++
        return result
    }
}

internal fun List<AggregatedFeed>.atOneFollowedPlace(): FollowedPlaceWarnings =
    FollowedPlaceWarnings(
        followedPlaces = listOf(gothenburg),
        placeWarnings = map { aggregatedFeed: AggregatedFeed ->
            PlaceWarning(
                aggregatedFeed = aggregatedFeed,
                followedPlaces = listOf(gothenburg)
            )
        }
    )

internal val gothenburg = FollowedPlace(
    favoriteId = null,
    name = "Göteborg",
    municipality = "Göteborg",
    county = "Västra Götaland",
    latitude = 57.7089,
    longitude = 11.9746,
    isChosen = true
)

internal val cabin = FollowedPlace(
    favoriteId = "favorite-cabin",
    name = "Stugan",
    municipality = null,
    county = null,
    latitude = 61.8286,
    longitude = 16.0912,
    isChosen = false
)

internal class FakeObserveFollowedPlaceWarningsTask : ObserveFollowedPlaceWarningsTask {

    val results: MutableStateFlow<Either<Failure, FollowedPlaceWarnings>> = MutableStateFlow(
        value = listOf(
            aggregatedFeed(
                identifier = "smhi-1-2",
                publishedAt = Instant.parse(input = "2026-10-24T08:00:00Z")
            ),
            aggregatedFeed(
                identifier = "smhi-1-1",
                publishedAt = Instant.parse(input = "2026-10-24T07:00:00Z")
            )
        ).atOneFollowedPlace().asRight()
    )
    val refreshes = mutableListOf<Boolean>()

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, FollowedPlaceWarnings>> =
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