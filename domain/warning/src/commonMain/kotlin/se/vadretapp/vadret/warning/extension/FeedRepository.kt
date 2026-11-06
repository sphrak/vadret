package se.vadretapp.vadret.warning.extension

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SavedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.whileRefreshing
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Instant

internal fun FeedRepository.observeRefreshedFeed(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>> =
    observeAggregatedFeed()
        .whileRefreshing(
            refresh = { refreshAggregatedFeed() },
            isNothingSaved = { savedFeed: SavedFeed -> savedFeed.fetchedAt == null },
            whenRefreshFails = { savedFeed: SavedFeed -> savedFeed },
            isRefreshFirst = isRefresh
        ).map { savedFeed: Either<Failure, SavedFeed> -> savedFeed.map { feed: SavedFeed -> feed.aggregatedFeeds } }

// A warning stays unseen until it is seen after it was last published, so an updated warning shows again.
internal fun FeedRepository.observeUnseenWarnings(
    warningPlaces: List<WarningPlace>,
    isRefresh: Boolean
): Flow<Either<Failure, List<AggregatedFeed>>> =
    combine(
        flow = observeRefreshedFeed(isRefresh = isRefresh),
        flow2 = observeSeenWarnings()
    ) { aggregatedFeeds: Either<Failure, List<AggregatedFeed>>, seenWarnings: Either<Failure, List<SeenWarning>> ->
        aggregatedFeeds.flatMap { feeds: List<AggregatedFeed> ->
            seenWarnings.map { seen: List<SeenWarning> ->
                feeds
                    .filter { aggregatedFeed: AggregatedFeed -> warningPlaces.any { warningPlace: WarningPlace -> aggregatedFeed.affects(warningPlace = warningPlace) } }
                    .unseen(seenWarnings = seen)
            }
        }
    }.distinctUntilChanged()

private fun List<AggregatedFeed>.unseen(seenWarnings: List<SeenWarning>): List<AggregatedFeed> {
    val seenAtByWarningIdentifier: Map<String, Instant> = seenWarnings.associate { seenWarning: SeenWarning ->
        seenWarning.warningIdentifier to seenWarning.seenAt
    }
    return filterNot { aggregatedFeed: AggregatedFeed -> aggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier = seenAtByWarningIdentifier) }
}

private fun AggregatedFeed.isSeenSincePublished(seenAtByWarningIdentifier: Map<String, Instant>): Boolean {
    val seenAt: Instant = seenAtByWarningIdentifier[identifier] ?: return false
    return seenAt >= publishedAt
}